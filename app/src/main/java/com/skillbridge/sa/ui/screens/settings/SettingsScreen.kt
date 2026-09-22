package com.skillbridge.sa.ui.screens.settings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.skillbridge.sa.R

@Composable
fun SettingsScreen() {
    AndroidView(
        factory = { context ->
            SettingsView(context)
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if needed
        }
    )
}

class SettingsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_settings, this, true)
        setupRows()
    }

    private fun setupRows() {
        setupRow(R.id.rowJobAlerts, "New job & internship alerts", true)
        setupRow(R.id.rowMentorshipAlerts, "Mentorship activity", true)
        setupRow(R.id.rowMessageAlerts, "Messages", true)
        setupRow(R.id.rowProfileVisible, "Profile visible to employers", true)
    }

    private fun setupRow(rowId: Int, label: String, checked: Boolean) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.tvLabel).text = label
        row.findViewById<SwitchMaterial>(R.id.switchSetting).isChecked = checked
    }
}
