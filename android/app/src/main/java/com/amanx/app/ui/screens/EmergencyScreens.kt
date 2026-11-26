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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amanx.app.data.local.ExportLogEntity
import com.amanx.app.data.local.IncidentTimelineEntity
import com.amanx.app.data.local.IntegrityCheckLogEntity
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.EvidenceItem
import com.amanx.app.domain.model.EvidenceType
import com.amanx.app.domain.model.Incident
import com.amanx.app.domain.model.IncidentRecoveryStep
import com.amanx.app.domain.model.IncidentType
import com.amanx.app.domain.model.IntegrityStatus
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.ui.theme.AmanXCard
import com.amanx.app.ui.theme.AmanXColors
import com.amanx.app.ui.theme.EmptyStateCard
import com.amanx.app.ui.theme.RiskLevelBadge
import kotlinx.coroutines.flow.Flow

@Composable
fun EmergencyCenterScreen(
    language: AppLanguage,
    incidents: List<Incident>,
    isAlarmActive: Boolean,
    alarmVibration: Boolean,
    alarmSound: Boolean,
    pinConfigured: Boolean,
    onSetAlarmOptions: (Boolean, Boolean) -> Unit,
    onTriggerAlarm: () -> Unit,
    onStopAlarm: () -> Unit,
    onCreateIncident: (String, IncidentType, RiskLevel, String) -> Unit,
    onOpenIncident: (Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    val isAr = language == AppLanguage.AR
    var showNewIncidentDialog by remember { mutableStateOf(false) }
    var incidentTitle by remember { mutableStateOf("") }
    var incidentType by remember { mutableStateOf(IncidentType.SUSPECTED_TRANSFER_SCAM) }
    var incidentNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isAr) "مركز الطوارئ والاستجابة" else "Emergency & Response Center",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (!pinConfigured) {
            item {
                AmanXCard(borderColor = AmanXColors.MediumAmber) {
                    Text(
                        text = if (isAr) "يلزم إعداد قفل التطبيق للوظائف الحساسة" else "PIN Lock Required for Sensitive Vaults",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmanXColors.MediumAmber
                    )
                    Text(
                        text = if (isAr) {
                            "قمت بتأجيل إعداد رمز PIN. لحماية الأدلة والتصدير، يجب تعيين PIN أولاً."
                        } else {
                            "You deferred PIN setup. To protect your Evidence Vault and Secure Exports, configure a PIN first."
                        }
                    )
                    Button(onClick = { onNavigate("setup_pin_required") }) {
                        Text(if (isAr) "إعداد رمز PIN الآن" else "Set Up PIN Now")
                    }
                }
            }
        }

        // Security Alarm & Guided Manual Response Card
        item {
            AmanXCard(borderColor = if (isAlarmActive) AmanXColors.CriticalRed else MaterialTheme.colorScheme.outline) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isAlarmActive) Icons.Outlined.Campaign else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (isAlarmActive) AmanXColors.CriticalRed else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isAr) "الإنذار الأمني والاستجابة اليدوية" else "Security Alarm & Guided Response",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isAr) {
                                "تنبيه واضح: AMAN X لا يستطيع إيقاف تحويل مصرفي أو تجميد حساب بنكي. اتبع الخطوات وتواصل مع بنكك رسمياً."
                            } else {
                                "Notice: AMAN X cannot stop bank transfers or freeze accounts. Follow the guide and contact your bank directly."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isAr) "الاهتزاز" else "Vibration")
                    Switch(checked = alarmVibration, onCheckedChange = { onSetAlarmOptions(it, alarmSound) })
                    Text(if (isAr) "الصوت" else "Sound")
                    Switch(checked = alarmSound, onCheckedChange = { onSetAlarmOptions(alarmVibration, it) })
                }

                if (isAlarmActive) {
                    Button(
                        onClick = onStopAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = AmanXColors.CriticalRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "إيقاف الإنذار الآن" else "Stop Security Alarm")
                    }
                } else {
                    Button(
                        onClick = onTriggerAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = AmanXColors.HighOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "تفعيل وضع الإنذار الأمني" else "Activate Security Alarm")
                    }
                }
            }
        }

        // Vault, Integrity Monitor, Secure Export Center shortcuts
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onNavigate("evidence_vault") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isAr) "خزنة الأدلة" else "Evidence")
                }
                OutlinedButton(onClick = { onNavigate("data_integrity") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isAr) "سلامة البيانات" else "Integrity")
                }
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onNavigate("secure_export") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (isAr) "مركز التصدير" else "Export Center")
                }
                Button(onClick = { showNewIncidentDialog = true }, modifier = Modifier.weight(1f)) {
                    Text(if (isAr) "+ حادث جديد" else "+ New Incident")
                }
            }
        }

        item {
            Text(
                text = if (isAr) "الحوادث الأمنية ومساعد التعافي" else "Incidents & Recovery Assistant",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (incidents.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (isAr) "لا توجد حوادث مسجلة" else "No Recorded Incidents",
                    subtitle = if (isAr) "عند رصد محاولة احتيال، افتح حادثاً هنا لمتابعة خطوات التعافي وحفظ الأدلة." else "Open an incident when facing suspicious activity to follow recovery steps and preserve evidence."
                )
            }
        } else {
            items(incidents, key = { it.id }) { inc ->
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(inc.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        RiskLevelBadge(inc.severity, language)
                    }
                    Text(
                        text = "${inc.incidentType.name} • ${inc.status.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(onClick = { onOpenIncident(inc.id) }) {
                        Text(if (isAr) "فتح مساعد التعافي والتسلسل الزمني" else "Open Recovery Assistant")
                    }
                }
            }
        }
    }

    if (showNewIncidentDialog) {
        AlertDialog(
            onDismissRequest = { showNewIncidentDialog = false },
            title = { Text(if (isAr) "فتح حادث أمني" else "Open Security Incident") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = incidentTitle,
                        onValueChange = { incidentTitle = it },
                        label = { Text(if (isAr) "عنوان الحادث" else "Incident Title") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = incidentType == IncidentType.SUSPECTED_TRANSFER_SCAM,
                            onClick = { incidentType = IncidentType.SUSPECTED_TRANSFER_SCAM },
                            label = { Text(if (isAr) "تحويل" else "Transfer") }
                        )
                        FilterChip(
                            selected = incidentType == IncidentType.CREDENTIAL_OR_OTP_REQUEST,
                            onClick = { incidentType = IncidentType.CREDENTIAL_OR_OTP_REQUEST },
                            label = { Text("OTP/PIN") }
                        )
                    }
                    OutlinedTextField(
                        value = incidentNotes,
                        onValueChange = { incidentNotes = it },
                        label = { Text(if (isAr) "تفاصيل وملاحظات" else "Details & Notes") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateIncident(
                        incidentTitle.ifBlank { if (isAr) "حادث اشتباه باحتيال" else "Suspected Fraud Incident" },
                        incidentType,
                        RiskLevel.HIGH,
                        incidentNotes
                    )
                    incidentTitle = ""
                    incidentNotes = ""
                    showNewIncidentDialog = false
                }) { Text(if (isAr) "إنشاء" else "Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewIncidentDialog = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun IncidentDetailScreen(
    language: AppLanguage,
    incidentFlow: Flow<Incident?>,
    stepsFlow: Flow<List<IncidentRecoveryStep>>,
    timelineFlow: Flow<List<IncidentTimelineEntity>>,
    onToggleStep: (Long, Long, Boolean) -> Unit,
    onResolve: (Long) -> Unit,
    onExportReport: (Uri, Long) -> Unit,
    onAddEvidence: (Long, String, EvidenceType, String, String) -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val incident by incidentFlow.collectAsState(initial = null)
    val steps by stepsFlow.collectAsState(initial = emptyList())
    val timeline by timelineFlow.collectAsState(initial = emptyList())

    var showAddEvidence by remember { mutableStateOf(false) }
    var evTitle by remember { mutableStateOf("") }
    var evText by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val id = incident?.id
        if (uri != null && id != null) onExportReport(uri, id)
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
                    text = if (isAr) "مساعد التعافي للحادث" else "Incident Recovery Assistant",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (incident != null) {
            val inc = incident!!
            item {
                AmanXCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(inc.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        RiskLevelBadge(inc.severity, language)
                    }
                    if (inc.notes.isNotBlank()) {
                        Text(inc.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showAddEvidence = true }) {
                            Text(if (isAr) "+ حفظ دليل" else "+ Add Evidence")
                        }
                        OutlinedButton(onClick = { exportLauncher.launch("amanx_incident_${inc.id}.json") }) {
                            Text(if (isAr) "تصدير التقرير" else "Export Report")
                        }
                    }
                    TextButton(onClick = { onResolve(inc.id) }) {
                        Text(if (isAr) "وضع علامة: تمت المعالجة" else "Mark as Resolved")
                    }
                }
            }

            item {
                Text(
                    text = if (isAr) "خطوات التعافي الموجهة" else "Guided Recovery Checklist",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(steps, key = { it.id }) { step ->
                AmanXCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = step.isCompleted,
                            onCheckedChange = { onToggleStep(inc.id, step.id, it) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isAr) step.titleAr else step.titleEn, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isAr) step.descriptionAr else step.descriptionEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (isAr) "التسلسل الزمني للحادث" else "Incident Timeline",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(timeline, key = { it.id }) { evt ->
                AmanXCard {
                    Text(if (isAr) evt.summaryAr else evt.summaryEn, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showAddEvidence && incident != null) {
        AlertDialog(
            onDismissRequest = { showAddEvidence = false },
            title = { Text(if (isAr) "حفظ دليل مرتبط بالحادث" else "Save Linked Evidence") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = evTitle, onValueChange = { evTitle = it }, label = { Text(if (isAr) "عنوان الدليل" else "Evidence Title") })
                    OutlinedTextField(value = evText, onValueChange = { evText = it }, label = { Text(if (isAr) "النص أو الوصف" else "Text / Snapshot") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddEvidence(incident!!.id, evTitle, EvidenceType.USER_TEXT_EXCERPT, evText, "")
                    evTitle = ""
                    evText = ""
                    showAddEvidence = false
                }) { Text(if (isAr) "تشفير وحفظ" else "Encrypt & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEvidence = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun EvidenceVaultScreen(
    language: AppLanguage,
    pinConfigured: Boolean,
    evidenceItems: List<EvidenceItem>,
    onAddEvidence: (Long?, String, EvidenceType, String, String) -> Unit,
    onDeleteEvidence: (Long) -> Unit,
    onRunIntegrityCheck: () -> Unit,
    onSetupPinRequired: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    var showAddDialog by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                    text = if (isAr) "خزنة الأدلة المشفرة" else "Encrypted Evidence Vault",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (!pinConfigured) {
            item {
                AmanXCard(borderColor = AmanXColors.CriticalRed) {
                    Text(
                        text = if (isAr) "خزنة الأدلة مقيدة حتى إنشاء رمز PIN" else "Evidence Vault Requires App PIN",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onSetupPinRequired) {
                        Text(if (isAr) "إعداد PIN الآن" else "Configure PIN Now")
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "+ حفظ دليل" else "+ Save Evidence")
                    }
                    OutlinedButton(onClick = onRunIntegrityCheck, modifier = Modifier.weight(1f)) {
                        Text(if (isAr) "فحص السلامة" else "Verify SHA-256")
                    }
                }
            }

            if (evidenceItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.FolderZip,
                        title = if (isAr) "الخزنة فارغة" else "Evidence Vault Empty",
                        subtitle = if (isAr) "تُحفظ الأدلة مشفرة محلياً باستخدام Android Keystore مع بصمة رقمية SHA-256 للتحقق من سلامتها." else "Evidence is encrypted locally with Android Keystore and SHA-256 digests."
                    )
                }
            } else {
                items(evidenceItems, key = { it.id }) { ev ->
                    AmanXCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = ev.integrityStatus.name,
                                color = if (ev.integrityStatus == IntegrityStatus.PASSED) AmanXColors.SafeGreen else AmanXColors.CriticalRed,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(ev.sanitizedSummary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "SHA-256: ${ev.sha256Digest.take(24)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        TextButton(onClick = { confirmDeleteId = ev.id }) {
                            Text(if (isAr) "حذف آمن" else "Secure Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isAr) "إضافة دليل مشفر" else "Add Encrypted Evidence") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isAr) "العنوان" else "Title") })
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(if (isAr) "المحتوى / النص" else "Content") }, minLines = 3)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(if (isAr) "ملاحظات" else "Notes") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddEvidence(null, title, EvidenceType.MANUAL_NOTE, content, notes)
                    title = ""
                    content = ""
                    notes = ""
                    showAddDialog = false
                }) { Text(if (isAr) "حفظ مشفر" else "Encrypt & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
            }
        )
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(if (isAr) "تأكيد الحذف الآمن" else "Confirm Secure Delete") },
            text = { Text(if (isAr) "سيتم حذف الملف المشفر وسجل الدليل نهائياً." else "This permanently deletes the encrypted evidence file.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteEvidence(confirmDeleteId!!)
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
fun DataIntegrityMonitorScreen(
    language: AppLanguage,
    logs: List<IntegrityCheckLogEntity>,
    onRunCheck: () -> Unit,
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
                    text = if (isAr) "مراقب سلامة البيانات" else "Data Integrity Monitor",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
            Spacer(Modifier.size(8.dp))
            Button(onClick = onRunCheck, modifier = Modifier.fillMaxWidth()) {
                Text(if (isAr) "تشغيل فحص السلامة الكامل الآن" else "Run Full Integrity Check Now")
            }
        }
        if (logs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (isAr) "لم يتم تشغيل فحص بعد" else "No Integrity Checks Yet",
                    subtitle = if (isAr) "اضغط الزر أعلاه لفحص مفاتيح Keystore وسلامة SQLite وبصمات SHA-256 لكل الأدلة." else "Run a check to verify Android Keystore, Room integrity, and SHA-256 evidence digests."
                )
            }
        } else {
            items(logs, key = { it.id }) { log ->
                AmanXCard(
                    borderColor = if (log.status == "PASSED") AmanXColors.SafeGreen.copy(alpha = 0.5f) else AmanXColors.CriticalRed
                ) {
                    Text(log.status, fontWeight = FontWeight.Bold, color = if (log.status == "PASSED") AmanXColors.SafeGreen else AmanXColors.CriticalRed)
                    Text(if (isAr) log.summaryAr else log.summaryEn)
                }
            }
        }
    }
}

@Composable
fun SecureExportCenterScreen(
    language: AppLanguage,
    pinConfigured: Boolean,
    exportLogs: List<ExportLogEntity>,
    onExportPrivacyJson: (Uri) -> Unit,
    onNavigateBackup: () -> Unit,
    onSetupPinRequired: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = language == AppLanguage.AR
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportPrivacyJson(uri)
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
                    text = if (isAr) "مركز التصدير الآمن" else "Secure Export Center",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedButton(onClick = onBack) { Text(if (isAr) "رجوع" else "Back") }
            }
        }

        if (!pinConfigured) {
            item {
                AmanXCard(borderColor = AmanXColors.CriticalRed) {
                    Text(if (isAr) "يتطلب مركز التصدير إعداد رمز PIN أولاً" else "PIN Required Before Exporting")
                    Button(onClick = onSetupPinRequired) {
                        Text(if (isAr) "إعداد PIN الآن" else "Set Up PIN")
                    }
                }
            }
        } else {
            item {
                AmanXCard {
                    Text(
                        text = if (isAr) "تصدير حزمة بيانات مشفرة (موصى به)" else "Encrypted Backup Archive (Recommended)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isAr) "يحميه رمز مرور + مفتاح استعادة مع تشفير AES-256-GCM." else "Protected by password + recovery key using AES-256-GCM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onNavigateBackup, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isAr) "الانتقال للنسخ الاحتياطي المشفر" else "Open Encrypted Backup")
                    }
                }
            }

            item {
                AmanXCard(borderColor = AmanXColors.MediumAmber.copy(alpha = 0.6f)) {
                    Text(
                        text = if (isAr) "تصدير سجل الخصوصية والأدلة (JSON نصي)" else "Export Privacy & Incident Data (JSON)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isAr) "تحذير: ملف JSON غير مشفر مخصص للمراجعة؛ احفظه في مكان آمن فقط." else "Warning: JSON exports are unencrypted for portability; store safely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmanXColors.MediumAmber
                    )
                    OutlinedButton(
                        onClick = { jsonExportLauncher.launch("amanx_privacy_export.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "تصدير JSON عبر مستعرض الملفات" else "Export JSON via System Picker")
                    }
                }
            }

            item {
                Text(if (isAr) "سجل عمليات التصدير" else "Export Audit Log", style = MaterialTheme.typography.titleMedium)
            }
            if (exportLogs.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = if (isAr) "لا توجد عمليات تصدير مسجلة" else "No Exports Recorded",
                        subtitle = if (isAr) "كل عملية تصدير توثق هنا ببصمتها الرقمية." else "All exports are audited here with their SHA-256 digest."
                    )
                }
            } else {
                items(exportLogs, key = { it.id }) { log ->
                    AmanXCard {
                        Text("${log.exportType} • ${if (log.isEncrypted) "ENCRYPTED" else "PLAIN_JSON"}", style = MaterialTheme.typography.titleMedium)
                        Text("SHA-256: ${log.sha256Digest.take(24)}...", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
