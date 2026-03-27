package com.vaultcall.ui.call

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.vaultcall.service.CallStateManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultcall.service.MyInCallService
import com.vaultcall.ui.theme.VaultCallTheme
import kotlinx.coroutines.delay

/**
 * Active call screen shown during an ongoing call.
 *
 * Launched by [MyInCallService] when a call becomes active,
 * or immediately by [IncomingCallActivity] after the user answers.
 */
@AndroidEntryPoint
class ActiveCallActivity : ComponentActivity() {

    @Inject lateinit var callStateManager: CallStateManager

    companion object {
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CALLER_NAME = "caller_name"

        fun launch(context: Context, callId: String, phoneNumber: String, callerName: String?) {
            val intent = Intent(context, ActiveCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CALLER_NAME, callerName)
            }
            context.startActivity(intent)
        }
    }

    private var callId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }

        callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME)

        // Prevent back button from ending call accidentally
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Move to background; call still active
                moveTaskToBack(true)
            }
        })

        setContent {
            VaultCallTheme {
                val activeCalls by callStateManager.activeCalls.collectAsState()
                val currentCall = activeCalls[callId]

                // Automatically close if call is disconnected or no longer exists
                LaunchedEffect(currentCall?.state) {
                    if (currentCall == null || currentCall.state == CallStateManager.CallState.DISCONNECTED) {
                        finish()
                    }
                }

                if (currentCall != null) {
                    ActiveCallScreen(
                        callerName = callerName,
                        phoneNumber = phoneNumber,
                        callState = currentCall.state,
                        onEndCall = { endCall() },
                        onMuteToggle = { muted -> MyInCallService.instance?.muteCall(muted) },
                        onSpeakerToggle = { on -> MyInCallService.instance?.setSpeakerphone(on) }
                    )
                }
            }
        }
    }

    private fun endCall() {
        MyInCallService.instance?.endCall(callId)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        callId = intent.getStringExtra(EXTRA_CALL_ID) ?: callId
    }
}

@Composable
private fun ActiveCallScreen(
    callerName: String?,
    phoneNumber: String,
    callState: CallStateManager.CallState,
    onEndCall: () -> Unit,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit
) {
    var elapsed by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }

    // Timer counting up only when ACTIVE
    LaunchedEffect(callState) {
        if (callState == CallStateManager.CallState.ACTIVE) {
            while (true) {
                delay(1000)
                elapsed++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF161B22))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Caller info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF30363D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = callerName ?: phoneNumber,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (callerName != null) {
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Elapsed time or status
                val statusText = when (callState) {
                    CallStateManager.CallState.DIALING -> "Dialing..."
                    CallStateManager.CallState.CONNECTING -> "Connecting..."
                    CallStateManager.CallState.HOLDING -> "On Hold"
                    CallStateManager.CallState.ACTIVE -> formatElapsed(elapsed)
                    else -> "Ringing..."
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (callState == CallStateManager.CallState.ACTIVE) Color(0xFF3FB950) else Color(0xFF8B949E),
                    letterSpacing = 2.sp
                )
            }

            // Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Secondary controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        activeColor = Color(0xFFF85149)
                    ) {
                        isMuted = !isMuted
                        onMuteToggle(isMuted)
                    }

                    CallControlButton(
                        icon = Icons.Default.VolumeUp,
                        label = if (isSpeaker) "Speaker On" else "Speaker",
                        isActive = isSpeaker,
                        activeColor = Color(0xFF58A6FF)
                    ) {
                        isSpeaker = !isSpeaker
                        onSpeakerToggle(isSpeaker)
                    }
                }

                // End call button (big, red, centered)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFDA3633), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "End Call",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.2f)
                    else Color(0xFF30363D),
                    RoundedCornerShape(16.dp)
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}
