package com.realestate.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Wires up a Cloudflare R2 client using the standard AWS S3 SDK (R2 speaks
 * the S3 API). Disabled by default (app.r2.enabled=false) so the app runs
 * fine before API credentials are in place - see application.yml.
 */
@Configuration
@Slf4j
public class R2Config {

    @Value("${app.r2.enabled:false}")
    private boolean enabled;

    @Value("${app.r2.account-id:}")
    private String accountId;

    @Value("${app.r2.access-key-id:}")
    private String accessKeyId;

    @Value("${app.r2.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.r2.bucket:}")
    private String bucket;

    @Bean
    public R2ClientHolder r2ClientHolder() {
        if (!enabled) {
            log.warn("R2 storage disabled (app.r2.enabled=false). Image upload to R2 won't work until it's configured.");
            return new R2ClientHolder(null, bucket, "not_enabled");
        }
        if (accountId.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank() || bucket.isBlank()) {
            log.warn("R2 credentials incomplete - image upload to R2 won't work until R2_ACCOUNT_ID / " +
                    "R2_ACCESS_KEY_ID / R2_SECRET_ACCESS_KEY are all set.");
            return new R2ClientHolder(null, bucket, "credentials_missing");
        }
        try {
            S3Client client = S3Client.builder()
                    .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    // R2 doesn't use AWS regions, but the SDK requires one - "auto" is Cloudflare's convention.
                    .region(Region.of("auto"))
                    .build();
            return new R2ClientHolder(client, bucket, null);
        } catch (Exception e) {
            log.error("Failed to initialize R2 client - image upload disabled.", e);
            return new R2ClientHolder(null, bucket, e.getMessage());
        }
    }
}
