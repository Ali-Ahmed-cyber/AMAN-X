package com.amanx.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amanx.app.data.academy.AcademyContentProvider
import com.amanx.app.data.local.AcademyProgressEntity
import com.amanx.app.data.local.ActivityLogEntity
import com.amanx.app.data.local.AmanXDatabase
import com.amanx.app.data.local.AnalyzedRecordEntity
import com.amanx.app.data.local.EvidenceItemEntity
import com.amanx.app.data.local.ExportLogEntity
import com.amanx.app.data.local.ExtractedAmountEntity
import com.amanx.app.data.local.FalsePositiveRuleEntity
import com.amanx.app.data.local.IncidentEntity
import com.amanx.app.data.local.IncidentRecoveryStepEntity
import com.amanx.app.data.local.IncidentTimelineEntity
import com.amanx.app.data.local.IntegrityCheckLogEntity
import com.amanx.app.data.local.NotificationSourceEntity
import com.amanx.app.data.local.ReceiptAnalysisEntity
import com.amanx.app.data.local.RetentionPolicyEntity
import com.amanx.app.data.local.SecurityAlertEntity
import com.amanx.app.data.preferences.AmanXPreferencesDataStore
import com.amanx.app.data.preferences.UserPreferencesState
import com.amanx.app.data.receipt.LocalReceiptOcrAnalyzer
import com.amanx.app.data.security.CryptoManager
import com.amanx.app.domain.engine.AmanXAnalysisEngine
import com.amanx.app.domain.engine.BankWalletRegistry
import com.amanx.app.domain.engine.SecurityHealthCalculator
import com.amanx.app.domain.model.AdapterVerificationStatus
import com.amanx.app.domain.model.AlertCategory
import com.amanx.app.domain.model.AlertStatus
import com.amanx.app.domain.model.AnalysisInputChannel
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.EvidenceItem
import com.amanx.app.domain.model.EvidenceType
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.FalsePositiveRule
import com.amanx.app.domain.model.Incident
import com.amanx.app.domain.model.IncidentRecoveryStep
import com.amanx.app.domain.model.IncidentStatus
import com.amanx.app.domain.model.IncidentType
import com.amanx.app.domain.model.IntegrityStatus
import com.amanx.app.domain.model.NotificationSource
import com.amanx.app.domain.model.ReceiptAnalysis
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.SecurityAlert
import com.amanx.app.domain.model.SecurityHealthReport
import com.amanx.app.domain.model.TransactionDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AmanXRepository(
    private val context: Context,
    private val db: AmanXDatabase,
    val preferences: AmanXPreferencesDataStore,
    val cryptoManager: CryptoManager,
    private val receiptOcrAnalyzer: LocalReceiptOcrAnalyzer
) {
    companion object {
        private const val DEDUP_WINDOW_MS = 10 * 60 * 1000L // 10 minutes
        private const val EVIDENCE_DIR = "amanx_evidence_enc"
    }

    private val evidenceDir: File by lazy {
        File(context.filesDir, EVIDENCE_DIR).apply { mkdirs() }
    }

    val userPreferences: Flow<UserPreferencesState> = preferences.preferencesFlow

    val alerts: Flow<List<SecurityAlert>> = db.securityAlertDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val extractedAmounts: Flow<List<ExtractedAmount>> = db.extractedAmountDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val notificationSources: Flow<List<NotificationSource>> = db.notificationSourceDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val falsePositiveRules: Flow<List<FalsePositiveRule>> = db.falsePositiveRuleDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val receiptAnalyses: Flow<List<ReceiptAnalysis>> = db.receiptAnalysisDao().observeAll().map { list ->
        list.map { entity ->
            ReceiptAnalysis(
                id = entity.id,
                title = entity.title,
                imageUriString = entity.imageUriString,
                rawExtractedText = runCatching { cryptoManager.decryptString(entity.encryptedExtractedText) }.getOrDefault(""),
                extractedAmountMinorUnits = entity.extractedAmountMinorUnits,
                currency = entity.currencyCode?.let { runCatching { CurrencyCode.valueOf(it) }.getOrNull() },
                extractedDateText = entity.extractedDateText,
                merchantName = entity.merchantName,
                referenceNumber = entity.referenceNumber,
                missingFieldsNoteAr = entity.missingFieldsNoteAr,
                missingFieldsNoteEn = entity.missingFieldsNoteEn,
                isUserReviewed = entity.isUserReviewed,
                createdAt = entity.createdAt
            )
        }
    }

    val incidents: Flow<List<Incident>> = db.incidentDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val evidenceItems: Flow<List<EvidenceItem>> = db.evidenceDao().observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val activityLogs: Flow<List<ActivityLogEntity>> = db.activityLogDao().observeAll()
    val academyProgress: Flow<List<AcademyProgressEntity>> = db.academyProgressDao().observeAll()
    val retentionPolicies: Flow<List<RetentionPolicyEntity>> = db.governanceDao().observeRetentionPolicies()
    val integrityLogs: Flow<List<IntegrityCheckLogEntity>> = db.governanceDao().observeIntegrityLogs()
    val exportLogs: Flow<List<ExportLogEntity>> = db.governanceDao().observeExportLogs()
    val processedCount: Flow<Int> = db.analyzedRecordDao().observeCount()

    fun observeAlertById(id: Long): Flow<SecurityAlert?> =
        db.securityAlertDao().observeById(id).map { it?.toDomain() }

    fun observeIncidentById(id: Long): Flow<Incident?> =
        db.incidentDao().observeById(id).map { it?.toDomain() }

    fun observeRecoverySteps(incidentId: Long): Flow<List<IncidentRecoveryStep>> =
        db.incidentDao().observeRecoverySteps(incidentId).map { list ->
            list.map {
                IncidentRecoveryStep(
                    id = it.id,
                    incidentId = it.incidentId,
                    stepOrder = it.stepOrder,
                    titleAr = it.titleAr,
                    titleEn = it.titleEn,
                    descriptionAr = it.descriptionAr,
                    descriptionEn = it.descriptionEn,
                    isCompleted = it.isCompleted,
                    completedAt = it.completedAt
                )
            }
        }

    fun observeIncidentTimeline(incidentId: Long): Flow<List<IncidentTimelineEntity>> =
        db.incidentDao().observeTimeline(incidentId)

    val securityHealthReport: Flow<SecurityHealthReport> = combine(
        preferences.preferencesFlow,
        db.securityAlertDao().observeUnreviewedCriticalCount(),
        db.analyzedRecordDao().observeCount(),
        db.governanceDao().observeIntegrityLogs()
    ) { prefs, criticalCount, processed, logs ->
        val latestIntegrity = logs.firstOrNull()?.status?.let {
            runCatching { IntegrityStatus.valueOf(it) }.getOrNull()
        } ?: IntegrityStatus.PASSED
        computeHealthReport(prefs, criticalCount, processed, latestIntegrity)
    }

    fun computeHealthReport(
        prefs: UserPreferencesState,
        criticalCount: Int,
        processedCount: Int,
        integrityStatus: IntegrityStatus
    ): SecurityHealthReport {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        return SecurityHealthCalculator.calculate(
            isAppLockConfigured = prefs.pinConfigured,
            isNotificationListenerEnabled = isNotificationListenerEnabled(),
            isPostNotificationsGranted = isPostNotificationsGranted(),
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(),
            isKeystoreHealthy = cryptoManager.isKeystoreKeyHealthy(),
            hasRecentBackup = prefs.lastBackupAt > 0L && (now - prefs.lastBackupAt) <= thirtyDaysMs,
            unreviewedCriticalCount = criticalCount,
            processedNotificationsCount = processedCount,
            integrityStatus = integrityStatus
        )
    }

    fun isNotificationListenerEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun isPostNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    suspend fun ensureDefaultSeeded() = withContext(Dispatchers.IO) {
        val defaults = BankWalletRegistry.defaultKnownSources().map {
            NotificationSourceEntity(
                packageName = it.packageName,
                displayNameAr = it.displayNameAr,
                displayNameEn = it.displayNameEn,
                category = it.category,
                verificationStatus = it.verificationStatus.name,
                isEnabled = it.isEnabled,
                sensitivityLevel = it.sensitivityLevel,
                lastSeenAt = it.lastSeenAt
            )
        }
        defaults.forEach { db.notificationSourceDao().insertIfAbsent(it) }

        val policies = db.governanceDao().getRetentionPoliciesSync()
        if (policies.isEmpty()) {
            val now = System.currentTimeMillis()
            listOf("NOTIFICATIONS", "ALERTS", "ACTIVITY_LOGS", "EVIDENCE").forEach { type ->
                db.governanceDao().upsertRetentionPolicy(
                    RetentionPolicyEntity(dataType = type, retentionDays = null, lastCleanupAt = now)
                )
            }
        }
    }

    data class IngestResult(
        val wasDeduplicated: Boolean,
        val alertId: Long?,
        val riskLevel: RiskLevel?
    )

    suspend fun analyzeAndPersistText(
        title: String,
        body: String,
        sourcePackage: String,
        channel: AnalysisInputChannel
    ): IngestResult = withContext(Dispatchers.IO) {
        ensureDefaultSeeded()
        val prefs = preferences.preferencesFlow.first()

        // Ensure source exists in NotificationSourceControl when coming from notification
        if (channel == AnalysisInputChannel.NOTIFICATION && sourcePackage.isNotBlank()) {
            val existingSource = db.notificationSourceDao().findByPackage(sourcePackage)
            if (existingSource == null) {
                db.notificationSourceDao().insertIfAbsent(
                    NotificationSourceEntity(
                        packageName = sourcePackage,
                        displayNameAr = sourcePackage,
                        displayNameEn = sourcePackage,
                        category = "DISCOVERED_APP",
                        verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN.name,
                        isEnabled = true,
                        sensitivityLevel = 2,
                        lastSeenAt = System.currentTimeMillis()
                    )
                )
            } else {
                if (!existingSource.isEnabled) {
                    return@withContext IngestResult(wasDeduplicated = false, alertId = null, riskLevel = null)
                }
                db.notificationSourceDao().updateLastSeen(sourcePackage, System.currentTimeMillis())
            }
        }

        val sourceEntity = db.notificationSourceDao().findByPackage(sourcePackage)
        val sensitivity = sourceEntity?.sensitivityLevel ?: 2
        val fpRules = db.falsePositiveRuleDao().getActiveSync().map { it.toDomain() }

        val output = AmanXAnalysisEngine.analyze(
            rawTitle = title,
            rawBody = body,
            sourcePackage = sourcePackage,
            channel = channel,
            activeFalsePositiveRules = fpRules,
            sensitivityLevel = sensitivity
        )

        val now = System.currentTimeMillis()
        if (prefs.smartAlertDeduplication &&
            channel == AnalysisInputChannel.NOTIFICATION &&
            db.analyzedRecordDao().existsDuplicateSince(output.dedupHash, now - DEDUP_WINDOW_MS)
        ) {
            return@withContext IngestResult(wasDeduplicated = true, alertId = null, riskLevel = null)
        }

        val encryptedText = cryptoManager.encryptString(output.sanitizedText)
        val recordId = db.analyzedRecordDao().insert(
            AnalyzedRecordEntity(
                sourcePackage = sourcePackage,
                inputChannel = channel.name,
                dedupHash = output.dedupHash,
                encryptedSanitizedText = encryptedText,
                containedRedactedOtp = output.containedRedactedOtp,
                adapterId = output.adapter.id,
                receivedAt = now,
                retentionExpiresAt = null
            )
        )

        if (output.extractedAmounts.isNotEmpty()) {
            db.extractedAmountDao().insertAll(
                output.extractedAmounts.map { amt ->
                    ExtractedAmountEntity(
                        recordId = recordId,
                        amountMinorUnits = amt.amountMinorUnits,
                        currencyCode = amt.currency.name,
                        direction = amt.direction.name,
                        isUserCorrected = false,
                        originalAmountMinorUnits = amt.amountMinorUnits,
                        sourcePackage = sourcePackage,
                        createdAt = now
                    )
                }
            )
        }

        val titleAr = when (output.assessment.riskLevel) {
            RiskLevel.CRITICAL -> "تحذير أمني حرج"
            RiskLevel.HIGH -> "تنبيه أمني مرتفع"
            RiskLevel.MEDIUM -> "مؤشرات تستحق الانتباه"
            RiskLevel.LOW -> if (output.extractedAmounts.isNotEmpty()) "إشعار مالي مستخرج" else "فحص آمن نسبياً"
        }
        val titleEn = when (output.assessment.riskLevel) {
            RiskLevel.CRITICAL -> "Critical Security Warning"
            RiskLevel.HIGH -> "High Risk Alert"
            RiskLevel.MEDIUM -> "Attention-Worthy Cues"
            RiskLevel.LOW -> if (output.extractedAmounts.isNotEmpty()) "Financial Notification Parsed" else "Low Risk Check"
        }

        val alertId = db.securityAlertDao().insert(
            SecurityAlertEntity(
                recordId = recordId,
                category = output.category.name,
                riskLevel = output.assessment.riskLevel.name,
                confidenceScore = output.assessment.confidenceScore,
                titleAr = titleAr,
                titleEn = titleEn,
                summaryAr = output.assessment.whatHappened.ar,
                summaryEn = output.assessment.whatHappened.en,
                whatHappenedAr = output.assessment.whatHappened.ar,
                whatHappenedEn = output.assessment.whatHappened.en,
                whyFlaggedAr = output.assessment.whyFlagged.ar,
                whyFlaggedEn = output.assessment.whyFlagged.en,
                evidenceBasisAr = output.assessment.evidenceBasis.ar,
                evidenceBasisEn = output.assessment.evidenceBasis.en,
                recommendationsAr = output.assessment.recommendations.joinToString("\n") { "• ${it.ar}" },
                recommendationsEn = output.assessment.recommendations.joinToString("\n") { "• ${it.en}" },
                suggestedActionAr = output.assessment.suggestedAction.ar,
                suggestedActionEn = output.assessment.suggestedAction.en,
                patternHash = output.patternHash,
                sourcePackage = sourcePackage,
                status = AlertStatus.NEW.name,
                createdAt = now
            )
        )

        logActivity(
            eventType = "ANALYSIS",
            titleAr = titleAr,
            titleEn = titleEn,
            detailsAr = "المصدر: $sourcePackage | المستوى: ${output.assessment.riskLevel.name}",
            detailsEn = "Source: $sourcePackage | Level: ${output.assessment.riskLevel.name}"
        )

        IngestResult(wasDeduplicated = false, alertId = alertId, riskLevel = output.assessment.riskLevel)
    }

    suspend fun getDecryptedRecordText(recordId: Long): String = withContext(Dispatchers.IO) {
        val record = db.analyzedRecordDao().getAllSync().firstOrNull { it.id == recordId } ?: return@withContext ""
        runCatching { cryptoManager.decryptString(record.encryptedSanitizedText) }.getOrDefault("")
    }

    suspend fun updateAlertStatus(alertId: Long, status: AlertStatus) = withContext(Dispatchers.IO) {
        db.securityAlertDao().updateStatus(alertId, status.name)
    }

    suspend fun deleteAlert(alertId: Long) = withContext(Dispatchers.IO) {
        db.securityAlertDao().deleteById(alertId)
        logActivity("ALERT_DELETE", "حذف تنبيه", "Alert deleted", "تم حذف التنبيه رقم $alertId", "Deleted alert #$alertId")
    }

    suspend fun reportFalsePositive(alertId: Long, note: String) = withContext(Dispatchers.IO) {
        val alert = db.securityAlertDao().getByIdSync(alertId) ?: return@withContext
        db.securityAlertDao().updateStatus(alertId, AlertStatus.FALSE_POSITIVE.name)
        db.falsePositiveRuleDao().insert(
            FalsePositiveRuleEntity(
                alertId = alertId,
                sourcePackage = alert.sourcePackage,
                patternHash = alert.patternHash,
                note = note.ifBlank { "Marked as false positive by user" },
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        logActivity(
            eventType = "FALSE_POSITIVE",
            titleAr = "إبلاغ عن تحليل خاطئ",
            titleEn = "False Positive Reported",
            detailsAr = "تم تسجيل قاعدة ضبط محلية للتنبيه #$alertId",
            detailsEn = "Recorded local tuning rule for alert #$alertId"
        )
    }

    suspend fun toggleFalsePositiveRule(ruleId: Long, active: Boolean) = withContext(Dispatchers.IO) {
        db.falsePositiveRuleDao().setActive(ruleId, active)
    }

    suspend fun deleteFalsePositiveRule(ruleId: Long) = withContext(Dispatchers.IO) {
        db.falsePositiveRuleDao().deleteById(ruleId)
    }

    suspend fun correctExtractedAmount(amountId: Long, newMinorUnits: Long, currency: CurrencyCode) =
        withContext(Dispatchers.IO) {
            db.extractedAmountDao().updateManualCorrection(amountId, newMinorUnits, currency.name)
            logActivity(
                eventType = "AMOUNT_CORRECTION",
                titleAr = "تصحيح مبلغ يدوياً",
                titleEn = "Manual Amount Correction",
                detailsAr = "تم تعديل المبلغ #$amountId إلى ${newMinorUnits / 100.0} ${currency.symbolAr}",
                detailsEn = "Updated amount #$amountId to ${newMinorUnits / 100.0} ${currency.symbolEn}"
            )
        }

    suspend fun setNotificationSourceEnabled(packageName: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        db.notificationSourceDao().setEnabled(packageName, enabled)
        logActivity(
            eventType = "SOURCE_CONTROL",
            titleAr = "تعديل مصدر إشعارات",
            titleEn = "Notification Source Updated",
            detailsAr = "$packageName -> ${if (enabled) "مفعّل" else "معطّل"}",
            detailsEn = "$packageName -> ${if (enabled) "Enabled" else "Disabled"}"
        )
    }

    suspend fun setNotificationSourceSensitivity(packageName: String, level: Int) = withContext(Dispatchers.IO) {
        db.notificationSourceDao().setSensitivity(packageName, level.coerceIn(1, 3))
    }

    suspend fun analyzeReceiptUri(uri: Uri, title: String): Long = withContext(Dispatchers.IO) {
        val parsed = receiptOcrAnalyzer.analyzeImageUri(uri, title)
        val id = db.receiptAnalysisDao().insert(
            ReceiptAnalysisEntity(
                title = parsed.title,
                imageUriString = parsed.imageUriString,
                encryptedExtractedText = cryptoManager.encryptString(parsed.rawExtractedText),
                extractedAmountMinorUnits = parsed.extractedAmountMinorUnits,
                currencyCode = parsed.currency?.name,
                extractedDateText = parsed.extractedDateText,
                merchantName = parsed.merchantName,
                referenceNumber = parsed.referenceNumber,
                missingFieldsNoteAr = parsed.missingFieldsNoteAr,
                missingFieldsNoteEn = parsed.missingFieldsNoteEn,
                isUserReviewed = false,
                createdAt = parsed.createdAt
            )
        )
        logActivity(
            eventType = "RECEIPT_OCR",
            titleAr = "تحليل صورة مستند/إيصال",
            titleEn = "Receipt Image Analyzed",
            detailsAr = "تحليل محلي مستقل للمستند: ${parsed.title}",
            detailsEn = "Independent local OCR for document: ${parsed.title}"
        )
        id
    }

    suspend fun updateReceiptReview(
        id: Long,
        amountMinorUnits: Long?,
        currency: CurrencyCode?,
        merchant: String?,
        dateText: String?,
        reference: String?
    ) = withContext(Dispatchers.IO) {
        db.receiptAnalysisDao().updateManualReview(
            id = id,
            amountMinorUnits = amountMinorUnits,
            currencyCode = currency?.name,
            merchant = merchant?.takeIf { it.isNotBlank() },
            dateText = dateText?.takeIf { it.isNotBlank() },
            refNum = reference?.takeIf { it.isNotBlank() }
        )
        logActivity(
            eventType = "RECEIPT_REVIEW",
            titleAr = "مراجعة بيانات إيصال",
            titleEn = "Receipt Data Reviewed",
            detailsAr = "تم تحديث ومراجعة المستند #$id",
            detailsEn = "Updated & reviewed receipt #$id"
        )
    }

    suspend fun createIncident(
        title: String,
        type: IncidentType,
        severity: RiskLevel,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = db.incidentDao().insert(
            IncidentEntity(
                title = title,
                incidentType = type.name,
                severity = severity.name,
                status = IncidentStatus.OPEN.name,
                encryptedNotes = cryptoManager.encryptString(notes),
                createdAt = now,
                updatedAt = now
            )
        )
        db.incidentDao().insertRecoverySteps(defaultRecoveryStepsFor(id, type))
        db.incidentDao().insertTimelineEvent(
            IncidentTimelineEntity(
                incidentId = id,
                eventType = "CREATED",
                summaryAr = "تم فتح سجل الحادث وتوليد خطوات الاستجابة اليدوية.",
                summaryEn = "Incident opened and guided manual recovery steps generated.",
                occurredAt = now
            )
        )
        logActivity(
            eventType = "INCIDENT_OPEN",
            titleAr = "فتح حادث أمني",
            titleEn = "Security Incident Opened",
            detailsAr = title,
            detailsEn = title
        )
        id
    }

    suspend fun toggleRecoveryStep(incidentId: Long, stepId: Long, completed: Boolean) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db.incidentDao().updateRecoveryStep(stepId, completed, if (completed) now else null)
            db.incidentDao().updateStatus(
                incidentId,
                if (completed) IncidentStatus.RECOVERING.name else IncidentStatus.OPEN.name,
                now
            )
            db.incidentDao().insertTimelineEvent(
                IncidentTimelineEntity(
                    incidentId = incidentId,
                    eventType = if (completed) "STEP_DONE" else "STEP_REOPENED",
                    summaryAr = if (completed) "تم إكمال خطوة استجابة يدوية." else "تمت إعادة فتح خطوة استجابة.",
                    summaryEn = if (completed) "Completed a guided recovery step." else "Reopened a guided recovery step.",
                    occurredAt = now
                )
            )
        }

    suspend fun markIncidentResolved(incidentId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.incidentDao().updateStatus(incidentId, IncidentStatus.RESOLVED.name, now)
        db.incidentDao().insertTimelineEvent(
            IncidentTimelineEntity(
                incidentId = incidentId,
                eventType = "RESOLVED",
                summaryAr = "تم وضع علامة 'تمت المعالجة' على الحادث.",
                summaryEn = "Incident marked as resolved by user.",
                occurredAt = now
            )
        )
    }

    suspend fun addEvidenceItem(
        incidentId: Long?,
        title: String,
        type: EvidenceType,
        rawContent: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val redacted = com.amanx.app.domain.security.OtpRedactor.redact(rawContent).sanitizedText
        val payloadBytes = redacted.toByteArray(Charsets.UTF_8)
        val sha256 = cryptoManager.sha256Hex(payloadBytes)
        val encryptedBytes = cryptoManager.encryptBytes(payloadBytes)

        val fileName = "ev_${now}_${sha256.take(8)}.enc"
        val file = File(evidenceDir, fileName)
        file.writeBytes(encryptedBytes)

        val id = db.evidenceDao().insert(
            EvidenceItemEntity(
                incidentId = incidentId,
                title = title,
                evidenceType = type.name,
                encryptedSummary = cryptoManager.encryptString(redacted.take(400)),
                encryptedPayloadPath = file.absolutePath,
                sha256Digest = sha256,
                encryptedNotes = cryptoManager.encryptString(notes),
                capturedAt = now,
                lastVerifiedAt = now,
                integrityStatus = IntegrityStatus.PASSED.name
            )
        )
        if (incidentId != null) {
            db.incidentDao().insertTimelineEvent(
                IncidentTimelineEntity(
                    incidentId = incidentId,
                    eventType = "EVIDENCE_ADDED",
                    summaryAr = "تم حفظ دليل مشفر ($title) ببصمة رقمية SHA-256.",
                    summaryEn = "Encrypted evidence ($title) preserved with SHA-256 digest.",
                    occurredAt = now
                )
            )
        }
        logActivity(
            eventType = "EVIDENCE_ADD",
            titleAr = "حفظ دليل أمني مشفر",
            titleEn = "Encrypted Evidence Saved",
            detailsAr = "$title (${sha256.take(12)}...)",
            detailsEn = "$title (${sha256.take(12)}...)"
        )
        id
    }

    suspend fun deleteEvidenceItem(id: Long) = withContext(Dispatchers.IO) {
        val item = db.evidenceDao().getAllSync().firstOrNull { it.id == id }
        item?.encryptedPayloadPath?.let { path ->
            runCatching { File(path).delete() }
        }
        db.evidenceDao().deleteById(id)
        logActivity("EVIDENCE_DELETE", "حذف دليل", "Evidence deleted", "تم حذف الدليل #$id", "Deleted evidence #$id")
    }

    /**
     * Real Data Integrity Monitor verifying Keystore health, Room database quick_check,
     * and decrypting + SHA-256 verifying every stored encrypted evidence file.
     */
    suspend fun runDataIntegrityCheck(): IntegrityCheckLogEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val keystoreOk = cryptoManager.isKeystoreKeyHealthy()
        val dbOk = runCatching {
            db.openHelper.readableDatabase.query("PRAGMA quick_check").use { cursor ->
                cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            }
        }.getOrDefault(false)

        val evidenceList = db.evidenceDao().getAllSync()
        var failedCount = 0
        for (ev in evidenceList) {
            val path = ev.encryptedPayloadPath
            val verified = if (path.isNullOrBlank()) {
                false
            } else {
                runCatching {
                    val file = File(path)
                    if (!file.exists()) {
                        false
                    } else {
                        val decrypted = cryptoManager.decryptBytes(file.readBytes())
                        cryptoManager.sha256Hex(decrypted) == ev.sha256Digest
                    }
                }.getOrDefault(false)
            }
            val itemStatus = if (verified) IntegrityStatus.PASSED else IntegrityStatus.FAILED
            if (!verified) failedCount++
            db.evidenceDao().updateIntegrity(ev.id, itemStatus.name, now)
        }

        val overall = when {
            !keystoreOk || !dbOk || failedCount > 0 -> IntegrityStatus.FAILED
            else -> IntegrityStatus.PASSED
        }
        val summaryAr = "المفاتيح: ${if (keystoreOk) "سليمة" else "عطل"} | القاعدة: ${if (dbOk) "سليمة" else "عطل"} | الأدلة المفحوصة: ${evidenceList.size} (تالف: $failedCount)"
        val summaryEn = "Keystore: ${if (keystoreOk) "OK" else "FAIL"} | DB: ${if (dbOk) "OK" else "FAIL"} | Evidence checked: ${evidenceList.size} (failed: $failedCount)"

        val log = IntegrityCheckLogEntity(
            scope = "FULL",
            status = overall.name,
            summaryAr = summaryAr,
            summaryEn = summaryEn,
            checkedAt = now
        )
        db.governanceDao().insertIntegrityLog(log)
        logActivity("INTEGRITY_CHECK", "فحص سلامة البيانات", "Data Integrity Check", summaryAr, summaryEn)
        log
    }

    suspend fun recordAcademyAttempt(contentId: String, contentType: String, score: Int) =
        withContext(Dispatchers.IO) {
            val existing = db.academyProgressDao().getAllSync().firstOrNull { it.contentId == contentId }
            val now = System.currentTimeMillis()
            db.academyProgressDao().upsert(
                AcademyProgressEntity(
                    contentId = contentId,
                    contentType = contentType,
                    isCompleted = true,
                    bestScore = maxOf(existing?.bestScore ?: 0, score),
                    attemptsCount = (existing?.attemptsCount ?: 0) + 1,
                    lastAttemptAt = now
                )
            )
            logActivity(
                eventType = "ACADEMY",
                titleAr = "إتمام نشاط توعوي",
                titleEn = "Awareness Activity Completed",
                detailsAr = "$contentId — الدرجة: $score%",
                detailsEn = "$contentId — Score: $score%"
            )
        }

    suspend fun setRetentionDays(dataType: String, days: Int?) = withContext(Dispatchers.IO) {
        db.governanceDao().upsertRetentionPolicy(
            RetentionPolicyEntity(
                dataType = dataType,
                retentionDays = days,
                lastCleanupAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun executeRetentionCleanup(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var deletedTotal = 0
        for (policy in db.governanceDao().getRetentionPoliciesSync()) {
            val days = policy.retentionDays ?: continue
            val cutoff = now - days.toLong() * 24L * 60L * 60L * 1000L
            deletedTotal += when (policy.dataType) {
                "NOTIFICATIONS" -> db.analyzedRecordDao().deleteOlderThan(cutoff)
                "ALERTS" -> db.securityAlertDao().deleteOlderThan(cutoff)
                "ACTIVITY_LOGS" -> db.activityLogDao().deleteOlderThan(cutoff)
                else -> 0
            }
            db.governanceDao().upsertRetentionPolicy(policy.copy(lastCleanupAt = now))
        }
        deletedTotal
    }

    /**
     * Secure Export Center: creates an encrypted backup archive protected by Password + Recovery Key.
     */
    suspend fun createEncryptedBackupBytes(password: String): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
        val recoveryKey = cryptoManager.generateRecoveryKey()
        val payloadJson = buildExportJsonPayload(includeFullDetails = true)
        val archive = cryptoManager.encryptBackupArchive(payloadJson, password, recoveryKey)
        val now = System.currentTimeMillis()
        preferences.recordBackupTimestamp(now)
        val sha = cryptoManager.sha256Hex(archive)
        db.governanceDao().insertExportLog(
            ExportLogEntity(
                exportType = "ENCRYPTED_BACKUP",
                isEncrypted = true,
                sha256Digest = sha,
                exportedAt = now
            )
        )
        logActivity("BACKUP_CREATE", "إنشاء نسخة احتياطية مشفرة", "Encrypted Backup Created", "SHA-256: ${sha.take(12)}...", "SHA-256: ${sha.take(12)}...")
        archive to recoveryKey
    }

    suspend fun restoreFromEncryptedBackup(
        archiveBytes: ByteArray,
        password: String,
        recoveryKey: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val plainJson = cryptoManager.decryptBackupArchive(archiveBytes, password, recoveryKey)
            val root = JSONObject(plainJson)
            require(root.optString("format") == "AMAN_X_EXPORT_V1") { "Invalid backup payload" }

            var restoredCount = 0
            val sourcesArr = root.optJSONArray("notification_sources") ?: JSONArray()
            for (i in 0 until sourcesArr.length()) {
                val obj = sourcesArr.getJSONObject(i)
                db.notificationSourceDao().upsertAll(
                    listOf(
                        NotificationSourceEntity(
                            packageName = obj.getString("packageName"),
                            displayNameAr = obj.getString("displayNameAr"),
                            displayNameEn = obj.getString("displayNameEn"),
                            category = obj.getString("category"),
                            verificationStatus = obj.getString("verificationStatus"),
                            isEnabled = obj.getBoolean("isEnabled"),
                            sensitivityLevel = obj.optInt("sensitivityLevel", 2),
                            lastSeenAt = obj.optLong("lastSeenAt", 0L)
                        )
                    )
                )
                restoredCount++
            }

            val incidentsArr = root.optJSONArray("incidents") ?: JSONArray()
            for (i in 0 until incidentsArr.length()) {
                val obj = incidentsArr.getJSONObject(i)
                val incidentId = db.incidentDao().insert(
                    IncidentEntity(
                        title = obj.getString("title"),
                        incidentType = obj.getString("incidentType"),
                        severity = obj.getString("severity"),
                        status = obj.getString("status"),
                        encryptedNotes = cryptoManager.encryptString(obj.optString("notes", "")),
                        createdAt = obj.getLong("createdAt"),
                        updatedAt = obj.getLong("updatedAt")
                    )
                )
                val type = runCatching { IncidentType.valueOf(obj.getString("incidentType")) }.getOrDefault(IncidentType.OTHER)
                db.incidentDao().insertRecoverySteps(defaultRecoveryStepsFor(incidentId, type))
                restoredCount++
            }

            val evidenceArr = root.optJSONArray("evidence") ?: JSONArray()
            for (i in 0 until evidenceArr.length()) {
                val obj = evidenceArr.getJSONObject(i)
                addEvidenceItem(
                    incidentId = null,
                    title = obj.getString("title"),
                    type = runCatching { EvidenceType.valueOf(obj.getString("evidenceType")) }.getOrDefault(EvidenceType.MANUAL_NOTE),
                    rawContent = obj.optString("summary", ""),
                    notes = obj.optString("notes", "")
                )
                restoredCount++
            }

            logActivity(
                eventType = "BACKUP_RESTORE",
                titleAr = "استعادة نسخة احتياطية مشفرة",
                titleEn = "Encrypted Backup Restored",
                detailsAr = "تمت استعادة $restoredCount سجل بنجاح.",
                detailsEn = "Restored $restoredCount records successfully."
            )
            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generatePrivacyOrIncidentExportJson(incidentId: Long? = null): Pair<String, String> =
        withContext(Dispatchers.IO) {
            val json = if (incidentId == null) {
                buildExportJsonPayload(includeFullDetails = true)
            } else {
                buildSingleIncidentReportJson(incidentId)
            }
            val sha = cryptoManager.sha256Hex(json)
            db.governanceDao().insertExportLog(
                ExportLogEntity(
                    exportType = if (incidentId == null) "PRIVACY_DATA_JSON" else "INCIDENT_REPORT_JSON",
                    isEncrypted = false,
                    sha256Digest = sha,
                    exportedAt = System.currentTimeMillis()
                )
            )
            logActivity(
                eventType = "EXPORT",
                titleAr = "تصدير بيانات من مركز التصدير",
                titleEn = "Data Exported from Secure Export Center",
                detailsAr = "SHA-256: ${sha.take(12)}...",
                detailsEn = "SHA-256: ${sha.take(12)}..."
            )
            json to sha
        }

    suspend fun wipeAllUserData() = withContext(Dispatchers.IO) {
        evidenceDir.listFiles()?.forEach { it.delete() }
        db.securityAlertDao().deleteAll()
        db.extractedAmountDao().deleteAll()
        db.analyzedRecordDao().deleteAll()
        db.falsePositiveRuleDao().deleteAll()
        db.receiptAnalysisDao().deleteAll()
        db.incidentDao().deleteAll()
        db.evidenceDao().deleteAll()
        db.activityLogDao().deleteAll()
        db.academyProgressDao().deleteAll()
        db.governanceDao().deleteIntegrityLogs()
        db.governanceDao().deleteExportLogs()
        db.governanceDao().deleteRetentionPolicies()
        db.notificationSourceDao().deleteAll()
        preferences.clearAllForWipe()
        ensureDefaultSeeded()
    }

    private suspend fun buildExportJsonPayload(includeFullDetails: Boolean): String {
        val root = JSONObject()
        root.put("format", "AMAN_X_EXPORT_V1")
        root.put("exportedAt", System.currentTimeMillis())

        val sourcesArr = JSONArray()
        db.notificationSourceDao().observeAll().first().forEach { s ->
            sourcesArr.put(JSONObject().apply {
                put("packageName", s.packageName)
                put("displayNameAr", s.displayNameAr)
                put("displayNameEn", s.displayNameEn)
                put("category", s.category)
                put("verificationStatus", s.verificationStatus)
                put("isEnabled", s.isEnabled)
                put("sensitivityLevel", s.sensitivityLevel)
                put("lastSeenAt", s.lastSeenAt)
            })
        }
        root.put("notification_sources", sourcesArr)

        val incidentsArr = JSONArray()
        db.incidentDao().getAllSync().forEach { inc ->
            incidentsArr.put(JSONObject().apply {
                put("id", inc.id)
                put("title", inc.title)
                put("incidentType", inc.incidentType)
                put("severity", inc.severity)
                put("status", inc.status)
                put("notes", runCatching { cryptoManager.decryptString(inc.encryptedNotes) }.getOrDefault(""))
                put("createdAt", inc.createdAt)
                put("updatedAt", inc.updatedAt)
            })
        }
        root.put("incidents", incidentsArr)

        if (includeFullDetails) {
            val evArr = JSONArray()
            db.evidenceDao().getAllSync().forEach { ev ->
                evArr.put(JSONObject().apply {
                    put("id", ev.id)
                    put("title", ev.title)
                    put("evidenceType", ev.evidenceType)
                    put("summary", runCatching { cryptoManager.decryptString(ev.encryptedSummary) }.getOrDefault(""))
                    put("notes", runCatching { cryptoManager.decryptString(ev.encryptedNotes) }.getOrDefault(""))
                    put("sha256Digest", ev.sha256Digest)
                    put("capturedAt", ev.capturedAt)
                })
            }
            root.put("evidence", evArr)

            val alertsArr = JSONArray()
            db.securityAlertDao().getAllSync().forEach { a ->
                alertsArr.put(JSONObject().apply {
                    put("id", a.id)
                    put("category", a.category)
                    put("riskLevel", a.riskLevel)
                    put("titleEn", a.titleEn)
                    put("summaryEn", a.summaryEn)
                    put("sourcePackage", a.sourcePackage)
                    put("status", a.status)
                    put("createdAt", a.createdAt)
                })
            }
            root.put("alerts", alertsArr)
        }
        return root.toString(2)
    }

    private suspend fun buildSingleIncidentReportJson(incidentId: Long): String {
        val inc = db.incidentDao().getAllSync().firstOrNull { it.id == incidentId }
        val root = JSONObject()
        root.put("format", "AMAN_X_INCIDENT_REPORT_V1")
        root.put("disclaimer", "AMAN X is a local security assistant and does not reverse or halt bank transactions.")
        if (inc != null) {
            root.put("title", inc.title)
            root.put("type", inc.incidentType)
            root.put("severity", inc.severity)
            root.put("status", inc.status)
            root.put("notes", runCatching { cryptoManager.decryptString(inc.encryptedNotes) }.getOrDefault(""))
            root.put("createdAt", inc.createdAt)
        }
        val evArr = JSONArray()
        db.evidenceDao().getAllSync().filter { it.incidentId == incidentId }.forEach { ev ->
            evArr.put(JSONObject().apply {
                put("title", ev.title)
                put("sha256Digest", ev.sha256Digest)
                put("capturedAt", ev.capturedAt)
                put("integrityStatus", ev.integrityStatus)
            })
        }
        root.put("evidence", evArr)
        return root.toString(2)
    }

    private suspend fun logActivity(
        eventType: String,
        titleAr: String,
        titleEn: String,
        detailsAr: String,
        detailsEn: String
    ) {
        db.activityLogDao().insert(
            ActivityLogEntity(
                eventType = eventType,
                titleAr = titleAr,
                titleEn = titleEn,
                detailsAr = detailsAr,
                detailsEn = detailsEn,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun defaultRecoveryStepsFor(incidentId: Long, type: IncidentType): List<IncidentRecoveryStepEntity> {
        val steps = when (type) {
            IncidentType.CREDENTIAL_OR_OTP_REQUEST -> listOf(
                Triple("لا تشارك أي رمز إضافي", "Do not share any further codes",
                    "توقف فوراً عن الرد على المتصل أو المرسل، ولا تعطه أي رمز تحقق أو كلمة مرور."),
                Triple("غيّر كلمة مرور حسابك من التطبيق الرسمي", "Change password in your official app",
                    "افتح تطبيق البنك أو المحفظة الرسمي مباشرة وقم بتغيير كلمة المرور والرقم السري."),
                Triple("تواصل مع الدعم الرسمي للبنك", "Contact official bank support",
                    "ابلغ البنك بمحاولة استهداف حسابك عبر الرقم المعتمد على البطاقة (AMAN X لا يوقف المعاملات).")
            )
            IncidentType.SUSPECTED_TRANSFER_SCAM -> listOf(
                Triple("توقف عن أي تحويل إضافي", "Stop any additional transfers",
                    "لا ترسل أي رسوم تفعيل أو مبالغ أخرى مهما كان الإلحاح."),
                Triple("تواصل فوراً مع البنك المعني", "Contact the relevant bank immediately",
                    "أبلغهم برقم العملية والوقت عبر قنواتهم الرسمية؛ التطبيق لا يستطيع إيقاف التحويلات."),
                Triple("احفظ الأدلة في خزنة AMAN X", "Preserve evidence in AMAN X Vault",
                    "احفظ نص المحادثة أو لقطة الإشعار وتأكد من بصمتها الرقمية.")
            )
            else -> listOf(
                Triple("تحقق عبر القناة الرسمية", "Verify via official channel",
                    "تأكد من صحة الادعاء عبر تطبيق البنك الرسمي أو الرقم الرسمي فقط."),
                Triple("احفظ نسخة مشفرة من الأدلة", "Save an encrypted copy of evidence",
                    "أضف الملاحظات والنصوص المشبوهة إلى خزنة الأدلة للرجوع إليها."),
                Triple("راجع إعدادات الأمان في جهازك", "Review device security settings",
                    "تأكد من تفعيل قفل التطبيق وسلامة البيانات في لوحة الصحة الأمنية.")
            )
        }
        return steps.mapIndexed { index, (titleAr, titleEn, descAr) ->
            val descEn = when (index) {
                0 -> "Stop engaging immediately and do not send credentials or further transfers."
                1 -> "Contact your official financial provider directly; AMAN X cannot halt bank transfers."
                else -> "Preserve encrypted evidence and verify SHA-256 integrity inside AMAN X."
            }
            IncidentRecoveryStepEntity(
                incidentId = incidentId,
                stepOrder = index + 1,
                titleAr = titleAr,
                titleEn = titleEn,
                descriptionAr = descAr,
                descriptionEn = descEn,
                isCompleted = false,
                completedAt = null
            )
        }
    }

    private fun SecurityAlertEntity.toDomain() = SecurityAlert(
        id = id,
        recordId = recordId,
        category = runCatching { AlertCategory.valueOf(category) }.getOrDefault(AlertCategory.SECURITY_WARNING),
        riskLevel = runCatching { RiskLevel.valueOf(riskLevel) }.getOrDefault(RiskLevel.LOW),
        confidenceScore = confidenceScore,
        titleAr = titleAr,
        titleEn = titleEn,
        summaryAr = summaryAr,
        summaryEn = summaryEn,
        whatHappenedAr = whatHappenedAr,
        whatHappenedEn = whatHappenedEn,
        whyFlaggedAr = whyFlaggedAr,
        whyFlaggedEn = whyFlaggedEn,
        evidenceBasisAr = evidenceBasisAr,
        evidenceBasisEn = evidenceBasisEn,
        recommendationsAr = recommendationsAr,
        recommendationsEn = recommendationsEn,
        suggestedActionAr = suggestedActionAr,
        suggestedActionEn = suggestedActionEn,
        sourcePackage = sourcePackage,
        status = runCatching { AlertStatus.valueOf(status) }.getOrDefault(AlertStatus.NEW),
        createdAt = createdAt
    )

    private fun ExtractedAmountEntity.toDomain() = ExtractedAmount(
        id = id,
        recordId = recordId,
        amountMinorUnits = amountMinorUnits,
        currency = runCatching { CurrencyCode.valueOf(currencyCode) }.getOrDefault(CurrencyCode.SDG),
        direction = runCatching { TransactionDirection.valueOf(direction) }.getOrDefault(TransactionDirection.UNKNOWN),
        isUserCorrected = isUserCorrected,
        originalAmountMinorUnits = originalAmountMinorUnits,
        sourcePackage = sourcePackage,
        createdAt = createdAt
    )

    private fun NotificationSourceEntity.toDomain() = NotificationSource(
        packageName = packageName,
        displayNameAr = displayNameAr,
        displayNameEn = displayNameEn,
        category = category,
        verificationStatus = runCatching { AdapterVerificationStatus.valueOf(verificationStatus) }
            .getOrDefault(AdapterVerificationStatus.UNVERIFIED_PATTERN),
        isEnabled = isEnabled,
        sensitivityLevel = sensitivityLevel,
        lastSeenAt = lastSeenAt
    )

    private fun FalsePositiveRuleEntity.toDomain() = FalsePositiveRule(
        id = id,
        alertId = alertId,
        sourcePackage = sourcePackage,
        patternHash = patternHash,
        note = note,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun IncidentEntity.toDomain() = Incident(
        id = id,
        title = title,
        incidentType = runCatching { IncidentType.valueOf(incidentType) }.getOrDefault(IncidentType.OTHER),
        severity = runCatching { RiskLevel.valueOf(severity) }.getOrDefault(RiskLevel.MEDIUM),
        status = runCatching { IncidentStatus.valueOf(status) }.getOrDefault(IncidentStatus.OPEN),
        notes = runCatching { cryptoManager.decryptString(encryptedNotes) }.getOrDefault(""),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun EvidenceItemEntity.toDomain() = EvidenceItem(
        id = id,
        incidentId = incidentId,
        title = title,
        evidenceType = runCatching { EvidenceType.valueOf(evidenceType) }.getOrDefault(EvidenceType.MANUAL_NOTE),
        sanitizedSummary = runCatching { cryptoManager.decryptString(encryptedSummary) }.getOrDefault(""),
        encryptedPayloadPath = encryptedPayloadPath,
        sha256Digest = sha256Digest,
        notes = runCatching { cryptoManager.decryptString(encryptedNotes) }.getOrDefault(""),
        capturedAt = capturedAt,
        lastVerifiedAt = lastVerifiedAt,
        integrityStatus = runCatching { IntegrityStatus.valueOf(integrityStatus) }.getOrDefault(IntegrityStatus.PASSED)
    )
}
