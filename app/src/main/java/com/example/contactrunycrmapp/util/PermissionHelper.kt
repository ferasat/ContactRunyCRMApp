package com.example.contactrunycrmapp.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class isolating all dangerous permission checks/requests. Keeping this isolated makes
 * it easier to audit and update dangerous permissions.
 */
object PermissionHelper {
    const val PERMISSION_REQUEST_CODE = 100

    val requiredPermissions = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG
    )

    fun hasReadContacts(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasWriteContacts(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasCallLog(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun hasAllCorePermissions(context: Context): Boolean =
        requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun requestAll(activity: Activity) {
        ActivityCompat.requestPermissions(activity, requiredPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }
}
