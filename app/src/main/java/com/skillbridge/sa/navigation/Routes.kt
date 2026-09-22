package com.skillbridge.sa.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MESSAGES = "messages"
    const val CONVERSATION = "conversation/{conversationId}/{otherUserId}"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val PROFILE_VIEW = "profile_view/{userId}"
    const val SETTINGS = "settings"

    const val OPPORTUNITY_DETAIL = "opportunity/{opportunityId}"
    const val LEARNING_CENTRE = "learning"
    const val MENTORSHIP = "mentorship"
    const val CAREER_PATHWAYS = "career_pathways"
    const val ADMIN_USERS = "admin_users"

    fun conversationRoute(conversationId: String, otherUserId: String) =
        "conversation/$conversationId/$otherUserId"

    fun opportunityDetailRoute(opportunityId: String) = "opportunity/$opportunityId"

    fun profileViewRoute(userId: String) = "profile_view/$userId"
}
