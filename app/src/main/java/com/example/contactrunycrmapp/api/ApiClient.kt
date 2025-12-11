package com.example.contactrunycrmapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provides a lazily initialised Retrofit client for talking to the CRM API.
 * To use, access [crmService]. Update [BASE_URL] to point to your real
 * backend before building a release APK.
 */
object ApiClient {

    /** Base URL of your CRM API. Must end with a slash. */
    private const val BASE_URL = "https://example-crm.com/api/"

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            // Log full request/response bodies in debug builds for easier debugging.
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Singleton Retrofit CRM API service instance.
     */
    val crmService: CrmApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CrmApiService::class.java)
    }
}