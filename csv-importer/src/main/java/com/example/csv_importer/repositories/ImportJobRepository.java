package com.example.csv_importer.repositories;


import com.example.csv_importer.models.ImportJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class ImportJobRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableJobs;

    public ImportJobRepository(DynamoDbClient dynamoDbClient, @Value("${app.aws.dynamodb.tableJobs}")  String tableJobs) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableJobs = tableJobs;
    }

    public void put(ImportJob importJob) {
        dynamoDbClient.putItem(r -> r.tableName(tableJobs).item(Map.of(
                "importId", AttributeValue.builder().s(importJob.getImportId()).build(),
                "s3Key", AttributeValue.builder().s(importJob.getS3Key()).build(),
                "status", AttributeValue.builder().s(importJob.getStatus()).build(),
                "processed", AttributeValue.builder().n(Integer.toString(importJob.getProcessed())).build(),
                "failed", AttributeValue.builder().n(Integer.toString(importJob.getFailed())).build(),
                "createdAt", AttributeValue.builder().s(importJob.getCreatedAt().toString()).build()
        )));
    }

    public void updateStatus(String importId, String status) {
        dynamoDbClient.updateItem(r -> r.tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(status).build()))
                .updateExpression("SET #s = :s")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(":s", AttributeValue.builder().s(status).build())));
    }

    public void incrementCounts(String importId, int processedDelta, int failedDelta) {
        dynamoDbClient.updateItem(r -> r.tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(importId).build()))
                .updateExpression("ADD processed :p, failed :f")
                .expressionAttributeValues(Map.of(
                        ":p", AttributeValue.builder().n(Integer.toString(processedDelta)).build(),
                        ":f", AttributeValue.builder().n(Integer.toString(failedDelta)).build()
                )));
    }

    public Optional<ImportJob> get(String importId) {
        GetItemResponse response = dynamoDbClient.getItem(r -> r.tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(importId).build())));
        if (!response.hasItem() || response.item().isEmpty()) return Optional.empty();
        var item = response.item();
        ImportJob importJob = new ImportJob();
        importJob.setImportId(item.get("importId").s());
        importJob.setS3Key(item.get("s3Key").s());
        importJob.setStatus(item.get("status").s());
        importJob.setProcessed(Integer.parseInt(item.get("processed").n()));
        importJob.setFailed(Integer.parseInt(item.get("failed").n()));
        importJob.setCreatedAt(Instant.parse(item.get("createdAt").s()));
        return Optional.of(importJob);
    }

}
