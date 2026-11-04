package com.skillbridge.sa.ui.screens.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.JobApplication
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    repo: DataRepository,
    onOpenExplore: () -> Unit,
    onOpenLearning: () -> Unit,
    onOpenMentorship: () -> Unit,
    onOpenCareerPathways: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenAdminUsers: () -> Unit,
    onOpenMessages: () -> Unit
) {
    AndroidView(
        factory = { context ->
            HomeDashboardView(context).apply {
                init(
                    profile = profile,
                    repo = repo,
                    onOpenExplore = onOpenExplore,
                    onOpenLearning = onOpenLearning,
                    onOpenMentorship = onOpenMentorship,
                    onOpenCareerPathways = onOpenCareerPathways,
                    onOpenApplications = onOpenApplications,
                    onOpenAdminUsers = onOpenAdminUsers,
                    onOpenMessages = onOpenMessages
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.updateProfile(profile)
        }
    )
}

class HomeDashboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var profile: UserProfile
    private lateinit var repo: DataRepository
    
    private lateinit var onOpenExplore: () -> Unit
    private lateinit var onOpenLearning: () -> Unit
    private lateinit var onOpenMentorship: () -> Unit
    private lateinit var onOpenCareerPathways: () -> Unit
    private lateinit var onOpenApplications: () -> Unit
    private lateinit var onOpenAdminUsers: () -> Unit
    private lateinit var onOpenMessages: () -> Unit

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_home_dashboard, this, true)
    }

    fun init(
        profile: UserProfile,
        repo: DataRepository,
        onOpenExplore: () -> Unit,
        onOpenLearning: () -> Unit,
        onOpenMentorship: () -> Unit,
        onOpenCareerPathways: () -> Unit,
        onOpenApplications: () -> Unit,
        onOpenAdminUsers: () -> Unit,
        onOpenMessages: () -> Unit
    ) {
        this.profile = profile
        this.repo = repo
        this.onOpenExplore = onOpenExplore
        this.onOpenLearning = onOpenLearning
        this.onOpenMentorship = onOpenMentorship
        this.onOpenCareerPathways = onOpenCareerPathways
        this.onOpenApplications = onOpenApplications
        this.onOpenAdminUsers = onOpenAdminUsers
        this.onOpenMessages = onOpenMessages

        setupUI()
        loadData()
    }

    fun updateProfile(newProfile: UserProfile) {
        this.profile = newProfile
        setupUI()
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.tvRoleBadge).apply {
            text = when (profile.role) {
                UserRole.STUDENT -> context.getString(R.string.role_student)
                UserRole.GRADUATE -> context.getString(R.string.role_graduate)
                UserRole.MENTOR -> context.getString(R.string.role_mentor)
                UserRole.EMPLOYER -> context.getString(R.string.role_employer)
                UserRole.ADMIN -> context.getString(R.string.role_admin)
            }
            val color = when (profile.role) {
                UserRole.STUDENT -> context.getColor(R.color.sb_green)
                UserRole.GRADUATE -> context.getColor(R.color.sb_blue_graduate)
                UserRole.MENTOR -> context.getColor(R.color.sb_purple_mentor)
                UserRole.EMPLOYER -> context.getColor(R.color.sb_navy)
                UserRole.ADMIN -> Color(0xFFB0272E).toArgb()
            }
            background.setTint(color)
        }

        findViewById<TextView>(R.id.tvUserName).text = profile.fullName.ifBlank { context.getString(R.string.app_name) }
        findViewById<TextView>(R.id.tvProfileComplete).text = "${profile.profileComplete}%"

        val overviewTitle = when (profile.role) {
            UserRole.MENTOR -> "Mentorship Overview"
            UserRole.EMPLOYER -> "Recruitment Overview"
            UserRole.ADMIN -> "Platform Overview"
            else -> "Career Overview"
        }
        findViewById<View>(R.id.headerOverview).findViewById<TextView>(R.id.tvSectionTitle).text = overviewTitle
        findViewById<View>(R.id.headerQuickActions).findViewById<TextView>(R.id.tvSectionTitle).text = "Quick Actions"

        setupQuickActions()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (scope.coroutineContext[kotlinx.coroutines.Job] as? kotlinx.coroutines.Job)?.cancel()
    }

    private fun setupQuickActions() {
        val actions = when (profile.role) {
            UserRole.STUDENT -> listOf(
                QuickAction("Find Jobs", "Browse all careers", onOpenExplore),
                QuickAction("Internships", "Student placements", onOpenExplore),
                QuickAction("Find a Mentor", "Get guidance", onOpenMentorship),
                QuickAction("My Applications", "Track your status", onOpenApplications),
                QuickAction("Learning Centre", "Build new skills", onOpenLearning),
                QuickAction("Career Pathways", "Guided roadmap", onOpenCareerPathways)
            )
            UserRole.GRADUATE -> listOf(
                QuickAction("Find Jobs", "Entry-level roles", onOpenExplore),
                QuickAction("Grad Programmes", "2026 intakes", onOpenExplore),
                QuickAction("Find a Mentor", "Career coaching", onOpenMentorship),
                QuickAction("My Applications", "Track your status", onOpenApplications),
                QuickAction("Learning Centre", "Upskill now", onOpenLearning),
                QuickAction("Career Pathways", "Professional roadmap", onOpenCareerPathways)
            )
            UserRole.MENTOR -> listOf(
                QuickAction("Mentorship Requests", "Review new applicants", onOpenMentorship),
                QuickAction("Active Mentees", "Manage your team", onOpenMentorship),
                QuickAction("Schedule Session", "Time management", onOpenMentorship),
                QuickAction("Direct Messages", "Chat with mentees", onOpenMessages),
                QuickAction("Learning Resources", "Share knowledge", onOpenLearning)
            )
            UserRole.EMPLOYER -> listOf(
                QuickAction("Post a Job", "Create new opening", onOpenExplore),
                QuickAction("Manage My Jobs", "Edit or close posts", onOpenExplore),
                QuickAction("View Candidates", "Discover talent", onOpenExplore),
                QuickAction("Review Applications", "Hire the best", onOpenApplications),
                QuickAction("Direct Messages", "Chat with talent", onOpenMessages)
            )
            UserRole.ADMIN -> listOf(
                QuickAction("Manage Users", "Moderate platform", onOpenAdminUsers),
                QuickAction("Opportunities", "Review job posts", onOpenExplore),
                QuickAction("Platform Stats", "Data & metrics", { /* Admin stats */ })
            )
        }

        val rv = findViewById<RecyclerView>(R.id.rvQuickActions)
        rv.layoutManager = GridLayoutManager(context, 2)
        rv.adapter = QuickActionAdapter(actions)
    }

    private fun loadData() {
        val pb = findViewById<ProgressBar>(R.id.pbOverview)
        pb.visibility = View.VISIBLE
        
        scope.launch {
            val opps = withContext(Dispatchers.IO) { repo.opportunities.getAll() }
            val apps = withContext(Dispatchers.IO) {
                when (profile.role) {
                    UserRole.STUDENT, UserRole.GRADUATE -> repo.applications.getForStudent(profile.userId)
                    UserRole.EMPLOYER -> repo.applications.getForEmployer(profile.userId)
                    else -> emptyList()
                }
            }
            val mentorReqs = withContext(Dispatchers.IO) {
                when (profile.role) {
                    UserRole.STUDENT, UserRole.GRADUATE -> repo.mentorship.getForStudent(profile.userId)
                    UserRole.MENTOR -> repo.mentorship.getForMentor(profile.userId)
                    else -> emptyList()
                }
            }

            withContext(Dispatchers.Main) {
                pb.visibility = View.GONE
                updateStats(opps.size, apps, mentorReqs)
                updateRecentApplications(apps)
            }
        }
    }

    private fun updateStats(oppCount: Int, apps: List<JobApplication>, mentorReqs: List<com.skillbridge.sa.data.model.MentorshipRequest>) {
        val llStats = findViewById<LinearLayout>(R.id.llStats)
        llStats.removeAllViews()

        val stats = when (profile.role) {
            UserRole.STUDENT, UserRole.GRADUATE -> listOf(
                Stat(apps.size.toString(), "Applications"),
                Stat(mentorReqs.count { it.status == com.skillbridge.sa.data.model.MentorshipStatus.ACCEPTED }.toString(), "Active Mentors"),
                Stat(oppCount.toString(), "New Opportunities")
            )
            UserRole.MENTOR -> listOf(
                Stat(mentorReqs.count { it.status == com.skillbridge.sa.data.model.MentorshipStatus.PENDING }.toString(), "Pending Req"),
                Stat(mentorReqs.count { it.status == com.skillbridge.sa.data.model.MentorshipStatus.ACCEPTED }.toString(), "Active Mentees"),
                Stat("0", "Total Sessions")
            )
            UserRole.EMPLOYER -> {
                val myOpps = oppCount // Filtering would be better
                listOf(
                    Stat(myOpps.toString(), "Active Posts"),
                    Stat(apps.size.toString(), "Applications"),
                    Stat(apps.count { it.status == com.skillbridge.sa.data.model.ApplicationStatus.SHORTLISTED }.toString(), "Shortlisted")
                )
            }
            UserRole.ADMIN -> listOf(
                Stat(oppCount.toString(), "Total Jobs"),
                Stat("0", "Active Users")
            )
        }

        stats.forEach { stat ->
            val view = LayoutInflater.from(context).inflate(R.layout.layout_stat_chip, llStats, false)
            view.findViewById<TextView>(R.id.tvStatValue).text = stat.value
            view.findViewById<TextView>(R.id.tvStatLabel).text = stat.label
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(4, 0, 4, 0)
            llStats.addView(view, params)
        }
    }

    private fun updateRecentApplications(apps: List<JobApplication>) {
        val header = findViewById<View>(R.id.headerRecentApplications)
        val container = findViewById<LinearLayout>(R.id.llRecentApplications)
        
        if (profile.role == UserRole.STUDENT || profile.role == UserRole.GRADUATE) {
            header.visibility = View.VISIBLE
            header.findViewById<TextView>(R.id.tvSectionTitle).text = "Recent Applications"
            container.visibility = View.VISIBLE
            container.removeAllViews()

            if (apps.isEmpty()) {
                val tv = TextView(context).apply {
                    text = "No applications yet — explore opportunities to get started."
                    setTextColor(context.getColor(R.color.sb_text_secondary))
                    setPadding(0, 10, 0, 10)
                }
                container.addView(tv)
            } else {
                apps.take(3).forEach { app ->
                    val card = LayoutInflater.from(context).inflate(R.layout.item_user, container, false)
                    card.findViewById<TextView>(R.id.tvUserName).text = "Application"
                    card.findViewById<TextView>(R.id.tvUserEmail).text = "Status: ${app.status.name.replace('_', ' ')}"
                    card.findViewById<View>(R.id.tvRoleBadge).visibility = View.GONE
                    card.setOnClickListener { onOpenApplications() }
                    container.addView(card)
                }
            }
        } else if (profile.role == UserRole.EMPLOYER || profile.role == UserRole.ADMIN) {
            header.visibility = View.VISIBLE
            header.findViewById<TextView>(R.id.tvSectionTitle).text = "Recent Candidate Submissions"
            container.visibility = View.VISIBLE
            container.removeAllViews()

            if (apps.isEmpty()) {
                val tv = TextView(context).apply {
                    text = "No candidate applications received yet."
                    setTextColor(context.getColor(R.color.sb_text_secondary))
                    setPadding(0, 10, 0, 10)
                }
                container.addView(tv)
            } else {
                scope.launch {
                    val users = withContext(Dispatchers.IO) { repo.users.getAll().associateBy { it.userId } }
                    val opps = withContext(Dispatchers.IO) { repo.opportunities.getAll().associateBy { it.opportunityId } }
                    withContext(Dispatchers.Main) {
                        container.removeAllViews()
                        apps.take(4).forEach { app ->
                            val student = users[app.studentProfileId]
                            val opp = opps[app.opportunityId]
                            val card = LayoutInflater.from(context).inflate(R.layout.item_user, container, false)
                            val name = student?.fullName ?: "Student Applicant"
                            val role = student?.role?.name ?: "STUDENT"
                            card.findViewById<TextView>(R.id.tvUserName).text = "$name ($role)"
                            card.findViewById<TextView>(R.id.tvUserEmail).text = "Applied for: ${opp?.title ?: "Job"} • Status: ${app.status.name.replace('_', ' ')}"
                            card.findViewById<View>(R.id.tvRoleBadge).visibility = View.GONE
                            card.setOnClickListener { onOpenApplications() }
                            container.addView(card)
                        }
                    }
                }
            }
        } else {
            header.visibility = View.GONE
            container.visibility = View.GONE
        }
    }

    data class Stat(val value: String, val label: String)
    data class QuickAction(val title: String, val subtitle: String, val onClick: () -> Unit)

    class QuickActionAdapter(private val actions: List<QuickAction>) : RecyclerView.Adapter<QuickActionViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickActionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_quick_action_card, parent, false)
            return QuickActionViewHolder(view)
        }
        override fun onBindViewHolder(holder: QuickActionViewHolder, position: Int) = holder.bind(actions[position])
        override fun getItemCount() = actions.size
    }

    class QuickActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(action: QuickAction) {
            itemView.findViewById<TextView>(R.id.tvActionTitle).text = action.title
            itemView.findViewById<TextView>(R.id.tvActionSubtitle).text = action.subtitle
            itemView.setOnClickListener { action.onClick() }
        }
    }
}
