package com.skillbridge.sa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.skillbridge.sa.SkillBridgeApp
import com.skillbridge.sa.data.model.AppNotification
import com.skillbridge.sa.data.model.NotificationType
import com.skillbridge.sa.data.model.Opportunity
import com.skillbridge.sa.data.model.OpportunityType
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
            var profile = dataRepository.users.getById(user.uid)
            if (profile == null) {
                val email = user.email ?: ""
                profile = UserProfile(
                    userId = user.uid,
                    email = email,
                    fullName = user.displayName ?: if (email.isNotEmpty()) email.substringBefore("@") else "SkillBridge User",
                    role = UserRole.STUDENT,
                    profileComplete = 1
                )
                dataRepository.users.upsert(profile)
            }
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
                    role = role,
                    profileComplete = 1
                )
                dataRepository.users.upsert(profile)
                _currentProfile.value = profile
                seedWelcomeNotification(profile)
                _authState.value = AuthUiState.Success
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is FirebaseAuthUserCollisionException -> "An account with this email address already exists."
                    is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email address format."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> e.localizedMessage ?: e.message ?: "Registration failed. Please try again."
                }
                _authState.value = AuthUiState.Error(errorMsg)
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
                    profile = UserProfile(
                        userId = fbUser.uid,
                        email = email,
                        fullName = email.substringBefore("@"),
                        profileComplete = 1
                    )
                    dataRepository.users.upsert(profile)
                }
                _currentProfile.value = profile
                _authState.value = AuthUiState.Success
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email address."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please check your credentials."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> e.localizedMessage ?: e.message ?: "Login failed. Please try again."
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
                        role = defaultRole,
                        profileComplete = 1
                    )
                    dataRepository.users.upsert(profile)
                    seedWelcomeNotification(profile)
                }
                _currentProfile.value = profile
                _authState.value = AuthUiState.Success
            }.onFailure { e ->
                val errorMsg = when (e) {
                    is FirebaseNetworkException -> "Network error during Google sign-in. Please check your internet connection."
                    else -> e.localizedMessage ?: e.message ?: "Google sign-in failed. Please try again."
                }
                _authState.value = AuthUiState.Error(errorMsg)
            }
        }
    }

    fun postOpportunity(
        title: String,
        company: String,
        location: String,
        type: OpportunityType,
        description: String,
        requirements: List<String>,
        salaryRange: String,
        onComplete: (Boolean) -> Unit
    ) {
        val current = _currentProfile.value ?: return
        viewModelScope.launch {
            val opp = Opportunity(
                title = title,
                company = company,
                location = location,
                type = type,
                description = description,
                requirements = requirements,
                salaryRange = salaryRange,
                postedByUserId = current.userId
            )
            val oppId = dataRepository.opportunities.insert(opp)

            // 1. Employer confirmation notification
            dataRepository.notifications.insert(
                AppNotification(
                    userId = current.userId,
                    type = NotificationType.OPPORTUNITY,
                    title = "Opportunity Published",
                    body = "Your listing '$title' is live."
                )
            )

            // 2. Broadcast to Students and Graduates
            val allUsers = dataRepository.users.getAll()
            val candidates = allUsers.filter { it.role == UserRole.STUDENT || it.role == UserRole.GRADUATE }
            candidates.forEach { candidate ->
                dataRepository.notifications.insert(
                    AppNotification(
                        userId = candidate.userId,
                        type = NotificationType.OPPORTUNITY,
                        title = "New Opportunity Available",
                        body = "$company posted '$title'. Apply now!"
                    )
                )
            }

            // 3. Broadcast to Admins
            val admins = allUsers.filter { it.role == UserRole.ADMIN }
            admins.forEach { admin ->
                dataRepository.notifications.insert(
                    AppNotification(
                        userId = admin.userId,
                        type = NotificationType.SYSTEM,
                        title = "New Opportunity Alert",
                        body = "$company posted '$title'."
                    )
                )
            }

            onComplete(oppId.isNotEmpty())
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            result.onSuccess {
                onResult(true, "Password reset email sent to $email. Please check your inbox.")
            }.onFailure { e ->
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account registered with this email address."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> e.localizedMessage ?: "Failed to send reset email. Please check the address."
                }
                onResult(false, msg)
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
        // Welcome notification to newly registered user
        dataRepository.notifications.insert(
            AppNotification(
                userId = profile.userId,
                type = NotificationType.SYSTEM,
                title = "Welcome to SkillBridge SA",
                body = "Your ${profile.role.name.lowercase()} profile is active. Complete your profile for better opportunities."
            )
        )
        // Alert Admins about new registration
        val allUsers = dataRepository.users.getAll()
        val admins = allUsers.filter { it.role == UserRole.ADMIN }
        admins.forEach { admin ->
            if (admin.userId != profile.userId) {
                dataRepository.notifications.insert(
                    AppNotification(
                        userId = admin.userId,
                        type = NotificationType.SYSTEM,
                        title = "New User Registered",
                        body = "${profile.fullName} registered as a ${profile.role.name.lowercase()}."
                    )
                )
            }
        }
    }
}
