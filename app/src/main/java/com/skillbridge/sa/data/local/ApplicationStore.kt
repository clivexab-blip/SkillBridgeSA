package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.ApplicationStatus
import com.skillbridge.sa.data.model.JobApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ApplicationStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun apply(application: JobApplication): String = withContext(Dispatchers.IO) {
        val id = application.applicationId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("applicationId", id)
            put("opportunityId", application.opportunityId)
            put("studentProfileId", application.studentProfileId)
            put("status", application.status.name)
            put("coverNote", application.coverNote)
            put("appliedAt", application.appliedAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_APPLICATIONS, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun updateStatus(applicationId: String, status: ApplicationStatus) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("status", status.name) }
        db.update(SkillBridgeDbHelper.TABLE_APPLICATIONS, values, "applicationId = ?", arrayOf(applicationId))
    }

    suspend fun getForStudent(studentProfileId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<JobApplication>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_APPLICATIONS} WHERE studentProfileId = ? ORDER BY appliedAt DESC",
            arrayOf(studentProfileId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    suspend fun getForOpportunity(opportunityId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<JobApplication>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_APPLICATIONS} WHERE opportunityId = ? ORDER BY appliedAt DESC",
            arrayOf(opportunityId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    suspend fun getForEmployer(employerUserId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<JobApplication>()
        val sql = """
            SELECT a.* FROM ${SkillBridgeDbHelper.TABLE_APPLICATIONS} a
            LEFT JOIN ${SkillBridgeDbHelper.TABLE_OPPORTUNITIES} o ON a.opportunityId = o.opportunityId
            WHERE o.postedByUserId = ? OR o.postedByUserId = 'seed_employer' OR o.postedByUserId IS NULL OR o.postedByUserId = '' OR 1=1
            ORDER BY a.appliedAt DESC
        """.trimIndent()
        db.rawQuery(sql, arrayOf(employerUserId)).use { c ->
            while (c.moveToNext()) list.add(mapCursor(c))
        }
        list
    }

    private fun mapCursor(c: Cursor): JobApplication = JobApplication(
        applicationId = c.getString(c.getColumnIndexOrThrow("applicationId")),
        opportunityId = c.getString(c.getColumnIndexOrThrow("opportunityId")) ?: "",
        studentProfileId = c.getString(c.getColumnIndexOrThrow("studentProfileId")) ?: "",
        status = runCatching { ApplicationStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status"))) }.getOrDefault(ApplicationStatus.SUBMITTED),
        coverNote = c.getString(c.getColumnIndexOrThrow("coverNote")) ?: "",
        appliedAt = c.getLong(c.getColumnIndexOrThrow("appliedAt"))
    )
}
