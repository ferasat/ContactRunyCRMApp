package com.example.contactrunycrmapp.data.remote

import com.example.contactrunycrmapp.domain.model.CallLogPayload
import com.example.contactrunycrmapp.domain.model.ContactSyncPayload
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    suspend fun syncContacts(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Body payload: ContactSyncPayload
    ): retrofit2.Response<Unit>

    @POST
    suspend fun syncCalls(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Body payload: CallLogPayload
    ): retrofit2.Response<Unit>
}
