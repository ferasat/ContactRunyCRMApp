package com.example.contactrunycrmapp.util

import com.example.contactrunycrmapp.BuildConfig

/**
 * Central place to expose CRM configuration. Changing values in BuildConfig makes it easy to
 * point to different environments without touching the rest of the code.
 */
object Config {
    const val CONTACT_SYNC_ENDPOINT = "/contacts/sync"
    const val CALL_SYNC_ENDPOINT = "/calls/sync"

    val baseUrl: String
        get() = BuildConfig.CRM_BASE_URL

    val apiKey: String
        get() = BuildConfig.CRM_API_KEY
}
