package com.example.contactrunycrmapp.model

/**
 * Data model representing a contact on the device. This mirrors what the CRM
 * expects to receive when syncing contacts. The [status] field can be used
 * by the sync logic to indicate whether the contact is new, updated or deleted.
 */
data class ContactModel(
    val id: String,
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val status: String,
    val lastModified: Long
)
