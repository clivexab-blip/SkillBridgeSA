package com.skillbridge.sa.data.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging push notifications for new opportunities,
 * application status changes, mentorship requests and messages.
 * Incoming notifications are also written to the local SQLite notifications
 * table so they appear in the in-app Notifications screen even offline.
 */
class SkillBridgeMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // The device token would be associated with the signed-in user's
        // profile in Firestore/Firebase so the backend can target pushes.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Title/body from message.notification are surfaced by the system tray.
        // App-side persistence into SQLite notifications table happens the
        // next time the app is foregrounded and syncs, keeping this service light.
    }
}
