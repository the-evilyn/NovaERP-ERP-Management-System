package com.novaerp.backend.common.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvUtilsTest {

    @Test
    @DisplayName("escape() handles null, empty, and regular strings")
    void testEscape_Basics() {
        assertThat(CsvUtils.escape(null)).isEmpty();
        assertThat(CsvUtils.escape("")).isEmpty();
        assertThat(CsvUtils.escape("hello")).isEqualTo("hello");
    }

    @Test
    @DisplayName("escape() handles quotes, commas, and newlines")
    void testEscape_SpecialChars() {
        assertThat(CsvUtils.escape("hello, world")).isEqualTo("\"hello, world\"");
        assertThat(CsvUtils.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvUtils.escape("line1\rline2")).isEqualTo("\"line1\rline2\"");
        assertThat(CsvUtils.escape("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"");
    }

    @Test
    @DisplayName("escape() preserves French and accented characters")
    void testEscape_AccentedCharacters() {
        String input = "Pièces détachées, Vérins & câbles intégrés (qualité supérieure)";
        String escaped = CsvUtils.escape(input);
        assertThat(escaped).isEqualTo("\"" + input + "\"");
    }

    @Test
    @DisplayName("row() creates comma-separated line with trailing newline")
    void testRow() {
        String line = CsvUtils.row("ref01", "Désignation avec, virgule", 123, null, true);
        assertThat(line).isEqualTo("ref01,\"Désignation avec, virgule\",123,,true\n");
    }

    @Test
    @DisplayName("toCsvBytes() prepends UTF-8 BOM correctly")
    void testToCsvBytes() {
        String content = "name,email\nAmine,amine@test.ma\n";
        byte[] bytes = CsvUtils.toCsvBytes(content);

        // UTF-8 BOM is 0xEF, 0xBB, 0xBF
        assertThat(bytes).hasSize(content.getBytes(StandardCharsets.UTF_8).length + 3);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String decoded = new String(bytes, StandardCharsets.UTF_8);
        assertThat(decoded).startsWith(CsvUtils.UTF8_BOM);
        assertThat(decoded).contains("Amine,amine@test.ma");
    }

    @Test
    @DisplayName("parse() handles BOM, quoted values, escaped quotes, and French text")
    void testParse() {
        String csv = CsvUtils.UTF8_BOM +
                "name,description,price\n" +
                "\"Écran 27\"\", 4K\",\"Moniteur haute fidélité\navec garantie\",3500.50\n" +
                "Clavier,Standard,150.00\n";

        List<String[]> rows = CsvUtils.parse(csv);
        assertThat(rows).hasSize(3);

        String[] header = rows.get(0);
        assertThat(header).containsExactly("name", "description", "price");

        String[] row1 = rows.get(1);
        assertThat(row1[0]).isEqualTo("Écran 27\", 4K");
        assertThat(row1[1]).isEqualTo("Moniteur haute fidélité\navec garantie");
        assertThat(row1[2]).isEqualTo("3500.50");

        String[] row2 = rows.get(2);
        assertThat(row2[0]).isEqualTo("Clavier");
        assertThat(row2[1]).isEqualTo("Standard");
        assertThat(row2[2]).isEqualTo("150.00");
    }

    @Test
    @DisplayName("field() safely accesses indices")
    void testField() {
        String[] row = new String[]{" A ", "B"};
        assertThat(CsvUtils.field(row, 0)).isEqualTo("A");
        assertThat(CsvUtils.field(row, 1)).isEqualTo("B");
        assertThat(CsvUtils.field(row, 2)).isEmpty();
    }
}
