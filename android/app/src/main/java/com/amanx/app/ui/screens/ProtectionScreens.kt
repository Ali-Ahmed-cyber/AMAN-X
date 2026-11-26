package com.amanx.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amanx.app.domain.model.AdapterVerificationStatus
import com.amanx.app.domain.model.AlertCategory
import com.amanx.app.domain.model.AlertStatus
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.FalsePositiveRule
import com.amanx.app.domain.model.IncidentType
import com.amanx.app.domain.model.NotificationSource
import com.amanx.app.domain.model.ReceiptAnalysis
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.SecurityAlert
import com.amanx.app.ui.theme.AmanXCard
import com.amanx.app.ui.theme.AmanXColors
import com.amanx.app.ui.theme.EmptyStateCard
import com.amanx.app.ui.theme.RiskLevelBadge

@Composable
fun AlertsCenterScreen(
    language: AppLanguage,
    alerts: List<SecurityAlert>,
    onOpenDetail: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    val isAr = language == AppLanguage.AR
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SECURITY, FINANCIAL, CRITICAL
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(alerts, search, selectedFilter) {
        alerts.filter { alert ->
            val matchesFilter = when (selectedFilter) {
                "SECURITY" -> alert.category == AlertCategory.SECURITY_WARNING
                "FINANCIAL" -> alert.category == AlertCategory.FINANCIAL_NOTIFICATION
                "CRITICAL" -> alert.riskLevel == RiskLevel.CRITICAL || alert.riskLevel == RiskLevel.HIGH
                else -> alert.status != AlertStatus.ARCHIVED
            }
            val q = search.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                alert.titleAr.lowercase().contains(q) ||
                alert.titleEn.lowercase().contains(q) ||
                alert.summaryAr.lowercase().contains(q) ||
                alert.summaryEn.lowercase().contains(q) ||
                alert.sourcePackage.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isAr) "مركز التنبيهات والحماية" else "Protection & Alerts Center",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onNavigate("notification_amounts") }, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "المبالغ" else "Amounts")
                }
                OutlinedButton(onClick = { onNavigate("notification_sources") }, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "المصادر" else "Sources")
                }
                OutlinedButton(onClick = { onNavigate("false_positives") }, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "الاستثناءات" else "Tuning")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(if (isAr) "بحث في التنبيهات" else "Search alerts") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text(if (isAr) "الكل" else "All") }
                )
                FilterChip(
                    selected = selectedFilter == "SECURITY",
                    onClick = { selectedFilter = "SECURITY" },
                    label = { Text(if (isAr) "أمنية" else "Security") }
                )
                FilterChip(
                    selected = selectedFilter == "FINANCIAL",
                    onClick = { selectedFilter = "FINANCIAL" },
                    label = { Text(if (isAr) "مالية" else "Financial") }
                )
                FilterChip(
                    selected = selectedFilter == "CRITICAL",
                    onClick = { selectedFilter = "CRITICAL" },
                    label = { Text(if (isAr) "عالية/حرجة" else "High+") }
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.NotificationsOff,
                    title = if (isAr) "لا توجد تنبيهات مطابقة" else "No Matching Alerts",
                    subtitle = if (isAr) {
                        "لا نخلط بين التنبيهات الأمنية وإشعارات المعاملات المالية. جرّب تغيير الفلتر أو فحص نص جديد."
                    } else {
                        "Security warnings and financial notification summaries are strictly separated. Adjust filters or analyze text."
                    }
                )
            }
        } else {
            items(filtered, key = { it.id }) { alert ->
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (alert.category == AlertCategory.FINANCIAL_NOTIFICATION) {
                                    if (isAr) "إشعار معاملة مالية" else "Financial Notification"
                                } else {
                                    if (isAr) "تنبيه أمني" else "Security Warning"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (isAr) alert.titleAr else alert.titleEn,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        RiskLevelBadge(alert.riskLevel, language)
                    }
                    Text(
                        text = if (isAr) alert.summaryAr else alert.summaryEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (alert.status == AlertStatus.NEW) onMarkRead(alert.id)
                            onOpenDetail(alert.id)
                        }) {
                            Text(if (isAr) "التفاصيل" else "Details")
                        }
                        OutlinedButton(onClick = { onArchive(alert.id) }) {
                            Text(if (isAr) "أرشفة" else "Archive")
                        }
                        TextButton(onClick = { confirmDeleteId = alert.id }) {
                            Text(if (isAr) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(if (isAr) "تأكيد الحذف" else "Confirm Deletion") },
            text = { Text(if (isAr) "هل تريد حذف هذا التنبيه نهائياً؟" else "Delete this alert permanently?") },
            confirmButton = {
                Button(onClick = {
                    onDelete(confirmDeleteId!!)
                    confirmDeleteId = null
                }) { Text(if (isAr) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun AlertDetailScreen(
    language: AppLanguage,
    alert: SecurityAlert?,
    loadDecryptedText: suspend (Long) -> String,
    onReportFalsePositive: (Long, String) -> Unit,
    onCreateIncident: (String, IncidentType, RiskLevel, String) -> Unit,
    onTriggerAlarm: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var decryptedBody by remember { mutableStateOf("") }
    var showFpDialog by remember { mutableStateOf(false) }
    var fpNote by remember { mutableStateOf("") }

    LaunchedEffect(alert?.recordId) {
        if (alert != null) {
            decryptedBody = loadDecryptedText(alert.recordId)
        }
    }

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
                    text = if (isAr) "تفاصيل الاستجابة الأمنية" else "Security Response Details",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (alert == null) {
            item {
                EmptyStateCard(
                    title = if (isAr) "التنبيه غير موجود" else "Alert Not Found",
                    subtitle = if (isAr) "ربما تم حذف هذا التنبيه." else "This alert may have been deleted."
                )
            }
        } else {
            item {
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) alert.titleAr else alert.titleEn, style = MaterialTheme.typography.titleLarge)
                        RiskLevelBadge(alert.riskLevel, language)
                    }
                    Text(
                        text = if (isAr) "درجة الثقة في التقييم: ${alert.confidenceScore}%" else "Assessment Confidence: ${alert.confidenceScore}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                AmanXCard {
                    Text(if (isAr) "1. ماذا حدث؟" else "1. What Happened?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (isAr) alert.whatHappenedAr else alert.whatHappenedEn)
                    Text(if (isAr) "2. لماذا ظهر التنبيه؟" else "2. Why It Appeared", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (isAr) alert.whyFlaggedAr else alert.whyFlaggedEn)
                    Text(if (isAr) "3. المعلومات التي استند إليها" else "3. Evidence Basis", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (isAr) alert.evidenceBasisAr else alert.evidenceBasisEn)
                    Text(if (isAr) "4. ماذا ينبغي فعله؟" else "4. Recommended Steps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (isAr) alert.recommendationsAr else alert.recommendationsEn)
                    Text(if (isAr) "5. الإجراء المتاح الآن" else "5. Available Action", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (isAr) alert.suggestedActionAr else alert.suggestedActionEn)
                }
            }

            if (decryptedBody.isNotBlank()) {
                item {
                    AmanXCard {
                        Text(if (isAr) "النص المحلل (محمي ومطمس منه أي OTP)" else "Analyzed Text (OTP-Redacted)", style = MaterialTheme.typography.titleMedium)
                        Text(decryptedBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onCreateIncident(
                                if (isAr) alert.titleAr else alert.titleEn,
                                IncidentType.SUSPECTED_TRANSFER_SCAM,
                                alert.riskLevel,
                                decryptedBody
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "فتح حادث أمني وحفظه" else "Open Security Incident")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showFpDialog = true }, modifier = Modifier.weight(1f)) {
                            Text(if (isAr) "إبلاغ عن تحليل خاطئ" else "Report False Positive")
                        }
                        OutlinedButton(onClick = onTriggerAlarm, modifier = Modifier.weight(1f)) {
                            Text(if (isAr) "إنذار أمني" else "Security Alarm")
                        }
                    }
                }
            }
        }
    }

    if (showFpDialog && alert != null) {
        AlertDialog(
            onDismissRequest = { showFpDialog = false },
            title = { Text(if (isAr) "إبلاغ عن تحليل خاطئ" else "Report False Positive") },
            text = {
                OutlinedTextField(
                    value = fpNote,
                    onValueChange = { fpNote = it },
                    label = { Text(if (isAr) "سبب التصحيح (اختياري)" else "Reason (optional)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onReportFalsePositive(alert.id, fpNote)
                    showFpDialog = false
                }) { Text(if (isAr) "حفظ المراجعة" else "Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showFpDialog = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun NotificationAmountsScreen(
    language: AppLanguage,
    amounts: List<ExtractedAmount>,
    onCorrectAmount: (Long, Long, CurrencyCode) -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var editingAmount by remember { mutableStateOf<ExtractedAmount?>(null) }
    var editInput by remember { mutableStateOf("") }
    var editCurrency by remember { mutableStateOf(CurrencyCode.SDG) }

    val sdgTotal = amounts.filter { it.currency == CurrencyCode.SDG }.sumOf { it.amountMinorUnits }
    val usdTotal = amounts.filter { it.currency == CurrencyCode.USD }.sumOf { it.amountMinorUnits }

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
                    text = if (isAr) "تجميع مبالغ الإشعارات" else "Notification Amounts Summary",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        item {
            AmanXCard(borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)) {
                Text(
                    text = if (isAr) {
                        "تنبيه مهم: إشعار البنك ليس فاتورة أو كشف حساب مصرفياً. هذه الإجماليات مستخرجة فقط من الإشعارات المستلمة أو النصوص المفحوصة، ويمكنك تصحيحها يدوياً."
                    } else {
                        "Important: Bank notifications are NOT invoices or official bank statements. Totals reflect parsed notification texts only and can be corrected manually."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AmanXCard(modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "إجمالي الجنيه السوداني (SDG)" else "Total SDG", style = MaterialTheme.typography.labelLarge)
                    Text(
                        ExtractedAmount(amountMinorUnits = sdgTotal, currency = CurrencyCode.SDG, direction = com.amanx.app.domain.model.TransactionDirection.UNKNOWN).formatted(language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                AmanXCard(modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "إجمالي الدولار (USD)" else "Total USD", style = MaterialTheme.typography.labelLarge)
                    Text(
                        ExtractedAmount(amountMinorUnits = usdTotal, currency = CurrencyCode.USD, direction = com.amanx.app.domain.model.TransactionDirection.UNKNOWN).formatted(language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (amounts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (isAr) "لا توجد مبالغ مستخرجة بعد" else "No Extracted Amounts Yet",
                    subtitle = if (isAr) "ستظهر هنا المبالغ المستخرجة من الإشعارات المالية أو النصوص المفحوصة." else "Amounts parsed from financial notifications or texts will appear here."
                )
            }
        } else {
            items(amounts, key = { it.id }) { amt ->
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(amt.formatted(language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${amt.sourcePackage} • ${amt.direction.name}" +
                                    if (amt.isUserCorrected) (if (isAr) " (معدّل يدوياً)" else " (Manually edited)") else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            editingAmount = amt
                            editInput = (amt.amountMinorUnits / 100.0).toString()
                            editCurrency = amt.currency
                        }) {
                            Text(if (isAr) "تصحيح" else "Correct")
                        }
                    }
                }
            }
        }
    }

    if (editingAmount != null) {
        AlertDialog(
            onDismissRequest = { editingAmount = null },
            title = { Text(if (isAr) "تصحيح المبلغ يدوياً" else "Manual Amount Correction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editInput,
                        onValueChange = { editInput = it },
                        label = { Text(if (isAr) "المبلغ الصحيح" else "Correct Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyCode.entries.forEach { code ->
                            FilterChip(
                                selected = editCurrency == code,
                                onClick = { editCurrency = code },
                                label = { Text(code.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = editInput.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onCorrectAmount(editingAmount!!.id, (parsed * 100).toLong(), editCurrency)
                    }
                    editingAmount = null
                }) { Text(if (isAr) "حفظ" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingAmount = null }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun NotificationSourceControlScreen(
    language: AppLanguage,
    sources: List<NotificationSource>,
    onToggleSource: (String, Boolean) -> Unit,
    onChangeSensitivity: (String, Int) -> Unit,
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
                    text = if (isAr) "التحكم في مصادر الإشعارات" else "Notification Source Control",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
            Text(
                text = if (isAr) {
                    "لا يدّعي AMAN X دعم أي بنك أو محفظة ما لم تُختبر إشعاراتها فعلياً. المصادر المميزة بـ «قيد الاختبار» تعتمد محولات نمطية قابلة للتوسيع."
                } else {
                    "AMAN X never claims verified bank/wallet support before real notification testing. Pattern adapters are clearly labeled."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sources, key = { it.packageName }) { source ->
            AmanXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isAr) source.displayNameAr else source.displayNameEn, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = source.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (source.verificationStatus == AdapterVerificationStatus.TESTED_VERIFIED) {
                                if (isAr) "مصدر مختبر" else "Tested Source"
                            } else {
                                if (isAr) "محوّل نمطي — لم يُختبر على إشعار فعلي بعد" else "Pattern Adapter — Pending real sample test"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (source.verificationStatus == AdapterVerificationStatus.TESTED_VERIFIED) AmanXColors.SafeGreen else AmanXColors.MediumAmber
                        )
                    }
                    Switch(
                        checked = source.isEnabled,
                        onCheckedChange = { onToggleSource(source.packageName, it) }
                    )
                }
                Text(
                    text = if (isAr) "مستوى الحساسية: ${source.sensitivityLevel}/3" else "Sensitivity: ${source.sensitivityLevel}/3",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = source.sensitivityLevel.toFloat(),
                    onValueChange = { onChangeSensitivity(source.packageName, it.toInt()) },
                    valueRange = 1f..3f,
                    steps = 1
                )
            }
        }
    }
}

@Composable
fun FalsePositiveManagementScreen(
    language: AppLanguage,
    rules: List<FalsePositiveRule>,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
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
                    text = if (isAr) "إدارة التنبيهات الخاطئة" else "False Positive Management",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }
        if (rules.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Rule,
                    title = if (isAr) "لا توجد استثناءات محلية" else "No False-Positive Rules Yet",
                    subtitle = if (isAr) "عند الإبلاغ عن تحليل خاطئ من تفاصيل أي تنبيه، ستظهر القاعدة هنا لإدارتها أو إلغائها." else "Reported false alerts appear here so you can toggle or revert tuning anytime."
                )
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.sourcePackage, style = MaterialTheme.typography.titleMedium)
                            Text(rule.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = rule.isActive, onCheckedChange = { onToggleRule(rule.id, it) })
                    }
                    TextButton(onClick = { onDeleteRule(rule.id) }) {
                        Text(if (isAr) "إزالة الاستثناء" else "Remove Rule", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptAnalyzerScreen(
    language: AppLanguage,
    receipts: List<ReceiptAnalysis>,
    pendingSharedImageUri: Uri?,
    onClearPendingImage: () -> Unit,
    onAnalyzeImage: (Uri, String) -> Unit,
    onUpdateReview: (Long, Long?, CurrencyCode?, String?, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var docTitle by remember { mutableStateOf("") }
    var editingReceipt by remember { mutableStateOf<ReceiptAnalysis?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editMerchant by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editRef by remember { mutableStateOf("") }
    var editCurrency by remember { mutableStateOf(CurrencyCode.SDG) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onAnalyzeImage(uri, docTitle.ifBlank { if (isAr) "إيصال مالي" else "Financial Receipt" })
            docTitle = ""
        }
    }

    LaunchedEffect(pendingSharedImageUri) {
        if (pendingSharedImageUri != null) {
            onAnalyzeImage(pendingSharedImageUri, if (isAr) "مستند مشارك" else "Shared Document")
            onClearPendingImage()
        }
    }

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
                    text = if (isAr) "تحليل الإيصالات والفواتير (OCR)" else "Receipts & Invoices OCR",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
            Text(
                text = if (isAr) {
                    "منظومة مستقلة تماماً عن إشعارات البنوك. غياب بعض الحقول لا يعني الاحتيال، كما لا يثبت الفحص النصي أصالة المستند."
                } else {
                    "Completely independent from bank notifications. Missing OCR fields never imply fraud, and text parsing does not prove document authenticity."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            AmanXCard {
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text(if (isAr) "عنوان المستند (اختياري)" else "Document Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.ReceiptLong, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (isAr) "اختيار صورة إيصال أو فاتورة" else "Select Receipt / Invoice Image")
                }
            }
        }

        if (receipts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ReceiptLong,
                    title = if (isAr) "لا توجد مستندات محللة" else "No Documents Analyzed",
                    subtitle = if (isAr) "اختر صورة إيصال أو إثبات عملية لتحليل حقولها محلياً دون اتصال بالإنترنت." else "Choose a receipt or payment proof image to extract available fields locally offline."
                )
            }
        } else {
            items(receipts, key = { it.id }) { receipt ->
                AmanXCard {
                    Text(receipt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isAr) {
                            "الجهة: ${receipt.merchantName ?: "غير مستخرج"} | التاريخ: ${receipt.extractedDateText ?: "غير مستخرج"} | المرجع: ${receipt.referenceNumber ?: "غير مستخرج"}"
                        } else {
                            "Entity: ${receipt.merchantName ?: "Not extracted"} | Date: ${receipt.extractedDateText ?: "Not extracted"} | Ref: ${receipt.referenceNumber ?: "Not extracted"}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isAr) receipt.missingFieldsNoteAr else receipt.missingFieldsNoteEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmanXColors.MediumAmber
                    )
                    OutlinedButton(onClick = {
                        editingReceipt = receipt
                        editAmount = receipt.extractedAmountMinorUnits?.let { (it / 100.0).toString() }.orEmpty()
                        editMerchant = receipt.merchantName.orEmpty()
                        editDate = receipt.extractedDateText.orEmpty()
                        editRef = receipt.referenceNumber.orEmpty()
                        editCurrency = receipt.currency ?: CurrencyCode.SDG
                    }) {
                        Text(if (isAr) "مراجعة وتصحيح" else "Review & Edit")
                    }
                }
            }
        }
    }

    if (editingReceipt != null) {
        AlertDialog(
            onDismissRequest = { editingReceipt = null },
            title = { Text(if (isAr) "مراجعة بيانات المستند" else "Review Document Fields") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editMerchant, onValueChange = { editMerchant = it }, label = { Text(if (isAr) "الجهة" else "Entity") })
                    OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, label = { Text(if (isAr) "المبلغ" else "Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = editDate, onValueChange = { editDate = it }, label = { Text(if (isAr) "التاريخ" else "Date") })
                    OutlinedTextField(value = editRef, onValueChange = { editRef = it }, label = { Text(if (isAr) "المرجع" else "Reference") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val minor = editAmount.toDoubleOrNull()?.let { (it * 100).toLong() }
                    onUpdateReview(editingReceipt!!.id, minor, editCurrency, editMerchant, editDate, editRef)
                    editingReceipt = null
                }) { Text(if (isAr) "حفظ" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingReceipt = null }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}
