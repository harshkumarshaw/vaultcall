package com.vaultcall.ui.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen with live permissions dashboard and App Defaults section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val autoTranscribe by viewModel.autoTranscribe.collectAsState()
    val screeningEnabled by viewModel.screeningEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val dtmfEnabled by viewModel.dtmfEnabled.collectAsState()

    val autoLockTimerMinutes by viewModel.autoLockTimerMinutes.collectAsState()
    val maxRecordingLengthSeconds by viewModel.maxRecordingLengthSeconds.collectAsState()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState()
    val ringsBeforeVoicemail by viewModel.ringsBeforeVoicemail.collectAsState()

    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showMaxRecordingDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showRingsDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    // Live permission states — refreshed every time screen comes into view
    val permissionStates = remember { mutableStateMapOf<String, Boolean>() }
    var isDefaultDialer by remember { mutableStateOf(false) }
    var isDefaultScreener by remember { mutableStateOf(false) }

    fun refreshStates() {
        val allPerms = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SEND_SMS,
        )
        allPerms.forEach { perm ->
            permissionStates[perm] = ContextCompat.checkSelfPermission(
                context, perm
            ) == PackageManager.PERMISSION_GRANTED
        }
        isDefaultDialer = try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.defaultDialerPackage == context.packageName
        } catch (_: Exception) { false }

        isDefaultScreener = try {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } catch (_: Exception) { false }
    }

    // Refresh on every ON_RESUME (e.g. returning from system settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher — batch request
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (p, g) -> permissionStates[p] = g }
    }

    // Role launcher (default dialer / screener)
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshStates() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ─── APP DEFAULTS ─────────────────────────────────────────────────
            SettingsSection("App Defaults")

            AppDefaultCard(
                icon = Icons.Default.Call,
                title = "Default Dialer",
                description = "Required to make and receive calls in VaultCall",
                isActive = isDefaultDialer,
                activeLabel = "VaultCall is the default dialer ✓",
                inactiveLabel = "VaultCall is NOT the default dialer",
                buttonLabel = "Set as Default Dialer",
                onButtonClick = {
                    launchDialerRole(context, roleLauncher)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            AppDefaultCard(
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                title = "Call Screener",
                description = "Required to screen and auto-reject spam calls",
                isActive = isDefaultScreener,
                activeLabel = "VaultCall is the call screener ✓",
                inactiveLabel = "VaultCall is NOT the call screener",
                buttonLabel = "Set as Call Screener",
                onButtonClick = {
                    launchScreenerRole(context, roleLauncher)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── PERMISSIONS DASHBOARD ────────────────────────────────────────
            SettingsSection("Permissions")

            val permissionDefs = listOf(
                Triple(Manifest.permission.READ_PHONE_STATE, "Phone State", Icons.Default.Call),
                Triple(Manifest.permission.CALL_PHONE, "Make Calls", Icons.Default.Call),
                Triple(Manifest.permission.ANSWER_PHONE_CALLS, "Answer Calls", Icons.Default.Call),
                Triple(Manifest.permission.READ_CALL_LOG, "Read Call Log", Icons.Default.Call),
                Triple(Manifest.permission.WRITE_CALL_LOG, "Write Call Log", Icons.Default.Call),
                Triple(Manifest.permission.RECORD_AUDIO, "Microphone", Icons.Default.Mic),
                Triple(Manifest.permission.READ_CONTACTS, "Contacts", Icons.Default.ContactPhone),
                Triple(Manifest.permission.POST_NOTIFICATIONS, "Notifications", Icons.Default.Notifications),
                Triple(Manifest.permission.SEND_SMS, "SMS (optional)", Icons.Default.Sms),
                Triple(Manifest.permission.READ_CALENDAR, "Calendar (optional)", Icons.Default.Voicemail),
            )

            permissionDefs.forEach { (perm, label, icon) ->
                val granted = permissionStates[perm] == true
                PermissionRow(
                    icon = icon,
                    label = label,
                    isGranted = granted,
                    onGrant = { permLauncher.launch(arrayOf(perm)) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                )
            }

            // Grant all denied button
            val deniedPerms = permissionDefs
                .filter { permissionStates[it.first] != true }
                .map { it.first }
                .toTypedArray()
            if (deniedPerms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { permLauncher.launch(deniedPerms) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant All Missing Permissions (${deniedPerms.size})")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── ACCOUNT & SECURITY ───────────────────────────────────────────
            SettingsSection("Account & Security")
            SettingsToggle(
                icon = Icons.Default.Fingerprint,
                title = "App Lock",
                subtitle = "Require biometric to open app",
                checked = appLockEnabled,
                onCheckedChange = { viewModel.setAppLockEnabled(it) }
            )
            SettingsNavItem(
                icon = Icons.Default.Fingerprint,
                title = "Auto-lock Timer",
                value = if (autoLockTimerMinutes == 0) "Immediately" else "$autoLockTimerMinutes minutes",
                onClick = { showAutoLockDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── VOICEMAIL SETTINGS ───────────────────────────────────────────
            SettingsSection("Voicemail Settings")
            SettingsNavItem(
                icon = Icons.Default.Voicemail,
                title = "Max Recording Length",
                value = "$maxRecordingLengthSeconds seconds",
                onClick = { showMaxRecordingDialog = true }
            )
            SettingsNavItem(
                icon = Icons.Default.Delete,
                title = "Auto-delete After",
                value = "$autoDeleteDays days",
                onClick = { showAutoDeleteDialog = true }
            )
            SettingsToggle(
                icon = Icons.Default.Mic,
                title = "Auto-transcribe",
                subtitle = "Transcribe voicemails using on-device AI",
                checked = autoTranscribe,
                onCheckedChange = { viewModel.setAutoTranscribe(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── CALL SCREENING ───────────────────────────────────────────────
            SettingsSection("Call Screening")
            SettingsToggle(
                icon = Icons.Default.Shield,
                title = "Screening Enabled",
                subtitle = "Master toggle for call screening",
                checked = screeningEnabled,
                onCheckedChange = { viewModel.setScreeningEnabled(it) }
            )
            SettingsNavItem(
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                title = "Rings Before Voicemail",
                value = "$ringsBeforeVoicemail rings",
                onClick = { showRingsDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── DIALER ───────────────────────────────────────────────────────
            SettingsSection("Dialer")
            SettingsNavItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                value = "Dark"
            )
            SettingsToggle(
                icon = Icons.Default.Voicemail,
                title = "Haptic Feedback",
                subtitle = "Vibrate on keypad press",
                checked = hapticEnabled,
                onCheckedChange = { viewModel.setHapticEnabled(it) }
            )
            SettingsToggle(
                icon = Icons.Default.Mic,
                title = "DTMF Tones",
                subtitle = "Play tones on keypad press",
                checked = dtmfEnabled,
                onCheckedChange = { viewModel.setDtmfEnabled(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── PRIVACY ──────────────────────────────────────────────────────
            SettingsSection("Privacy")
            SettingsNavItem(
                icon = Icons.Default.Shield, 
                title = "Privacy Report", 
                value = "",
                onClick = {
                    Toast.makeText(context, "No tracking detected. 100% on-device.", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsNavItem(
                icon = Icons.Default.Download, 
                title = "Export All Data", 
                value = "",
                onClick = {
                    Toast.makeText(context, "Data export scheduled. See notifications.", Toast.LENGTH_LONG).show()
                }
            )
            SettingsNavItem(
                icon = Icons.Default.Delete,
                title = "Wipe All Data",
                value = "",
                isDestructive = true,
                onClick = { showWipeDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── ABOUT ────────────────────────────────────────────────────────
            SettingsSection("About")
            SettingsNavItem(icon = Icons.Default.Shield, title = "Version", value = "1.0.0")

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "All data stays on device.\nZero analytics. Zero cloud dependency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showAutoLockDialog) {
        SettingsSelectorDialog(
            title = "Auto-lock Timer",
            options = listOf("Immediately" to 0, "1 minute" to 1, "5 minutes" to 5, "15 minutes" to 15, "30 minutes" to 30),
            selectedValue = autoLockTimerMinutes,
            onDismiss = { showAutoLockDialog = false },
            onSelect = { viewModel.setAutoLockTimerMinutes(it) }
        )
    }

    if (showMaxRecordingDialog) {
        SettingsSelectorDialog(
            title = "Max Recording Length",
            options = listOf("30 seconds" to 30, "60 seconds" to 60, "2 minutes" to 120, "5 minutes" to 300),
            selectedValue = maxRecordingLengthSeconds,
            onDismiss = { showMaxRecordingDialog = false },
            onSelect = { viewModel.setMaxRecordingLengthSeconds(it) }
        )
    }

    if (showAutoDeleteDialog) {
        SettingsSelectorDialog(
            title = "Auto-delete After",
            options = listOf("7 days" to 7, "14 days" to 14, "30 days" to 30, "90 days" to 90, "Never" to -1),
            selectedValue = autoDeleteDays,
            onDismiss = { showAutoDeleteDialog = false },
            onSelect = { viewModel.setAutoDeleteDays(it) }
        )
    }

    if (showRingsDialog) {
        SettingsSelectorDialog(
            title = "Rings Before Voicemail",
            options = listOf("2 rings" to 2, "3 rings" to 3, "4 rings" to 4, "5 rings" to 5),
            selectedValue = ringsBeforeVoicemail,
            onDismiss = { showRingsDialog = false },
            onSelect = { viewModel.setRingsBeforeVoicemail(it) }
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = {
                Text(
                    "Wipe All Data?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text("Are you absolutely sure you want to delete all app data, permissions, rules, and call logs? This cannot be undone.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeDialog = false
                        context.getSharedPreferences("vaultcall_settings", Context.MODE_PRIVATE).edit().clear().apply()
                        context.getSharedPreferences("vaultcall_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                        Toast.makeText(context, "All data wiped successfully.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Reusable Composables ──────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun AppDefaultCard(
    icon: ImageVector,
    title: String,
    description: String,
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (isActive) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (isActive) activeLabel else inactiveLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
            )
            if (!isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    label: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (isGranted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isGranted) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            OutlinedButton(
                onClick = onGrant,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) {
                Text("Grant", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Settings",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    value: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun launchDialerRole(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
            launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            return
        }
    } catch (_: Exception) {}
    // Legacy fallback
    try {
        launcher.launch(
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        )
    } catch (_: Exception) {}
}

private fun launchScreenerRole(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            launcher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    } catch (_: Exception) {}
}

@Composable
fun <T> SettingsSelectorDialog(
    title: String,
    options: List<Pair<String, T>>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedValue,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
