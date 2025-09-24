package com.example.csv_importer.controllers;

import com.example.csv_importer.dtos.ImportStatusResponse;
import com.example.csv_importer.dtos.PresignResponse;
import com.example.csv_importer.dtos.StartImportRequest;
import com.example.csv_importer.models.ImportJob;
import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.services.CsvImportService;
import com.example.csv_importer.services.ImportJobService;
import com.example.csv_importer.services.PresignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final PresignService presignService;
    private final ImportJobService importJobService;
    private final ImportJobRepository importJobRepository;
    private final CsvImportService csvImportService;

    public ImportController(PresignService presignService, ImportJobService importJobService, ImportJobRepository importJobRepository, CsvImportService csvImportService) {
        this.presignService = presignService;
        this.importJobService = importJobService;
        this.importJobRepository = importJobRepository;
        this.csvImportService = csvImportService;
    }

    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> createPresignUrl() {
        PresignResponse presignResponse = presignService.createCsvPresign();
        return ResponseEntity.ok(presignResponse);
    }

    @PostMapping("/start")
    public ResponseEntity<ImportStatusResponse> startImport(@Valid @RequestBody StartImportRequest startImportRequest) throws Exception {
        ImportJob newImportJob = importJobService.createPending(startImportRequest.s3Key());

        csvImportService.runImport(newImportJob.getImportId(), startImportRequest.s3Key());

        ImportJob finalImportJobState = importJobRepository
                .get(newImportJob.getImportId())
                .orElse(newImportJob);

        ImportStatusResponse response = new ImportStatusResponse(
                finalImportJobState.getImportId(),
                finalImportJobState.getStatus(),
                finalImportJobState.getProcessed(),
                finalImportJobState.getFailed()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{importId}/status")
    public ResponseEntity<ImportStatusResponse> getImportStatus(@PathVariable String importId) {
        Optional<ImportJob> maybeImportJob = importJobRepository.get(importId);

        if (maybeImportJob.isPresent()) {
            ImportJob importJob = maybeImportJob.get();
            ImportStatusResponse response = new ImportStatusResponse(
                    importJob.getImportId(),
                    importJob.getStatus(),
                    importJob.getProcessed(),
                    importJob.getFailed()
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}
