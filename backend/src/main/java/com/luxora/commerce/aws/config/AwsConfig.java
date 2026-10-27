package com.luxora.commerce.aws.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration
public class AwsConfig {

    private final AwsProperties properties;

    public AwsConfig(AwsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                && properties.getSecretAccessKey() != null && !properties.getSecretAccessKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
        SesClientBuilder builder = SesClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SnsClient snsClient(AwsCredentialsProvider credentialsProvider) {
        SnsClientBuilder builder = SnsClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }
}
