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
import com.google.android.material.chip.ChipGroup
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.CAREER_PATHWAYS
import com.skillbridge.sa.data.model.CareerPathway
import com.skillbridge.sa.data.model.UserProfile
import androidx.core.content.ContextCompat

@Composable
fun CareerPathwaysScreen(profile: UserProfile) {
    AndroidView(
        factory = { context ->
            CareerPathwaysView(context).apply {
                init(profile)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class CareerPathwaysView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var profile: UserProfile
    private lateinit var adapter: CareerPathwayAdapter
    private val rvCareerPathways: RecyclerView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_career_pathways, this, true)
        rvCareerPathways = findViewById(R.id.rvCareerPathways)
    }

    fun init(profile: UserProfile) {
        this.profile = profile
        adapter = CareerPathwayAdapter(profile)
        rvCareerPathways.layoutManager = LinearLayoutManager(context)
        rvCareerPathways.adapter = adapter
    }

    class CareerPathwayAdapter(private val profile: UserProfile) : RecyclerView.Adapter<CareerPathwayViewHolder>() {
        private var expandedIndex = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareerPathwayViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_career_pathway, parent, false)
            return CareerPathwayViewHolder(view, profile)
        }

        override fun onBindViewHolder(holder: CareerPathwayViewHolder, position: Int) {
            holder.bind(CAREER_PATHWAYS[position], position == expandedIndex) {
                val old = expandedIndex
                expandedIndex = if (expandedIndex == position) -1 else position
                if (old != -1) notifyItemChanged(old)
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = CAREER_PATHWAYS.size
    }

    class CareerPathwayViewHolder(itemView: View, private val profile: UserProfile) : RecyclerView.ViewHolder(itemView) {
        fun bind(pathway: CareerPathway, isExpanded: Boolean, onClick: () -> Unit) {
            itemView.findViewById<TextView>(R.id.tvCareerName).text = pathway.careerName
            itemView.findViewById<TextView>(R.id.tvDescription).text = pathway.description

            val llExpanded = itemView.findViewById<View>(R.id.llExpanded)
            llExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                val mySkills = profile.skills.map { it.lowercase() }
                val cg = itemView.findViewById<ChipGroup>(R.id.cgSkills)
                cg.removeAllViews()
                pathway.skills.take(4).forEach { skill ->
                    val known = skill.lowercase() in mySkills
                    val chip = Chip(itemView.context).apply {
                        text = skill
                        isClickable = false
                        if (known) {
                            setChipBackgroundColorResource(R.color.sb_green_light)
                            setTextColor(ContextCompat.getColor(context, R.color.sb_green))
                        }
                    }
                    cg.addView(chip)
                }

                val tvGap = itemView.findViewById<TextView>(R.id.tvSkillGap)
                val missingSkills = pathway.skills.filter { it.lowercase() !in mySkills }
                if (missingSkills.isEmpty()) {
                    tvGap.text = "Great news — your listed skills already cover this pathway."
                    tvGap.setTextColor(ContextCompat.getColor(itemView.context, R.color.sb_green))
                } else {
                    tvGap.text = "Skill gap: ${missingSkills.joinToString(", ")}"
                    tvGap.setTextColor(ContextCompat.getColor(itemView.context, R.color.sb_text_secondary))
                }
            }

            itemView.setOnClickListener { onClick() }
        }
    }
}
