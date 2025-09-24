package com.example.csv_importer.dtos;

public record ImportStatusResponse(String importId, String status, int processed, int failed) {
}
