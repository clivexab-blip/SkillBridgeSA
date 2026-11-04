package com.skillbridge.sa.ui.screens.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.LocaleListCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.skillbridge.sa.MainActivity
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.utils.LocaleHelper

@Composable
fun SettingsScreen(
    profile: UserProfile? = null,
    onUpdateProfile: ((UserProfile) -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            SettingsView(context).apply {
                init(profile, onUpdateProfile)
            }
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

    private var profile: UserProfile? = null
    private var onUpdateProfile: ((UserProfile) -> Unit)? = null

    private val tvCurrentLanguage: TextView
    private val cardLanguage: View

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_settings, this, true)
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage)
        cardLanguage = findViewById(R.id.cardLanguage)
        setupRows()
    }

    fun init(profile: UserProfile?, onUpdateProfile: ((UserProfile) -> Unit)?) {
        this.profile = profile
        this.onUpdateProfile = onUpdateProfile

        val savedLang = LocaleHelper.getSavedLanguage(context)
        tvCurrentLanguage.text = LocaleHelper.getLanguageDisplayName(savedLang)

        cardLanguage.setOnClickListener {
            showLanguagePickerDialog()
        }
    }

    private fun showLanguagePickerDialog() {
        val languages = arrayOf("English", "isiXhosa", "isiZulu")
        val codes = arrayOf("en", "xh", "zu")
        val savedLang = LocaleHelper.getSavedLanguage(context)
        val selectedIndex = codes.indexOf(savedLang).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.language))
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val newCode = codes[which]
                if (newCode != savedLang) {
                    LocaleHelper.saveLanguage(context, newCode)
                    LocaleHelper.applyLanguage(context, newCode)
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newCode))

                    profile?.let { p ->
                        onUpdateProfile?.invoke(p.copy(language = newCode))
                    }

                    dialog.dismiss()

                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                } else {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
