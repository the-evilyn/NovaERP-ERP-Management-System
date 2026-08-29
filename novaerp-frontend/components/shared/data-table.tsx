"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowRight01Icon,
  Search01Icon,
} from "@hugeicons/core-free-icons";
import type React from "react";
import { Fragment, type ReactNode, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Card,
  CardContent,
  CardFrame,
  CardHeader,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export interface TableColumn<T> {
  key: string;
  label: ReactNode;
  render?: (item: T) => ReactNode;
  className?: string;
}

export interface DataTableProps<T> {
  data: T[];
  columns: TableColumn<T>[];
  searchKeys: string[];
  searchPlaceholder?: string;
  onRowClick?: (item: T) => void;
  actions?: (item: T) => ReactNode;
  emptyMessage?: string;
  className?: string;
  customHeader?: ReactNode;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  rowKey?: (item: T) => string | number;
  selectable?: boolean;
  selectedIds?: (string | number)[];
  onSelectionChange?: (ids: (string | number)[]) => void;
  expandable?: boolean;
  renderExpandedRow?: (item: T) => ReactNode;
  onRowExpand?: (item: T) => void;
  footer?: ReactNode;
}

export function DataTable<T extends object>({
  data,
  columns,
  searchKeys,
  searchPlaceholder = "Search...",
  onRowClick,
  actions,
  emptyMessage,
  className = "",
  customHeader,
  searchValue,
  onSearchChange,
  rowKey,
  selectable = false,
  selectedIds = [],
  onSelectionChange,
  expandable = false,
  renderExpandedRow,
  onRowExpand,
  footer,
}: DataTableProps<T>): React.ReactElement {
  const [internalSearch, setInternalSearch] = useState("");
  const searchTerm = searchValue ?? internalSearch;
  const [expandedRows, setExpandedRows] = useState<Set<string | number>>(
    new Set(),
  );

  const getNestedValue = (item: T, path: string): unknown => {
    return path.split(".").reduce((curr: unknown, key: string) => {
      if (curr && typeof curr === "object" && key in curr) {
        return (curr as Record<string, unknown>)[key];
      }
      return undefined;
    }, item as unknown);
  };

  const getRowKey = (item: T, index: number): string | number => {
    if (rowKey) return rowKey(item);
    if ("id" in item) return (item as { id: string | number }).id;
    return `row-${index}`;
  };

  const filteredData = useMemo(() => {
    if (!searchTerm) return data;
    return data.filter((item) =>
      searchKeys.some((key) => {
        const value = getNestedValue(item, key);
        return (
          value != null &&
          value.toString().toLowerCase().includes(searchTerm.toLowerCase())
        );
      }),
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, searchTerm, searchKeys]);

  const renderCell = (item: T, column: TableColumn<T>): ReactNode => {
    if (column.render) return column.render(item);
    const value = getNestedValue(item, column.key);
    if (value === null || value === undefined) return "—";
    return String(value);
  };

  const allSelected =
    filteredData.length > 0 && selectedIds.length === filteredData.length;
  const someSelected =
    selectedIds.length > 0 && selectedIds.length < filteredData.length;

  const toggleAll = (checked: boolean) => {
    onSelectionChange?.(
      checked ? filteredData.map((item, i) => getRowKey(item, i)) : [],
    );
  };

  const toggleOne = (id: string | number, checked: boolean) => {
    if (!onSelectionChange) return;
    onSelectionChange(
      checked
        ? [...selectedIds, id]
        : selectedIds.filter((selected) => selected !== id),
    );
  };

  const toggleRow = (item: T, key: string | number) => {
    const next = new Set(expandedRows);
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
      onRowExpand?.(item);
    }
    setExpandedRows(next);
  };

  const colSpan =
    columns.length +
    (actions ? 1 : 0) +
    (selectable ? 1 : 0) +
    (expandable ? 1 : 0);

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative min-w-[200px] max-w-xl flex-1">
            <HugeiconsIcon
              icon={Search01Icon}
              strokeWidth={2}
              className="-translate-y-1/2 absolute top-1/2 left-2.5 size-4 text-muted-foreground"
            />
            <Input
              placeholder={searchPlaceholder}
              value={searchTerm}
              onChange={(e) =>
                onSearchChange
                  ? onSearchChange(e.target.value)
                  : setInternalSearch(e.target.value)
              }
              className="ps-8"
            />
          </div>
          {customHeader && (
            <div className="flex items-center gap-2">{customHeader}</div>
          )}
        </div>
      </CardHeader>

      <CardContent>
        <CardFrame>
          <Table variant="card">
            <TableHeader>
              <TableRow className="bg-muted/40 hover:bg-muted/40">
                {expandable && <TableHead className="w-px first:ps-4" />}
                {selectable && (
                  <TableHead className="w-px first:ps-4">
                    <Checkbox
                      checked={allSelected}
                      indeterminate={someSelected}
                      onCheckedChange={(checked) => toggleAll(checked === true)}
                      aria-label="Tout sélectionner"
                    />
                  </TableHead>
                )}
                {columns.map((column) => (
                  <TableHead
                    key={column.key}
                    className={cn("first:ps-4 last:pe-4", column.className)}
                  >
                    {column.label}
                  </TableHead>
                ))}
                {actions && (
                  <TableHead className="pe-4 text-right">Actions</TableHead>
                )}
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredData.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={colSpan}
                    className="h-24 px-4 text-center text-muted-foreground"
                  >
                    {emptyMessage ??
                      (searchTerm
                        ? "No items match your search."
                        : "No items found.")}
                  </TableCell>
                </TableRow>
              ) : (
                filteredData.map((item, index) => {
                  const key = getRowKey(item, index);
                  const isSelected = selectedIds.includes(key);
                  const isExpanded = expandedRows.has(key);

                  return (
                    <Fragment key={key}>
                      <TableRow
                        data-state={isSelected ? "selected" : undefined}
                        className={onRowClick ? "cursor-pointer" : undefined}
                        onClick={() => onRowClick?.(item)}
                      >
                        {expandable && (
                          <TableCell
                            className="first:ps-4"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => toggleRow(item, key)}
                            >
                              <HugeiconsIcon
                                icon={isExpanded ? ArrowDown01Icon : ArrowRight01Icon}
                                strokeWidth={2}
                              />
                            </Button>
                          </TableCell>
                        )}
                        {selectable && (
                          <TableCell
                            className="first:ps-4"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Checkbox
                              checked={isSelected}
                              onCheckedChange={(checked) =>
                                toggleOne(key, checked === true)
                              }
                              aria-label="Sélectionner la ligne"
                            />
                          </TableCell>
                        )}
                        {columns.map((column) => (
                          <TableCell
                            key={column.key}
                            className={cn(
                              "first:ps-4 last:pe-4",
                              column.className,
                            )}
                          >
                            {renderCell(item, column)}
                          </TableCell>
                        ))}
                        {actions && (
                          <TableCell
                            className="pe-4 text-right"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <div className="flex items-center justify-end gap-2">
                              {actions(item)}
                            </div>
                          </TableCell>
                        )}
                      </TableRow>

                      {expandable && isExpanded && renderExpandedRow && (
                        <TableRow>
                          <TableCell colSpan={colSpan} className="bg-muted/20 p-0">
                            {renderExpandedRow(item)}
                          </TableCell>
                        </TableRow>
                      )}
                    </Fragment>
                  );
                })
              )}
            </TableBody>
            {footer && (
              <TableFooter>
                <TableRow>
                  <TableCell
                    colSpan={colSpan}
                    className="bg-transparent px-4"
                  >
                    {footer}
                  </TableCell>
                </TableRow>
              </TableFooter>
            )}
          </Table>
        </CardFrame>
      </CardContent>
    </Card>
  );
}
