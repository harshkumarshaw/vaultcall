package com.vaultcall.ui.recents

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Represents a call log entry from the system call log.
 */
data class SystemCallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long
)

/**
 * Filter tabs for the call log.
 */
enum class CallFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    REJECTED("Rejected")
}

/**
 * Call Log / Recents screen that reads from the system call log.
 * Shows categorized calls with filter chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen() {
    val context = LocalContext.current
    val allCalls = remember { mutableStateListOf<SystemCallLogEntry>() }
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Load call log
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            allCalls.clear()
            allCalls.addAll(readSystemCallLog(context))
        }
    }

    val filteredCalls = remember(selectedFilter, allCalls.toList()) {
        when (selectedFilter) {
            CallFilter.ALL -> allCalls.toList()
            CallFilter.MISSED -> allCalls.filter { it.type == CallLog.Calls.MISSED_TYPE }
            CallFilter.INCOMING -> allCalls.filter { it.type == CallLog.Calls.INCOMING_TYPE }
            CallFilter.OUTGOING -> allCalls.filter { it.type == CallLog.Calls.OUTGOING_TYPE }
            CallFilter.REJECTED -> allCalls.filter { it.type == CallLog.Calls.REJECTED_TYPE }
        }
    }

    // Group by date
    val groupedCalls = remember(filteredCalls) {
        filteredCalls.groupBy { getDateGroupLabel(it.date) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Recents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CallFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            filter.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (!hasPermission) {
            // No permission state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Call log permission required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Grant the Call Log permission in Settings to see your recent calls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else if (filteredCalls.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No ${selectedFilter.label.lowercase()} calls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your call history will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                groupedCalls.forEach { (dateLabel, calls) ->
                    item {
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 16.dp, bottom = 4.dp
                            )
                        )
                    }
                    items(calls, key = { it.id }) { entry ->
                        CallLogItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogItem(entry: SystemCallLogEntry) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to detail or call back */ }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call type icon
        val (icon, tint) = when (entry.type) {
            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF3FB950)
            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF58A6FF)
            CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFF85149)
            CallLog.Calls.REJECTED_TYPE -> Icons.Default.Block to Color(0xFFF85149)
            else -> Icons.Default.Call to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Caller info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name ?: entry.number.ifEmpty { "Unknown" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.type == CallLog.Calls.MISSED_TYPE) FontWeight.Bold else FontWeight.Normal,
                color = if (entry.type == CallLog.Calls.MISSED_TYPE)
                    Color(0xFFF85149) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (entry.type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else -> "Unknown"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.duration > 0) {
                    Text(
                        " · ${formatDuration(entry.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatCallTime(entry.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Call back button
        IconButton(
            onClick = {
                if (entry.number.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:${entry.number}")
                    }
                    context.startActivity(intent)
                }
            }
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Read up to 200 recent calls from the system call log.
 */
private fun readSystemCallLog(context: Context): List<SystemCallLogEntry> {
    val entries = mutableListOf<SystemCallLogEntry>()
    try {
        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            var count = 0
            while (it.moveToNext() && count < 200) {
                entries.add(
                    SystemCallLogEntry(
                        id = it.getLong(idIdx),
                        number = it.getString(numberIdx) ?: "",
                        name = it.getString(nameIdx),
                        type = it.getInt(typeIdx),
                        date = it.getLong(dateIdx),
                        duration = it.getLong(durationIdx)
                    )
                )
                count++
            }
        }
    } catch (e: SecurityException) {
        // Permission revoked
    } catch (e: Exception) {
        // Other error
    }
    return entries
}

private fun getDateGroupLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val call = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(now, call) -> "Today"
        isYesterday(now, call) -> "Yesterday"
        isThisWeek(now, call) -> "This Week"
        isThisMonth(now, call) -> "This Month"
        else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, other: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, other)
}

private fun isThisWeek(now: Calendar, other: Calendar): Boolean {
    val weekAgo = now.timeInMillis - TimeUnit.DAYS.toMillis(7)
    return other.timeInMillis >= weekAgo
}

private fun isThisMonth(now: Calendar, other: Calendar): Boolean {
    return now.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == other.get(Calendar.MONTH)
}

private fun formatCallTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val call = Calendar.getInstance().apply { timeInMillis = timestamp }

    return if (isSameDay(now, call)) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
