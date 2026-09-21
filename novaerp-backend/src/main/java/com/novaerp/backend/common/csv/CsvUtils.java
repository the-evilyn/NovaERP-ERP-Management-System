package com.novaerp.backend.common.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC4180-ish CSV reader/writer. No external dependency is pulled in
 * just for this - the data we import/export (article, category, supplier
 * rows) is simple enough that a small hand-rolled parser is sufficient.
 */
public final class CsvUtils {

    public static final String UTF8_BOM = "\uFEFF";

    private CsvUtils() {
    }

    public static byte[] toCsvBytes(String content) {
        if (content == null) {
            return new byte[0];
        }
        String withBom = content.startsWith(UTF8_BOM) ? content : UTF8_BOM + content;
        return withBom.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    public static String row(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i] == null ? "" : String.valueOf(fields[i])));
        }
        sb.append('\n');
        return sb.toString();
    }

    public static List<String[]> parse(String content) {
        List<String[]> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        // strip a UTF-8 BOM if present (common when exporting from Excel)
        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }

        int i = 0;
        int n = content.length();
        while (i < n) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(ch);
                    i++;
                }
            } else if (ch == '"') {
                inQuotes = true;
                i++;
            } else if (ch == ',') {
                current.add(field.toString());
                field.setLength(0);
                i++;
            } else if (ch == '\r') {
                i++;
            } else if (ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                rows.add(current.toArray(new String[0]));
                current = new ArrayList<>();
                i++;
            } else {
                field.append(ch);
                i++;
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current.toArray(new String[0]));
        }

        rows.removeIf(r -> r.length == 1 && r[0].isBlank());
        return rows;
    }

    public static String field(String[] row, int index) {
        return index < row.length ? row[index].trim() : "";
    }
}
