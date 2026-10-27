package com.luxora.commerce.aws.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxora.commerce.aws.config.AwsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class AwsCommerceEventPublisher implements CommerceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AwsCommerceEventPublisher.class);

    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final AwsProperties properties;
    private final ObjectMapper objectMapper;

    public AwsCommerceEventPublisher(
            SnsClient snsClient,
            SqsClient sqsClient,
            AwsProperties properties,
            ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper != null
                ? objectMapper
                : new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Override
    public void publishEvent(CommerceEvent event) {
        try {
            String messageJson = objectMapper.writeValueAsString(event);

            // Publish to SNS Topic if configured
            String topicArn = properties.getSns().getTopicArn();
            if (topicArn != null && !topicArn.isBlank()) {
                PublishRequest publishRequest = PublishRequest.builder()
                        .topicArn(topicArn)
                        .subject(event.eventType())
                        .message(messageJson)
                        .build();

                snsClient.publish(publishRequest);
                log.info("Published event '{}' [ID: {}] to SNS Topic: {}", event.eventType(), event.eventId(), topicArn);
            }

            // Publish to SQS Queue if configured
            String queueUrl = properties.getSqs().getQueueUrl();
            if (queueUrl != null && !queueUrl.isBlank()) {
                SendMessageRequest sendRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageJson)
                        .build();

                sqsClient.sendMessage(sendRequest);
                log.info("Sent event '{}' [ID: {}] to SQS Queue: {}", event.eventType(), event.eventId(), queueUrl);
            }

            log.debug("Event payload: {}", messageJson);
        } catch (Exception e) {
            log.warn("Failed to publish event '{}' to AWS SNS/SQS: {}. (Non-blocking fallback)", event.eventType(), e.getMessage());
        }
    }
}
