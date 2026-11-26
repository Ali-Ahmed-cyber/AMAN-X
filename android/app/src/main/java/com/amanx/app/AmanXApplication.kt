package com.amanx.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amanx.app.data.local.AmanXDatabase
import com.amanx.app.data.preferences.AmanXPreferencesDataStore
import com.amanx.app.data.receipt.LocalReceiptOcrAnalyzer
import com.amanx.app.data.repository.AmanXRepository
import com.amanx.app.data.security.CryptoManager
import com.amanx.app.data.worker.RetentionCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val cryptoManager = CryptoManager()
    val preferences = AmanXPreferencesDataStore(context, cryptoManager)
    val database = AmanXDatabase.getInstance(context)
    val receiptOcrAnalyzer = LocalReceiptOcrAnalyzer(context)
    val repository = AmanXRepository(
        context = context,
        db = database,
        preferences = preferences,
        cryptoManager = cryptoManager,
        receiptOcrAnalyzer = receiptOcrAnalyzer
    )
}

class AmanXApplication : Application() {
    companion object {
        const val CHANNEL_SECURITY_ALERTS = "amanx_security_alerts"
    }

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        scheduleRetentionWorker()
        appScope.launch {
            container.repository.ensureDefaultSeeded()
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_SECURITY_ALERTS,
            "AMAN X Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical and high-risk local security warnings"
        }
        manager.createNotificationChannel(channel)
    }

    private fun scheduleRetentionWorker() {
        val request = PeriodicWorkRequestBuilder<RetentionCleanupWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "amanx_retention_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
