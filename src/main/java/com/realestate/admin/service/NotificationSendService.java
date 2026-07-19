package com.realestate.admin.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.realestate.admin.config.FirebaseMessagingHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sends marketing push notifications by FCM **topic condition**, not by
 * per-device token - there's no device-token table in this schema. The
 * Android/iOS app is expected to subscribe each device to topics matching
 * its own zone / category interest / role on login, using this exact
 * naming convention:
 *
 *   zone_{zoneId}        e.g. zone_5
 *   category_{categoryId} e.g. category_12
 *   role_agent  |  role_provider
 *
 * If the mobile client doesn't yet subscribe to these topics, sends will
 * succeed at the API level but reach zero devices - that's a mobile-app
 * change, not something this admin panel can do on its own.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSendService {

    private final FirebaseMessagingHolder firebaseMessagingHolder;

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

        String condition = buildCondition(zoneId, categoryId, audience);

        Message.Builder builder = Message.builder()
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());

        if (condition != null) {
            builder.setCondition(condition);
        } else {
            builder.setTopic("all_users");
        }

        try {
            String messageId = firebaseMessagingHolder.messaging().send(builder.build());
            return new SendResult(true, messageId, null);
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed", e);
            return new SendResult(false, null, e.getMessage());
        }
    }

    /** Builds an FCM condition string like: 'zone_5' in topics && 'role_agent' in topics */
    private String buildCondition(Long zoneId, Long categoryId, String audience) {
        StringBuilder sb = new StringBuilder();
        if (zoneId != null) {
            append(sb, "'zone_" + zoneId + "' in topics");
        }
        if (categoryId != null) {
            append(sb, "'category_" + categoryId + "' in topics");
        }
        if (audience != null && !audience.isBlank() && !"all".equals(audience)) {
            append(sb, "'role_" + audience + "' in topics");
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private void append(StringBuilder sb, String clause) {
        if (!sb.isEmpty()) sb.append(" && ");
        sb.append(clause);
    }
}
