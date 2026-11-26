package com.amanx.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amanx.app.AmanXApplication

class RetentionCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AmanXApplication ?: return Result.failure()
        return try {
            app.container.repository.executeRetentionCleanup()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
