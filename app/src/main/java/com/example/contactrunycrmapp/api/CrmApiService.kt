package com.example.contactrunycrmapp.api

import com.example.contactrunycrmapp.model.CallLogEntry
import com.example.contactrunycrmapp.model.ContactModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service definition for communicating with the external CRM.
 * This API assumes there are endpoints for syncing contacts and calls.
 */
interface CrmApiService {

    /**
     * Send a batch of contacts to the CRM. The server should determine which
     * contacts are new, updated or deleted based on the [status] field.
     */
    @POST("contacts/sync")
    suspend fun syncContacts(@Body contactsPayload: ContactsPayload): Response<Unit>

    /**
     * Send a batch of call log entries to the CRM.
     */
    @POST("calls/sync")
    suspend fun syncCalls(@Body callsPayload: CallsPayload): Response<Unit>

    /**
     * Wrapper type for sending contact data to the API.
     */
    data class ContactsPayload(val contacts: List<ContactModel>)

    /**
     * Wrapper type for sending call log data to the API.
     */
    data class CallsPayload(val calls: List<CallLogEntry>)
}
