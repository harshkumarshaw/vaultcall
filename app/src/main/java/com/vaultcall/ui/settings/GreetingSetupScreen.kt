package com.vaultcall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Greeting setup screen where users can configure TTS or recorded
 * greetings for different rule types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingSetupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Greetings",
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
                .padding(16.dp)
        ) {
            GreetingCard(
                label = "DEFAULT GREETING",
                text = "This call is being screened. Please state your name and the reason for your call after the tone.",
                isCustom = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            GreetingCard(
                label = "DND GREETING",
                text = "Using default",
                isCustom = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            GreetingCard(
                label = "NIGHT MODE GREETING",
                text = "Custom recorded",
                isCustom = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            GreetingCard(
                label = "MEETING GREETING",
                text = "Using default",
                isCustom = false
            )
        }
    }
}

@Composable
fun GreetingCard(
    label: String,
    text: String,
    isCustom: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCustom) {
                    IconButton(onClick = { /* Preview */ }) {
                        Icon(Icons.Default.PlayArrow, "Preview",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, "Edit",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* Re-record */ }) {
                        Icon(Icons.Default.Mic, "Record",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TextButton(onClick = { /* Customize */ }) {
                        Text("+ Customize")
                    }
                }
            }
        }
    }
}
