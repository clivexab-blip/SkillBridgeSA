package com.skillbridge.sa.data.model

enum class UserRole { STUDENT, GRADUATE, MENTOR, EMPLOYER, ADMIN }

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.STUDENT,
    val professionalTitle: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val expertise: List<String> = emptyList(),
    val qualification: String = "",
    val experience: String = "",
    val availability: String = "",
    val careerInterest: String = "",
    val location: String = "",
    val profileComplete: Int = 40,
    val language: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)

enum class OpportunityType { JOB, INTERNSHIP, GRADUATE_PROGRAMME }

data class Opportunity(
    val opportunityId: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val type: OpportunityType = OpportunityType.JOB,
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val salaryRange: String = "",
    val postedByUserId: String = "",
    val closingDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ApplicationStatus { SUBMITTED, UNDER_REVIEW, SHORTLISTED, REJECTED, OFFER }

data class JobApplication(
    val applicationId: String = "",
    val opportunityId: String = "",
    val studentProfileId: String = "",
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    val coverNote: String = "",
    val appliedAt: Long = System.currentTimeMillis()
)

enum class MentorshipStatus { PENDING, ACCEPTED, REJECTED, COMPLETED }

data class MentorshipRequest(
    val requestId: String = "",
    val studentProfileId: String = "",
    val mentorProfileId: String = "",
    val message: String = "",
    val status: MentorshipStatus = MentorshipStatus.PENDING,
    val sessionTime: Long = 0L,
    val requestedAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderUserId: String = "",
    val receiverUserId: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType { OPPORTUNITY, APPLICATION_UPDATE, MESSAGE, MENTORSHIP, SYSTEM }

data class AppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class LearningResource(
    val resourceId: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val resourceType: String = "Article",
    val fileUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class SavedOpportunity(
    val savedId: String = "",
    val studentProfileId: String = "",
    val opportunityId: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

data class CareerPathway(
    val careerName: String,
    val skills: List<String>,
    val description: String
)

val CAREER_PATHWAYS = listOf(
    CareerPathway(
        "Software Engineering",
        listOf("Java", "Kotlin", "Python", "C#.NET Core (MVC)", "SQL", "Git", "APIs", "Databases"),
        "Build applications and systems. Learn programming fundamentals, version control and software design principles."
    ),
    CareerPathway(
        "Data Analysis",
        listOf("Excel", "SQL", "Power BI", "Python", "Statistics"),
        "Turn raw data into insight using analytical tools and visualisation."
    ),
    CareerPathway(
        "UI/UX Design",
        listOf("Figma", "User Research", "Wireframing", "Prototyping"),
        "Design usable, accessible interfaces for web and mobile products."
    ),
    CareerPathway(
        "IT Support",
        listOf("Networking", "Windows Server", "Helpdesk", "Troubleshooting"),
        "Keep organisations running by supporting users, devices and networks."
    )
)
