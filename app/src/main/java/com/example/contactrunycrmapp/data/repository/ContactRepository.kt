package com.example.contactrunycrmapp.data.repository

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.example.contactrunycrmapp.data.local.AppDatabase
import com.example.contactrunycrmapp.data.local.ContactEntity
import com.example.contactrunycrmapp.data.local.SyncMetadata
import com.example.contactrunycrmapp.data.remote.RetrofitProvider
import com.example.contactrunycrmapp.domain.model.ContactData
import com.example.contactrunycrmapp.domain.model.ContactSyncItem
import com.example.contactrunycrmapp.domain.model.ContactSyncPayload
import com.example.contactrunycrmapp.domain.model.SyncStatus
import com.example.contactrunycrmapp.util.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val contactDao = AppDatabase.getInstance(context).contactDao()

    /**
     * Query device contacts with names, phones, and emails.
     */
    suspend fun readDeviceContacts(): List<ContactData> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactData>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
        )
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val lastIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex) ?: "Unnamed"
                val lastModified = cursor.getLong(lastIndex)
                val phones = fetchPhones(id)
                val emails = fetchEmails(id)
                contacts.add(ContactData(id, name, phones, emails, lastModified))
            }
        }
        contacts
    }

    private fun fetchPhones(contactId: String): List<String> {
        val numbers = mutableListOf<String>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                numbers.add(cursor.getString(numberIndex))
            }
        }
        return numbers
    }

    private fun fetchEmails(contactId: String): List<String> {
        val emails = mutableListOf<String>()
        contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            val emailIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                emails.add(cursor.getString(emailIndex))
            }
        }
        return emails
    }

    /**
     * Build payload by comparing device contacts vs. last synced contacts.
     */
    suspend fun buildContactPayload(): ContactSyncPayload = withContext(Dispatchers.IO) {
        val deviceContacts = readDeviceContacts()
        val synced = contactDao.getAll().associateBy { it.id }

        val payloadItems = mutableListOf<ContactSyncItem>()
        deviceContacts.forEach { contact ->
            val existing = synced[contact.id]
            val status = when {
                existing == null -> SyncStatus.NEW
                existing.name != contact.name || existing.phones.toSet() != contact.phones.toSet() || existing.emails.toSet() != contact.emails.toSet() -> SyncStatus.UPDATED
                else -> SyncStatus.SYNCED
            }
            if (status != SyncStatus.SYNCED) {
                payloadItems.add(contact.toPayload(status))
            }
        }

        // Detect deletions
        synced.values.filter { existing -> deviceContacts.none { it.id == existing.id } }
            .forEach { deleted ->
                payloadItems.add(
                    ContactSyncItem(
                        id = deleted.id,
                        name = deleted.name,
                        phones = deleted.phones,
                        emails = deleted.emails,
                        status = SyncStatus.DELETED.name.lowercase(),
                        lastModified = deleted.lastModified
                    )
                )
            }

        ContactSyncPayload(payloadItems)
    }

    /**
     * Persist synced contacts locally.
     */
    private suspend fun persistSyncedContacts(deviceContacts: List<ContactData>) {
        val entities = deviceContacts.map {
            ContactEntity(
                id = it.id,
                name = it.name,
                phones = it.phones,
                emails = it.emails,
                lastModified = it.lastModified,
                status = SyncStatus.SYNCED
            )
        }
        contactDao.clear()
        contactDao.insertAll(entities)
        contactDao.saveMetadata(SyncMetadata("contacts_last_sync", System.currentTimeMillis()))
    }

    suspend fun syncContacts(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val deviceContacts = readDeviceContacts()
        val payload = buildContactPayload()
        val response = RetrofitProvider.api.syncContacts(
            url = Config.baseUrl + Config.CONTACT_SYNC_ENDPOINT,
            apiKey = Config.apiKey,
            payload = payload
        )
        val success = response.isSuccessful
        if (success) {
            persistSyncedContacts(deviceContacts)
        }
        payload.contacts.size to success
    }

    /**
     * Inserts a sample contact to demonstrate write capability.
     */
    suspend fun addSampleContact(name: String, phone: String, email: String?) = withContext(Dispatchers.IO) {
        val ops = arrayListOf<ContentProviderOperation>()
        val rawContactInsertIndex = ops.size
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        if (!email.isNullOrBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build()
            )
        }
        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    /**
     * Delete a contact by display name. This is a simple demo to show deletion support.
     */
    suspend fun deleteContactByName(name: String): Int = withContext(Dispatchers.IO) {
        contentResolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY}=?",
            arrayOf(name)
        )
    }
}

private fun ContactData.toPayload(status: SyncStatus): ContactSyncItem = ContactSyncItem(
    id = id,
    name = name,
    phones = phones,
    emails = emails,
    status = status.name.lowercase(),
    lastModified = lastModified
)
