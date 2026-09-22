package com.skillbridge.sa.ui.screens.admin

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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminUsersScreen(repo: DataRepository) {
    AndroidView(
        factory = { context ->
            AdminUsersView(context).apply {
                init(repo)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.refreshData()
        }
    )
}

class AdminUsersView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var repo: DataRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var adapter: UserAdapter

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_admin_users, this, true)
    }

    fun init(repository: DataRepository) {
        this.repo = repository
        setupRecyclerView()
        refreshData()
    }

    private fun setupRecyclerView() {
        val rvUsers = findViewById<RecyclerView>(R.id.rvUsers)
        adapter = UserAdapter()
        rvUsers.layoutManager = LinearLayoutManager(context)
        rvUsers.adapter = adapter
    }

    fun refreshData() {
        scope.launch {
            val users = withContext(Dispatchers.IO) {
                repo.users.getAll()
            }
            updateUI(users)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.launch {
            // Cancel is better handled by a Job or using a custom lifecycle
        }
        // Actually, just cancel the scope's job
        (scope.coroutineContext[kotlinx.coroutines.Job] as? kotlinx.coroutines.Job)?.cancel()
    }

    private fun updateUI(users: List<UserProfile>) {
        val studentCount = users.count { it.role == UserRole.STUDENT }
        val graduateCount = users.count { it.role == UserRole.GRADUATE }
        val mentorCount = users.count { it.role == UserRole.MENTOR }
        val employerCount = users.count { it.role == UserRole.EMPLOYER }

        findViewById<View>(R.id.statStudents).findViewById<TextView>(R.id.tvStatValue).text = studentCount.toString()
        findViewById<View>(R.id.statStudents).findViewById<TextView>(R.id.tvStatLabel).setText(R.string.role_student)

        findViewById<View>(R.id.statGraduates).findViewById<TextView>(R.id.tvStatValue).text = graduateCount.toString()
        findViewById<View>(R.id.statGraduates).findViewById<TextView>(R.id.tvStatLabel).setText(R.string.role_graduate)

        findViewById<View>(R.id.statMentors).findViewById<TextView>(R.id.tvStatValue).text = mentorCount.toString()
        findViewById<View>(R.id.statMentors).findViewById<TextView>(R.id.tvStatLabel).setText(R.string.role_mentor)

        findViewById<View>(R.id.statEmployers).findViewById<TextView>(R.id.tvStatValue).text = employerCount.toString()
        findViewById<View>(R.id.statEmployers).findViewById<TextView>(R.id.tvStatLabel).setText(R.string.role_employer)

        adapter.submitList(users)
    }

    class UserAdapter : RecyclerView.Adapter<UserViewHolder>() {
        private var users = listOf<UserProfile>()

        fun submitList(newList: List<UserProfile>) {
            users = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            holder.bind(users[position])
        }

        override fun getItemCount() = users.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvUserName)
        private val tvEmail = itemView.findViewById<TextView>(R.id.tvUserEmail)
        private val tvBadge = itemView.findViewById<TextView>(R.id.tvRoleBadge)

        fun bind(user: UserProfile) {
            tvName.text = user.fullName.ifBlank { "Unnamed user" }
            tvEmail.text = user.email
            tvBadge.text = user.role.name.lowercase().replaceFirstChar { it.uppercase() }

            val colorRes = when (user.role) {
                UserRole.STUDENT -> R.color.sb_green
                UserRole.GRADUATE -> R.color.sb_blue_graduate
                UserRole.MENTOR -> R.color.sb_purple_mentor
                UserRole.EMPLOYER -> R.color.sb_navy
                UserRole.ADMIN -> R.color.sb_error
            }
            tvBadge.text = user.role.name
            tvBadge.backgroundTintList = ContextCompat.getColorStateList(itemView.context, colorRes)
        }
    }
}
