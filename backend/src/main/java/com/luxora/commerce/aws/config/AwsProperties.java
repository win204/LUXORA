package com.luxora.commerce.aws.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "luxora.aws")
public class AwsProperties {

    private String region = "ap-southeast-1";
    private String endpoint;
    private String accessKeyId = "mock-access-key";
    private String secretAccessKey = "mock-secret-key";
    private S3Properties s3 = new S3Properties();
    private SesProperties ses = new SesProperties();
    private SqsProperties sqs = new SqsProperties();
    private SnsProperties sns = new SnsProperties();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public S3Properties getS3() {
        return s3;
    }

    public void setS3(S3Properties s3) {
        this.s3 = s3;
    }

    public SesProperties getSes() {
        return ses;
    }

    public void setSes(SesProperties ses) {
        this.ses = ses;
    }

    public SqsProperties getSqs() {
        return sqs;
    }

    public void setSqs(SqsProperties sqs) {
        this.sqs = sqs;
    }

    public SnsProperties getSns() {
        return sns;
    }

    public void setSns(SnsProperties sns) {
        this.sns = sns;
    }

    public static class S3Properties {
        private String bucket = "luxora-media";
        private Duration presignedUrlExpiration = Duration.ofMinutes(15);

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public Duration getPresignedUrlExpiration() {
            return presignedUrlExpiration;
        }

        public void setPresignedUrlExpiration(Duration presignedUrlExpiration) {
            this.presignedUrlExpiration = presignedUrlExpiration;
        }
    }

    public static class SesProperties {
        private String fromEmail = "noreply@luxora.com";

        public String getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }
    }

    public static class SqsProperties {
        private String queueUrl;

        public String getQueueUrl() {
            return queueUrl;
        }

        public void setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
        }
    }

    public static class SnsProperties {
        private String topicArn;

        public String getTopicArn() {
            return topicArn;
        }

        public void setTopicArn(String topicArn) {
            this.topicArn = topicArn;
        }
    }
}
