package com.skillbridge.sa.data.local

import android.content.ContentValues
import android.content.Context
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProfileStore(context: Context) {
    private val dbHelper = SkillBridgeDbHelper.getInstance(context)

    suspend fun upsert(profile: UserProfile) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("userId", profile.userId)
            put("fullName", profile.fullName)
            put("email", profile.email)
            put("phoneNumber", profile.phoneNumber)
            put("role", profile.role.name)
            put("professionalTitle", profile.professionalTitle)
            put("bio", profile.bio)
            put("skills", SqliteUtils.listToString(profile.skills))
            put("expertise", SqliteUtils.listToString(profile.expertise))
            put("qualification", profile.qualification)
            put("experience", profile.experience)
            put("availability", profile.availability)
            put("careerInterest", profile.careerInterest)
            put("location", profile.location)
            put("profileComplete", profile.profileComplete)
            put("language", profile.language)
            put("createdAt", profile.createdAt)
        }
        db.insertWithOnConflict(
            SkillBridgeDbHelper.TABLE_USERS, null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun getById(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM ${SkillBridgeDbHelper.TABLE_USERS} WHERE userId = ?", arrayOf(userId)).use { c ->
            if (c.moveToFirst()) mapCursorToProfile(c) else null
        }
    }

    suspend fun getMentors(): List<UserProfile> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<UserProfile>()
        db.rawQuery(
            "SELECT * FROM ${SkillBridgeDbHelper.TABLE_USERS} WHERE role = ?",
            arrayOf(UserRole.MENTOR.name)
        ).use { c ->
            while (c.moveToNext()) list.add(mapCursorToProfile(c))
        }
        list
    }

    suspend fun getAll(): List<UserProfile> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<UserProfile>()
        db.rawQuery("SELECT * FROM ${SkillBridgeDbHelper.TABLE_USERS}", null).use { c ->
            while (c.moveToNext()) list.add(mapCursorToProfile(c))
        }
        list
    }

    suspend fun search(query: String): List<UserProfile> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<UserProfile>()
        val sql = "SELECT * FROM ${SkillBridgeDbHelper.TABLE_USERS} WHERE fullName LIKE ? OR bio LIKE ? OR skills LIKE ?"
        val args = arrayOf("%$query%", "%$query%", "%$query%")
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) list.add(mapCursorToProfile(c))
        }
        list
    }

    private fun mapCursorToProfile(c: android.database.Cursor): UserProfile {
        return UserProfile(
            userId = c.getString(c.getColumnIndexOrThrow("userId")),
            fullName = c.getString(c.getColumnIndexOrThrow("fullName")) ?: "",
            email = c.getString(c.getColumnIndexOrThrow("email")) ?: "",
            phoneNumber = c.getString(c.getColumnIndexOrThrow("phoneNumber")) ?: "",
            role = runCatching { UserRole.valueOf(c.getString(c.getColumnIndexOrThrow("role"))) }.getOrDefault(UserRole.STUDENT),
            professionalTitle = c.getString(c.getColumnIndexOrThrow("professionalTitle")) ?: "",
            bio = c.getString(c.getColumnIndexOrThrow("bio")) ?: "",
            skills = SqliteUtils.stringToList(c.getString(c.getColumnIndexOrThrow("skills"))),
            expertise = SqliteUtils.stringToList(c.getString(c.getColumnIndexOrThrow("expertise"))),
            qualification = c.getString(c.getColumnIndexOrThrow("qualification")) ?: "",
            experience = c.getString(c.getColumnIndexOrThrow("experience")) ?: "",
            availability = c.getString(c.getColumnIndexOrThrow("availability")) ?: "",
            careerInterest = c.getString(c.getColumnIndexOrThrow("careerInterest")) ?: "",
            location = c.getString(c.getColumnIndexOrThrow("location")) ?: "",
            profileComplete = c.getInt(c.getColumnIndexOrThrow("profileComplete")),
            language = c.getString(c.getColumnIndexOrThrow("language")) ?: "en",
            createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
        )
    }
}
