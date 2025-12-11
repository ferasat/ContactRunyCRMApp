package com.example.contactrunycrmapp.model

/**
 * Represents a single call log entry on the device. The [type] field is a
 * human‑readable string describing whether the call was incoming, outgoing
 * or missed. The [timestamp] is milliseconds since epoch when the call
 * started and [durationSeconds] is the call length in seconds.
 */
data class CallLogEntry(
    val phoneNumber: String,
    val type: String,
    val timestamp: Long,
    val durationSeconds: Int
)