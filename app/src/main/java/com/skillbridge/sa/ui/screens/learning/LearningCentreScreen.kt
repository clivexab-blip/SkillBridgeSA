package com.skillbridge.sa.ui.screens.learning

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
import com.google.android.material.chip.Chip
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.LearningResource
import com.skillbridge.sa.data.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LearningCentreScreen(repo: DataRepository) {
    AndroidView(
        factory = { context ->
            LearningCentreView(context).apply {
                init(repo)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class LearningCentreView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repo: DataRepository
    private lateinit var adapter: LearningResourceAdapter
    private val rvLearningResources: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_learning_centre, this, true)
        rvLearningResources = findViewById(R.id.rvLearningResources)
    }

    fun init(repo: DataRepository) {
        this.repo = repo
        adapter = LearningResourceAdapter()
        rvLearningResources.layoutManager = LinearLayoutManager(context)
        rvLearningResources.adapter = adapter
        
        loadResources()
    }

    private fun loadResources() {
        scope.launch {
            val resources = withContext(Dispatchers.IO) { repo.learningResources.getAll() }
            adapter.submitList(resources)
        }
    }

    class LearningResourceAdapter : RecyclerView.Adapter<LearningResourceViewHolder>() {
        private var items = listOf<LearningResource>()
        fun submitList(newList: List<LearningResource>) {
            items = newList
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LearningResourceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_learning_resource, parent, false)
            return LearningResourceViewHolder(view)
        }
        override fun onBindViewHolder(holder: LearningResourceViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }

    class LearningResourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(res: LearningResource) {
            itemView.findViewById<TextView>(R.id.tvResTitle).text = res.title
            itemView.findViewById<TextView>(R.id.tvResDescription).text = res.description
            itemView.findViewById<Chip>(R.id.chipCategory).text = res.category
        }
    }
}
