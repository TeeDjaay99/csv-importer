package com.example.csv_importer;

import com.example.csv_importer.utils.CsvParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CsvParserTest {

    @Test
    void parsesCsvWithHeaderAndRows() throws Exception{
        String csv = "id, name, value\n1, Alice, 100\n2, Bob, 200\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.getBytes());

        List<Map<String, String>> rows = CsvParser.parse(inputStream);

        assertEquals(2, rows.size());
        assertEquals("Alice", rows.get(0).get("name"));
        assertEquals("200", rows.get(1).get("value"));
    }
}
