package com.example.contactrunycrmapp.domain.model

enum class SyncStatus { NEW, UPDATED, DELETED, SYNCED }

data class ContactData(
    val id: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val lastModified: Long,
    val status: SyncStatus = SyncStatus.NEW
)

data class ContactSyncItem(
    val id: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val status: String,
    val lastModified: Long
)

data class ContactSyncPayload(
    val contacts: List<ContactSyncItem>
)

data class CallLogItem(
    val phoneNumber: String,
    val type: String,
    val timestamp: Long,
    val durationSeconds: Long
)

data class CallLogPayload(
    val calls: List<CallLogItem>
)
