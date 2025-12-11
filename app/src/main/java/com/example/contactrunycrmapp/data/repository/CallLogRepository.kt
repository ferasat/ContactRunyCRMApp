package com.example.contactrunycrmapp.data.repository

import android.content.Context
import android.provider.CallLog
import com.example.contactrunycrmapp.data.local.AppDatabase
import com.example.contactrunycrmapp.data.local.SyncMetadata
import com.example.contactrunycrmapp.data.remote.RetrofitProvider
import com.example.contactrunycrmapp.domain.model.CallLogItem
import com.example.contactrunycrmapp.domain.model.CallLogPayload
import com.example.contactrunycrmapp.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Call log access lives in its own class so it can be easily disabled (remove permission and this
 * file will compile out). Note: Google Play restricts CALL_LOG permissions to default dialer/
 * assistant apps; non-privileged apps may be rejected during review.
 */
class CallLogRepository(private val context: Context) {
    private val contentResolver = context.contentResolver
    private val contactDao = AppDatabase.getInstance(context).contactDao()

    suspend fun readRecentCalls(): List<CallLogItem> = withContext(Dispatchers.IO) {
        val since = contactDao.getMetadata(KEY_CALL_SYNC)?.lastSyncTime ?: 0L
        val calls = mutableListOf<CallLogItem>()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val selection = if (since > 0) "${CallLog.Calls.DATE} > ?" else null
        val selectionArgs = if (since > 0) arrayOf(since.toString()) else null
        contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val type = when (cursor.getInt(typeIndex)) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "OTHER"
                }
                val date = cursor.getLong(dateIndex)
                val duration = cursor.getLong(durationIndex)
                calls.add(CallLogItem(number, type, date, duration))
            }
        }
        calls
    }

    suspend fun syncCalls(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val calls = readRecentCalls()
        if (calls.isEmpty()) return@withContext 0 to true
        val response = RetrofitProvider.api.syncCalls(
            url = Config.baseUrl + Config.CALL_SYNC_ENDPOINT,
            apiKey = Config.apiKey,
            payload = CallLogPayload(calls)
        )
        val success = response.isSuccessful
        if (success) {
            contactDao.saveMetadata(SyncMetadata(KEY_CALL_SYNC, System.currentTimeMillis()))
        }
        calls.size to success
    }

    companion object {
        const val KEY_CALL_SYNC = "call_log_last_sync"
    }
}
