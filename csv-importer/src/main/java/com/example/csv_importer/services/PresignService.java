package com.example.csv_importer.services;


import com.example.csv_importer.dtos.PresignResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;


import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class PresignService {
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final int presignExpirySeconds;

    public PresignService(S3Presigner s3Presigner, @Value("${app.aws.s3.bucket:csv-importer-bucket-tony}") String bucket,
                          @Value("${app.aws.s3.presignExpirySeconds:900}") int presignExpirySeconds) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.presignExpirySeconds = presignExpirySeconds;
    }

    public PresignResponse createCsvPresign(){
        String key = "imports/" + UUID.randomUUID() + ".csv";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType("text/csv").build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofSeconds(presignExpirySeconds))

                .putObjectRequest(putObjectRequest));

        URL url = presignedPutObjectRequest.url();
        return new PresignResponse(url.toString(), key);
    }
}
