#!/usr/bin/env python3
"""
Import a legacy stock export (xlsx or csv) into novaerp-backend.

Source columns expected on the first sheet / as CSV header:
  Reference article | Designation | Prix d'achat | Prix de vente |
  Stock reel | Famille | Stock calcule | Fournisseur principal
  (any further trailing columns, e.g. "Unnamed: 8" or "T", are ignored)

Cleanup rules applied (see conversation for rationale):
  - Duplicate references are merged: stock quantities are summed, the first
    non-empty value wins for every other field.
  - Missing purchase/sale price defaults to 0 and is flagged in the
    exceptions report.
  - Category ("Famille") names are normalized to upper case so
    "electrique" and "ELECTRIQUE" collapse into one category.
  - Negative "Stock reel" is imported as 0; the original negative value is
    written to the exceptions report instead of being stored anywhere.
  - "Stock calcule" is not imported - there is no matching concept in the
    backend (only one stockQuantity field exists).
  - No purchase-TTC / tax data exists in the source, so unitCostTtc and the
    supplier price's priceTtc are set equal to the HT price (taxRate 0).
    Adjust manually afterwards if real tax rates are known.
  - The "Fournisseur principal" column only contains a code, not a name.
    A Supplier is created using that code as its name if it doesn't exist
    yet, and is linked as the article's primary supplier price.

The script is idempotent: articles whose reference already exists in the
backend are left untouched (no duplicate stock movements or supplier
prices are created for them) on a re-run.

Usage:
  python3 scripts/import_legacy_stock.py --file path/to/all.xlsx \
      --base-url http://localhost:8080 --email admin@example.com
  python3 scripts/import_legacy_stock.py --file path/to/all.csv \
      --base-url http://localhost:8080 --email admin@example.com

  Add --dry-run to only produce the cleaned CSV + exceptions report
  without calling the API.
"""

import argparse
import csv
import getpass
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from collections import OrderedDict
from xml.etree import ElementTree as ET

NS = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
COL_REF, COL_DESIG, COL_BUY, COL_SELL, COL_STOCK, COL_FAMILY, COL_CALC, COL_SUPPLIER = (
    "A", "B", "C", "D", "E", "F", "G", "H",
)
CSV_COLUMNS = [COL_REF, COL_DESIG, COL_BUY, COL_SELL, COL_STOCK, COL_FAMILY, COL_CALC, COL_SUPPLIER]


# ---------------------------------------------------------------------------
# xlsx parsing (stdlib only, no openpyxl dependency available in this env)
# ---------------------------------------------------------------------------

def col_row(cell_ref):
    m = re.match(r"([A-Z]+)(\d+)", cell_ref)
    return m.group(1), int(m.group(2))


def load_sheet(zf, sheet_path, shared_strings):
    tree = ET.parse(zf.open(sheet_path))
    root = tree.getroot()
    rows = OrderedDict()
    for row in root.findall(".//a:row", NS):
        r = int(row.get("r"))
        values = {}
        for c in row.findall("a:c", NS):
            col, _ = col_row(c.get("r"))
            t = c.get("t")
            v = c.find("a:v", NS)
            val = v.text if v is not None else None
            if t == "s" and val is not None:
                val = shared_strings[int(val)]
            if val is not None:
                values[col] = val
        if values:
            rows[r] = values
    return rows


def load_shared_strings(zf):
    if "xl/sharedStrings.xml" not in zf.namelist():
        return []
    tree = ET.parse(zf.open("xl/sharedStrings.xml"))
    root = tree.getroot()
    out = []
    for si in root.findall("a:si", NS):
        texts = si.findall(".//a:t", NS)
        out.append("".join(t.text or "" for t in texts))
    return out


def parse_xlsx(path):
    with zipfile.ZipFile(path) as zf:
        shared = load_shared_strings(zf)
        rows = load_sheet(zf, "xl/worksheets/sheet1.xml", shared)
    return rows


