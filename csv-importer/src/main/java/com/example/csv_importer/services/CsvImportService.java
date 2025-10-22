package com.example.csv_importer.services;


import com.example.csv_importer.repositories.ImportJobRepository;
import com.example.csv_importer.utils.CsvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 *  Servicen hämtar och gör själva "Importjobbet"
 *  Hämtar CSV-filen i S3
 *  Tolkar raderna (id, name, age)
 *  Skriver varje rad som objekt i DynamoDB
 *  Håller koll på antalet lyckade/misslyckade rader
 *  Skickar start och slut notiser via SNS
 */

@Service
public class CsvImportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final ImportJobRepository importJobRepository;
    private final NotificationService notificationService;

    private final String bucket;      // S3 bucketen där filerna ligger
    private final String tableData;  // DynamoDB tabellen där raderna sparas

    public CsvImportService(S3Client s3Client, DynamoDbClient dynamoDbClient, ImportJobRepository importJobRepository,
                            NotificationService notificationService,
                            @Value("${app.aws.s3.bucket:csv-importer-bucket-tony}") String bucket,
                            @Value("${app.aws.dynamodb.tableData:DataItems}") String tableData
    ) {
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
        this.importJobRepository = importJobRepository;
        this.notificationService = notificationService;
        this.bucket = bucket;
        this.tableData = tableData;
    }

    public void runImport(String importId, String s3Key) throws Exception {
        // Markera imporjobbet som pågående
        importJobRepository.updateStatus(importId, "IN_PROGRESS");

        int processedCount = 0; // Antal rader som gick bra
        int failedCount = 0;    // Antal rader som misslyckades

        // Skickar notis om att importen startar
        notificationService.publish("IMPORT_STARTED",Map.of("importId", importId, "s3Key", s3Key));

        try (InputStream inputStream = s3Client.getObject(b -> b.bucket(bucket).key(s3Key))) {

            // Tolka CSV till en lista av rader (Map med kolumnnamn -> värden)
            List<Map<String, String>> CsvRows = CsvParser.parse(inputStream);

            // Loopa igenom varje rad och försök skriva till DynamoDB
            for (Map<String, String> currentRow : CsvRows) {
                try {
                    // Gör om radens fält till DynamoDB-format
                    Map<String, AttributeValue> dynamoDbItem = new HashMap<>();

                    String rowId = currentRow.getOrDefault("id", UUID.randomUUID().toString());
                    dynamoDbItem.put("id", AttributeValue.builder().s(rowId).build());

                    for (Map.Entry<String, String> column : currentRow.entrySet()) {
                        if (!column.getKey().equals("id")) {
                            dynamoDbItem.put(column.getKey(), AttributeValue.builder().s(column.getValue()).build());
                        }
                    }

                    dynamoDbClient.putItem(
                            PutItemRequest.builder()
                                    .tableName(tableData)
                                    .item(dynamoDbItem)
                                    .build()
                    );

                    processedCount++;


                } catch (Exception rowError) {
                    failedCount++;
                    log.error("Fel vid import av rad: {}", rowError.getMessage());
                    notificationService.publish("ROW_FAILED", Map.of("importId", importId, "error", rowError.getMessage()));

                }
            }


            String finalStatus = (failedCount == 0) ? "DONE" : "FAILED";
            importJobRepository.updateStatus(importId, finalStatus);
            importJobRepository.incrementCounts(importId, processedCount, failedCount);

            notificationService.publish("IMPORT_FINISHED", Map.of("importId", importId, "s3Key", s3Key, "processed", processedCount,
                    "failed", failedCount, "status", finalStatus));

        } catch (Exception e) {
            log.error("Fel vid import: {}", e.getMessage());
            importJobRepository.updateStatus(importId, "FAILED");

            notificationService.publish("IMPORT_FAILED", Map.of("importId", importId, "error", e.getMessage()));

            throw e;
        }
        log.info("Import slutförd - processed={}, failed={}", processedCount, failedCount); // visar importen färdig och sen hur mycket som processats och hur många som kan ha failat


    }


}


