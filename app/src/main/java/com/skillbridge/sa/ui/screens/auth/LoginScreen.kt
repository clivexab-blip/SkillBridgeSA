package com.skillbridge.sa.ui.screens.auth

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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

    AndroidView(
        factory = { context ->
            LoginView(context).apply {
                init(
                    onLogin = { e, p -> viewModel.login(e, p) },
                    onGoToRegister = onGoToRegister,
                    onGoogleSignIn = onGoogleSignInClick
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.updateState(authState)
        }
    )

    if (authState is AuthUiState.Success) {
        onLoginSuccess()
        viewModel.resetAuthState()
    }
}

class LoginView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val etEmail: EditText
    private val etPassword: EditText
    private val btnLogin: Button
    private val tvError: TextView
    private val tilEmail: TextInputLayout
    private val tilPassword: TextInputLayout

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.layout_login, this, true)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
    }

    fun init(
        onLogin: (String, String) -> Unit,
        onGoToRegister: () -> Unit,
        onGoogleSignIn: () -> Unit
    ) {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            
            tilEmail.error = null
            tilPassword.error = null
            tvError.visibility = View.GONE

            var valid = true
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

        findViewById<View>(R.id.btnGoogle).setOnClickListener { onGoogleSignIn() }
        findViewById<View>(R.id.tvGoToRegister).setOnClickListener { onGoToRegister() }
    }

    fun updateState(state: AuthUiState) {
        when (state) {
            is AuthUiState.Loading -> {
                btnLogin.isEnabled = false
                btnLogin.text = "Signing in..."
                tvError.visibility = View.GONE
            }
            is AuthUiState.Error -> {
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                tvError.text = state.message
                tvError.visibility = View.VISIBLE
            }
            else -> {
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                tvError.visibility = View.GONE
            }
        }
    }
}
