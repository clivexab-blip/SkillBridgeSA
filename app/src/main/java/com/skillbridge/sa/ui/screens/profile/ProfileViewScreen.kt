package com.skillbridge.sa.ui.screens.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.MentorshipRequest
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileViewScreen(
    userId: String,
    currentUser: UserProfile,
    repo: DataRepository,
    onBack: () -> Unit,
    onOpenConversation: (String, String) -> Unit
) {
    AndroidView(
        factory = { context ->
            ProfileViewLayout(context).apply {
                init(userId, currentUser, repo, onBack, onOpenConversation)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class ProfileViewLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var currentUser: UserProfile
    private var targetUser: UserProfile? = null

    private val tvFullName: TextView
    private val tvProfessionalTitle: TextView
    private val tvRole: TextView
    private val tvBio: TextView
    private val tvEducation: TextView
    private val tvExperience: TextView
    private val tvLanguages: TextView
    private val tvLocation: TextView
    private val tvAvailability: TextView
    private val tvInitial: TextView
    private val tvHeaderTitle: TextView
    private val cgExpertise: ChipGroup
    private val cgSkills: ChipGroup
    private val btnAction: Button
    private val btnMessage: Button
    private val btnBack: ImageButton

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_profile_view, this, true)
        tvFullName = findViewById(R.id.tvFullName)
        tvProfessionalTitle = findViewById(R.id.tvProfessionalTitle)
        tvRole = findViewById(R.id.tvRole)
        tvBio = findViewById(R.id.tvBio)
        tvEducation = findViewById(R.id.tvEducation)
        tvExperience = findViewById(R.id.tvExperience)
        tvLanguages = findViewById(R.id.tvLanguages)
        tvLocation = findViewById(R.id.tvLocation)
        tvAvailability = findViewById(R.id.tvAvailability)
        tvInitial = findViewById(R.id.tvInitial)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        cgExpertise = findViewById(R.id.cgExpertise)
        cgSkills = findViewById(R.id.cgSkills)
        btnAction = findViewById(R.id.btnAction)
        btnMessage = findViewById(R.id.btnMessage)
        btnBack = findViewById(R.id.btnBack)
    }

    fun init(
        userId: String,
        currentUser: UserProfile,
        repo: DataRepository,
        onBack: () -> Unit,
        onOpenConversation: (String, String) -> Unit
    ) {
        this.repo = repo
        this.currentUser = currentUser

        btnBack.setOnClickListener { onBack() }

        scope.launch {
            targetUser = withContext(Dispatchers.IO) { repo.users.getById(userId) }
            val user = targetUser ?: return@launch
            
            tvHeaderTitle.text = if (user.userId == currentUser.userId) "My Profile" else "Professional Profile"
            tvFullName.text = user.fullName
            
            if (user.professionalTitle.isNotBlank()) {
                tvProfessionalTitle.text = user.professionalTitle
                tvProfessionalTitle.visibility = View.VISIBLE
            } else {
                tvProfessionalTitle.visibility = View.GONE
            }

            tvRole.text = user.role.name
            tvBio.text = user.bio.ifBlank { "No bio provided." }
            tvEducation.text = user.qualification.ifBlank { "Not specified" }
            tvExperience.text = user.experience.ifBlank { "Not specified" }
            
            val langMap = mapOf("en" to "English", "xh" to "isiXhosa", "zu" to "isiZulu")
            tvLanguages.text = langMap[user.language] ?: "English"
            
            tvLocation.text = user.location.ifBlank { "Location unknown" }
            tvAvailability.text = user.availability.ifBlank { "Not specified" }
            tvInitial.text = user.fullName.take(1).uppercase()

            cgExpertise.removeAllViews()
            user.expertise.forEach { item ->
                cgExpertise.addView(createChip(item))
            }

            cgSkills.removeAllViews()
            user.skills.forEach { skill ->
                cgSkills.addView(createChip(skill))
            }

            setupActions(user, onOpenConversation)
        }
    }

    private fun createChip(label: String): Chip {
        return Chip(context).apply {
            text = label
            isClickable = false
        }
    }

    private suspend fun setupActions(user: UserProfile, onOpenConversation: (String, String) -> Unit) {
        if (user.userId == currentUser.userId) {
            btnAction.visibility = View.GONE
            btnMessage.visibility = View.GONE
            return
        }

        btnMessage.visibility = View.VISIBLE
        btnMessage.setOnClickListener {
            val convId = listOf(currentUser.userId, user.userId).sorted().joinToString("_")
            onOpenConversation(convId, user.userId)
        }

        if (user.role == UserRole.MENTOR && (currentUser.role == UserRole.STUDENT || currentUser.role == UserRole.GRADUATE)) {
            btnAction.visibility = View.VISIBLE
            val requests = withContext(Dispatchers.IO) { repo.mentorship.getForStudent(currentUser.userId) }
            val existingReq = requests.find { it.mentorProfileId == user.userId }
            
            if (existingReq != null) {
                btnAction.text = "Status: ${existingReq.status.name}"
                btnAction.isEnabled = false
            } else {
                btnAction.text = "Request Mentorship"
                btnAction.isEnabled = true
                btnAction.setOnClickListener {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            repo.mentorship.request(
                                MentorshipRequest(
                                    studentProfileId = currentUser.userId,
                                    mentorProfileId = user.userId,
                                    message = "Hi ${user.fullName}, I'd love your guidance."
                                )
                            )
                        }
                        btnAction.text = "Status: PENDING"
                        btnAction.isEnabled = false
                    }
                }
            }
        } else {
            btnAction.visibility = View.GONE
        }
    }
}
