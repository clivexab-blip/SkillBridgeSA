package com.skillbridge.sa.ui.screens.auth

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.skillbridge.sa.R
import com.skillbridge.sa.viewmodel.AppViewModel
import com.skillbridge.sa.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetAuthState()
        }
    }

    AndroidView(
        factory = { context ->
            LoginView(context).apply {
                init(
                    onLogin = { email, password ->
                        viewModel.login(email, password)
                    },
                    onGoToRegister = onGoToRegister,
                    onGoogleSignIn = onGoogleSignInClick,
                    onForgotPassword = { email ->
                        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(context, "Please enter a valid email address in the field above.", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.sendPasswordReset(email) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.updateState(authState)
        }
    )
}

class LoginView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val etEmail: EditText
    private val etPassword: EditText
    private val btnLogin: Button
    private val btnGoogle: MaterialButton
    private val tvError: TextView
    private val tvForgotPassword: TextView
    private val tilEmail: TextInputLayout
    private val tilPassword: TextInputLayout

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_login, this, true)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvError = findViewById(R.id.tvError)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)

        setupInputWatchers()
    }

    private fun setupInputWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilEmail.error = null
                tilPassword.error = null
                tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
    }

    fun init(
        onLogin: (String, String) -> Unit,
        onGoToRegister: () -> Unit,
        onGoogleSignIn: () -> Unit,
        onForgotPassword: (String) -> Unit
    ) {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            tilEmail.error = null
            tilPassword.error = null
            tvError.visibility = View.GONE

            var valid = true
            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email address"
                valid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email address"
                valid = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "Please enter your password"
                valid = false
            }

            if (valid) {
                onLogin(email, password)
            }
        }

        btnGoogle.setOnClickListener { onGoogleSignIn() }
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                val dialogInput = EditText(context).apply {
                    hint = "Email Address"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
                AlertDialog.Builder(context)
                    .setTitle("Reset Password")
                    .setMessage("Enter your registered email address to receive password reset instructions.")
                    .setView(dialogInput)
                    .setPositiveButton("Send") { _, _ ->
                        val inputEmail = dialogInput.text.toString().trim()
                        onForgotPassword(inputEmail)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                onForgotPassword(email)
            }
        }
        findViewById<View>(R.id.tvGoToRegister).setOnClickListener { onGoToRegister() }
    }

    fun updateState(state: AuthUiState) {
        when (state) {
            is AuthUiState.Loading -> {
                btnLogin.isEnabled = false
                btnGoogle.isEnabled = false
                btnLogin.text = "Signing in..."
                tvError.visibility = View.GONE
            }
            is AuthUiState.Error -> {
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                btnLogin.text = "Login"
                tvError.text = state.message
                tvError.visibility = View.VISIBLE
            }
            else -> {
                btnLogin.isEnabled = true
                btnGoogle.isEnabled = true
                btnLogin.text = "Login"
                tvError.visibility = View.GONE
            }
        }
    }
}
