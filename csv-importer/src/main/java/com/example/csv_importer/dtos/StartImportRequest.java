package com.example.csv_importer.dtos;

import jakarta.validation.constraints.NotBlank;

public record StartImportRequest(@NotBlank String s3Key) {
}
