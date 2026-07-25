package com.realestate.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Uploads files to the `abaad-media-assets` R2 bucket, mirroring the
 * folder layout already in use: estate/, offers/, categories/, profile/,
 * service-providers/, videos/.
 *
 * Credentials come from `business_settings` (via SettingsService), the
 * same live-editable-from-the-admin pattern already used for the public
 * bucket URL and the Maps API key - no environment variables, no
 * restart needed to change them. The R2 API token this expects is scoped
 * to Object Read & Write on this one bucket only (not a full-account
 * key), which keeps the blast radius small if it's ever compromised -
 * still, only an admin with access to this settings page should be able
 * to see/change it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class R2StorageService {

    private final SettingsService settingsService;

    public record UploadResult(boolean success, String filename, String error) {
    }

    public boolean isConfigured() {
        return !accountId().isBlank() && !accessKeyId().isBlank() && !secretAccessKey().isBlank() && !bucket().isBlank();
    }

    /** folder is one of: estate, offers, categories, profile, service-providers, videos */
    public UploadResult upload(MultipartFile file, String folder) {
        if (!isConfigured()) {
            return new UploadResult(false, null, "not_configured:credentials_missing");
        }
        if (file == null || file.isEmpty()) {
            return new UploadResult(false, null, "empty_file");
        }
        try (S3Client client = buildClient()) {
            String extension = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }
            String filename = Instant.now().getEpochSecond() + "_" + UUID.randomUUID() + extension;
            String key = folder + "/" + filename;

            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return new UploadResult(true, filename, null);
        } catch (Exception e) {
            log.error("R2 upload failed", e);
            return new UploadResult(false, null, e.getMessage());
        }
    }

    private S3Client buildClient() {
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId() + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId(), secretAccessKey())))
                .region(Region.of("auto"))
                .build();
    }

    private String accountId() {
        return settingsService.get("r2_account_id", "");
    }

    private String accessKeyId() {
        return settingsService.get("r2_access_key_id", "");
    }

    private String secretAccessKey() {
        return settingsService.get("r2_secret_access_key", "");
    }

    private String bucket() {
        return settingsService.get("r2_bucket", "abaad-media-assets");
    }
}
