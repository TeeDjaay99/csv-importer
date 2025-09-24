package com.example.csv_importer.services;


import com.example.csv_importer.models.ImportJob;
import com.example.csv_importer.repositories.ImportJobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ImportJobService {

    private final ImportJobRepository importJobRepository;

    public ImportJobService(ImportJobRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    public ImportJob createPending(String s3Key) {
        ImportJob importJob = new ImportJob();
        importJob.setImportId("imp_" + UUID.randomUUID());
        importJob.setS3Key(s3Key);
        importJob.setStatus("PENDING");
        importJob.setProcessed(0);
        importJob.setFailed(0);
        importJob.setCreatedAt(Instant.now());
        importJobRepository.put(importJob);
        return importJob;
    }
}
