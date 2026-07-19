package com.realestate.admin.config;

import com.google.firebase.messaging.FirebaseMessaging;

/** Either a working FirebaseMessaging client, or the reason it isn't ready yet. */
public record FirebaseMessagingHolder(FirebaseMessaging messaging, String unavailableReason) {

    public boolean isReady() {
        return messaging != null;
    }
}
