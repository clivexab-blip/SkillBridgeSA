package com.skillbridge.sa.ui.screens.splash

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.skillbridge.sa.R

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    AndroidView(
        factory = { context ->
            OnboardingView(context).apply {
                this.onDone = onDone
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

private data class OnboardPage(val title: String, val body: String)

private val pages = listOf(
    OnboardPage("Learn from the Best", "Connect with industry leaders and mentors tailored to your career goals."),
    OnboardPage("Find Real Opportunities", "Search jobs, internships and graduate programmes made for you."),
    OnboardPage("Grow Your Skills", "Track your progress and close the gap between you and your dream career.")
)

class OnboardingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onDone: (() -> Unit)? = null
    private var currentIndex = 0

    private val tvTitle: TextView
    private val tvBody: TextView
    private val btnNext: Button
    private val llIndicators: LinearLayout

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_onboarding, this, true)
        tvTitle = findViewById(R.id.tvOnboardTitle)
        tvBody = findViewById(R.id.tvOnboardBody)
        btnNext = findViewById(R.id.btnNext)
        llIndicators = findViewById(R.id.llIndicators)

        setupUI()
    }

    private fun setupUI() {
        updatePage()

        btnNext.setOnClickListener {
            if (currentIndex == pages.lastIndex) {
                onDone?.invoke()
            } else {
                currentIndex++
                updatePage()
            }
        }

        findViewById<View>(R.id.tvSkip).setOnClickListener { onDone?.invoke() }
    }

    private fun updatePage() {
        val page = pages[currentIndex]
        tvTitle.text = page.title
        tvBody.text = page.body
        btnNext.text = if (currentIndex == pages.lastIndex) "Get Started" else "Next"
        
        updateIndicators()
    }

    private fun updateIndicators() {
        llIndicators.removeAllViews()
        val density = context.resources.displayMetrics.density
        pages.indices.forEach { i ->
            val dot = View(context).apply {
                val widthDp = if (i == currentIndex) 24 else 8
                val heightDp = 8
                val widthPx = (widthDp * density).toInt()
                val heightPx = (heightDp * density).toInt()
                val marginPx = (4 * density).toInt()

                layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
                setBackgroundResource(R.drawable.bg_role_badge)
                alpha = if (i == currentIndex) 1.0f else 0.3f
            }
            llIndicators.addView(dot)
        }
    }
}
