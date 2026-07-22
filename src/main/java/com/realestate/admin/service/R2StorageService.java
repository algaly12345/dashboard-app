package com.realestate.admin.service;

import com.realestate.admin.config.R2ClientHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Uploads files to the `abaad-media-assets` R2 bucket, mirroring the
 * folder layout already in use: estate/, offers/, categories/, profile/,
 * service-providers/, videos/.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class R2StorageService {

    private final R2ClientHolder r2ClientHolder;

    public record UploadResult(boolean success, String filename, String error) {
    }

    /** folder is one of: estate, offers, categories, profile, service-providers, videos */
    public UploadResult upload(MultipartFile file, String folder) {
        if (!r2ClientHolder.isReady()) {
            return new UploadResult(false, null, "not_configured:" + r2ClientHolder.unavailableReason());
        }
        if (file == null || file.isEmpty()) {
            return new UploadResult(false, null, "empty_file");
        }
        try {
            String extension = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }
            String filename = Instant.now().getEpochSecond() + "_" + UUID.randomUUID() + extension;
            String key = folder + "/" + filename;

            r2ClientHolder.client().putObject(
                    PutObjectRequest.builder()
                            .bucket(r2ClientHolder.bucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return new UploadResult(true, filename, null);
        } catch (IOException e) {
            log.error("R2 upload failed", e);
            return new UploadResult(false, null, e.getMessage());
        } catch (Exception e) {
            log.error("R2 upload failed", e);
            return new UploadResult(false, null, e.getMessage());
        }
    }
}
