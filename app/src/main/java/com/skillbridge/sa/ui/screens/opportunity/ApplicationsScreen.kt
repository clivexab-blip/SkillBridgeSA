package com.skillbridge.sa.ui.screens.opportunity

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
import com.skillbridge.sa.data.model.JobApplication
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.UserProfile
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
    onOpenProfile: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            ApplicationsView(context).apply {
                init(profile, repo, onOpenProfile)
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
    private lateinit var adapter: ApplicationsAdapter

    private val tvEmpty: TextView
    private val rvApplications: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_applications, this, true)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvApplications = findViewById(R.id.rvApplications)
    }

    fun init(profile: UserProfile, repo: DataRepository, onOpenProfile: (String) -> Unit) {
        this.profile = profile
        this.repo = repo
        this.onOpenProfile = onOpenProfile
        
        adapter = ApplicationsAdapter(profile, onOpenProfile)
        rvApplications.layoutManager = LinearLayoutManager(context)
        rvApplications.adapter = adapter
        
        loadApplications()
    }

    private fun loadApplications() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                if (profile.role == com.skillbridge.sa.data.model.UserRole.EMPLOYER) {
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

    class ApplicationsAdapter(private val currentUser: UserProfile, private val onProfile: (String) -> Unit) : RecyclerView.Adapter<ApplicationViewHolder>() {
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
            return ApplicationViewHolder(view, currentUser, onProfile)
        }
        override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
            val app = items[position]
            holder.bind(app, opportunities[app.opportunityId], users[app.studentProfileId])
        }
        override fun getItemCount() = items.size
    }

    class ApplicationViewHolder(itemView: View, private val currentUser: UserProfile, private val onProfile: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(app: JobApplication, opp: Opportunity?, student: UserProfile?) {
            val tvTitle = itemView.findViewById<TextView>(R.id.tvOppTitle)
            val tvSubtitle = itemView.findViewById<TextView>(R.id.tvCompany)
            
            if (currentUser.role == com.skillbridge.sa.data.model.UserRole.EMPLOYER) {
                tvTitle.text = student?.fullName ?: "Unknown Applicant"
                tvSubtitle.text = "Applied for: ${opp?.title ?: "Job"}"
                itemView.setOnClickListener { onProfile(app.studentProfileId) }
            } else {
                tvTitle.text = opp?.title ?: "Opportunity"
                tvSubtitle.text = opp?.company ?: ""
            }
            
            itemView.findViewById<TextView>(R.id.tvStatus).text = "Status: ${app.status.name.replace('_', ' ')}"
        }
    }
}
