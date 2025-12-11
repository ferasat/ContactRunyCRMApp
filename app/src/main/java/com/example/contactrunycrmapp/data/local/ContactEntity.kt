package com.example.contactrunycrmapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.contactrunycrmapp.domain.model.SyncStatus

@Entity(tableName = "synced_contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val lastModified: Long,
    val status: SyncStatus
)

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val key: String,
    val lastSyncTime: Long
)
