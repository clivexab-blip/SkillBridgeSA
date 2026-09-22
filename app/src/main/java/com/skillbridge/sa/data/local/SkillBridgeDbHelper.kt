package com.skillbridge.sa.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SkillBridgeDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "skillbridge_sa.db"
        const val DB_VERSION = 2

        const val TABLE_USERS = "user_profiles"
        const val TABLE_OPPORTUNITIES = "opportunities"
        const val TABLE_APPLICATIONS = "applications"
        const val TABLE_MENTORSHIP = "mentorship_requests"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_NOTIFICATIONS = "notifications"
        const val TABLE_LEARNING = "learning_resources"
        const val TABLE_SAVED = "saved_opportunities"

        @Volatile private var instance: SkillBridgeDbHelper? = null

        fun getInstance(context: Context): SkillBridgeDbHelper =
            instance ?: synchronized(this) {
                instance ?: SkillBridgeDbHelper(context).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                userId TEXT PRIMARY KEY,
                fullName TEXT,
                email TEXT,
                phoneNumber TEXT,
                role TEXT,
                professionalTitle TEXT,
                bio TEXT,
                skills TEXT,
                expertise TEXT,
                qualification TEXT,
                experience TEXT,
                availability TEXT,
                careerInterest TEXT,
                location TEXT,
                profileComplete INTEGER,
                language TEXT,
                createdAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_OPPORTUNITIES (
                opportunityId TEXT PRIMARY KEY,
                title TEXT,
                company TEXT,
                location TEXT,
                type TEXT,
                description TEXT,
                requirements TEXT,
                salaryRange TEXT,
                postedByUserId TEXT,
                closingDate INTEGER,
                createdAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_APPLICATIONS (
                applicationId TEXT PRIMARY KEY,
                opportunityId TEXT,
                studentProfileId TEXT,
                status TEXT,
                coverNote TEXT,
                appliedAt INTEGER,
                FOREIGN KEY(opportunityId) REFERENCES $TABLE_OPPORTUNITIES(opportunityId)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MENTORSHIP (
                requestId TEXT PRIMARY KEY,
                studentProfileId TEXT,
                mentorProfileId TEXT,
                message TEXT,
                status TEXT,
                sessionTime INTEGER,
                requestedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                messageId TEXT PRIMARY KEY,
                conversationId TEXT,
                senderUserId TEXT,
                receiverUserId TEXT,
                body TEXT,
                isRead INTEGER,
                createdAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                notificationId TEXT PRIMARY KEY,
                userId TEXT,
                type TEXT,
                title TEXT,
                body TEXT,
                isRead INTEGER,
                createdAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_LEARNING (
                resourceId TEXT PRIMARY KEY,
                title TEXT,
                category TEXT,
                description TEXT,
                resourceType TEXT,
                fileUrl TEXT,
                createdAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SAVED (
                savedId TEXT PRIMARY KEY,
                studentProfileId TEXT,
                opportunityId TEXT,
                savedAt INTEGER
            )
            """.trimIndent()
        )

        seedLearningResources(db)
        seedOpportunities(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OPPORTUNITIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APPLICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MENTORSHIP")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEARNING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAVED")
        onCreate(db)
    }

    private fun seedLearningResources(db: SQLiteDatabase) {
        val resources = listOf(
            Triple("CV Writing Masterclass", "Career Skills", "Beginner"),
            Triple("Interview Skills", "Career Skills", "Beginner"),
            Triple("Python for Data Analysis", "Technical Skills", "Beginner"),
            Triple("Git & Version Control Basics", "Technical Skills", "Beginner"),
            Triple("Workplace Readiness", "Professional Skills", "Beginner")
        )
        resources.forEachIndexed { index, (title, category, level) ->
            db.execSQL(
                "INSERT INTO $TABLE_LEARNING (resourceId, title, category, description, resourceType, fileUrl, createdAt) VALUES (?,?,?,?,?,?,?)",
                arrayOf("seed_res_$index", title, category, "$level level resource to help you grow.", "Course", "", System.currentTimeMillis())
            )
        }
    }

    private fun seedOpportunities(db: SQLiteDatabase) {
        val seed = listOf(
            arrayOf("seed_op_1", "Junior Software Developer", "ZikTech Solutions", "Durban, SA", "JOB", "Build and maintain features across our product suite.", "Kotlin|~|Java|~|REST API", "R18,000 - R25,000 pm"),
            arrayOf("seed_op_2", "Data Analyst Intern", "DigiServe SA", "Johannesburg, SA", "INTERNSHIP", "Support the analytics team with reporting and dashboards.", "SQL|~|Excel|~|Power BI", "Stipend"),
            arrayOf("seed_op_3", "Graduate Programme 2026", "BuildIt Group", "Nationwide, SA", "GRADUATE_PROGRAMME", "Rotational graduate programme across engineering and operations.", "Leadership|~|Training", "Competitive")
        )
        seed.forEach { row ->
            db.execSQL(
                "INSERT INTO $TABLE_OPPORTUNITIES (opportunityId, title, company, location, type, description, requirements, salaryRange, postedByUserId, closingDate, createdAt) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], "seed_employer", 0L, System.currentTimeMillis())
            )
        }
    }
}
