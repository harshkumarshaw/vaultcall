package com.vaultcall.ui.dialer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultcall.service.DefaultDialerHelper

/**
 * Custom dialer screen with T9 number pad, number display,
 * backspace, and dial button with haptic feedback.
 */
@Composable
fun DialerScreen(
    viewModel: DialerViewModel = hiltViewModel()
) {
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Number display
        Spacer(modifier = Modifier.weight(1f))

        // Matched Contacts
        val matchedContacts by viewModel.matchedContacts.collectAsState()
        if (matchedContacts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(matchedContacts, key = { it.phoneNumber }) { contact ->
                    SuggestionChip(
                        onClick = { viewModel.setNumber(contact.phoneNumber.filter { it.isDigit() || it == '+' }) },
                        label = { Text(contact.name) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Text(
            text = if (phoneNumber.isEmpty()) "Enter number" else formatPhoneNumber(phoneNumber),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (phoneNumber.length > 12) 24.sp else 32.sp,
                letterSpacing = 2.sp
            ),
            color = if (phoneNumber.isEmpty())
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )

        // Backspace
        if (phoneNumber.isNotEmpty()) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.deleteDigit()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad grid
        val keys = listOf(
            Triple("1", "", ""),
            Triple("2", "ABC", ""),
            Triple("3", "DEF", ""),
            Triple("4", "GHI", ""),
            Triple("5", "JKL", ""),
            Triple("6", "MNO", ""),
            Triple("7", "PQRS", ""),
            Triple("8", "TUV", ""),
            Triple("9", "WXYZ", ""),
            Triple("*", "", ""),
            Triple("0", "+", "long"),
            Triple("#", "", "")
        )

        for (row in keys.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters, _) ->
                    DialerKey(
                        digit = digit,
                        letters = letters,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.appendDigit(digit)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dial button
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (phoneNumber.isNotEmpty()) {
                    DefaultDialerHelper.makeCall(context, phoneNumber)
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(72.dp),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DialerKey(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private fun formatPhoneNumber(number: String): String {
    // Simple formatting for display
    return when {
        number.startsWith("+91") && number.length > 5 -> {
            val digits = number.drop(3)
            "+91 ${digits.take(5)} ${digits.drop(5)}"
        }
        number.length > 6 -> {
            "${number.take(3)} ${number.substring(3, minOf(6, number.length))} ${number.drop(6)}"
        }
        else -> number
    }
}
