package com.skillbridge.sa.data.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.skillbridge.sa.SkillBridgeApp
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SkillBridgeMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "SkillBridge Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new update."
        val userId = message.data["userId"] ?: ""
        val typeStr = message.data["type"] ?: "SYSTEM"
        val type = runCatching { NotificationType.valueOf(typeStr) }.getOrDefault(NotificationType.SYSTEM)

        val app = applicationContext as? SkillBridgeApp ?: return
        serviceScope.launch {
            app.dataRepository.notifications.insert(
                AppNotification(
                    userId = userId,
                    type = type,
                    title = title,
                    body = body
                )
            )
        }
    }
}
