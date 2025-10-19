package com.example.csv_importer.repositories;


import com.example.csv_importer.models.ImportJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.time.Instant;
import java.util.HashMap;
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

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("importId", AttributeValue.builder().s(importJob.getImportId()).build());
        item.put("s3Key", AttributeValue.builder().s(importJob.getS3Key()).build());
        item.put("status", AttributeValue.builder().s(importJob.getStatus()).build());
        item.put("processed", AttributeValue.builder().n(Integer.toString(importJob.getProcessed())).build());
        item.put("failed", AttributeValue.builder().n(Integer.toString(importJob.getFailed())).build());

        if (importJob.getCreatedAt() != null) {
            item.put("createdAt", AttributeValue.builder().s(importJob.getCreatedAt().toString()).build());
        }

        dynamoDbClient.putItem(r -> r.tableName(tableJobs).item(item));
    }

    public void updateStatus(String importId, String status) {
        dynamoDbClient.updateItem(r -> r.tableName(tableJobs)

                .key(Map.of("importId", AttributeValue.builder().s(importId).build()))
                .updateExpression("SET #s = :s")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(":s", AttributeValue.builder().s(status).build())));
    }

    public void overwriteTotalsAndStatus(String importId, int processed, int failed, String status) {
        // Hämta befintlig post (för att behålla fält som s3Key/createdAt)
        var getRes = dynamoDbClient.getItem(r -> r
                .tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(importId).build()))
        );

        Map<String, AttributeValue> item = new HashMap<>();
        // Nyckel är obligatorisk
        item.put("importId", AttributeValue.builder().s(importId).build());

        if (getRes.hasItem()) {
            item.putAll(getRes.item()); // behåll övriga fält om de fanns
        }

        // Sätt/ersätt dessa fält
        item.put("status", AttributeValue.builder().s(status).build());
        item.put("processed", AttributeValue.builder().n(Integer.toString(processed)).build());
        item.put("failed", AttributeValue.builder().n(Integer.toString(failed)).build());

        dynamoDbClient.putItem(r -> r.tableName(tableJobs).item(item));
    }



    public void incrementCounts(String importId, int processedDelta, int failedDelta) {
        dynamoDbClient.updateItem(r -> r.tableName(tableJobs)
                .tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(importId).build()))
                .updateExpression("ADD #p :p, #f :f")
                .expressionAttributeNames(Map.of(
                        "#p", "processed",
                        "#f", "failed"
                ))
                .expressionAttributeValues(Map.of(
                        ":p", AttributeValue.builder().n(Integer.toString(processedDelta)).build(),
                        ":f", AttributeValue.builder().n(Integer.toString(failedDelta)).build()
                )));
    }

    // Hämta jobb för status-endpointen
    public Optional<ImportJob> get(String importId) {
        var res = dynamoDbClient.getItem(r -> r
                .tableName(tableJobs)
                .key(Map.of("importId", AttributeValue.builder().s(importId).build()))
        );

        if (!res.hasItem() || res.item() == null || res.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToModel(res.item()));
    }

    private ImportJob mapToModel(Map<String, AttributeValue> item) {
        ImportJob job = new ImportJob();
        job.setImportId(s(item.get("importId")));
        job.setS3Key(s(item.get("s3Key")));
        job.setStatus(s(item.get("status")));
        job.setProcessed(n(item.get("processed")));
        job.setFailed(n(item.get("failed")));
        String createdAtStr = s(item.get("createdAt")); // spara som sträng om du använder String i modellen
        if (createdAtStr != null){
            job.setCreatedAt(Instant.parse(createdAtStr));
        }
        return job;
    }

    private static String s(AttributeValue v) {
        return v == null ? null : v.s();
    }

    private static int n(AttributeValue v) {
        if (v == null || v.n() == null) return 0;
        try {
            return Integer.parseInt(v.n());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