def parse_csv(path):
    """Read a CSV export with the same 8 leading columns as the xlsx sheet
    (any extra trailing columns are ignored) and return it in the same
    {row_number: {col_letter: value}} shape parse_xlsx produces, so
    clean_rows() can stay format-agnostic."""
    rows = OrderedDict()
    with open(path, newline="", encoding="utf-8-sig") as f:
        reader = csv.reader(f)
        for r, raw_row in enumerate(reader, start=1):
            values = {}
            for col, cell in zip(CSV_COLUMNS, raw_row):
                cell = cell.strip()
                if cell != "":
                    values[col] = cell
            if values:
                rows[r] = values
    return rows


def to_decimal_str(raw):
    if raw is None:
        return None
    try:
        return str(float(raw))
    except ValueError:
        return None


# ---------------------------------------------------------------------------
# cleaning
# ---------------------------------------------------------------------------

def clean_rows(raw_rows):
    """Returns (cleaned: dict[reference -> record], exceptions: list[dict])"""
    cleaned = OrderedDict()
    exceptions = []

    for r, row in raw_rows.items():
        if r == 1:
            continue  # header
        reference = (row.get(COL_REF) or "").strip()
        if not reference:
            continue

        designation = (row.get(COL_DESIG) or "").strip()
        buy_raw = to_decimal_str(row.get(COL_BUY))
        sell_raw = to_decimal_str(row.get(COL_SELL))
        stock_raw = to_decimal_str(row.get(COL_STOCK))
        family_raw = (row.get(COL_FAMILY) or "").strip()
        supplier_raw = (row.get(COL_SUPPLIER) or "").strip()

        buy_price = float(buy_raw) if buy_raw is not None else None
        sell_price = float(sell_raw) if sell_raw is not None else None
        stock = float(stock_raw) if stock_raw is not None else 0.0
        family = family_raw.upper() if family_raw else None

        entry = cleaned.get(reference)
        if entry is None:
            entry = {
                "reference": reference,
                "designation": designation,
                "buyPrice": buy_price,
                "sellPrice": sell_price,
                "stock": 0.0,
                "family": family,
                "supplier": supplier_raw or None,
                "sourceRows": [],
            }
            cleaned[reference] = entry
        else:
            exceptions.append({
                "reference": reference, "issue": "duplicate_reference",
                "detail": f"row {r} merged into first occurrence (stock summed)",
            })
            if not entry["designation"]:
                entry["designation"] = designation
            if entry["buyPrice"] is None:
                entry["buyPrice"] = buy_price
            if entry["sellPrice"] is None:
                entry["sellPrice"] = sell_price
            if not entry["family"]:
                entry["family"] = family
            if not entry["supplier"]:
                entry["supplier"] = supplier_raw or None

        entry["stock"] += stock
        entry["sourceRows"].append(r)

    for reference, entry in cleaned.items():
        if entry["buyPrice"] is None:
            exceptions.append({
                "reference": reference, "issue": "missing_purchase_price",
                "detail": "defaulted to 0",
            })
            entry["buyPrice"] = 0.0
        if entry["sellPrice"] is None:
            exceptions.append({
                "reference": reference, "issue": "missing_sale_price",
                "detail": "defaulted to 0",
            })
            entry["sellPrice"] = 0.0
        if entry["stock"] < 0:
            exceptions.append({
                "reference": reference, "issue": "negative_stock",
                "detail": f"original value {entry['stock']}, imported as 0",
            })
            entry["stock"] = 0.0
        if not entry["family"]:
            exceptions.append({
                "reference": reference, "issue": "missing_category",
                "detail": "imported without a category",
            })
        if not entry["supplier"]:
            exceptions.append({
                "reference": reference, "issue": "missing_supplier",
                "detail": "imported without a primary supplier",
            })

    return cleaned, exceptions


def write_reports(cleaned, exceptions, out_dir):
    articles_path = f"{out_dir}/cleaned_articles.csv"
    exceptions_path = f"{out_dir}/import_exceptions.csv"

    with open(articles_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["reference", "designation", "buyPrice", "sellPrice", "stock", "family", "supplier"])
        for e in cleaned.values():
            w.writerow([e["reference"], e["designation"], e["buyPrice"], e["sellPrice"], e["stock"], e["family"], e["supplier"]])

    with open(exceptions_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["reference", "issue", "detail"])
        for exc in exceptions:
            w.writerow([exc["reference"], exc["issue"], exc["detail"]])

    return articles_path, exceptions_path


