package com.example.contactrunycrmapp.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.contactrunycrmapp.data.repository.CallLogRepository
import com.example.contactrunycrmapp.data.repository.ContactRepository
import com.example.contactrunycrmapp.util.PermissionHelper

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private val contactRepository = ContactRepository(appContext)
    private val callLogRepository = CallLogRepository(appContext)

    override suspend fun doWork(): Result {
        // Respect permissions; skip portions that are not granted.
        val hasContactPermission = PermissionHelper.hasReadContacts(applicationContext)
        val hasCallPermission = PermissionHelper.hasCallLog(applicationContext)

        return try {
            if (hasContactPermission) {
                contactRepository.syncContacts()
            }
            if (hasCallPermission) {
                callLogRepository.syncCalls()
            }
            Result.success()
        } catch (ex: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "periodic_contact_call_sync"

        fun buildConstraints(requireUnmetered: Boolean, requireCharging: Boolean): Constraints {
            val builder = Constraints.Builder()
            if (requireUnmetered) builder.setRequiredNetworkType(NetworkType.UNMETERED) else builder.setRequiredNetworkType(NetworkType.CONNECTED)
            if (requireCharging) builder.setRequiresCharging(true)
            return builder.build()
        }
    }
}
