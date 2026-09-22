package com.skillbridge.sa.data.repository

import android.content.Context
import com.skillbridge.sa.data.local.ApplicationStore
import com.skillbridge.sa.data.local.LearningResourceStore
import com.skillbridge.sa.data.local.MentorshipStore
import com.skillbridge.sa.data.local.MessageStore
import com.skillbridge.sa.data.local.NotificationStore
import com.skillbridge.sa.data.local.OpportunityStore
import com.skillbridge.sa.data.local.SavedOpportunityStore
import com.skillbridge.sa.data.local.UserProfileStore

/**
 * Single access point to all local SQLite-backed data stores.
 * Firebase Auth handles identity only; everything else (profiles, jobs,
 * applications, mentorship, messages, notifications, learning resources)
 * is persisted locally with raw SQLite, per the project's offline-first design.
 */
class DataRepository(context: Context) {
    val users = UserProfileStore(context)
    val opportunities = OpportunityStore(context)
    val applications = ApplicationStore(context)
    val mentorship = MentorshipStore(context)
    val messages = MessageStore(context)
    val notifications = NotificationStore(context)
    val learningResources = LearningResourceStore(context)
    val savedOpportunities = SavedOpportunityStore(context)
}
