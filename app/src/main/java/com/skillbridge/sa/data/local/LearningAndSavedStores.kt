package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.LearningResource
import com.skillbridge.sa.data.model.SavedOpportunity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class LearningResourceStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun getAll(): List<LearningResource> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<LearningResource>()
        db.rawQuery("SELECT * FROM ${SkillBridgeDbHelper.TABLE_LEARNING} ORDER BY createdAt DESC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LearningResource(
                        resourceId = c.getString(c.getColumnIndexOrThrow("resourceId")),
                        title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
                        category = c.getString(c.getColumnIndexOrThrow("category")) ?: "",
                        description = c.getString(c.getColumnIndexOrThrow("description")) ?: "",
                        resourceType = c.getString(c.getColumnIndexOrThrow("resourceType")) ?: "",
                        fileUrl = c.getString(c.getColumnIndexOrThrow("fileUrl")) ?: "",
                        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
                    )
                )
            }
        }
        list
    }
}

class SavedOpportunityStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun save(saved: SavedOpportunity): String = withContext(Dispatchers.IO) {
        val id = saved.savedId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("savedId", id)
            put("studentProfileId", saved.studentProfileId)
            put("opportunityId", saved.opportunityId)
            put("savedAt", saved.savedAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_SAVED, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun remove(studentProfileId: String, opportunityId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            SkillBridgeDbHelper.TABLE_SAVED,
            "studentProfileId = ? AND opportunityId = ?",
            arrayOf(studentProfileId, opportunityId)
        )
    }

    suspend fun getForStudent(studentProfileId: String): List<SavedOpportunity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<SavedOpportunity>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_SAVED} WHERE studentProfileId = ? ORDER BY savedAt DESC",
            arrayOf(studentProfileId)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SavedOpportunity(
                        savedId = c.getString(c.getColumnIndexOrThrow("savedId")),
                        studentProfileId = c.getString(c.getColumnIndexOrThrow("studentProfileId")) ?: "",
                        opportunityId = c.getString(c.getColumnIndexOrThrow("opportunityId")) ?: "",
                        savedAt = c.getLong(c.getColumnIndexOrThrow("savedAt"))
                    )
                )
            }
        }
        list
    }

    private fun mapCursor(c: Cursor): SavedOpportunity = SavedOpportunity(
        savedId = c.getString(c.getColumnIndexOrThrow("savedId")),
        studentProfileId = c.getString(c.getColumnIndexOrThrow("studentProfileId")) ?: "",
        opportunityId = c.getString(c.getColumnIndexOrThrow("opportunityId")) ?: "",
        savedAt = c.getLong(c.getColumnIndexOrThrow("savedAt"))
    )
}
