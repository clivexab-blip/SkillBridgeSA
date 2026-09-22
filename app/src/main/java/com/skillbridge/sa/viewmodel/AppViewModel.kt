package com.skillbridge.sa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillbridge.sa.SkillBridgeApp
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data object Success : AuthUiState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SkillBridgeApp
    private val authRepository = app.authRepository
    private val dataRepository = app.dataRepository

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _isSessionCheckDone = MutableStateFlow(false)
    val isSessionCheckDone: StateFlow<Boolean> = _isSessionCheckDone.asStateFlow()

    fun repo() = dataRepository

    fun tryRestoreSession() {
        val user = authRepository.currentUser
        if (user == null) {
            _isSessionCheckDone.value = true
            return
        }
        viewModelScope.launch {
            val profile = dataRepository.users.getById(user.uid)
            _currentProfile.value = profile
            _isSessionCheckDone.value = true
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = authRepository.registerWithEmail(email, password)
            result.onSuccess { fbUser ->
                val profile = UserProfile(
                    userId = fbUser.uid,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phone,
                    role = role
                )
                dataRepository.users.upsert(profile)
                _currentProfile.value = profile
                seedWelcomeNotification(profile)
                _authState.value = AuthUiState.Success
            }.onFailure {
                _authState.value = AuthUiState.Error(it.message ?: "Registration failed")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = authRepository.loginWithEmail(email, password)
            result.onSuccess { fbUser ->
                var profile = dataRepository.users.getById(fbUser.uid)
                if (profile == null) {
                    profile = UserProfile(userId = fbUser.uid, email = email, fullName = email.substringBefore("@"))
                    dataRepository.users.upsert(profile)
                }
                _currentProfile.value = profile
                _authState.value = AuthUiState.Success
            }.onFailure {
                val errorMsg = when (it) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email"
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password"
                    else -> it.message ?: "Login failed"
                }
                _authState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun signInWithGoogle(idToken: String, defaultRole: UserRole = UserRole.STUDENT) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = authRepository.signInWithGoogleIdToken(idToken)
            result.onSuccess { fbUser ->
                var profile = dataRepository.users.getById(fbUser.uid)
                if (profile == null) {
                    profile = UserProfile(
                        userId = fbUser.uid,
                        fullName = fbUser.displayName ?: "SkillBridge User",
                        email = fbUser.email ?: "",
                        role = defaultRole
                    )
                    dataRepository.users.upsert(profile)
                    seedWelcomeNotification(profile)
                }
                _currentProfile.value = profile
                _authState.value = AuthUiState.Success
            }.onFailure {
                _authState.value = AuthUiState.Error(it.message ?: "Google sign-in failed")
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            dataRepository.users.upsert(profile)
            _currentProfile.value = profile
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentProfile.value = null
        _authState.value = AuthUiState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthUiState.Idle
    }

    private suspend fun seedWelcomeNotification(profile: UserProfile) {
        dataRepository.notifications.insert(
            com.skillbridge.sa.data.model.AppNotification(
                userId = profile.userId,
                type = com.skillbridge.sa.data.model.NotificationType.SYSTEM,
                title = "Welcome to SkillBridge SA",
                body = "Your account is ready. Complete your profile to get better recommendations."
            )
        )
    }
}
