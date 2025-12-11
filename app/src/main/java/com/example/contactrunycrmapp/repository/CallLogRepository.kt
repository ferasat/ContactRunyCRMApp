package com.example.contactrunycrmapp.repository

import android.content.Context
import android.provider.CallLog
import com.example.contactrunycrmapp.api.ApiClient
import com.example.contactrunycrmapp.api.CrmApiService
import com.example.contactrunycrmapp.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads call logs from the device and sends them to the CRM API.
 */
class CallLogRepository(private val context: Context) {

    /**
     * Retrieve call logs since a given timestamp. If [sinceTimestamp] is zero
     * all calls will be returned. The logs are ordered by default call log
     * ordering (most recent first). The caller can reverse the list if needed.
     */
    suspend fun getCallLogs(sinceTimestamp: Long = 0L): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val selection = if (sinceTimestamp > 0) "${CallLog.Calls.DATE} > ?" else null
        val selectionArgs = if (sinceTimestamp > 0) arrayOf(sinceTimestamp.toString()) else null

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        cursor?.use { c ->
            val numberIndex = c.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = c.getColumnIndex(CallLog.Calls.DURATION)

            while (c.moveToNext()) {
                val number = c.getString(numberIndex)
                val typeInt = c.getInt(typeIndex)
                val date = c.getLong(dateIndex)
                val duration = c.getInt(durationIndex)
                val type = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "UNKNOWN"
                }
                logs.add(CallLogEntry(number, type, date, duration))
            }
        }
        logs
    }

    /**
     * Send the collected call logs to the CRM API. All entries returned by
     * [getCallLogs] will be sent in one request. The caller should catch
     * exceptions thrown by Retrofit if the network call fails.
     */
    suspend fun syncCallLogs() = withContext(Dispatchers.IO) {
        val logs = getCallLogs()
        val payload = CrmApiService.CallsPayload(logs)
        ApiClient.crmService.syncCalls(payload)
    }
}