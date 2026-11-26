package com.amanx.app.data.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.amanx.app.AmanXApplication
import com.amanx.app.MainActivity
import com.amanx.app.R
import com.amanx.app.domain.model.AnalysisInputChannel
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.RiskLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Authorized NotificationListenerService analyzing incoming notifications locally.
 * Skips FLAG_GROUP_SUMMARY, self-notifications, and disabled sources.
 */
class AmanXNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val sourcePkg = sbn.packageName ?: return

        // Ignore own notifications & Android group summaries to prevent duplication
        if (sourcePkg == packageName) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = bigText ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val app = application as? AmanXApplication ?: return
        serviceScope.launch {
            val result = app.container.repository.analyzeAndPersistText(
                title = title,
                body = text,
                sourcePackage = sourcePkg,
                channel = AnalysisInputChannel.NOTIFICATION
            )

            if (!result.wasDeduplicated &&
                (result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.CRITICAL)
            ) {
                val lang = app.container.repository.userPreferences.first().language
                postHighRiskLocalWarning(result.riskLevel, lang, result.alertId)
            }
        }
    }

    private fun postHighRiskLocalWarning(level: RiskLevel, language: AppLanguage, alertId: Long?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (alertId != null) putExtra("extra_alert_id", alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (alertId ?: 1L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (language == AppLanguage.AR) {
            if (level == RiskLevel.CRITICAL) "تحذير أمني حرج من AMAN X" else "تنبيه أمني مرتفع من AMAN X"
        } else {
            if (level == RiskLevel.CRITICAL) "AMAN X Critical Security Warning" else "AMAN X High Risk Alert"
        }
        val body = if (language == AppLanguage.AR) {
            "تم رصد مؤشرات احتيال محتملة في إشعار وارد. اضغط للمراجعة الآمنة."
        } else {
            "Suspicious indicators detected in an incoming notification. Tap to review safely."
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, AmanXApplication.CHANNEL_SECURITY_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        nm.notify((alertId ?: System.currentTimeMillis()).toInt(), builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
