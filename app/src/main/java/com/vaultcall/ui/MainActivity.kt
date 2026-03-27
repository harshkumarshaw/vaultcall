package com.vaultcall.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vaultcall.data.repository.SettingsRepository
import com.vaultcall.service.NotificationHelper
import com.vaultcall.ui.dialer.DialerScreen
import com.vaultcall.ui.onboarding.PermissionsScreen
import com.vaultcall.ui.recents.RecentsScreen
import com.vaultcall.ui.rules.RulesScreen
import com.vaultcall.ui.settings.SettingsScreen
import com.vaultcall.ui.theme.VaultCallTheme
import com.vaultcall.ui.voicemail.VoicemailListScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point Activity for VaultCall.
 *
 * On first launch, shows the PermissionsScreen to request all required permissions.
 * After onboarding, shows the main app with bottom navigation between
 * Voicemails, Recents, Dialer, Rules, and Settings tabs.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        notificationHelper.createChannels()

        if (settingsRepository.currentAppLockEnabled) {
            val biometricManager = BiometricManager.from(this)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                showBiometricPrompt()
            } else {
                launchApp()
            }
        } else {
            launchApp()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    launchApp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock VaultCall")
            .setSubtitle("Confirm your identity to access your private vault")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun launchApp() {
        setContent {
            VaultCallTheme {
                val prefs = getSharedPreferences("vaultcall_prefs", Context.MODE_PRIVATE)
                var onboardingComplete by remember {
                    mutableStateOf(prefs.getBoolean("onboarding_complete", false))
                }

                if (!onboardingComplete) {
                    PermissionsScreen(
                        onAllGranted = {
                            onboardingComplete = true
                        }
                    )
                } else {
                    VaultCallMainApp()
                }
            }
        }
    }
}

/** Bottom navigation destinations. */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Voicemails : Screen("voicemails", "Voicemails", Icons.Default.Voicemail)
    data object Recents : Screen("recents", "Recents", Icons.Default.History)
    data object Dialer : Screen("dialer", "Dialer", Icons.Default.Call)
    data object Rules : Screen("rules", "Rules", Icons.Default.Shield)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Voicemails,
    Screen.Recents,
    Screen.Dialer,
    Screen.Rules,
    Screen.Settings
)

@Composable
fun VaultCallMainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Voicemails.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Voicemails.route) {
                VoicemailListScreen()
            }
            composable(Screen.Recents.route) {
                RecentsScreen()
            }
            composable(Screen.Dialer.route) {
                DialerScreen()
            }
            composable(Screen.Rules.route) {
                RulesScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
