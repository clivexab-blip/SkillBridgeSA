package com.skillbridge.sa.ui.screens.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.chip.ChipGroup
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.local.SqliteUtils

@Composable
fun ProfileScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    AndroidView(
        factory = { context ->
            ProfileView(context).apply {
                init(profile, onSave, onLogout)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update if profile changes externally
        }
    )
}

class ProfileView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val etFullName: EditText
    private val etProfessionalTitle: EditText
    private val etBio: EditText
    private val etQualification: EditText
    private val etExperience: EditText
    private val etExpertise: EditText
    private val etSkills: EditText
    private val etLocation: EditText
    private val etAvailability: EditText
    private val cgLanguages: ChipGroup
    private val btnSave: Button
    private val btnLogout: Button
    private val tvRoleBadge: TextView

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_profile, this, true)
        etFullName = findViewById(R.id.etFullName)
        etProfessionalTitle = findViewById(R.id.etProfessionalTitle)
        etBio = findViewById(R.id.etBio)
        etQualification = findViewById(R.id.etQualification)
        etExperience = findViewById(R.id.etExperience)
        etExpertise = findViewById(R.id.etExpertise)
        etSkills = findViewById(R.id.etSkills)
        etLocation = findViewById(R.id.etLocation)
        etAvailability = findViewById(R.id.etAvailability)
        cgLanguages = findViewById(R.id.cgLanguages)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        tvRoleBadge = findViewById(R.id.tvRoleBadge)
    }

    fun init(profile: UserProfile, onSave: (UserProfile) -> Unit, onLogout: () -> Unit) {
        tvRoleBadge.text = profile.role.name
        etFullName.setText(profile.fullName)
        etProfessionalTitle.setText(profile.professionalTitle)
        etBio.setText(profile.bio)
        etQualification.setText(profile.qualification)
        etExperience.setText(profile.experience)
        etExpertise.setText(profile.expertise.joinToString(", "))
        etSkills.setText(profile.skills.joinToString(", "))
        etLocation.setText(profile.location)
        etAvailability.setText(profile.availability)

        when (profile.language) {
            "en" -> cgLanguages.check(R.id.chipEn)
            "xh" -> cgLanguages.check(R.id.chipXh)
            "zu" -> cgLanguages.check(R.id.chipZu)
        }

        btnSave.setOnClickListener {
            val language = when (cgLanguages.checkedChipId) {
                R.id.chipXh -> "xh"
                R.id.chipZu -> "zu"
                else -> "en"
            }
            
            val updatedExpertise = etExpertise.text.toString().split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            val updatedSkills = etSkills.text.toString().split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }

            onSave(
                profile.copy(
                    fullName = etFullName.text.toString().trim(),
                    professionalTitle = etProfessionalTitle.text.toString().trim(),
                    bio = etBio.text.toString().trim(),
                    qualification = etQualification.text.toString().trim(),
                    experience = etExperience.text.toString().trim(),
                    expertise = updatedExpertise,
                    skills = updatedSkills,
                    location = etLocation.text.toString().trim(),
                    availability = etAvailability.text.toString().trim(),
                    language = language
                )
            )
        }

        btnLogout.setOnClickListener { onLogout() }
    }
}
