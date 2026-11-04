package com.skillbridge.sa.ui.screens.opportunity

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.JobApplication
import com.skillbridge.sa.data.model.NotificationType
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.SavedOpportunity
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OpportunityDetailScreen(
    opportunityId: String,
    profile: UserProfile,
    repo: DataRepository,
    onOpenConversation: (String, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            OpportunityDetailView(context).apply {
                init(opportunityId, profile, repo, onOpenConversation, onOpenProfile)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class OpportunityDetailView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var onOpenConversation: (String, String) -> Unit
    private lateinit var onOpenProfile: (String) -> Unit
    private var opportunity: Opportunity? = null

    private val tvTitle: TextView
    private val tvCompany: TextView
    private val tvLocation: TextView
    private val tvSalary: TextView
    private val cgSkills: ChipGroup
    private val tvDescription: TextView
    private val btnApply: Button
    private val btnSave: Button
    private val btnMessage: Button

    private var applied = false
    private var saved = false

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_opportunity_detail, this, true)
        tvTitle = findViewById(R.id.tvTitle)
        tvCompany = findViewById(R.id.tvCompany)
        tvLocation = findViewById(R.id.tvLocation)
        tvSalary = findViewById(R.id.tvSalary)
        cgSkills = findViewById(R.id.cgSkills)
        tvDescription = findViewById(R.id.tvDescription)
        btnApply = findViewById(R.id.btnApply)
        btnSave = findViewById(R.id.btnSave)
        btnMessage = findViewById(R.id.btnMessage)
    }

    fun init(
        opportunityId: String,
        profile: UserProfile,
        repo: DataRepository,
        onOpenConversation: (String, String) -> Unit,
        onOpenProfile: (String) -> Unit
    ) {
        this.repo = repo
        this.profile = profile
        this.onOpenConversation = onOpenConversation
        this.onOpenProfile = onOpenProfile

        scope.launch {
            opportunity = withContext(Dispatchers.IO) { repo.opportunities.getById(opportunityId) }
            applied = withContext(Dispatchers.IO) {
                repo.applications.getForStudent(profile.userId).any { it.opportunityId == opportunityId }
            }
            saved = withContext(Dispatchers.IO) {
                repo.savedOpportunities.getForStudent(profile.userId).any { it.opportunityId == opportunityId }
            }
            bindUI()
        }
    }

    private fun bindUI() {
        val opp = opportunity ?: return
        tvTitle.text = opp.title
        tvCompany.text = opp.company
        tvLocation.text = opp.location

        tvCompany.setOnClickListener {
            if (opp.postedByUserId.isNotEmpty()) {
                onOpenProfile(opp.postedByUserId)
            }
        }
        
        if (opp.salaryRange.isNotBlank()) {
            tvSalary.visibility = View.VISIBLE
            tvSalary.text = opp.salaryRange
        } else {
            tvSalary.visibility = View.GONE
        }

        cgSkills.removeAllViews()
        opp.requirements.forEach { skill ->
            val chip = Chip(context).apply {
                text = skill
                isClickable = false
            }
            cgSkills.addView(chip)
        }

        tvDescription.text = opp.description

        updateApplyButton()
        updateSaveButton()

        btnApply.setOnClickListener { handleApply() }
        btnSave.setOnClickListener { handleSave() }
        btnMessage.setOnClickListener {
            val otherUserId = opp.postedByUserId
            if (otherUserId.isNotEmpty() && otherUserId != profile.userId) {
                val convId = listOf(profile.userId, otherUserId).sorted().joinToString("_")
                onOpenConversation(convId, otherUserId)
            }
        }
    }

    private fun handleApply() {
        val opp = opportunity ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                repo.applications.apply(
                    JobApplication(opportunityId = opp.opportunityId, studentProfileId = profile.userId)
                )
                // 1. Notify Applicant
                repo.notifications.insert(
                    AppNotification(
                        userId = profile.userId,
                        type = NotificationType.APPLICATION_UPDATE,
                        title = "Application Submitted",
                        body = "Your application for ${opp.title} at ${opp.company} was submitted."
                    )
                )
                // 2. Notify Employer
                if (opp.postedByUserId.isNotEmpty() && opp.postedByUserId != profile.userId) {
                    repo.notifications.insert(
                        AppNotification(
                            userId = opp.postedByUserId,
                            type = NotificationType.APPLICATION_UPDATE,
                            title = "New Job Application",
                            body = "${profile.fullName} applied for ${opp.title}."
                        )
                    )
                }
                // 3. Notify Admins
                val admins = repo.users.getAll().filter { it.role == com.skillbridge.sa.data.model.UserRole.ADMIN }
                admins.forEach { admin ->
                    repo.notifications.insert(
                        AppNotification(
                            userId = admin.userId,
                            type = NotificationType.SYSTEM,
                            title = "New Application Activity",
                            body = "${profile.fullName} applied for ${opp.title} at ${opp.company}."
                        )
                    )
                }
            }
            applied = true
            updateApplyButton()
        }
    }

    private fun handleSave() {
        val opp = opportunity ?: return
        scope.launch {
            withContext(Dispatchers.IO) {
                if (saved) {
                    repo.savedOpportunities.remove(profile.userId, opp.opportunityId)
                } else {
                    repo.savedOpportunities.save(
                        SavedOpportunity(studentProfileId = profile.userId, opportunityId = opp.opportunityId)
                    )
                }
            }
            saved = !saved
            updateSaveButton()
        }
    }

    private fun updateApplyButton() {
        btnApply.text = if (applied) "Applied ✓" else "Apply Now"
        btnApply.isEnabled = !applied
    }

    private fun updateSaveButton() {
        btnSave.text = if (saved) "Saved ✓" else "Save for Later"
    }
}
