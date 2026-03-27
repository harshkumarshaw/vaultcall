package com.vaultcall.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultcall.data.model.CallLogEntry
import com.vaultcall.data.model.CallType
import com.vaultcall.ui.theme.CallIncoming
import com.vaultcall.ui.theme.CallMissed
import com.vaultcall.ui.theme.CallOutgoing
import com.vaultcall.ui.theme.CallScreened

/**
 * Reusable composable card for displaying a call log entry.
 *
 * Color-coded by call type: green (outgoing), blue (incoming),
 * red (missed), purple (screened).
 */
@Composable
fun CallCard(
    entry: CallLogEntry,
    modifier: Modifier = Modifier
) {
    val color = when (entry.type) {
        CallType.INCOMING -> CallIncoming
        CallType.OUTGOING -> CallOutgoing
        CallType.MISSED -> CallMissed
        CallType.SCREENED -> CallScreened
        CallType.REJECTED -> CallMissed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = entry.type.name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.contactName ?: entry.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = entry.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            if (entry.note != null) {
                Text(
                    "📝",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
