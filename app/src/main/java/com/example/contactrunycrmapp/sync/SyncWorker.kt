package com.example.contactrunycrmapp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.contactrunycrmapp.repository.CallLogRepository
import com.example.contactrunycrmapp.repository.ContactRepository

/**
 * Background worker that performs contact and call log synchronisation with the CRM.
 * This worker should be scheduled via WorkManager, either periodically or as a
 * one‑off task. If either sync fails, the worker will return [Result.retry]
 * and WorkManager will attempt to run it again according to its backoff policy.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val contactRepo = ContactRepository(applicationContext.contentResolver)
        val callRepo = CallLogRepository(applicationContext)
        return try {
            contactRepo.syncContacts()
            callRepo.syncCallLogs()
            Result.success()
        } catch (e: Exception) {
            // Log the exception and signal a retry.
            e.printStackTrace()
            Result.retry()
        }
    }
}