# ---------------------------------------------------------------------------
# db wipe (docker-compose.yml default container names)
# ---------------------------------------------------------------------------

WIPE_TABLES = (
    "article_supplier_prices", "articles", "categories", "stock_movements",
    "suppliers", "units", "password_reset_tokens", "users",
)


def wipe_database(postgres_container, backend_container, db_user, db_name, health_url, health_timeout=90):
    """Truncate every stock/user table and restart the backend so its
    seed-profile DataSeeder repopulates demo users/categories/articles.
    Requires the containers from docker-compose.yml to be running."""
    sql = f"TRUNCATE {', '.join(WIPE_TABLES)} RESTART IDENTITY CASCADE;"
    print(f"Wiping database ({postgres_container})...", file=sys.stderr)
    subprocess.run(
        ["docker", "exec", postgres_container, "psql", "-U", db_user, "-d", db_name, "-c", sql],
        check=True,
    )

    print(f"Restarting backend ({backend_container}) to reseed demo data...", file=sys.stderr)
    subprocess.run(["docker", "restart", backend_container], check=True)

    print("Waiting for backend to come back up...", file=sys.stderr)
    deadline = time.time() + health_timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(health_url, timeout=3) as resp:
                if resp.status == 200 and json.loads(resp.read()).get("status") == "UP":
                    print("Backend is up.", file=sys.stderr)
                    return
        except (urllib.error.URLError, TimeoutError, ConnectionError, json.JSONDecodeError):
            pass
        time.sleep(2)
    raise SystemExit(f"Backend did not become healthy within {health_timeout}s of restarting")


# ---------------------------------------------------------------------------
# API client
# ---------------------------------------------------------------------------

class ApiClient:
    def __init__(self, base_url, token=None):
        self.base_url = base_url.rstrip("/")
        self.token = token

    def _request(self, method, path, body=None):
        url = f"{self.base_url}{path}"
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        if self.token:
            req.add_header("Authorization", f"Bearer {self.token}")
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read()
                return resp.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                payload = json.loads(raw) if raw else None
            except json.JSONDecodeError:
                payload = raw.decode("utf-8", errors="replace")
            return e.code, payload

    def login(self, email, password):
        status, payload = self._request("POST", "/api/auth/login", {"email": email, "password": password})
        if status != 200:
            raise SystemExit(f"Login failed ({status}): {payload}")
        self.token = payload["token"]

    def list_all(self, path, size=500):
        page = 0
        items = []
        while True:
            status, payload = self._request("GET", f"{path}?page={page}&size={size}")
            if status != 200:
                raise SystemExit(f"GET {path} failed ({status}): {payload}")
            items.extend(payload["content"])
            if page + 1 >= payload["totalPages"]:
                break
            page += 1
        return items

    def post(self, path, body):
        return self._request("POST", path, body)


# ---------------------------------------------------------------------------
# import
# ---------------------------------------------------------------------------

