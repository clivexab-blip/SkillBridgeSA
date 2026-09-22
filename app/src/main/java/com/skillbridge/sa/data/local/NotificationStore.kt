package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class NotificationStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun insert(notification: AppNotification): String = withContext(Dispatchers.IO) {
        val id = notification.notificationId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("notificationId", id)
            put("userId", notification.userId)
            put("type", notification.type.name)
            put("title", notification.title)
            put("body", notification.body)
            put("isRead", if (notification.isRead) 1 else 0)
            put("createdAt", notification.createdAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_NOTIFICATIONS, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun markRead(notificationId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("isRead", 1) }
        db.update(SkillBridgeDbHelper.TABLE_NOTIFICATIONS, values, "notificationId = ?", arrayOf(notificationId))
    }

    suspend fun getForUser(userId: String): List<AppNotification> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<AppNotification>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_NOTIFICATIONS} WHERE userId = ? ORDER BY createdAt DESC",
            arrayOf(userId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    private fun mapCursor(c: Cursor): AppNotification = AppNotification(
        notificationId = c.getString(c.getColumnIndexOrThrow("notificationId")),
        userId = c.getString(c.getColumnIndexOrThrow("userId")) ?: "",
        type = runCatching { NotificationType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))) }.getOrDefault(NotificationType.SYSTEM),
        title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
        body = c.getString(c.getColumnIndexOrThrow("body")) ?: "",
        isRead = c.getInt(c.getColumnIndexOrThrow("isRead")) == 1,
        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
    )
}
