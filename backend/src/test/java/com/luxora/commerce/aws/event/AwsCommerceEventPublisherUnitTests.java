package com.luxora.commerce.aws.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxora.commerce.aws.config.AwsProperties;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AwsCommerceEventPublisherUnitTests {

    @Mock
    private SnsClient snsClient;

    @Mock
    private SqsClient sqsClient;

    private AwsProperties properties;
    private AwsCommerceEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new AwsProperties();
        properties.setRegion("ap-southeast-1");
        properties.getSns().setTopicArn("arn:aws:sns:ap-southeast-1:123456789012:luxora-events");
        properties.getSqs().setQueueUrl("https://sqs.ap-southeast-1.amazonaws.com/123456789012/luxora-orders");
        ObjectMapper mapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        publisher = new AwsCommerceEventPublisher(snsClient, sqsClient, properties, mapper);
    }

    @Test
    void publishEventSendsToBothSnsAndSqsWhenConfigured() {
        CommerceEvent event = CommerceEvent.of("ORDER_PLACED", Map.of("orderId", "ord-123", "amount", 899.00));
        publisher.publishEvent(event);

        verify(snsClient).publish(any(PublishRequest.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }
}
