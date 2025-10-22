package com.example.csv_importer.configs;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

@Configuration
public class AwsClientsConfig {

    @Value("${app.aws.region}")
    private String region;

    @Bean
    public Region awsRegion() {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(Region region) {
        S3ClientBuilder b = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build());

        XRayConfig.instrument(b); // Lägger på X-Ray spårning

        return b.build();
    }

    @Bean
    public S3Presigner s3Presigner(Region region) {
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient(Region region) {
        DynamoDbClientBuilder b = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build());

        XRayConfig.instrument(b);

        return b.build();
    }

    @Bean
    public SnsClient snsClient(Region region) {
        SnsClientBuilder b = SnsClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build());

        XRayConfig.instrument(b);
        return b.build();
    }

}
