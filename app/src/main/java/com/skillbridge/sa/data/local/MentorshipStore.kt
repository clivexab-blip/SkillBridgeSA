package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.MentorshipRequest
import com.skillbridge.sa.data.model.MentorshipStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class MentorshipStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun request(request: MentorshipRequest): String = withContext(Dispatchers.IO) {
        val id = request.requestId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("requestId", id)
            put("studentProfileId", request.studentProfileId)
            put("mentorProfileId", request.mentorProfileId)
            put("message", request.message)
            put("status", request.status.name)
            put("sessionTime", request.sessionTime)
            put("requestedAt", request.requestedAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_MENTORSHIP, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun updateStatus(requestId: String, status: MentorshipStatus, sessionTime: Long? = null) =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("status", status.name)
                if (sessionTime != null) put("sessionTime", sessionTime)
            }
            db.update(SkillBridgeDbHelper.TABLE_MENTORSHIP, values, "requestId = ?", arrayOf(requestId))
        }

    suspend fun getForStudent(studentProfileId: String): List<MentorshipRequest> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<MentorshipRequest>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_MENTORSHIP} WHERE studentProfileId = ? ORDER BY requestedAt DESC",
            arrayOf(studentProfileId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    suspend fun getForMentor(mentorProfileId: String): List<MentorshipRequest> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<MentorshipRequest>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_MENTORSHIP} WHERE mentorProfileId = ? ORDER BY requestedAt DESC",
            arrayOf(mentorProfileId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    private fun mapCursor(c: Cursor): MentorshipRequest = MentorshipRequest(
        requestId = c.getString(c.getColumnIndexOrThrow("requestId")),
        studentProfileId = c.getString(c.getColumnIndexOrThrow("studentProfileId")) ?: "",
        mentorProfileId = c.getString(c.getColumnIndexOrThrow("mentorProfileId")) ?: "",
        message = c.getString(c.getColumnIndexOrThrow("message")) ?: "",
        status = runCatching { MentorshipStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))) }.getOrDefault(MentorshipStatus.PENDING),
        sessionTime = c.getLong(c.getColumnIndexOrThrow("sessionTime")),
        requestedAt = c.getLong(c.getColumnIndexOrThrow("requestedAt"))
    )
}
