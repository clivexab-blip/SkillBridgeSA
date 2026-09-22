package com.skillbridge.sa.ui.screens.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.ChatMessage
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MessagesListScreen(
    profile: UserProfile,
    repo: DataRepository,
    onOpenConversation: (String, String) -> Unit
) {
    AndroidView(
        factory = { context ->
            MessagesListView(context).apply {
                init(profile, repo, onOpenConversation)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

@Composable
fun ConversationScreen(
    conversationId: String,
    otherUserId: String,
    profile: UserProfile,
    repo: DataRepository,
    onBack: () -> Unit
) {
    AndroidView(
        factory = { context ->
            ConversationView(context).apply {
                init(conversationId, otherUserId, profile, repo, onBack)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class MessagesListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var onOpenConversation: (String, String) -> Unit
    private lateinit var adapter: ConversationAdapter

    private val tvEmpty: TextView
    private val rvConversations: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_messages_list, this, true)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvConversations = findViewById(R.id.rvConversations)
    }

    fun init(profile: UserProfile, repo: DataRepository, onOpenConversation: (String, String) -> Unit) {
        this.profile = profile
        this.repo = repo
        this.onOpenConversation = onOpenConversation

        adapter = ConversationAdapter(profile, onOpenConversation)
        rvConversations.layoutManager = LinearLayoutManager(context)
        rvConversations.adapter = adapter

        loadConversations()
    }

    private fun loadConversations() {
        scope.launch {
            val convs = withContext(Dispatchers.IO) { repo.messages.getConversationsForUser(profile.userId) }
            val users = withContext(Dispatchers.IO) { repo.users.getAll() }
            val names = users.associate { it.userId to it.fullName }
            
            if (convs.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvConversations.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvConversations.visibility = View.VISIBLE
                adapter.submitList(convs, names)
            }
        }
    }

    class ConversationAdapter(
        private val profile: UserProfile,
        private val onItemClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<ConversationViewHolder>() {
        private var items = listOf<ChatMessage>()
        private var namesById = mapOf<String, String>()

        fun submitList(newList: List<ChatMessage>, names: Map<String, String>) {
            items = newList
            namesById = names
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
            return ConversationViewHolder(view, profile, onItemClick)
        }

        override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
            val msg = items[position]
            val otherUserId = if (msg.senderUserId == profile.userId) msg.receiverUserId else msg.senderUserId
            holder.bind(msg, namesById[otherUserId] ?: "SkillBridge User")
        }

        override fun getItemCount() = items.size
    }

    class ConversationViewHolder(
        itemView: View,
        private val profile: UserProfile,
        private val onClick: (String, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage, otherUserName: String) {
            itemView.findViewById<TextView>(R.id.tvUserName).text = otherUserName
            itemView.findViewById<TextView>(R.id.tvLastMessage).text = msg.body
            itemView.findViewById<TextView>(R.id.tvInitial).text = otherUserName.take(1).uppercase()
            
            val otherUserId = if (msg.senderUserId == profile.userId) msg.receiverUserId else msg.senderUserId
            itemView.setOnClickListener { onClick(msg.conversationId, otherUserId) }
        }
    }
}

class ConversationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var profile: UserProfile
    private lateinit var conversationId: String
    private lateinit var otherUserId: String
    private lateinit var adapter: MessageAdapter

    private val rvMessages: RecyclerView
    private val etMessage: EditText
    private val btnSend: ImageButton
    private val tvHeaderName: TextView
    private val btnBack: ImageButton

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_conversation, this, true)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvHeaderName = findViewById(R.id.tvHeaderName)
        btnBack = findViewById(R.id.btnBack)
    }

    fun init(conversationId: String, otherUserId: String, profile: UserProfile, repo: DataRepository, onBack: () -> Unit) {
        this.conversationId = conversationId
        this.otherUserId = otherUserId
        this.profile = profile
        this.repo = repo

        adapter = MessageAdapter(profile.userId)
        rvMessages.layoutManager = LinearLayoutManager(context)
        rvMessages.adapter = adapter

        btnBack.setOnClickListener { onBack() }

        scope.launch {
            val user = withContext(Dispatchers.IO) { repo.users.getById(otherUserId) }
            tvHeaderName.text = user?.fullName ?: "SkillBridge User"
        }

        startPolling()

        btnSend.setOnClickListener {
            val body = etMessage.text.toString().trim()
            if (body.isNotEmpty()) {
                etMessage.text.clear()
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.messages.send(
                            ChatMessage(
                                conversationId = conversationId,
                                senderUserId = profile.userId,
                                receiverUserId = otherUserId,
                                body = body
                            )
                        )
                        repo.notifications.insert(
                            com.skillbridge.sa.data.model.AppNotification(
                                userId = otherUserId,
                                type = com.skillbridge.sa.data.model.NotificationType.MESSAGE,
                                title = "New Message",
                                body = "You have a new message from ${profile.fullName}"
                            )
                        )
                    }
                    loadMessages()
                }
            }
        }
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                loadMessages()
                kotlinx.coroutines.delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun loadMessages() {
        scope.launch {
            val msgs = withContext(Dispatchers.IO) { repo.messages.getConversation(conversationId) }
            // Only update if count changed or for initial load
            if (msgs.size != adapter.itemCount) {
                adapter.submitList(msgs)
                if (msgs.isNotEmpty()) {
                    rvMessages.post {
                        rvMessages.scrollToPosition(msgs.size - 1)
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (scope.coroutineContext[kotlinx.coroutines.Job] as? kotlinx.coroutines.Job)?.cancel()
    }

    class MessageAdapter(private val currentUserId: String) : RecyclerView.Adapter<MessageViewHolder>() {
        private var items = listOf<ChatMessage>()
        fun submitList(newList: List<ChatMessage>) {
            items = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view, currentUserId)
        }
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    class MessageViewHolder(itemView: View, private val currentUserId: String) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage) {
            val isMine = msg.senderUserId == currentUserId
            val tvBody = itemView.findViewById<TextView>(R.id.tvMessageBody)
            tvBody.text = msg.body
            
            val spacerStart = itemView.findViewById<View>(R.id.spacerStart)
            val spacerEnd = itemView.findViewById<View>(R.id.spacerEnd)

            if (isMine) {
                spacerStart.visibility = View.VISIBLE
                spacerEnd.visibility = View.GONE
                tvBody.setBackgroundResource(R.drawable.bg_message_mine)
                tvBody.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                spacerStart.visibility = View.GONE
                spacerEnd.visibility = View.VISIBLE
                tvBody.setBackgroundResource(R.drawable.bg_message_theirs)
                tvBody.setTextColor(itemView.context.getColor(R.color.sb_navy))
            }
        }
    }
}
