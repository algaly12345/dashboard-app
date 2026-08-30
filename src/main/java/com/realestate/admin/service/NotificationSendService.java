package com.realestate.admin.service;

import com.google.firebase.messaging.*;
import com.realestate.admin.config.FirebaseMessagingHolder;
import com.realestate.admin.entity.AppUser;
import com.realestate.admin.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends marketing push notifications directly to each target device's FCM
 * registration token (users.cm_firebase_token), NOT by topic - the mobile
 * app doesn't subscribe devices to any FCM topic, only registers a raw
 * device token per user on login. Matches the working Laravel Tinker
 * script (FcmV1Service::sendToToken) that confirmed delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSendService {

    private final FirebaseMessagingHolder firebaseMessagingHolder;
    private final AppUserRepository appUserRepository;

    public boolean isFirebaseReady() {
        return firebaseMessagingHolder.isReady();
    }

    public record SendResult(boolean sent, String messageId, String error) {
    }

    public SendResult send(String title, String body, Long zoneId, Long categoryId, String audience) {
        if (!firebaseMessagingHolder.isReady()) {
            return new SendResult(false, null,
                    "not_configured:" + firebaseMessagingHolder.unavailableReason());
        }

        List<String> tokens = appUserRepository.findAll().stream()
                .filter(u -> u.getCmFirebaseToken() != null && u.getCmFirebaseToken().length() > 50)
                .filter(u -> zoneId == null || zoneId.equals(u.getZoneId()))
                .filter(u -> audience == null || audience.isBlank() || "all".equals(audience)
                        || audience.equals(u.getUserType()))
                .map(AppUser::getCmFirebaseToken)
                .distinct()
                .toList();

        if (tokens.isEmpty()) {
            log.warn("Notification send skipped - no users matched the filters with a valid FCM token.");
            return new SendResult(false, null, "no_valid_tokens");
        }

        int successCount = 0;
        int failureCount = 0;

        // FCM allows at most 500 tokens per multicast call - chunk accordingly.
        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> batch = tokens.subList(i, Math.min(i + 500, tokens.size()));

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-priority", "10")
                            .putHeader("apns-push-type", "alert")
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder().setTitle(title).setBody(body).build())
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            try {
                BatchResponse response = firebaseMessagingHolder.messaging().sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();
                log.info("Notification batch sent - success: {}, failure: {}",
                        response.getSuccessCount(), response.getFailureCount());

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse r = responses.get(j);
                    if (!r.isSuccessful()) {
                        String token = batch.get(j);
                        String shortToken = token.length() > 15 ? token.substring(0, 15) + "..." : token;
                        log.warn("Token FAILED - token starts with: {}, error: {}",
                                shortToken, r.getException() != null ? r.getException().getMessage() : "unknown");
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("Notification batch failed entirely", e);
                failureCount += batch.size();
            }
        }

        boolean sent = successCount > 0;
        String summary = successCount + " تم التسليم، " + failureCount + " فشل";
        return new SendResult(sent, summary, sent ? null : "all_failed");
    }
}
