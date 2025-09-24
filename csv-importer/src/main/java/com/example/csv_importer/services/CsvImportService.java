package com.example.csv_importer.services;


import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.utils.CsvParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CsvImportService {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final ImportJobRepository importJobRepository;
    private final String bucket;
    private final String tableData;

    public CsvImportService(S3Client s3Client, DynamoDbClient dynamoDbClient, ImportJobRepository importJobRepository,
                            @Value("${app.aws.s3.bucket}") String bucket, @Value("${app.aws.s3.key}") String tableData) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.importJobRepository = importJobRepository;
        this.bucket = bucket;
        this.tableData = tableData;
    }

    public void runImport(String importId, String s3Key) throws Exception {
        importJobRepository.updateStatus(importId, "IN_PROGRESS");

        int processed = 0, failed = 0;

        try (InputStream inputStream = s3Client.getObject(request -> request.bucket(bucket).key(s3Key))) {
            List<Map<String, String>> parsedRows = CsvParser.parse(inputStream);
            for (Map<String, String> rowMap : parsedRows) {
                try {
                    Map<String, AttributeValue> dynamoItem = new HashMap<>();

                    String id = rowMap.getOrDefault("id", UUID.randomUUID().toString());
                    dynamoItem.put("id", AttributeValue.builder().s(id).build());
                    for (Map.Entry<String, String> entry : rowMap.entrySet()) {
                        if (!entry.getKey().equals("id")) {
                            dynamoItem.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
                        }
                    }

                    dynamoDbClient.putItem(PutItemRequest.builder()
                            .tableName(tableData)
                            .item(dynamoItem)
                            .build());

                    processed++;
                } catch (Exception rowException) {
                    failed++;
                }
            }
        }
        importJobRepository.updateStatus(importId, failed == 0 ? "DONE" : "FAILED");
    }
}


