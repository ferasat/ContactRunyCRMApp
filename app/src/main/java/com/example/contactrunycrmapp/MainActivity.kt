package com.example.contactrunycrmapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.widget.Button
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.contactrunycrmapp.sync.SyncWorker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissionsIfNeeded()

        // Schedule periodic background sync when the activity is created. This will
        // continue running even if the app is in the background as long as the
        // WorkManager constraints are met (default constraints: none). See
        // setupPeriodicSync() for more details.
        setupPeriodicSync()

        // Set up the Sync Now button. When clicked, this enqueues a one‑time
        // WorkManager job that executes SyncWorker immediately.
        val syncButton: Button = findViewById(R.id.btnSync)
        syncButton.setOnClickListener {
            val work = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(this).enqueue(work)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        val toRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 0)
        }
    }

    /**
     * Schedule periodic background sync using WorkManager. This method enqueues
     * a unique periodic work request that runs every 2 hours. If a periodic
     * request with the same name already exists it will be kept rather than
     * replaced.
     */
    private fun setupPeriodicSync() {
        val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "contactCallSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}
