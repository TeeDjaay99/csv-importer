package com.example.csv_importer.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SnsClient sns;
    private final String topicArn;
    private final ObjectMapper objectMapper;

    public NotificationService(SnsClient sns, ObjectMapper objectMapper, @Value("${app.aws.sns.topicArn}") String topicArn) {
        this.sns = sns;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    public void publish(String event, Map<String, ?> attributes) {
        if (topicArn == null || topicArn.isBlank()) {
            log.debug("SNS topicArn saknas - hoppar över publish för event={}", event);
            return;
        }
        try {
            Map<String, Object> messageBody = new HashMap<>();
            if (attributes != null) messageBody.putAll(attributes);
            messageBody.putIfAbsent("event", event);
            messageBody.putIfAbsent("timestamp", Instant.now().toString());

            String json = objectMapper.writeValueAsString(messageBody);

            sns.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject(event)
                    .message(json)
                    .build());

            log.info("SNS event '{}' skickat", event);

        } catch (Exception e) {
            log.error("Fel vid SNS publish (event={}", event, e);
        }
    }


}
