package com.example.csv_importer.configs;


import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration
public class XRayConfig {
    static {
        AWSXRay.setGlobalRecorder(AWSXRayRecorderBuilder.defaultRecorder());
    }

    @Bean
    public FilterRegistrationBean<AWSXRayServletFilter> xrayFilter() {
        FilterRegistrationBean<AWSXRayServletFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AWSXRayServletFilter("csv-importer")); // service namn i X-Ray
        bean.setOrder(1);
        return bean;
    }

    public static void instrument(S3ClientBuilder builder) {
        builder.overrideConfiguration(
                ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
        );
    }

    public static void instrument(DynamoDbClientBuilder builder) {
        builder.overrideConfiguration(
                ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
        );
    }

    public static void instrument(SqsClientBuilder builder){
        builder.overrideConfiguration(
                ClientOverrideConfiguration.builder()
                        .addExecutionInterceptor(new TracingInterceptor())
                        .build()
        );
    }

}
