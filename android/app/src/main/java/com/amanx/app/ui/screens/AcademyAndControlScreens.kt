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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.amanx.app.data.local.AcademyProgressEntity
import com.amanx.app.data.local.ActivityLogEntity
import com.amanx.app.data.local.RetentionPolicyEntity
import com.amanx.app.data.preferences.UserPreferencesState
import com.amanx.app.domain.model.AcademyLesson
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.FraudSimulationScenario
import com.amanx.app.domain.model.ThemeMode
import com.amanx.app.ui.theme.AmanXCard
import com.amanx.app.ui.theme.AmanXColors
import com.amanx.app.ui.theme.EmptyStateCard

@Composable
fun AcademyHubScreen(
    language: AppLanguage,
    lessons: List<AcademyLesson>,
    simulations: List<FraudSimulationScenario>,
    progress: List<AcademyProgressEntity>,
    onRecordScore: (String, String, Int) -> Unit
) {
    val isAr = language == AppLanguage.AR
    var selectedLesson by remember { mutableStateOf<AcademyLesson?>(null) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var quizFeedback by remember { mutableStateOf<String?>(null) }

    var activeSimulation by remember { mutableStateOf<FraudSimulationScenario?>(null) }
    var simStepIndex by remember { mutableIntStateOf(0) }
    var simFeedback by remember { mutableStateOf<String?>(null) }
    var simScore by remember { mutableIntStateOf(0) }

    val progressMap = remember(progress) { progress.associateBy { it.contentId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isAr) "الأكاديمية الأمنية ومحاكي الاحتيال" else "Security Academy & Fraud Simulator",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (isAr) {
                    "تعمل جميع الدروس والمحاكاة محلياً دون اتصال ودون أي ارتباط بخدمات مصرفية حقيقية."
                } else {
                    "All lessons and simulations run 100% locally offline and are isolated from any real banking services."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            val completedCount = progress.count { it.isCompleted }
            AmanXCard {
                Text(
                    text = if (isAr) "السجل التعليمي الشخصي: $completedCount نشاط مكتمل" else "Personal Learning History: $completedCount completed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Text(if (isAr) "محاكي أساليب الاحتيال التفاعلي" else "Interactive Fraud Simulator", style = MaterialTheme.typography.titleMedium)
        }
        items(simulations, key = { it.id }) { sim ->
            val p = progressMap[sim.id]
            AmanXCard(borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)) {
                Text(sim.title.get(language), style = MaterialTheme.typography.titleMedium)
                Text(sim.contextLabel.get(language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (p != null) {
                    Text(if (isAr) "أفضل نتيجة: ${p.bestScore}%" else "Best score: ${p.bestScore}%", style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = {
                    activeSimulation = sim
                    simStepIndex = 0
                    simFeedback = null
                    simScore = 0
                }) {
                    Text(if (isAr) "بدء المحاكاة" else "Start Simulation")
                }
            }
        }

        item {
            Text(if (isAr) "الدروس الأمنية والاختبارات" else "Security Lessons & Quizzes", style = MaterialTheme.typography.titleMedium)
        }
        items(lessons, key = { it.id }) { lesson ->
            val p = progressMap[lesson.id]
            AmanXCard {
                Text(lesson.title.get(language), style = MaterialTheme.typography.titleMedium)
                Text(lesson.summary.get(language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (p != null) {
                    Text(
                        text = if (isAr) "مكتمل — ${p.bestScore}%" else "Completed — ${p.bestScore}%",
                        color = AmanXColors.SafeGreen,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(onClick = {
                    selectedLesson = lesson
                    selectedAnswer = null
                    quizFeedback = null
                }) {
                    Text(if (isAr) "قراءة الدرس والاختبار" else "Open Lesson & Quiz")
                }
            }
        }
    }

    if (selectedLesson != null) {
        val lesson = selectedLesson!!
        AlertDialog(
            onDismissRequest = { selectedLesson = null },
            title = { Text(lesson.title.get(language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lesson.body.get(language), style = MaterialTheme.typography.bodyMedium)
                    Text(lesson.quizQuestion.get(language), style = MaterialTheme.typography.titleMedium)
                    lesson.quizOptions.forEachIndexed { idx, opt ->
                        FilterChip(
                            selected = selectedAnswer == idx,
                            onClick = {
                                selectedAnswer = idx
                                val correct = idx == lesson.correctOptionIndex
                                val score = if (correct) 100 else 40
                                onRecordScore(lesson.id, "LESSON", score)
                                quizFeedback = if (correct) {
                                    if (isAr) "إجابة صحيحة!" else "Correct answer!"
                                } else {
                                    if (isAr) "إجابة غير دقيقة — راجع الدرس." else "Not quite right — review the lesson."
                                }
                            },
                            label = { Text(opt.get(language)) }
                        )
                    }
                    if (quizFeedback != null) {
                        Text(quizFeedback!!, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLesson = null }) { Text(if (isAr) "إغلاق" else "Close") }
            }
        )
    }

    if (activeSimulation != null) {
        val sim = activeSimulation!!
        val step = sim.steps.getOrNull(simStepIndex)
        AlertDialog(
            onDismissRequest = { activeSimulation = null },
            title = { Text(sim.title.get(language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step != null) {
                        Text(step.prompt.get(language))
                        step.choices.forEach { choice ->
                            OutlinedButton(
                                onClick = {
                                    simFeedback = choice.feedback.get(language)
                                    if (choice.isSafeChoice) simScore += (100 / sim.steps.size)
                                    if (simStepIndex < sim.steps.lastIndex) {
                                        simStepIndex++
                                    } else {
                                        onRecordScore(sim.id, "SIMULATION", simScore.coerceIn(0, 100))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(choice.label.get(language))
                            }
                        }
                        if (simFeedback != null) {
                            Text(simFeedback!!, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSimulation = null }) { Text(if (isAr) "إنهاء" else "Finish") }
            }
        )
    }
}

@Composable
fun ControlCenterScreen(
    language: AppLanguage,
    prefs: UserPreferencesState,
    onNavigate: (String) -> Unit
) {
    val isAr = language == AppLanguage.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = if (isAr) "مركز التحكم والإعدادات" else "AMAN X Control Center",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        val menu = listOf(
            Triple("security_lock_settings", if (isAr) "الحماية والقفل والبيومتري" else "Security, PIN & Biometrics",
                if (prefs.pinConfigured) (if (isAr) "القفل مفعّل" else "Lock active") else (if (isAr) "القفل مؤجل" else "Lock deferred")),
            Triple("privacy_control", if (isAr) "مركز التحكم بالخصوصية والبيانات" else "Privacy Control Center",
                if (isAr) "البيانات، مدد الاحتفاظ، الحذف المؤكد" else "Data review, retention, confirmed wipe"),
            Triple("backup_restore", if (isAr) "النسخ الاحتياطي المشفر والاستعادة" else "Encrypted Backup & Restore",
                if (isAr) "محلي بكلمة مرور ومفتاح استعادة" else "Local password + recovery key"),
            Triple("appearance_language", if (isAr) "المظهر واللغة وإمكانية الوصول" else "Appearance, Language & Accessibility",
                "${prefs.themeMode.name} • ${prefs.language.name}"),
            Triple("activity_history", if (isAr) "سجل النشاط والتاريخ" else "Activity & Security History",
                if (isAr) "تتبع التحليلات والتعديلات" else "Searchable activity audit log"),
            Triple("notification_sources", if (isAr) "مصادر الإشعارات والبنوك" else "Notification Sources & Adapters",
                if (isAr) "إدارة التطبيقات والحساسية" else "Per-app rules & sensitivity"),
            Triple("account_roadmap", if (isAr) "الهوية المحلية والمزامنة المستقبلية" else "Local Identity & Future Sync",
                if (isAr) "يعمل محلياً دون حساب أو خادم" else "Autonomous Local-First mode")
        )
        items(menu) { (route, title, subtitle) ->
            AmanXCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { onNavigate(route) }) {
                        Text(if (isAr) "فتح" else "Open")
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityLockSettingsScreen(
    language: AppLanguage,
    prefs: UserPreferencesState,
    onSetupOrChangePin: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onToggleSmartDedup: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "الأمان وقفل التطبيق" else "Security & App Lock", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }
        item {
            AmanXCard {
                Text(
                    text = if (prefs.pinConfigured) (if (isAr) "قفل PIN مفعّل" else "PIN Lock Enabled") else (if (isAr) "قفل PIN غير مفعّل" else "PIN Not Configured"),
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = onSetupOrChangePin, modifier = Modifier.fillMaxWidth()) {
                    Text(if (prefs.pinConfigured) (if (isAr) "تغيير رمز PIN" else "Change PIN") else (if (isAr) "إعداد PIN الآن" else "Set Up PIN"))
                }
                if (prefs.pinConfigured) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isAr) "الفتح بالبصمة" else "Biometric Unlock")
                        Switch(checked = prefs.biometricEnabled, onCheckedChange = onToggleBiometric)
                    }
                    OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isAr) "قفل التطبيق فوراً" else "Lock App Now")
                    }
                }
            }
        }
        item {
            AmanXCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "منع تكرار التنبيهات الذكي" else "Smart Alert Deduplication", style = MaterialTheme.typography.titleMedium)
                        Text(if (isAr) "يتجاهل الإشعارات المتطابقة خلال نافذة زمنية قصيرة" else "Skips duplicate identical notifications within a short window", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = prefs.smartAlertDeduplication, onCheckedChange = onToggleSmartDedup)
                }
            }
        }
    }
}

@Composable
fun PrivacyControlCenterScreen(
    language: AppLanguage,
    pinConfigured: Boolean,
    policies: List<RetentionPolicyEntity>,
    processedCount: Int,
    alertsCount: Int,
    evidenceCount: Int,
    onSetRetentionDays: (String, Int?) -> Unit,
    onRunCleanup: () -> Unit,
    onOpenExportCenter: () -> Unit,
    onWipeAllData: () -> Unit,
    onSetupPinRequired: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var showWipeStep1 by remember { mutableStateOf(false) }
    var wipeConfirmText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "مركز التحكم بالخصوصية" else "Privacy Control Center", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (!pinConfigured) {
            item {
                AmanXCard(borderColor = AmanXColors.CriticalRed) {
                    Text(if (isAr) "يتطلب مركز الخصوصية إعداد PIN أولاً" else "Privacy Control Requires App PIN")
                    Button(onClick = onSetupPinRequired) { Text(if (isAr) "إعداد PIN" else "Set PIN") }
                }
            }
        } else {
            item {
                AmanXCard {
                    Text(if (isAr) "جرد البيانات المحلية" else "Local Data Inventory", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isAr) {
                            "الإشعارات/النصوص المحللة: $processedCount | التنبيهات: $alertsCount | الأدلة المشفرة: $evidenceCount\nالمزامنة السحابية: غير مفعّلة (لا إرسال خارج الجهاز)."
                        } else {
                            "Analyzed records: $processedCount | Alerts: $alertsCount | Encrypted evidence: $evidenceCount\nCloud sync: Inactive (zero external transmission)."
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenExportCenter) { Text(if (isAr) "تصدير البيانات" else "Export Data") }
                        OutlinedButton(onClick = onRunCleanup) { Text(if (isAr) "تنظيف المنتهي الآن" else "Clean Expired Now") }
                    }
                }
            }

            item {
                Text(if (isAr) "مدد الاحتفاظ اليدوية" else "Manual Retention Policies", style = MaterialTheme.typography.titleMedium)
            }
            items(policies, key = { it.dataType }) { policy ->
                AmanXCard {
                    Text(policy.dataType, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null, 7, 30, 90).forEach { days ->
                            FilterChip(
                                selected = policy.retentionDays == days,
                                onClick = { onSetRetentionDays(policy.dataType, days) },
                                label = { Text(if (days == null) (if (isAr) "يدوي" else "Manual") else "${days}d") }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { showWipeStep1 = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AmanXColors.CriticalRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isAr) "حذف جميع البيانات المحلية (يتطلب تأكيداً إضافياً)" else "Wipe All Local Data (Extra Confirmation)")
                }
            }
        }
    }

    if (showWipeStep1) {
        AlertDialog(
            onDismissRequest = { showWipeStep1 = false },
            title = { Text(if (isAr) "تأكيد الحذف الشامل" else "Confirm Complete Wipe") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isAr) "اكتب DELETE لتأكيد مسح جميع السجلات والأدلة المحلية نهائياً:" else "Type DELETE to permanently erase all local records and evidence:")
                    OutlinedTextField(value = wipeConfirmText, onValueChange = { wipeConfirmText = it }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = wipeConfirmText.trim().equals("DELETE", ignoreCase = true),
                    onClick = {
                        showWipeStep1 = false
                        wipeConfirmText = ""
                        onWipeAllData()
                    }
                ) { Text(if (isAr) "مسح نهائي" else "Erase All") }
            },
            dismissButton = {
                TextButton(onClick = { showWipeStep1 = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun BackupRestoreScreen(
    language: AppLanguage,
    pinConfigured: Boolean,
    lastGeneratedRecoveryKey: String?,
    onClearRecoveryKey: () -> Unit,
    onCreateBackup: (Uri, String) -> Unit,
    onRestoreBackup: (Uri, String, String) -> Unit,
    onSetupPinRequired: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var backupPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var restoreRecoveryKey by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) onCreateBackup(uri, backupPassword)
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackup(uri, restorePassword, restoreRecoveryKey)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "النسخ الاحتياطي المشفر" else "Encrypted Backup & Restore", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (!pinConfigured) {
            item {
                AmanXCard(borderColor = AmanXColors.CriticalRed) {
                    Text(if (isAr) "يلزم تفعيل PIN قبل النسخ أو الاستعادة" else "App PIN Required First")
                    Button(onClick = onSetupPinRequired) { Text(if (isAr) "إعداد PIN" else "Set PIN") }
                }
            }
        } else {
            item {
                AmanXCard {
                    Text(if (isAr) "إنشاء نسخة احتياطية محلية مشفرة" else "Create Encrypted Local Backup", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text(if (isAr) "كلمة مرور النسخة (6 أحرف فأكثر)" else "Backup Password (6+ chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = backupPassword.length >= 6,
                        onClick = { createBackupLauncher.launch("amanx_backup.amxb") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "حفظ النسخة المشفرة" else "Save Encrypted Backup")
                    }
                }
            }

            if (lastGeneratedRecoveryKey != null) {
                item {
                    AmanXCard(borderColor = AmanXColors.RoyalGold) {
                        Text(if (isAr) "مفتاح الاستعادة الخاص بك (احفظه بأمان!)" else "Your Recovery Key (Save Safely!)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(lastGeneratedRecoveryKey, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(if (isAr) "لا يمكن استعادة النسخة بدون كلمة المرور + هذا المفتاح معاً." else "Restore requires BOTH your password and this recovery key.")
                        TextButton(onClick = onClearRecoveryKey) { Text(if (isAr) "تم الحفظ" else "Saved") }
                    }
                }
            }

            item {
                AmanXCard {
                    Text(if (isAr) "استعادة من نسخة مشفرة" else "Restore Encrypted Backup", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text(if (isAr) "كلمة مرور النسخة" else "Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = restoreRecoveryKey,
                        onValueChange = { restoreRecoveryKey = it },
                        label = { Text(if (isAr) "مفتاح الاستعادة (AMX-...)" else "Recovery Key (AMX-...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        enabled = restorePassword.isNotBlank() && restoreRecoveryKey.isNotBlank(),
                        onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "اختيار ملف النسخة واستعادته" else "Select Backup File & Restore")
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryScreen(
    language: AppLanguage,
    logs: List<ActivityLogEntity>,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var query by remember { mutableStateOf("") }
    val filtered = remember(logs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) logs else logs.filter {
            it.titleAr.lowercase().contains(q) || it.titleEn.lowercase().contains(q) ||
                it.detailsAr.lowercase().contains(q) || it.detailsEn.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "سجل النشاط والتاريخ" else "Activity & History Log", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(if (isAr) "بحث في السجل" else "Search history") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (isAr) "السجل فارغ" else "No Activity History",
                    subtitle = if (isAr) "تُسجل هنا عمليات التحليل والتعديل وإدارة الأدلة." else "Analyses, corrections, and evidence actions are logged here."
                )
            }
        } else {
            items(filtered, key = { it.id }) { log ->
                AmanXCard {
                    Text(if (isAr) log.titleAr else log.titleEn, style = MaterialTheme.typography.titleMedium)
                    Text(if (isAr) log.detailsAr else log.detailsEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AppearanceLanguageScreen(
    language: AppLanguage,
    prefs: UserPreferencesState,
    onSetTheme: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "المظهر واللغة وإمكانية الوصول" else "Appearance & Accessibility", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }
        item {
            AmanXCard {
                Text(if (isAr) "المظهر (معاينة فورية)" else "Theme Mode (Live Preview)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { onSetTheme(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
                Text(if (isAr) "اللغة والاتجاه" else "Language & Direction", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = prefs.language == AppLanguage.AR, onClick = { onSetLanguage(AppLanguage.AR) }, label = { Text("العربية (RTL)") })
                    FilterChip(selected = prefs.language == AppLanguage.EN, onClick = { onSetLanguage(AppLanguage.EN) }, label = { Text("English (LTR)") })
                }
            }
        }
        item {
            AmanXCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "تقليل الحركة والمؤثرات" else "Reduce Motion")
                    Switch(checked = prefs.reduceMotion, onCheckedChange = onSetReduceMotion)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "تباين مرتفع" else "High Contrast")
                    Switch(checked = prefs.highContrast, onCheckedChange = onSetHighContrast)
                }
                Text(if (isAr) "حجم الخط: ${"%.2f".format(prefs.fontScaleMultiplier)}x" else "Font Scale: ${"%.2f".format(prefs.fontScaleMultiplier)}x")
                Slider(value = prefs.fontScaleMultiplier, onValueChange = onSetFontScale, valueRange = 0.85f..1.35f)
                OutlinedButton(onClick = onResetDefaults, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isAr) "استعادة الافتراضي" else "Restore Defaults")
                }
            }
        }
    }
}

@Composable
fun AccountRoadmapScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAr) "الهوية المحلية والمزامنة" else "Local Identity & Sync", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }
        item {
            AmanXCard {
                Icon(Icons.Outlined.CloudOff, null, tint = AmanXColors.SafeGreen, modifier = Modifier.size(32.dp))
                Text(
                    text = if (isAr) "هوية محلية مستقلة 100%" else "100% Autonomous Local Identity",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (isAr) {
                        "لا يتطلب AMAN X تسجيل دخول ولا يتصل بأي خادم سحابي في الإصدار الأول. تم تصميم معمارية البيانات بحيث يمكن ربط حساب ومزامنة اختيارية مشفرة مستقبلاً بموافقة مستقلة منك بعد اعتماد البنية السحابية المناسبة."
                    } else {
                        "AMAN X requires no sign-in and connects to zero cloud servers in v1. The architecture is cleanly decoupled to support optional encrypted account sync in a future release only after explicit user opt-in."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
