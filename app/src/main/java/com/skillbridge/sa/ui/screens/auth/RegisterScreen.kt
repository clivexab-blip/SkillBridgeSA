package com.skillbridge.sa.ui.screens.auth

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.skillbridge.sa.R
import com.skillbridge.sa.data.model.UserRole
import com.skillbridge.sa.viewmodel.AppViewModel
import com.skillbridge.sa.viewmodel.AuthUiState

@Composable
fun RegisterScreen(
    viewModel: AppViewModel,
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    AndroidView(
        factory = { context ->
            RegisterView(context).apply {
                init(
                    onRegister = { name, email, phone, pass, role ->
                        viewModel.register(name, email, phone, pass, role)
                    },
                    onGoToLogin = onGoToLogin
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.updateState(authState)
        }
    )

    if (authState is AuthUiState.Success) {
        onRegisterSuccess()
        viewModel.resetAuthState()
    }
}

class RegisterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val etFullName: EditText
    private val etEmail: EditText
    private val etPhone: EditText
    private val etPassword: EditText
    private val etConfirmPassword: EditText
    private val cgRoles: ChipGroup
    private val btnRegister: Button
    private val tvError: TextView
    private val cbTerms: CheckBox
    private val pbStrength: ProgressBar
    private val tvStrengthLabel: TextView
    private val tilEmail: TextInputLayout
    private val tilPassword: TextInputLayout
    private val tilConfirmPassword: TextInputLayout

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_register, this, true)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cgRoles = findViewById(R.id.cgRoles)
        btnRegister = findViewById(R.id.btnRegister)
        tvError = findViewById(R.id.tvError)
        cbTerms = findViewById(R.id.cbTerms)
        pbStrength = findViewById(R.id.pbStrength)
        tvStrengthLabel = findViewById(R.id.tvStrengthLabel)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        setupPasswordStrength()
    }

    private fun setupPasswordStrength() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s?.toString() ?: ""
                updateStrength(password)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateStrength(password: String) {
        if (password.isEmpty()) {
            pbStrength.progress = 0
            tvStrengthLabel.text = ""
            return
        }

        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        val progress = (score / 5f * 100).toInt()
        pbStrength.progress = progress

        when (score) {
            0, 1, 2 -> {
                tvStrengthLabel.text = "Weak"
                pbStrength.progressTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.sb_error))
            }
            3, 4 -> {
                tvStrengthLabel.text = "Fair"
                pbStrength.progressTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.sb_gold))
            }
            5 -> {
                tvStrengthLabel.text = "Strong"
                pbStrength.progressTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.sb_green))
            }
        }
    }

    fun init(
        onRegister: (String, String, String, String, UserRole) -> Unit,
        onGoToLogin: () -> Unit
    ) {
        btnRegister.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val pass = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Invalid email format. Please check and try again."
                return@setOnClickListener
            }

            if (!isPasswordStrong(pass)) {
                tilPassword.error = "Password must include uppercase, lowercase, numbers, and special characters."
                return@setOnClickListener
            }

            if (pass != confirm) {
                tilConfirmPassword.error = "Passwords do not match."
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                tvError.text = "Please agree to the Terms of Service."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val role = when (cgRoles.checkedChipId) {
                R.id.chipGraduate -> UserRole.GRADUATE
                R.id.chipMentor -> UserRole.MENTOR
                R.id.chipEmployer -> UserRole.EMPLOYER
                else -> UserRole.STUDENT
            }

            if (name.isNotEmpty()) {
                onRegister(name, email, phone, pass, role)
            }
        }

        findViewById<View>(R.id.tvGoToLogin).setOnClickListener { onGoToLogin() }
        findViewById<View>(R.id.btnBack).setOnClickListener { onGoToLogin() }
    }

    private fun isPasswordStrong(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() }
    }

    fun updateState(state: AuthUiState) {
        when (state) {
            is AuthUiState.Loading -> {
                btnRegister.isEnabled = false
                btnRegister.text = "Creating account..."
                tvError.visibility = View.GONE
            }
            is AuthUiState.Error -> {
                btnRegister.isEnabled = true
                btnRegister.text = "Register Account →"
                tvError.text = state.message
                tvError.visibility = View.VISIBLE
            }
            else -> {
                btnRegister.isEnabled = true
                btnRegister.text = "Register Account →"
                tvError.visibility = View.GONE
            }
        }
    }
}
