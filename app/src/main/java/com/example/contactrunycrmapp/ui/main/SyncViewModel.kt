package com.example.contactrunycrmapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.contactrunycrmapp.data.repository.CallLogRepository
import com.example.contactrunycrmapp.data.repository.ContactRepository
import com.example.contactrunycrmapp.work.SyncWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository(application)
    private val callLogRepository = CallLogRepository(application)

    private val _lastSync = MutableLiveData<Long>()
    val lastSync: LiveData<Long> = _lastSync

    private val _contactSynced = MutableLiveData<Int>()
    val contactSynced: LiveData<Int> = _contactSynced

    private val _callSynced = MutableLiveData<Int>()
    val callSynced: LiveData<Int> = _callSynced

    private val _info = MutableLiveData<String>()
    val info: LiveData<String> = _info

    init {
        viewModelScope.launch {
            // Load persisted sync timestamps if present.
            val db = com.example.contactrunycrmapp.data.local.AppDatabase.getInstance(application)
            val contactsMeta = db.contactDao().getMetadata("contacts_last_sync")?.lastSyncTime ?: 0L
            val callsMeta = db.contactDao().getMetadata(CallLogRepository.KEY_CALL_SYNC)?.lastSyncTime ?: 0L
            _lastSync.postValue(maxOf(contactsMeta, callsMeta))
        }
    }

    fun syncNow(includeCalls: Boolean) {
        viewModelScope.launch {
            val contactResult = runCatching { contactRepository.syncContacts() }.getOrElse { 0 to false }
            _contactSynced.value = contactResult.first

            val callResult = if (includeCalls) {
                runCatching { callLogRepository.syncCalls() }.getOrElse { 0 to false }
            } else 0 to true
            _callSynced.value = callResult.first

            _info.value = when {
                !contactResult.second || !callResult.second -> "Sync completed with errors. Check CRM endpoint or permissions."
                else -> "Sync successful."
            }
            _lastSync.value = System.currentTimeMillis()
        }
    }

    fun schedulePeriodicSync(intervalHours: Long = 2, requireUnmetered: Boolean = true, requireCharging: Boolean = true) {
        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(SyncWorker.buildConstraints(requireUnmetered, requireCharging))
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun addSampleContact(name: String, phone: String, email: String?) {
        viewModelScope.launch {
            runCatching { contactRepository.addSampleContact(name, phone, email) }
                .onSuccess { _info.postValue("Added sample contact $name") }
                .onFailure { _info.postValue("Failed to add contact: ${it.message}") }
        }
    }

    fun deleteContactByName(name: String) {
        viewModelScope.launch {
            val deleted = runCatching { contactRepository.deleteContactByName(name) }.getOrDefault(0)
            _info.postValue(if (deleted > 0) "Deleted $deleted contacts named $name" else "No contacts removed")
        }
    }
}
