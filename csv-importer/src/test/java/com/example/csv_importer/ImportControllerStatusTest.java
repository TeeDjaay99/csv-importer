package com.example.csv_importer;

import com.example.csv_importer.controllers.ImportController;
import com.example.csv_importer.dtos.ImportStatusResponse;
import com.example.csv_importer.models.ImportJob;
import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.services.CsvImportService;
import com.example.csv_importer.services.ImportJobService;
import com.example.csv_importer.services.PresignService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportControllerStatusTest {

    @Test
    void getStatusReturnsJobIfExists() {
        ImportJobRepository repositoryMock = mock(ImportJobRepository.class);

        ImportJob importJob = new ImportJob();
        importJob.setImportId("imp_123");
        importJob.setStatus("DONE");
        importJob.setProcessed(2);
        importJob.setFailed(0);
        importJob.setCreatedAt(Instant.now());

        when(repositoryMock.get("imp_123")).thenReturn(Optional.of(importJob));

        ImportController importController = new ImportController(
                mock(PresignService.class),
                mock(ImportJobService.class),
                repositoryMock,
                mock(CsvImportService.class)
        );

        ResponseEntity<ImportStatusResponse> response = importController.getImportStatus("imp_123");

        assertEquals(200, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("DONE", response.getBody().status());
        assertEquals(2,response.getBody().processed());
    }
}
