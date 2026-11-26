package com.amanx.app.domain.engine

import com.amanx.app.domain.model.IntegrityStatus
import com.amanx.app.domain.model.LocalizedText
import com.amanx.app.domain.model.ReadinessCheckItem
import com.amanx.app.domain.model.SecurityHealthReport

object SecurityHealthCalculator {

    fun calculate(
        isAppLockConfigured: Boolean,
        isNotificationListenerEnabled: Boolean,
        isPostNotificationsGranted: Boolean,
        isBatteryOptimizationIgnored: Boolean,
        isKeystoreHealthy: Boolean,
        hasRecentBackup: Boolean,
        unreviewedCriticalCount: Int,
        processedNotificationsCount: Int,
        integrityStatus: IntegrityStatus
    ): SecurityHealthReport {
        val items = listOf(
            ReadinessCheckItem(
                id = "app_lock",
                title = LocalizedText("قفل التطبيق بالرمز السري (PIN)", "Application PIN Lock"),
                description = LocalizedText(
                    "يحمي سجل التنبيهات وخزنة الأدلة والتصدير.",
                    "Protects alert logs, Evidence Vault, and exports."
                ),
                isReady = isAppLockConfigured,
                isRequiredForCore = true,
                actionRoute = "security_lock_settings"
            ),
            ReadinessCheckItem(
                id = "notification_listener",
                title = LocalizedText("إذن مراقبة الإشعارات", "Notification Listener Access"),
                description = LocalizedText(
                    "يلزم لقراءة الإشعارات المصرح بها محلياً (يُفعّل يدوياً من إعدادات النظام).",
                    "Required to analyze authorized notifications locally (enabled manually in system settings)."
                ),
                isReady = isNotificationListenerEnabled,
                isRequiredForCore = true,
                actionRoute = "permissions_guide"
            ),
            ReadinessCheckItem(
                id = "post_notifications",
                title = LocalizedText("إذن إرسال تنبيهات الحماية", "Protection Alert Notifications"),
                description = LocalizedText(
                    "يسمح لـ AMAN X بإظهار التحذيرات الحرجة على الجهاز.",
                    "Allows AMAN X to display critical warnings on your device."
                ),
                isReady = isPostNotificationsGranted,
                isRequiredForCore = false,
                actionRoute = "permissions_guide"
            ),
            ReadinessCheckItem(
                id = "battery_optimization",
                title = LocalizedText("استقرار العمل في الخلفية", "Background Stability Exemption"),
                description = LocalizedText(
                    "يقلل احتمالية إيقاف خدمة الإشعارات من قِبل واجهات الشركات المصنعة.",
                    "Reduces the chance of OEM battery managers killing the notification monitor."
                ),
                isReady = isBatteryOptimizationIgnored,
                isRequiredForCore = false,
                actionRoute = "battery_performance"
            ),
            ReadinessCheckItem(
                id = "keystore_integrity",
                title = LocalizedText("سلامة مفاتيح Android Keystore", "Android Keystore Key Health"),
                description = LocalizedText(
                    "يتحقق من جاهزية مفتاح التشفير المحلي للأدلة والبيانات.",
                    "Verifies local hardware-backed or TEE encryption key availability."
                ),
                isReady = isKeystoreHealthy && integrityStatus != IntegrityStatus.FAILED,
                isRequiredForCore = true,
                actionRoute = "data_integrity"
            ),
            ReadinessCheckItem(
                id = "backup_recency",
                title = LocalizedText("حداثة النسخ الاحتياطي المشفر", "Recent Encrypted Backup"),
                description = LocalizedText(
                    "إنشاء نسخة مشفرة محلية لحماية الأدلة والإعدادات عند فقدان الجهاز.",
                    "Maintain a local encrypted backup to recover evidence and settings safely."
                ),
                isReady = hasRecentBackup,
                isRequiredForCore = false,
                actionRoute = "backup_restore"
            )
        )

        var score = 0
        if (isAppLockConfigured) score += 25
        if (isNotificationListenerEnabled) score += 25
        if (isKeystoreHealthy && integrityStatus == IntegrityStatus.PASSED) score += 20
        if (isPostNotificationsGranted) score += 10
        if (isBatteryOptimizationIgnored) score += 10
        if (hasRecentBackup) score += 10
        if (unreviewedCriticalCount > 0) score = (score - 15).coerceAtLeast(10)

        val statusLabel = when {
            unreviewedCriticalCount > 0 -> LocalizedText("توجد تنبيهات حرجة تحتاج مراجعة", "Critical Alerts Need Review")
            score >= 85 -> LocalizedText("جاهزية حماية محلية عالية", "Strong Local Readiness")
            score >= 55 -> LocalizedText("جاهزية متوسطة — راجع المتطلبات", "Moderate Readiness — Review Items")
            else -> LocalizedText("تحتاج الحماية إلى إكمال الإعداد", "Setup Incomplete — Action Recommended")
        }

        return SecurityHealthReport(
            scorePercent = score.coerceIn(0, 100),
            statusLabel = statusLabel,
            readinessItems = items,
            unreviewedCriticalCount = unreviewedCriticalCount,
            processedNotificationsCount = processedNotificationsCount,
            integrityStatus = integrityStatus
        )
    }
}
