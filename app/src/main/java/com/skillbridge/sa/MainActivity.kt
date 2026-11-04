package com.skillbridge.sa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.skillbridge.sa.data.notifications.SystemNotificationHelper
import com.skillbridge.sa.navigation.SkillBridgeNavGraph
import com.skillbridge.sa.ui.theme.SkillBridgeSATheme
import com.skillbridge.sa.utils.LocaleHelper
import com.skillbridge.sa.viewmodel.AppViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: AppViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase, lang))
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            }
        } catch (_: ApiException) {
            // Sign-in cancelled or failed; the user can retry from the Login screen.
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val lang = LocaleHelper.getSavedLanguage(this)
        LocaleHelper.applyLanguage(this, lang)
        super.onCreate(savedInstanceState)

        SystemNotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            SkillBridgeSATheme {
                SkillBridgeNavGraph(
                    viewModel = viewModel,
                    onGoogleSignInClick = {
                        val signInIntent: Intent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
