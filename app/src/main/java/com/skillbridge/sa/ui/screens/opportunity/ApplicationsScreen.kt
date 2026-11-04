package com.skillbridge.sa.ui.screens.opportunity

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.ApplicationStatus
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.JobApplication
import com.skillbridge.sa.data.model.NotificationType
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ApplicationsScreen(
    profile: UserProfile,
    repo: DataRepository,
    onOpenProfile: (String) -> Unit,
    onOpenConversation: ((String, String) -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            ApplicationsView(context).apply {
                init(profile, repo, onOpenProfile, onOpenConversation)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class ApplicationsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var onOpenProfile: (String) -> Unit
    private var onOpenConversation: ((String, String) -> Unit)? = null
    private lateinit var adapter: ApplicationsAdapter

    private val tvHeaderTitle: TextView?
    private val tvHeaderSubtitle: TextView?
    private val tvEmpty: TextView
    private val rvApplications: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_applications, this, true)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvApplications = findViewById(R.id.rvApplications)
    }

    fun init(
        profile: UserProfile,
        repo: DataRepository,
        onOpenProfile: (String) -> Unit,
        onOpenConversation: ((String, String) -> Unit)? = null
    ) {
        this.profile = profile
        this.repo = repo
        this.onOpenProfile = onOpenProfile
        this.onOpenConversation = onOpenConversation

        if (profile.role == UserRole.EMPLOYER || profile.role == UserRole.ADMIN) {
            tvHeaderTitle?.text = "Received Applications"
            tvHeaderSubtitle?.text = "Review applications submitted by Students and Graduates."
            tvEmpty.text = "No candidate applications received yet."
        } else {
            tvHeaderTitle?.text = "My Applications"
            tvHeaderSubtitle?.text = "Track the status of every opportunity you've applied for."
            tvEmpty.text = "You haven't applied to anything yet."
        }
        
        adapter = ApplicationsAdapter(profile, onOpenProfile) { app, opp ->
            showApplicantOptionsDialog(app, opp)
        }
        rvApplications.layoutManager = LinearLayoutManager(context)
        rvApplications.adapter = adapter
        
        loadApplications()
    }

    private fun showApplicantOptionsDialog(app: JobApplication, opp: Opportunity?) {
        val options = arrayOf("Update Application Status", "View Candidate Profile", "Send Direct Message")
        AlertDialog.Builder(context)
            .setTitle("Applicant Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStatusUpdatePicker(app, opp)
                    1 -> onOpenProfile(app.studentProfileId)
                    2 -> {
                        val convId = listOf(profile.userId, app.studentProfileId).sorted().joinToString("_")
                        onOpenConversation?.invoke(convId, app.studentProfileId)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStatusUpdatePicker(app: JobApplication, opp: Opportunity?) {
        val statuses = ApplicationStatus.entries.toTypedArray()
        val statusNames = statuses.map { it.name.replace('_', ' ') }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Select New Status")
            .setItems(statusNames) { _, which ->
                val newStatus = statuses[which]
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.applications.updateStatus(app.applicationId, newStatus)
                        repo.notifications.insert(
                            AppNotification(
                                userId = app.studentProfileId,
                                type = NotificationType.APPLICATION_UPDATE,
                                title = "Application Status Updated",
                                body = "Your application for ${opp?.title ?: "Opportunity"} was updated to ${newStatus.name.replace('_', ' ')}."
                            )
                        )
                    }
                    loadApplications()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadApplications() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                if (profile.role == UserRole.EMPLOYER || profile.role == UserRole.ADMIN) {
                    repo.applications.getForEmployer(profile.userId)
                } else {
                    repo.applications.getForStudent(profile.userId)
                }
            }
            val opps = withContext(Dispatchers.IO) { repo.opportunities.getAll().associateBy { it.opportunityId } }
            val users = withContext(Dispatchers.IO) { repo.users.getAll().associateBy { it.userId } }
            
            if (apps.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvApplications.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvApplications.visibility = View.VISIBLE
                adapter.submitList(apps, opps, users)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (scope.coroutineContext[kotlinx.coroutines.Job] as? kotlinx.coroutines.Job)?.cancel()
    }

    class ApplicationsAdapter(
        private val currentUser: UserProfile,
        private val onProfile: (String) -> Unit,
        private val onUpdateStatus: (JobApplication, Opportunity?) -> Unit
    ) : RecyclerView.Adapter<ApplicationViewHolder>() {
        private var items = listOf<JobApplication>()
        private var opportunities = mapOf<String, Opportunity>()
        private var users = mapOf<String, UserProfile>()
        
        fun submitList(newList: List<JobApplication>, opps: Map<String, Opportunity>, userMap: Map<String, UserProfile>) {
            items = newList
            opportunities = opps
            users = userMap
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_application, parent, false)
            return ApplicationViewHolder(view, currentUser, onProfile, onUpdateStatus)
        }
        override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
            val app = items[position]
            holder.bind(app, opportunities[app.opportunityId], users[app.studentProfileId])
        }
        override fun getItemCount() = items.size
    }

    class ApplicationViewHolder(
        itemView: View,
        private val currentUser: UserProfile,
        private val onProfile: (String) -> Unit,
        private val onUpdateStatus: (JobApplication, Opportunity?) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(app: JobApplication, opp: Opportunity?, student: UserProfile?) {
            val tvTitle = itemView.findViewById<TextView>(R.id.tvOppTitle)
            val tvSubtitle = itemView.findViewById<TextView>(R.id.tvCompany)
            
            if (currentUser.role == UserRole.EMPLOYER || currentUser.role == UserRole.ADMIN) {
                val roleTag = student?.role?.name ?: "CANDIDATE"
                tvTitle.text = "${student?.fullName ?: "Applicant"} ($roleTag)"
                tvSubtitle.text = "Applied for: ${opp?.title ?: "Job"}"
                itemView.setOnClickListener { onUpdateStatus(app, opp) }
            } else {
                tvTitle.text = opp?.title ?: "Opportunity"
                tvSubtitle.text = opp?.company ?: ""
                itemView.setOnClickListener { onProfile(app.studentProfileId) }
            }
            
            itemView.findViewById<TextView>(R.id.tvStatus).text = "Status: ${app.status.name.replace('_', ' ')}"
        }
    }
}
