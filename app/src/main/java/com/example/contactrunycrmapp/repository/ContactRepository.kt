package com.example.contactrunycrmapp.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.example.contactrunycrmapp.api.ApiClient
import com.example.contactrunycrmapp.api.CrmApiService
import com.example.contactrunycrmapp.model.ContactModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads contacts from the device and sends them to the CRM API for syncing.
 */
class ContactRepository(private val contentResolver: ContentResolver) {

    /**
     * Query all contacts from the device. For simplicity this method gathers
     * phone numbers and emails for each contact and treats them all as new.
     */
    suspend fun getContacts(): List<ContactModel> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactModel>()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (c.moveToNext()) {
                val id = c.getString(idIndex)
                val name = c.getString(nameIndex) ?: ""
                val hasPhone = c.getInt(hasPhoneIndex) > 0

                val phones = mutableListOf<String>()
                val emails = mutableListOf<String>()

                if (hasPhone) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phoneCursor?.use { pc ->
                        val numberIndex = pc.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                        while (pc.moveToNext()) {
                            val number = pc.getString(numberIndex)
                            phones.add(number)
                        }
                    }
                }

                // email addresses
                val emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                emailCursor?.use { ec ->
                    val addressIndex = ec.getColumnIndex(
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    )
                    while (ec.moveToNext()) {
                        val email = ec.getString(addressIndex)
                        emails.add(email)
                    }
                }

                contacts.add(
                    ContactModel(
                        id = id,
                        name = name,
                        phones = phones,
                        emails = emails,
                        status = "new",
                        lastModified = System.currentTimeMillis()
                    )
                )
            }
        }
        contacts
    }

    /**
     * Send the contacts to the CRM for synchronisation. On error, the HTTP
     * exception will be thrown. The caller should decide how to handle
     * failures (retry, log, etc.).
     */
    suspend fun syncContacts() = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        val payload = CrmApiService.ContactsPayload(contacts)
        ApiClient.crmService.syncContacts(payload)
    }
}