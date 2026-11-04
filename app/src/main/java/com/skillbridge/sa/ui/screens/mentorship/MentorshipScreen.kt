package com.skillbridge.sa.ui.screens.mentorship

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.MentorshipRequest
import com.skillbridge.sa.data.model.MentorshipStatus
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MentorshipScreen(
    profile: UserProfile,
    repo: DataRepository,
    onOpenConversation: (String, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            MentorshipView(context).apply {
                init(profile, repo, onOpenConversation, onOpenProfile)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class MentorshipView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var adapter: MentorshipAdapter
    private lateinit var onOpenConversation: (String, String) -> Unit
    private lateinit var onOpenProfile: (String) -> Unit

    private val tvTitle: TextView
    private val tvSubtitle: TextView
    private val tvEmpty: TextView
    private val rvMentorship: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_mentorship, this, true)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvMentorship = findViewById(R.id.rvMentorship)
    }

    fun init(
        profile: UserProfile,
        repo: DataRepository,
        onOpenConversation: (String, String) -> Unit,
        onOpenProfile: (String) -> Unit
    ) {
        this.profile = profile
        this.repo = repo
        this.onOpenConversation = onOpenConversation
        this.onOpenProfile = onOpenProfile

        adapter = MentorshipAdapter(profile, { action, id ->
            handleAction(action, id)
        }, { otherUserId ->
            val convId = listOf(profile.userId, otherUserId).sorted().joinToString("_")
            onOpenConversation(convId, otherUserId)
        }, { userId ->
            onOpenProfile(userId)
        })
        rvMentorship.layoutManager = LinearLayoutManager(context)
        rvMentorship.adapter = adapter

        updateHeader()
        loadData()
    }

    private fun updateHeader() {
        if (profile.role == UserRole.MENTOR) {
            tvTitle.text = "Mentorship Requests"
            tvSubtitle.text = "Review requests and guide the next generation."
        } else {
            tvTitle.text = "Find & Connect With Mentors"
            tvSubtitle.text = "Get guidance from experienced professionals."
        }
    }

    private fun loadData() {
        scope.launch {
            if (profile.role == UserRole.MENTOR) {
                val requests = withContext(Dispatchers.IO) { repo.mentorship.getForMentor(profile.userId) }
                val users = withContext(Dispatchers.IO) { repo.users.getAll() }
                val names = users.associate { it.userId to it.fullName }
                
                if (requests.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No mentorship requests yet."
                    rvMentorship.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvMentorship.visibility = View.VISIBLE
                    adapter.submitMentorData(requests, names)
                }
            } else {
                val mentors = withContext(Dispatchers.IO) { repo.users.getMentors() }
                val myReqs = withContext(Dispatchers.IO) { repo.mentorship.getForStudent(profile.userId) }

                if (mentors.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "No mentors have joined yet — check back soon."
                    rvMentorship.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvMentorship.visibility = View.VISIBLE
                    adapter.submitStudentData(mentors, myReqs)
                }
            }
        }
    }

    private fun handleAction(action: String, id: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                when (action) {
                    "ACCEPT" -> {
                        repo.mentorship.updateStatus(id, MentorshipStatus.ACCEPTED)
                        val req = repo.mentorship.getForMentor(profile.userId).find { it.requestId == id }
                        if (req != null) {
                            repo.notifications.insert(
                                com.skillbridge.sa.data.model.AppNotification(
                                    userId = req.studentProfileId,
                                    type = com.skillbridge.sa.data.model.NotificationType.MENTORSHIP,
                                    title = "Mentorship Accepted",
                                    body = "${profile.fullName} accepted your mentorship request."
                                )
                            )
                            repo.notifications.insert(
                                com.skillbridge.sa.data.model.AppNotification(
                                    userId = profile.userId,
                                    type = com.skillbridge.sa.data.model.NotificationType.MENTORSHIP,
                                    title = "Mentorship Confirmed",
                                    body = "You accepted the mentorship request."
                                )
                            )
                        }
                        Unit
                    }
                    "REJECT" -> {
                        repo.mentorship.updateStatus(id, MentorshipStatus.REJECTED)
                        val req = repo.mentorship.getForMentor(profile.userId).find { it.requestId == id }
                        if (req != null) {
                            repo.notifications.insert(
                                com.skillbridge.sa.data.model.AppNotification(
                                    userId = req.studentProfileId,
                                    type = com.skillbridge.sa.data.model.NotificationType.MENTORSHIP,
                                    title = "Mentorship Declined",
                                    body = "${profile.fullName} declined your mentorship request."
                                )
                            )
                        }
                        Unit
                    }
                    "REQUEST" -> {
                        repo.mentorship.request(
                            MentorshipRequest(
                                studentProfileId = profile.userId,
                                mentorProfileId = id,
                                message = "Hi, I'd love your guidance on my career journey."
                            )
                        )
                        repo.notifications.insert(
                            com.skillbridge.sa.data.model.AppNotification(
                                userId = id,
                                type = com.skillbridge.sa.data.model.NotificationType.MENTORSHIP,
                                title = "New Mentorship Request",
                                body = "${profile.fullName} requested your mentorship."
                            )
                        )
                        repo.notifications.insert(
                            com.skillbridge.sa.data.model.AppNotification(
                                userId = profile.userId,
                                type = com.skillbridge.sa.data.model.NotificationType.MENTORSHIP,
                                title = "Mentorship Request Sent",
                                body = "Your mentorship request has been sent."
                            )
                        )
                    }
                    else -> {}
                }
            }
            loadData()
        }
    }

    class MentorshipAdapter(
        private val profile: UserProfile,
        private val onAction: (String, String) -> Unit,
        private val onMessage: (String) -> Unit,
        private val onProfile: (String) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var mentorRequests = listOf<MentorshipRequest>()
        private var studentNames = mapOf<String, String>()
        
        private var mentors = listOf<UserProfile>()
        private var myRequests = listOf<MentorshipRequest>()

        fun submitMentorData(reqs: List<MentorshipRequest>, names: Map<String, String>) {
            this.mentorRequests = reqs
            this.studentNames = names
            notifyDataSetChanged()
        }

        fun submitStudentData(mentors: List<UserProfile>, reqs: List<MentorshipRequest>) {
            this.mentors = mentors
            this.myRequests = reqs
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return if (profile.role == UserRole.MENTOR) {
                mentorRequests.size
            } else {
                mentors.size
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mentorship_card, parent, false)
            return MentorshipViewHolder(view, onAction, onMessage, onProfile)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val vh = holder as MentorshipViewHolder
            if (profile.role == UserRole.MENTOR) {
                val req = mentorRequests[position]
                vh.bindMentorView(req, studentNames[req.studentProfileId] ?: "Student")
            } else {
                val mentor = mentors[position]
                val alreadyRequested = myRequests.any { it.mentorProfileId == mentor.userId }
                vh.bindStudentView(mentor, alreadyRequested)
            }
        }
    }

    class MentorshipViewHolder(
        itemView: View,
        private val onAction: (String, String) -> Unit,
        private val onMessage: (String) -> Unit,
        private val onProfile: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bindMentorView(req: MentorshipRequest, studentName: String) {
            itemView.findViewById<TextView>(R.id.tvName).text = studentName
            itemView.findViewById<TextView>(R.id.tvDescription).text = req.message.ifBlank { "Requested your mentorship." }
            itemView.findViewById<TextView>(R.id.tvInitial).text = studentName.take(1).uppercase()
            
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = req.status.name
            
            val context = itemView.context
            val color = when (req.status) {
                MentorshipStatus.ACCEPTED -> context.getColor(R.color.sb_green)
                MentorshipStatus.REJECTED -> context.getColor(R.color.sb_error)
                else -> context.getColor(R.color.sb_gold)
            }
            tvStatus.setTextColor(color)

            val llActions = itemView.findViewById<View>(R.id.llActions)
            if (req.status == MentorshipStatus.PENDING) {
                llActions.visibility = View.VISIBLE
                itemView.findViewById<Button>(R.id.btnAccept).setOnClickListener { onAction("ACCEPT", req.requestId) }
                itemView.findViewById<Button>(R.id.btnDecline).setOnClickListener { onAction("REJECT", req.requestId) }
            } else {
                llActions.visibility = View.GONE
            }
            
            itemView.findViewById<View>(R.id.btnRequest).visibility = View.GONE
            itemView.findViewById<View>(R.id.btnMessage).setOnClickListener { onMessage(req.studentProfileId) }
            itemView.setOnClickListener { onProfile(req.studentProfileId) }
        }

        fun bindStudentView(mentor: UserProfile, alreadyRequested: Boolean) {
            itemView.findViewById<TextView>(R.id.tvName).text = mentor.fullName
            itemView.findViewById<TextView>(R.id.tvDescription).text = mentor.bio.ifBlank { mentor.careerInterest }
            itemView.findViewById<TextView>(R.id.tvInitial).text = mentor.fullName.take(1).uppercase()
            
            itemView.findViewById<View>(R.id.tvStatus).visibility = View.GONE
            itemView.findViewById<View>(R.id.llActions).visibility = View.GONE
            
            val btnRequest = itemView.findViewById<Button>(R.id.btnRequest)
            btnRequest.visibility = View.VISIBLE
            btnRequest.text = if (alreadyRequested) "Requested ✓" else "Request Mentorship"
            btnRequest.isEnabled = !alreadyRequested
            btnRequest.setOnClickListener { onAction("REQUEST", mentor.userId) }

            itemView.findViewById<View>(R.id.btnMessage).setOnClickListener { onMessage(mentor.userId) }
            itemView.setOnClickListener { onProfile(mentor.userId) }
        }
    }
}
