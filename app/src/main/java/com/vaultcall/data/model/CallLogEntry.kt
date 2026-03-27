package com.vaultcall.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single entry in the in-app call log.
 *
 * Tracks all incoming, outgoing, missed, screened, and rejected calls
 * independently from the system call log for privacy.
 *
 * @property phoneNumber The phone number of the other party.
 * @property contactName Resolved contact name, if available.
 * @property timestamp Unix timestamp in milliseconds.
 * @property durationSeconds Call duration in seconds.
 * @property type The type/direction of the call.
 * @property note User-added note about the call, if any.
 * @property simSlot SIM slot used for the call (0 = default, 1 = second SIM).
 */
@Entity(
    tableName = "call_logs",
    indices = [
        Index("phoneNumber"),
        Index("timestamp")
    ]
)
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val timestamp: Long,
    val durationSeconds: Int,
    val type: CallType,
    val note: String? = null,
    val simSlot: Int = 0
)

/**
 * Types of call log entries.
 */
enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    SCREENED,
    REJECTED
}
