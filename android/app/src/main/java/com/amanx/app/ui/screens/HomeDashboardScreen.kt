package com.amanx.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddModerator
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amanx.app.data.local.ActivityLogEntity
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.SecurityAlert
import com.amanx.app.domain.model.SecurityHealthReport
import com.amanx.app.ui.theme.AmanXCard
import com.amanx.app.ui.theme.AmanXColors
import com.amanx.app.ui.theme.EmptyStateCard
import com.amanx.app.ui.theme.RiskLevelBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    language: AppLanguage,
    healthReport: SecurityHealthReport?,
    alerts: List<SecurityAlert>,
    amounts: List<ExtractedAmount>,
    processedCount: Int,
    recentActivities: List<ActivityLogEntity>,
    isOnline: Boolean,
    isListenerEnabled: Boolean,
    pendingQuickText: String?,
    onClearPendingQuickText: () -> Unit,
    onAnalyzeText: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenAlertDetail: (Long) -> Unit
) {
    val isAr = language == AppLanguage.AR
    var showQuickAnalyzeSheet by remember(pendingQuickText) { mutableStateOf(pendingQuickText != null) }
    var quickInputText by remember(pendingQuickText) { mutableStateOf(pendingQuickText.orEmpty()) }

    val criticalCount = alerts.count { it.riskLevel == RiskLevel.CRITICAL }
    val sdgTotal = amounts.filter { it.currency == CurrencyCode.SDG }.sumOf { it.amountMinorUnits }
    val usdTotal = amounts.filter { it.currency == CurrencyCode.USD }.sumOf { it.amountMinorUnits }

    val shieldColor = when {
        criticalCount > 0 -> AmanXColors.CriticalRed
        alerts.any { it.riskLevel == RiskLevel.HIGH } -> AmanXColors.HighOrange
        (healthReport?.scorePercent ?: 0) >= 75 -> AmanXColors.SafeGreen
        else -> MaterialTheme.colorScheme.primary
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Smart Shield & Protection Status Card
        item {
            AmanXCard(borderColor = shieldColor.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = shieldColor,
                            modifier = Modifier.size(44.dp)
                        )
                        Column {
                            Text(
                                text = "AMAN X",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = healthReport?.statusLabel?.get(language)
                                    ?: if (isAr) "جارٍ تقييم الجاهزية المحلية" else "Assessing local readiness",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "${healthReport?.scorePercent ?: 0}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = shieldColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (healthReport?.scorePercent ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = shieldColor
                )

                Text(
                    text = if (isAr) {
                        "الحالة الآمنة نسبياً تعكس فحص المؤشرات المحلية المتاحة فقط ولا تعني منع كل التهديدات أو المعاملات."
                    } else {
                        "Local status reflects available on-device indicators only and does not guarantee prevention of all threats."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showQuickAnalyzeSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "فحص نص" else "Analyze Text")
                    }
                    FilledTonalButton(
                        onClick = { onNavigate("readiness_check") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "فحص الجاهزية" else "Readiness")
                    }
                }
            }
        }

        // 2. Real Metrics Grid (Processed, Critical, Extracted SDG/USD)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AmanXCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "الإشعارات المعالجة" else "Processed Items",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = processedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAr) "حرجة: $criticalCount" else "Critical: $criticalCount",
                        color = if (criticalCount > 0) AmanXColors.CriticalRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                AmanXCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "ملخص مبالغ الإشعارات" else "Notification Amounts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ExtractedAmount(amountMinorUnits = sdgTotal, currency = CurrencyCode.SDG, direction = com.amanx.app.domain.model.TransactionDirection.UNKNOWN).formatted(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ExtractedAmount(amountMinorUnits = usdTotal, currency = CurrencyCode.USD, direction = com.amanx.app.domain.model.TransactionDirection.UNKNOWN).formatted(language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 3. Offline & Permissions Status Strip
        item {
            AmanXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = AmanXColors.SafeGreen)
                        Text(
                            text = if (isAr) {
                                if (isOnline) "الحماية المحلية نشطة (الجهاز متصل — لا إرسال للخارج)" else "وضع عدم الاتصال: الحماية المحلية تعمل 100%"
                            } else {
                                if (isOnline) "Local Protection Active (Device Online — Zero Upload)" else "Offline Mode: Local Protection 100% Active"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) {
                            "مراقب الإشعارات: " + if (isListenerEnabled) "مفعّل" else "غير مفعّل"
                        } else {
                            "Notification Monitor: " + if (isListenerEnabled) "Enabled" else "Not Enabled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isListenerEnabled) AmanXColors.SafeGreen else AmanXColors.MediumAmber
                    )
                    OutlinedButton(onClick = { onNavigate("offline_status") }) {
                        Text(if (isAr) "مركز Offline" else "Offline Center")
                    }
                }
            }
        }

        // 4. Quick Section Shortcuts (All real routes)
        item {
            Text(
                text = if (isAr) "الاختصارات السريعة" else "Quick Shortcuts",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onNavigate("security_health") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.HealthAndSafety, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "الصحة الأمنية" else "Health")
                    }
                    OutlinedButton(onClick = { onNavigate("receipt_analyzer") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ReceiptLong, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "فحص إيصال" else "Receipts OCR")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onNavigate("notification_sources") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.NotificationsActive, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "مصادر الإشعارات" else "Sources")
                    }
                    OutlinedButton(onClick = { onNavigate("notification_amounts") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.AddModerator, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (isAr) "ملخص المبالغ" else "Amounts")
                    }
                }
            }
        }

        // 5. Recent Alerts (Real Data or Helpful Empty State)
        item {
            Text(
                text = if (isAr) "آخر التنبيهات" else "Recent Alerts",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (alerts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Shield,
                    title = if (isAr) "لا توجد تنبيهات مسجلة بعد" else "No Recorded Alerts Yet",
                    subtitle = if (isAr) {
                        "استخدم «فحص نص» للصق رسالة مشبوهة، أو فعّل مراقب الإشعارات لتحليل الإشعارات المصرح بها محلياً."
                    } else {
                        "Tap 'Analyze Text' to check a suspicious message, or enable Notification Monitor to parse authorized notifications locally."
                    }
                )
            }
        } else {
            items(alerts.take(5), key = { it.id }) { alert ->
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) alert.titleAr else alert.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        RiskLevelBadge(alert.riskLevel, language)
                    }
                    Text(
                        text = if (isAr) alert.summaryAr else alert.summaryEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { onOpenAlertDetail(alert.id) }) {
                        Text(if (isAr) "مراجعة التفاصيل" else "Review Details")
                    }
                }
            }
        }

        // 6. Recent Security Activities
        if (recentActivities.isNotEmpty()) {
            item {
                Text(
                    text = if (isAr) "آخر الأنشطة الأمنية" else "Recent Security Activities",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(recentActivities.take(3), key = { it.id }) { log ->
                AmanXCard {
                    Text(
                        text = if (isAr) log.titleAr else log.titleEn,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isAr) log.detailsAr else log.detailsEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showQuickAnalyzeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showQuickAnalyzeSheet = false
                onClearPendingQuickText()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAr) "فحص رسالة أو نص محلياً" else "Local Message / Text Analysis",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (isAr) {
                        "ألصق نص الرسالة لتحليله محلياً دون طلب إذن SMS ودون أي اتصال بالإنترنت. يتم طمس أي رمز OTP تلقائياً."
                    } else {
                        "Paste message text to analyze locally without READ_SMS or internet. Any OTP digits are automatically redacted."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = quickInputText,
                    onValueChange = { quickInputText = it },
                    label = { Text(if (isAr) "النص المراد فحصه" else "Text to analyze") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onAnalyzeText(quickInputText)
                        quickInputText = ""
                        showQuickAnalyzeSheet = false
                        onClearPendingQuickText()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAr) "بدء التحليل المحلي" else "Run Local Analysis")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SecurityHealthAndReadinessScreen(
    language: AppLanguage,
    report: SecurityHealthReport?,
    isReadinessOnly: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isReadinessOnly) {
                        if (isAr) "فحص الجاهزية للحماية" else "Protection Readiness Check"
                    } else {
                        if (isAr) "لوحة الصحة الأمنية" else "Security Health Dashboard"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) {
                    Text(if (isAr) "رجوع" else "Back")
                }
            }
        }

        if (report != null) {
            item {
                AmanXCard {
                    Text(
                        text = "${report.scorePercent}% — ${report.statusLabel.get(language)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { report.scorePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Text(
                        text = if (isAr) {
                            "التنبيهات الحرجة غير المراجعة: ${report.unreviewedCriticalCount} | الإشعارات المعالجة: ${report.processedNotificationsCount}"
                        } else {
                            "Unreviewed critical alerts: ${report.unreviewedCriticalCount} | Processed: ${report.processedNotificationsCount}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(report.readinessItems, key = { it.id }) { item ->
                AmanXCard(
                    borderColor = if (item.isReady) AmanXColors.SafeGreen.copy(alpha = 0.5f)
                    else AmanXColors.MediumAmber.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (item.isReady) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = if (item.isReady) AmanXColors.SafeGreen else AmanXColors.MediumAmber
                            )
                            Text(item.title.get(language), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text(
                        text = item.description.get(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.isReady) {
                        OutlinedButton(onClick = {
                            when (item.actionRoute) {
                                "permissions_guide" -> context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                "battery_performance" -> context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                                else -> onNavigate(item.actionRoute)
                            }
                        }) {
                            Text(if (isAr) "إصلاح / ضبط الآن" else "Fix / Configure Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineStatusCenterScreen(
    language: AppLanguage,
    isOnline: Boolean,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "مركز حالة عدم الاتصال" else "Offline Status Center",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }
        item {
            AmanXCard {
                Text(
                    text = if (isAr) {
                        "حالة الشبكة الحالية: ${if (isOnline) "متصل بالإنترنت (AMAN X لا يستخدم الشبكة في v1)" else "غير متصل (وضع Offline الكامل)"}"
                    } else {
                        "Current Network State: ${if (isOnline) "Online (AMAN X makes zero network calls in v1)" else "Offline (100% Autonomous)"}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = AmanXColors.SafeGreen
                )
                Text(
                    text = if (isAr) "إصدار قواعد الحماية المدمجة: 1.0.0-local (تُحدّث مع إصدارات التطبيق)" else "Bundled Ruleset Version: 1.0.0-local (Updated with app releases)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            val modules = if (isAr) listOf(
                "• محرك تحليل النصوص والإشعارات: نشط محلياً 100%",
                "• استخراج مبالغ SDG / USD: نشط محلياً 100%",
                "• خزنة الأدلة والتشفير والتحقق SHA-256: نشط محلياً 100%",
                "• فحص الإيصالات بالـ OCR المحلي: نشط محلياً 100%",
                "• الأكاديمية والمحاكي التفاعلي: نشط محلياً 100%",
                "• النسخ الاحتياطي المشفر والاستعادة: نشط محلياً 100%",
                "• المزامنة السحابية الاختيارية: مؤجلة لما بعد الإصدار الأول"
            ) else listOf(
                "• Text & Notification Analysis Engine: 100% Active Locally",
                "• SDG / USD Amount Extraction: 100% Active Locally",
                "• Evidence Vault, Encryption & SHA-256 Monitor: 100% Active Locally",
                "• Local Receipt OCR Analyzer: 100% Active Locally",
                "• Security Academy & Fraud Simulator: 100% Active Locally",
                "• Encrypted Local Backup & Restore: 100% Active Locally",
                "• Optional Cloud Sync: Deferred until post-v1"
            )
            AmanXCard {
                modules.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
