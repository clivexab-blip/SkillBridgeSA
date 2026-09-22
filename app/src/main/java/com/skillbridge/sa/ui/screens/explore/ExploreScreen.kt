package com.skillbridge.sa.ui.screens.explore

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.OpportunityType
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExploreScreen(
    repo: DataRepository,
    currentUser: UserProfile,
    onOpenOpportunity: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            ExploreView(context).apply {
                init(repo, currentUser, onOpenOpportunity, onOpenProfile)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update logic if needed
        }
    )
}

class ExploreView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var currentUser: UserProfile
    private lateinit var onOpenOpportunity: (String) -> Unit
    private lateinit var onOpenProfile: (String) -> Unit
    private lateinit var adapter: OpportunityAdapter
    private lateinit var userAdapter: UserAdapter

    private val etSearch: EditText
    private val cgFilters: ChipGroup
    private val tvResultsCount: TextView
    private val rvOpportunities: RecyclerView

    private var currentKeyword = ""
    private var currentType: OpportunityType? = null

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_explore, this, true)
        etSearch = findViewById(R.id.etSearch)
        cgFilters = findViewById(R.id.cgFilters)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        rvOpportunities = findViewById(R.id.rvOpportunities)
    }

    fun init(repo: DataRepository, currentUser: UserProfile, onOpenOpportunity: (String) -> Unit, onOpenProfile: (String) -> Unit) {
        this.repo = repo
        this.currentUser = currentUser
        this.onOpenOpportunity = onOpenOpportunity
        this.onOpenProfile = onOpenProfile
        
        adapter = OpportunityAdapter(onOpenOpportunity)
        userAdapter = UserAdapter(onOpenProfile)
        
        rvOpportunities.layoutManager = LinearLayoutManager(context)
        rvOpportunities.adapter = adapter

        if (currentUser.role == UserRole.EMPLOYER || currentUser.role == UserRole.ADMIN) {
            findViewById<View>(R.id.chipTalent).visibility = View.VISIBLE
        }

        setupUI()
        loadResults()
    }

    private fun setupUI() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentKeyword = s?.toString() ?: ""
                loadResults()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cgFilters.setOnCheckedChangeListener { group, checkedId ->
            currentType = when (checkedId) {
                R.id.chipJobs -> OpportunityType.JOB
                R.id.chipInternships -> OpportunityType.INTERNSHIP
                R.id.chipGraduate -> OpportunityType.GRADUATE_PROGRAMME
                else -> null
            }
            if (checkedId == R.id.chipAll && currentUser.role == UserRole.EMPLOYER) {
                // For employers, maybe "All" includes users? 
                // Let's just switch adapters if a "Talent" chip exists.
            }
            loadResults()
        }
    }

    private fun loadResults() {
        scope.launch {
            if (cgFilters.checkedChipId == R.id.chipTalent) {
                val results = withContext(Dispatchers.IO) {
                    repo.users.search(currentKeyword)
                }
                tvResultsCount.text = "${results.size} Candidates Found"
                rvOpportunities.adapter = userAdapter
                userAdapter.submitList(results)
            } else {
                val results = withContext(Dispatchers.IO) {
                    repo.opportunities.search(keyword = currentKeyword, type = currentType)
                }
                tvResultsCount.text = "${results.size} Results Found"
                rvOpportunities.adapter = adapter
                adapter.submitList(results)
            }
        }
    }

    class UserAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<UserViewHolder>() {
        private var items = listOf<UserProfile>()
        fun submitList(newList: List<UserProfile>) {
            items = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view, onItemClick)
        }
        override fun onBindViewHolder(holder: UserViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    class UserViewHolder(itemView: View, private val onClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: UserProfile) {
            itemView.findViewById<TextView>(R.id.tvUserName).text = user.fullName
            itemView.findViewById<TextView>(R.id.tvUserEmail).text = user.email
            
            val tvBadge = itemView.findViewById<TextView>(R.id.tvRoleBadge)
            tvBadge.text = user.role.name.lowercase().replaceFirstChar { it.uppercase() }
            
            itemView.setOnClickListener { onClick(user.userId) }
        }
    }

    class OpportunityAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<OpportunityViewHolder>() {
        private var items = listOf<Opportunity>()
        fun submitList(newList: List<Opportunity>) {
            items = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpportunityViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_opportunity, parent, false)
            return OpportunityViewHolder(view, onItemClick)
        }
        override fun onBindViewHolder(holder: OpportunityViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    class OpportunityViewHolder(itemView: View, private val onClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        fun bind(opp: Opportunity) {
            itemView.findViewById<TextView>(R.id.tvOppTitle).text = opp.title
            itemView.findViewById<TextView>(R.id.tvOppCompany).text = opp.company
            itemView.findViewById<TextView>(R.id.tvOppLocation).text = opp.location
            
            val tvSalary = itemView.findViewById<TextView>(R.id.tvOppSalary)
            if (opp.salaryRange.isNotBlank()) {
                tvSalary.visibility = View.VISIBLE
                tvSalary.text = opp.salaryRange
            } else {
                tvSalary.visibility = View.GONE
            }

            val cg = itemView.findViewById<ChipGroup>(R.id.cgRequirements)
            cg.removeAllViews()
            opp.requirements.take(3).forEach { skill ->
                val chip = Chip(itemView.context).apply {
                    text = skill
                    isClickable = false
                    isCheckable = false
                }
                cg.addView(chip)
            }

            itemView.setOnClickListener { onClick(opp.opportunityId) }
        }
    }
}
