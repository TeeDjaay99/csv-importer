package com.example.csv_importer.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class CsvParser {
    public static List<Map<String, String>> parse(InputStream inputStream) throws Exception {

        // UTF-8 används för att hantera svenska och andra specialtecken korrekt
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Läs första raden som innehåller kolumnnamnen
            String headerLine = bufferedReader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Empty CSV");

            String[] columns = headerLine.split(",");

            // Lista som ska innehålla alla parsade rader
            List<Map<String, String>> rowsList = new ArrayList<>();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] values = line.split(",", -1);

                Map<String, String> rowMap = new LinkedHashMap<>();

                // Matcha varje värde med motsvarande kolumnnamn
                // Loopar bara så långt som det finns både kolumner och värden
                for (int i = 0; i < columns.length && i < values.length; i++) {
                    rowMap.put(columns[i].trim(), values[i].trim());
                }
                rowsList.add(rowMap); // Lägg till den parsade raden i resultatlistan
            }
            return rowsList;

        }
    }
}
