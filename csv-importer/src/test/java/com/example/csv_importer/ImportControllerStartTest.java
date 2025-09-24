package com.example.csv_importer;

import com.example.csv_importer.controllers.ImportController;
import com.example.csv_importer.dtos.ImportStatusResponse;
import com.example.csv_importer.dtos.StartImportRequest;
import com.example.csv_importer.models.ImportJob;
import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.services.CsvImportService;
import com.example.csv_importer.services.ImportJobService;
import com.example.csv_importer.services.PresignService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImportControllerStartTest {

    @Test
    void startImportRunsAndReturnsStatus() throws Exception {
        ImportJobService importJobServiceMock = mock(ImportJobService.class);
        ImportJobRepository repositoryMock = mock(ImportJobRepository.class);
        CsvImportService csvImportServiceMock = mock(CsvImportService.class);

        ImportJob importJob = new ImportJob();
        importJob.setImportId("imp_999");
        importJob.setS3Key("imports/file.csv");
        importJob.setStatus("PENDING");
        importJob.setCreatedAt(Instant.now());

        when(importJobServiceMock.createPending("imports/file.csv")).thenReturn(importJob);

        ImportJob finishedImportJob = new ImportJob();
        finishedImportJob.setImportId("imp_999");
        finishedImportJob.setStatus("DONE");
        finishedImportJob.setProcessed(3);
        finishedImportJob.setFailed(0);
        finishedImportJob.setCreatedAt(importJob.getCreatedAt());

        when(repositoryMock.get("imp_999")).thenReturn(Optional.of(finishedImportJob));


        ImportController importController = new ImportController(
                mock(PresignService.class),
                importJobServiceMock,
                repositoryMock,
                csvImportServiceMock
        );

        ResponseEntity<ImportStatusResponse> response = importController.startImport(new StartImportRequest("imports/file.csv"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("DONE", response.getBody().status());
        assertEquals(3, response.getBody().processed());
    }
}
