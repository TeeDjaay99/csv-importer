package com.example.csv_importer;

import com.example.csv_importer.dtos.PresignResponse;
import com.example.csv_importer.services.PresignService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class PreSignServiceTest {

    @Test
    void createCsvPresignReturnsResponse() {
        S3Presigner presignerMock = mock(S3Presigner.class, RETURNS_DEEP_STUBS);

        PresignService serviceMock = new PresignService(presignerMock, "bucket-name", 300);

        assertDoesNotThrow(() -> {
            PresignResponse response = serviceMock.createCsvPresign();
            assertNotNull(response);
            assertNotNull(response.uploadUrl());
            assertTrue(response.objectKey().endsWith(".csv"));
        });
    }
}
