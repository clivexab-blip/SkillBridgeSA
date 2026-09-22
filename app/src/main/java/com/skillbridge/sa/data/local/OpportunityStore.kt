package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.OpportunityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class OpportunityStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun insert(opportunity: Opportunity): String = withContext(Dispatchers.IO) {
        val id = opportunity.opportunityId.ifBlank { UUID.randomUUID().toString() }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("opportunityId", id)
            put("title", opportunity.title)
            put("company", opportunity.company)
            put("location", opportunity.location)
            put("type", opportunity.type.name)
            put("description", opportunity.description)
            put("requirements", SqliteUtils.listToString(opportunity.requirements))
            put("salaryRange", opportunity.salaryRange)
            put("postedByUserId", opportunity.postedByUserId)
            put("closingDate", opportunity.closingDate)
            put("createdAt", opportunity.createdAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_OPPORTUNITIES, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        id
    }

    suspend fun search(keyword: String = "", location: String = "", type: OpportunityType? = null): List<Opportunity> =
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val where = StringBuilder("1=1")
            val args = mutableListOf<String>()
            if (keyword.isNotBlank()) {
                where.append(" AND (title LIKE ? OR company LIKE ? OR requirements LIKE ?)")
                repeat(3) { args.add("%$keyword%") }
            }
            if (location.isNotBlank() && location != "All Locations") {
                where.append(" AND location LIKE ?")
                args.add("%$location%")
            }
            if (type != null) {
                where.append(" AND type = ?")
                args.add(type.name)
            }
            val list = mutableListOf<Opportunity>()
            db.rawQuery(
                "SELECT * FROM ${SkillBridgeDbHelper.TABLE_OPPORTUNITIES} WHERE $where ORDER BY createdAt DESC",
                args.toTypedArray()
            ).use { c ->
                while (c.moveToNext()) list.add(mapCursor(c))
            }
            list
        }

    suspend fun getAll(): List<Opportunity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Opportunity>()
        db.rawQuery("SELECT * FROM ${SkillBridgeDbHelper.TABLE_OPPORTUNITIES} ORDER BY createdAt DESC", null).use { c ->
            while (c.moveToNext()) list.add(mapCursor(c))
        }
        list
    }

    suspend fun getById(opportunityId: String): Opportunity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_OPPORTUNITIES} WHERE opportunityId = ?",
            arrayOf(opportunityId)
        ).use { c -> if (c.moveToFirst()) mapCursor(c) else null }
    }

    suspend fun getByEmployer(employerUserId: String): List<Opportunity> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Opportunity>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_OPPORTUNITIES} WHERE postedByUserId = ? ORDER BY createdAt DESC",
            arrayOf(employerUserId)
        ).use { c -> while (c.moveToNext()) list.add(mapCursor(c)) }
        list
    }

    private fun mapCursor(c: Cursor): Opportunity {
        return Opportunity(
            opportunityId = c.getString(c.getColumnIndexOrThrow("opportunityId")),
            title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
            company = c.getString(c.getColumnIndexOrThrow("company")) ?: "",
            location = c.getString(c.getColumnIndexOrThrow("location")) ?: "",
            type = runCatching { OpportunityType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))) }.getOrDefault(OpportunityType.JOB),
            description = c.getString(c.getColumnIndexOrThrow("description")) ?: "",
            requirements = SqliteUtils.stringToList(c.getString(c.getColumnIndexOrThrow("requirements"))),
            salaryRange = c.getString(c.getColumnIndexOrThrow("salaryRange")) ?: "",
            postedByUserId = c.getString(c.getColumnIndexOrThrow("postedByUserId")) ?: "",
            closingDate = c.getLong(c.getColumnIndexOrThrow("closingDate")),
            createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
        )
    }
}
