package com.skillbridge.sa.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication for SkillBridge SA.
 * Handles email/password registration & login, and Google Single Sign-On.
 * All other application data (profiles, jobs, applications, messages, etc.)
 * lives in the local SQLite database — see data.local package.
 */
class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        result.user ?: throw IllegalStateException("Registration failed - no user returned")
    }

    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user ?: throw IllegalStateException("Login failed - no user returned")
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        result.user ?: throw IllegalStateException("Google sign-in failed - no user returned")
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
