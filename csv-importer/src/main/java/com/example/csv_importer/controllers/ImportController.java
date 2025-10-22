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
    private final PresignService presignService;                // Gör presigned URL till S3
    private final ImportJobService importJobService;            // Skapar/hanterar jobb
    private final ImportJobRepository importJobRepository;
    private final CsvImportService csvImportService;            // Kör importen

    public ImportController(PresignService presignService, ImportJobService importJobService,
                            ImportJobRepository importJobRepository, CsvImportService csvImportService) {
        this.presignService = presignService;
        this.importJobService = importJobService;
        this.importJobRepository = importJobRepository;
        this.csvImportService = csvImportService;
    }
    // Skapa presign URL, klienten använder för att ladda upp csv till S3
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> createPresignUrl() {
        PresignResponse presignResponse = presignService.createCsvPresign();
        return ResponseEntity.ok(presignResponse);
    }
    // Start import, får s3Key fil och triggar import
    @PostMapping("/start")
    public ResponseEntity<ImportStatusResponse> startImport(@Valid @RequestBody StartImportRequest startImportRequest) throws Exception {
        // Skapa ett nytt import-jobb (id + status i DynamoDB ImportJobs)
        ImportJob newImportJob = importJobService.createPending(startImportRequest.s3Key());

        // Kör själva importen (läs S3, spara till DDB, skicka SNS)
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

    // Hämta status för importjobb (Import-id)
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
