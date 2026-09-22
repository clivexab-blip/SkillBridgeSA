package com.skillbridge.sa.ui.screens.notifications

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
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NotificationsScreen(profile: UserProfile, repo: DataRepository) {
    AndroidView(
        factory = { context ->
            NotificationsView(context).apply {
                init(profile, repo)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class NotificationsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var adapter: NotificationAdapter
    
    private val tvEmpty: TextView
    private val rvNotifications: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_notifications, this, true)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvNotifications = findViewById(R.id.rvNotifications)
    }

    fun init(profile: UserProfile, repo: DataRepository) {
        this.profile = profile
        this.repo = repo
        
        adapter = NotificationAdapter { note ->
            scope.launch {
                withContext(Dispatchers.IO) { repo.notifications.markRead(note.notificationId) }
                loadNotifications()
            }
        }
        rvNotifications.layoutManager = LinearLayoutManager(context)
        rvNotifications.adapter = adapter
        
        loadNotifications()
    }

    private fun loadNotifications() {
        scope.launch {
            val notes = withContext(Dispatchers.IO) { repo.notifications.getForUser(profile.userId) }
            if (notes.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvNotifications.visibility = View.VISIBLE
                adapter.submitList(notes)
            }
        }
    }

    class NotificationAdapter(private val onNoteClick: (AppNotification) -> Unit) : RecyclerView.Adapter<NotificationViewHolder>() {
        private var items = listOf<AppNotification>()
        fun submitList(newList: List<AppNotification>) {
            items = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view, onNoteClick)
        }
        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    class NotificationViewHolder(itemView: View, private val onClick: (AppNotification) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(note: AppNotification) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = note.title
            itemView.findViewById<TextView>(R.id.tvBody).text = note.body
            
            val card = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardContainer)
            val context = itemView.context
            if (note.isRead) {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.sb_white)))
                card.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
            } else {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.sb_green_light)))
                card.strokeWidth = 0
            }

            itemView.setOnClickListener { onClick(note) }
        }
    }
}
