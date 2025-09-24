package com.example.csv_importer;

import com.example.csv_importer.models.ImportJob;
import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.services.ImportJobService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ImportJobServiceTest {

    @Test
    void createPendingJobHasCorrectDefaults() {
        ImportJobRepository repositoryMock = mock(ImportJobRepository.class);
        ImportJobService service = new ImportJobService(repositoryMock);

        ImportJob importJob = service.createPending("imports/test.csv");

        assertNotNull(importJob.getImportId());
        assertEquals("PENDING", importJob.getStatus());
        assertEquals("imports/test.csv", importJob.getS3Key());

        verify(repositoryMock, times(1)).put(importJob);
    }
}
