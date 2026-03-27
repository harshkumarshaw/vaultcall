package com.vaultcall.ui.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Data class representing a permission to request.
 */
data class PermissionItem(
    val permission: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = true
)

/**
 * All permissions needed by the app, grouped logically.
 */
val requiredPermissions = listOf(
    PermissionItem(
        Manifest.permission.READ_PHONE_STATE,
        "Phone State",
        "Detect incoming calls for screening",
        Icons.Default.Call,
        isRequired = true
    ),
    PermissionItem(
        Manifest.permission.CALL_PHONE,
        "Make Calls",
        "Place outgoing calls from the dialer",
        Icons.Default.Call,
        isRequired = true
    ),
    PermissionItem(
        Manifest.permission.ANSWER_PHONE_CALLS,
        "Answer Calls",
        "Auto-answer screened calls for voicemail",
        Icons.Default.Call,
        isRequired = true
    ),
    PermissionItem(
        Manifest.permission.READ_CALL_LOG,
        "Call Log",
        "Read call history for the recents tab",
        Icons.Default.Call
    ),
    PermissionItem(
        Manifest.permission.WRITE_CALL_LOG,
        "Write Call Log",
        "Record calls to the private history",
        Icons.Default.Call
    ),
    PermissionItem(
        Manifest.permission.RECORD_AUDIO,
        "Microphone",
        "Record voicemail messages",
        Icons.Default.Mic,
        isRequired = true
    ),
    PermissionItem(
        Manifest.permission.READ_CONTACTS,
        "Contacts",
        "Show caller names in the inbox",
        Icons.Default.Contacts
    ),
    PermissionItem(
        Manifest.permission.READ_CALENDAR,
        "Calendar",
        "Auto-screen during busy events",
        Icons.Default.CalendarMonth
    ),
    PermissionItem(
        Manifest.permission.POST_NOTIFICATIONS,
        "Notifications",
        "Alert you about screened calls",
        Icons.Default.Notifications,
        isRequired = true
    )
)

/**
 * Optional permissions that may fail due to OS restrictions.
 */
val optionalPermissions = listOf(
    PermissionItem(
        Manifest.permission.SEND_SMS,
        "SMS",
        "Send auto-reply to screened callers (optional)",
        Icons.Default.Sms,
        isRequired = false
    )
)

/**
 * Full-screen onboarding/permissions screen shown on first launch.
 */
@Composable
fun PermissionsScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionStates = remember { mutableStateMapOf<String, Boolean>() }
    var isDefaultDialer by remember { mutableStateOf(false) }
    var showDefaultDialerSection by remember { mutableStateOf(true) }

    // Check initial states
    LaunchedEffect(Unit) {
        requiredPermissions.forEach { p ->
            permissionStates[p.permission] = ContextCompat.checkSelfPermission(
                context, p.permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        optionalPermissions.forEach { p ->
            permissionStates[p.permission] = ContextCompat.checkSelfPermission(
                context, p.permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        isDefaultDialer = isAppDefaultDialer(context)
    }

    // Multi-permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, granted) ->
            permissionStates[permission] = granted
        }
    }

    // Default dialer launcher using RoleManager
    val roleManagerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = isAppDefaultDialer(context)
    }

    val allRequiredGranted = requiredPermissions.all {
        permissionStates[it.permission] == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Hero
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Welcome to VaultCall",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Privacy-first smart voicemail.\nAll data stays on your device — always.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions section
        Text(
            "REQUIRED PERMISSIONS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        requiredPermissions.forEach { perm ->
            PermissionCard(
                item = perm,
                isGranted = permissionStates[perm.permission] == true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grant All button
        Button(
            onClick = {
                val toRequest = requiredPermissions
                    .filter { permissionStates[it.permission] != true }
                    .map { it.permission }
                    .toTypedArray()
                if (toRequest.isNotEmpty()) {
                    permissionLauncher.launch(toRequest)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !allRequiredGranted
        ) {
            Text(
                if (allRequiredGranted) "✓ All Required Permissions Granted"
                else "Grant Required Permissions"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional permissions
        Text(
            "OPTIONAL PERMISSIONS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        optionalPermissions.forEach { perm ->
            PermissionCard(
                item = perm,
                isGranted = permissionStates[perm.permission] == true
            )
        }

        OutlinedButton(
            onClick = {
                val toRequest = optionalPermissions
                    .filter { permissionStates[it.permission] != true }
                    .map { it.permission }
                    .toTypedArray()
                if (toRequest.isNotEmpty()) {
                    permissionLauncher.launch(toRequest)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Optional Permissions")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Default Dialer section
        Text(
            "DEFAULT DIALER",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDefaultDialer)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (isDefaultDialer) "✓ VaultCall is the default dialer"
                    else "VaultCall needs to be your default dialer to screen calls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDefaultDialer)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (!isDefaultDialer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: Some phones may not support third-party dialers. The app will still work for voicemail and rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            requestDefaultDialerRole(context, roleManagerLauncher)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set as Default Dialer")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        Button(
            onClick = {
                // Save first-launch flag
                context.getSharedPreferences("vaultcall_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding_complete", true)
                    .apply()
                onAllGranted()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Continue to VaultCall",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!allRequiredGranted) {
            Text(
                "Some features may not work without required permissions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun isAppDefaultDialer(context: Context): Boolean {
    return try {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecom.defaultDialerPackage == context.packageName
    } catch (e: Exception) {
        false
    }
}

private fun requestDefaultDialerRole(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    try {
        // API 29+ use RoleManager
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            launcher.launch(intent)
        }
    } catch (e: Exception) {
        // Fallback to telecom intent
        try {
            val intent = android.content.Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
            launcher.launch(intent)
        } catch (_: Exception) { }
    }
}