def run_import(client, cleaned, log):
    categories = {c["name"].upper(): c["id"] for c in client.list_all("/api/stock/categories")}
    suppliers = {s["name"]: s["id"] for s in client.list_all("/api/stock/suppliers")}
    existing_articles = {a["reference"] for a in client.list_all("/api/stock/articles")}

    created_articles = skipped_existing = failed = 0

    for entry in cleaned.values():
        reference = entry["reference"]
        if reference in existing_articles:
            skipped_existing += 1
            continue

        category_id = None
        if entry["family"]:
            category_id = categories.get(entry["family"])
            if category_id is None:
                status, payload = client.post("/api/stock/categories", {"name": entry["family"]})
                if status == 201:
                    category_id = payload["id"]
                    categories[entry["family"]] = category_id
                else:
                    log(f"WARN category '{entry['family']}' create failed ({status}): {payload}")

        supplier_id = None
        if entry["supplier"]:
            supplier_id = suppliers.get(entry["supplier"])
            if supplier_id is None:
                status, payload = client.post("/api/stock/suppliers", {"name": entry["supplier"]})
                if status == 201:
                    supplier_id = payload["id"]
                    suppliers[entry["supplier"]] = supplier_id
                else:
                    log(f"WARN supplier '{entry['supplier']}' create failed ({status}): {payload}")

        buy_price = round(entry["buyPrice"], 4)
        article_body = {
            "reference": reference,
            "designation": entry["designation"] or reference,
            "categoryId": category_id,
            "unitId": None,
            "purchasePriceHt": buy_price,
            "unitCostTtc": buy_price,
            "salePriceHt": round(entry["sellPrice"], 4),
            "minStockQuantity": 0,
            "serialTracked": False,
        }
        status, payload = client.post("/api/stock/articles", article_body)
        if status != 201:
            log(f"ERROR article '{reference}' create failed ({status}): {payload}")
            failed += 1
            continue

        article_id = payload["id"]
        created_articles += 1
        existing_articles.add(reference)

        if entry["stock"] > 0:
            status, payload = client.post("/api/stock/movements", {
                "articleId": article_id,
                "type": "IN",
                "quantity": entry["stock"],
                "reference": "legacy-import",
                "note": "Initial stock from legacy import",
            })
            if status != 201:
                log(f"WARN initial stock for '{reference}' failed ({status}): {payload}")

        if supplier_id is not None:
            status, payload = client.post(f"/api/stock/articles/{article_id}/supplier-prices", {
                "supplierId": supplier_id,
                "primary": True,
                "priceHt": buy_price,
                "taxRate": 0,
                "priceTtc": buy_price,
            })
            if status != 201:
                log(f"WARN primary supplier price for '{reference}' failed ({status}): {payload}")

    return created_articles, skipped_existing, failed


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--file", required=True, help="Path to the legacy .xlsx or .csv export")
    parser.add_argument("--out-dir", default="scripts/output", help="Where to write cleaned_articles.csv / import_exceptions.csv")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--email", help="Backend login email (required unless --dry-run)")
    parser.add_argument("--password", help="Backend login password (omit to be prompted)")
    parser.add_argument("--dry-run", action="store_true", help="Only produce the cleaned CSV + exceptions report, no API calls")
    parser.add_argument("--wipe", action="store_true",
                         help="Truncate all stock/user tables and restart the backend (so DataSeeder reseeds "
                              "demo data) before importing. Destructive - requires the docker-compose stack "
                              "to be running. Uses the default container/db names from docker-compose.yml "
                              "unless overridden below.")
    parser.add_argument("--postgres-container", default="novaerp-postgres")
    parser.add_argument("--backend-container", default="novaerp-backend")
    parser.add_argument("--db-user", default="novaerp")
    parser.add_argument("--db-name", default="novaerp")
    args = parser.parse_args()

    if args.wipe:
        if args.dry_run:
            raise SystemExit("--wipe and --dry-run cannot be used together")
        confirm = input(
            f"This will PERMANENTLY DELETE all data in '{args.db_name}' (container "
            f"{args.postgres_container}) and reseed demo data. Type 'yes' to continue: "
        )
        if confirm.strip().lower() != "yes":
            raise SystemExit("Aborted.")
        wipe_database(
            args.postgres_container, args.backend_container, args.db_user, args.db_name,
            health_url=f"{args.base_url.rstrip('/')}/actuator/health",
        )

    import os
    os.makedirs(args.out_dir, exist_ok=True)

    raw_rows = parse_csv(args.file) if args.file.lower().endswith(".csv") else parse_xlsx(args.file)
    cleaned, exceptions = clean_rows(raw_rows)
    articles_path, exceptions_path = write_reports(cleaned, exceptions, args.out_dir)

    print(f"Parsed {len(cleaned)} unique articles, {len(exceptions)} flagged issues.")
    print(f"Cleaned data:  {articles_path}")
    print(f"Exceptions:    {exceptions_path}")

    if args.dry_run:
        print("Dry run: no API calls made.")
        return

    if not args.email:
        raise SystemExit("--email is required when not using --dry-run")
    password = args.password or getpass.getpass("Backend password: ")

    client = ApiClient(args.base_url)
    client.login(args.email, password)

    created, skipped, failed = run_import(client, cleaned, log=lambda m: print(m, file=sys.stderr))
    print(f"Created: {created}, skipped (already existed): {skipped}, failed: {failed}")


if __name__ == "__main__":
    main()
