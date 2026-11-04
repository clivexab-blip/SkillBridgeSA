package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MessageStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun send(message: ChatMessage): String = withContext(Dispatchers.IO) {
        val id = message.messageId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("messageId", id)
            put("conversationId", message.conversationId)
            put("senderUserId", message.senderUserId)
            put("receiverUserId", message.receiverUserId)
            put("body", message.body)
            put("isRead", if (message.isRead) 1 else 0)
            put("createdAt", message.createdAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_MESSAGES, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun getConversation(conversationId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ChatMessage>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_MESSAGES} WHERE conversationId = ? ORDER BY createdAt ASC",
            arrayOf(conversationId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    suspend fun getConversationsForUser(userId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<ChatMessage>()
        db.rawQuery(
            """
            SELECT * FROM ${SkillBridgeDbHelper.TABLE_MESSAGES}
            WHERE senderUserId = ? OR receiverUserId = ? OR receiverUserId = 'seed_employer' OR senderUserId = 'seed_employer'
            ORDER BY createdAt DESC
            """.trimIndent(),
            arrayOf(userId, userId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        // Reduce to the most recent message per conversation
        list.groupBy { it.conversationId }.map { it.value.first() }
    }

    private fun mapCursor(c: Cursor): ChatMessage = ChatMessage(
        messageId = c.getString(c.getColumnIndexOrThrow("messageId")),
        conversationId = c.getString(c.getColumnIndexOrThrow("conversationId")) ?: "",
        senderUserId = c.getString(c.getColumnIndexOrThrow("senderUserId")) ?: "",
        receiverUserId = c.getString(c.getColumnIndexOrThrow("receiverUserId")) ?: "",
        body = c.getString(c.getColumnIndexOrThrow("body")) ?: "",
        isRead = c.getInt(c.getColumnIndexOrThrow("isRead")) == 1,
        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
    )
}
