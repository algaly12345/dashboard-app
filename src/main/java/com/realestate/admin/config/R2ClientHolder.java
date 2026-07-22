package com.realestate.admin.config;

import software.amazon.awssdk.services.s3.S3Client;

/** Either a working R2 (S3) client, or the reason it isn't ready yet. */
public record R2ClientHolder(S3Client client, String bucket, String unavailableReason) {

    public boolean isReady() {
        return client != null;
    }
}
