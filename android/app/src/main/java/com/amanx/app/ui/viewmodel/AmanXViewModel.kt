package com.amanx.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amanx.app.AmanXApplication
import com.amanx.app.data.academy.AcademyContentProvider
import com.amanx.app.data.local.ActivityLogEntity
import com.amanx.app.data.local.ExportLogEntity
import com.amanx.app.data.local.IntegrityCheckLogEntity
import com.amanx.app.data.local.RetentionPolicyEntity
import com.amanx.app.data.preferences.UserPreferencesState
import com.amanx.app.domain.model.AlertStatus
import com.amanx.app.domain.model.AnalysisInputChannel
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.EvidenceItem
import com.amanx.app.domain.model.EvidenceType
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.FalsePositiveRule
import com.amanx.app.domain.model.Incident
import com.amanx.app.domain.model.IncidentRecoveryStep
import com.amanx.app.domain.model.IncidentType
import com.amanx.app.domain.model.NotificationSource
import com.amanx.app.domain.model.ReceiptAnalysis
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.SecurityAlert
import com.amanx.app.domain.model.SecurityHealthReport
import com.amanx.app.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiFeedbackMessage(
    val textAr: String,
    val textEn: String,
    val isError: Boolean = false
)

class AmanXViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as AmanXApplication).container.repository

    val preferences: StateFlow<UserPreferencesState> = repo.userPreferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesState())

    val alerts: StateFlow<List<SecurityAlert>> = repo.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val extractedAmounts: StateFlow<List<ExtractedAmount>> = repo.extractedAmounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notificationSources: StateFlow<List<NotificationSource>> = repo.notificationSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val falsePositiveRules: StateFlow<List<FalsePositiveRule>> = repo.falsePositiveRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val receiptAnalyses: StateFlow<List<ReceiptAnalysis>> = repo.receiptAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incidents: StateFlow<List<Incident>> = repo.incidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val evidenceItems: StateFlow<List<EvidenceItem>> = repo.evidenceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repo.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val academyProgress = repo.academyProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val retentionPolicies: StateFlow<List<RetentionPolicyEntity>> = repo.retentionPolicies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val integrityLogs: StateFlow<List<IntegrityCheckLogEntity>> = repo.integrityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exportLogs: StateFlow<List<ExportLogEntity>> = repo.exportLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val processedCount: StateFlow<Int> = repo.processedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val securityHealth: StateFlow<SecurityHealthReport?> = repo.securityHealthReport
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private val _isVaultReAuthenticated = MutableStateFlow(false)
    val isVaultReAuthenticated: StateFlow<Boolean> = _isVaultReAuthenticated.asStateFlow()

    private val _pendingSharedText = MutableStateFlow<String?>(null)
    val pendingSharedText: StateFlow<String?> = _pendingSharedText.asStateFlow()

    private val _pendingSharedImageUri = MutableStateFlow<Uri?>(null)
    val pendingSharedImageUri: StateFlow<Uri?> = _pendingSharedImageUri.asStateFlow()

    private val _pendingAlertDeepLink = MutableStateFlow<Long?>(null)
    val pendingAlertDeepLink: StateFlow<Long?> = _pendingAlertDeepLink.asStateFlow()

    private val _feedback = MutableStateFlow<UiFeedbackMessage?>(null)
    val feedback: StateFlow<UiFeedbackMessage?> = _feedback.asStateFlow()

    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _lastGeneratedRecoveryKey = MutableStateFlow<String?>(null)
    val lastGeneratedRecoveryKey: StateFlow<String?> = _lastGeneratedRecoveryKey.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var alarmJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    fun observeAlert(id: Long): Flow<SecurityAlert?> = repo.observeAlertById(id)
    fun observeIncident(id: Long): Flow<Incident?> = repo.observeIncidentById(id)
    fun observeRecoverySteps(incidentId: Long): Flow<List<IncidentRecoveryStep>> = repo.observeRecoverySteps(incidentId)
    fun observeIncidentTimeline(incidentId: Long) = repo.observeIncidentTimeline(incidentId)

    fun setPendingSharedText(text: String?) {
        _pendingSharedText.value = text
    }

    fun setPendingSharedImageUri(uri: Uri?) {
        _pendingSharedImageUri.value = uri
    }

    fun setPendingAlertDeepLink(alertId: Long?) {
        _pendingAlertDeepLink.value = alertId
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun clearRecoveryKeyNotice() {
        _lastGeneratedRecoveryKey.value = null
    }

    fun isSystemPowerSaveOrReduceMotion(): Boolean {
        val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
        return preferences.value.reduceMotion || (pm?.isPowerSaveMode == true)
    }

    fun isDeviceOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isNotificationListenerEnabled(): Boolean = repo.isNotificationListenerEnabled()
    fun isPostNotificationsGranted(): Boolean = repo.isPostNotificationsGranted()
    fun isBatteryOptimizationIgnored(): Boolean = repo.isBatteryOptimizationIgnored()

    suspend fun getDecryptedRecordText(recordId: Long): String = repo.getDecryptedRecordText(recordId)

    fun unlockWithBiometric() {
        _isSessionUnlocked.value = true
        _isVaultReAuthenticated.value = true
    }

    fun verifyPinAndUnlock(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.preferences.verifyPin(pin.toCharArray())
            if (ok) {
                _isSessionUnlocked.value = true
                _isVaultReAuthenticated.value = true
            }
            onResult(ok)
        }
    }

    fun lockSession() {
        _isSessionUnlocked.value = false
        _isVaultReAuthenticated.value = false
    }

    fun configurePin(pin: String, enableBiometric: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.preferences.configurePin(pin.toCharArray(), enableBiometric)
            _isSessionUnlocked.value = true
            _isVaultReAuthenticated.value = true
            _feedback.value = UiFeedbackMessage("تم تفعيل قفل التطبيق بنجاح", "App Lock configured successfully")
            onDone()
        }
    }

    fun deferPinSetup(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.preferences.deferPinSetup()
            _isSessionUnlocked.value = true
            onDone()
        }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.preferences.setOnboardingCompleted(true)
            onDone()
        }
    }

    fun analyzeManualText(
        text: String,
        channel: AnalysisInputChannel = AnalysisInputChannel.MANUAL_PASTE,
        onAnalyzed: (Long?) -> Unit = {}
    ) {
        if (text.isBlank()) {
            _feedback.value = UiFeedbackMessage("الرجاء إدخال نص للفحص", "Please enter text to analyze", isError = true)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repo.analyzeAndPersistText(
                title = "",
                body = text,
                sourcePackage = if (channel == AnalysisInputChannel.SHARE_SHEET) "share_sheet" else "manual_input",
                channel = channel
            )
            _isBusy.value = false
            _feedback.value = UiFeedbackMessage("اكتمل التحليل المحلي بنجاح", "Local analysis completed")
            onAnalyzed(result.alertId)
        }
    }

    fun updateAlertStatus(alertId: Long, status: AlertStatus) {
        viewModelScope.launch { repo.updateAlertStatus(alertId, status) }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            repo.deleteAlert(alertId)
            _feedback.value = UiFeedbackMessage("تم حذف التنبيه", "Alert deleted")
        }
    }

    fun reportFalsePositive(alertId: Long, note: String) {
        viewModelScope.launch {
            repo.reportFalsePositive(alertId, note)
            _feedback.value = UiFeedbackMessage("تم تسجيل المراجعة في إدارة التنبيهات الخاطئة", "Saved to False Positive Management")
        }
    }

    fun toggleFalsePositiveRule(ruleId: Long, active: Boolean) {
        viewModelScope.launch { repo.toggleFalsePositiveRule(ruleId, active) }
    }

    fun deleteFalsePositiveRule(ruleId: Long) {
        viewModelScope.launch { repo.deleteFalsePositiveRule(ruleId) }
    }

    fun correctAmount(amountId: Long, newMinorUnits: Long, currency: CurrencyCode) {
        viewModelScope.launch {
            repo.correctExtractedAmount(amountId, newMinorUnits, currency)
            _feedback.value = UiFeedbackMessage("تم تصحيح المبلغ وحفظ السجل", "Amount corrected and logged")
        }
    }

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationSourceEnabled(packageName, enabled) }
    }

    fun setSourceSensitivity(packageName: String, level: Int) {
        viewModelScope.launch { repo.setNotificationSourceSensitivity(packageName, level) }
    }

    fun analyzeReceiptImage(uri: Uri, title: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repo.analyzeReceiptUri(uri, title)
            _isBusy.value = false
            _feedback.value = UiFeedbackMessage("تم تحليل الصورة محلياً", "Receipt image analyzed locally")
        }
    }

    fun updateReceiptReview(
        id: Long,
        amountMinorUnits: Long?,
        currency: CurrencyCode?,
        merchant: String?,
        dateText: String?,
        reference: String?
    ) {
        viewModelScope.launch {
            repo.updateReceiptReview(id, amountMinorUnits, currency, merchant, dateText, reference)
            _feedback.value = UiFeedbackMessage("تم حفظ مراجعة المستند", "Receipt review saved")
        }
    }

    fun createIncident(title: String, type: IncidentType, severity: RiskLevel, notes: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.createIncident(title.ifBlank { "Security Incident" }, type, severity, notes)
            _feedback.value = UiFeedbackMessage("تم فتح سجل الحادث", "Security incident created")
            onCreated(id)
        }
    }

    fun toggleRecoveryStep(incidentId: Long, stepId: Long, completed: Boolean) {
        viewModelScope.launch { repo.toggleRecoveryStep(incidentId, stepId, completed) }
    }

    fun resolveIncident(incidentId: Long) {
        viewModelScope.launch {
            repo.markIncidentResolved(incidentId)
            _feedback.value = UiFeedbackMessage("تم تحديث حالة الحادث إلى محلول", "Incident marked resolved")
        }
    }

    fun addEvidence(incidentId: Long?, title: String, type: EvidenceType, content: String, notes: String) {
        viewModelScope.launch {
            repo.addEvidenceItem(incidentId, title.ifBlank { "Evidence Item" }, type, content, notes)
            _feedback.value = UiFeedbackMessage("تم تشفير الدليل وحفظه ببصمة SHA-256", "Evidence encrypted & hashed")
        }
    }

    fun deleteEvidence(id: Long) {
        viewModelScope.launch {
            repo.deleteEvidenceItem(id)
            _feedback.value = UiFeedbackMessage("تم حذف الدليل بشكل آمن", "Evidence securely deleted")
        }
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            _isBusy.value = true
            val log = repo.runDataIntegrityCheck()
            _isBusy.value = false
            _feedback.value = UiFeedbackMessage(log.summaryAr, log.summaryEn, isError = log.status == "FAILED")
        }
    }

    fun recordAcademyAttempt(contentId: String, contentType: String, score: Int) {
        viewModelScope.launch { repo.recordAcademyAttempt(contentId, contentType, score) }
    }

    fun setRetentionDays(dataType: String, days: Int?) {
        viewModelScope.launch {
            repo.setRetentionDays(dataType, days)
            _feedback.value = UiFeedbackMessage("تم تحديث سياسة الاحتفاظ", "Retention policy updated")
        }
    }

    fun runManualRetentionCleanup() {
        viewModelScope.launch {
            val count = repo.executeRetentionCleanup()
            _feedback.value = UiFeedbackMessage("تم تنظيف $count سجل(ات) منتهية", "Cleaned $count expired record(s)")
        }
    }

    fun writeEncryptedBackupToUri(uri: Uri, password: String) {
        if (password.length < 6) {
            _feedback.value = UiFeedbackMessage("كلمة المرور يجب أن تكون 6 أحرف على الأقل", "Password must be at least 6 characters", isError = true)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                val (bytes, recoveryKey) = repo.createEncryptedBackupBytes(password)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("Unable to open destination file")
                }
                recoveryKey
            }
            _isBusy.value = false
            result.onSuccess { key ->
                _lastGeneratedRecoveryKey.value = key
                _feedback.value = UiFeedbackMessage("تم حفظ النسخة المشفرة بنجاح", "Encrypted backup saved successfully")
            }.onFailure {
                _feedback.value = UiFeedbackMessage("فشل إنشاء النسخة الاحتياطية", "Backup export failed", isError = true)
            }
        }
    }

    fun restoreEncryptedBackupFromUri(uri: Uri, password: String, recoveryKey: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Unable to read backup file")
                }
                repo.restoreFromEncryptedBackup(bytes, password, recoveryKey).getOrThrow()
            }
            _isBusy.value = false
            result.onSuccess { count ->
                _feedback.value = UiFeedbackMessage("تمت الاستعادة بنجاح ($count سجل)", "Restored successfully ($count items)")
            }.onFailure {
                _feedback.value = UiFeedbackMessage("فشل التحقق: كلمة المرور أو مفتاح الاستعادة غير صحيح", "Restore rejected: invalid password or recovery key", isError = true)
            }
        }
    }

    fun exportJsonToUri(uri: Uri, incidentId: Long? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching {
                val (json, sha) = repo.generatePrivacyOrIncidentExportJson(incidentId)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination")
                }
                sha
            }
            _isBusy.value = false
            result.onSuccess { sha ->
                _feedback.value = UiFeedbackMessage("تم التصدير (SHA-256: ${sha.take(10)}...)", "Exported (SHA-256: ${sha.take(10)}...)")
            }.onFailure {
                _feedback.value = UiFeedbackMessage("فشل تصدير الملف", "Export failed", isError = true)
            }
        }
    }

    fun wipeAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repo.wipeAllUserData()
            _isBusy.value = false
            _feedback.value = UiFeedbackMessage("تم مسح جميع البيانات المحلية بنجاح", "All local data wiped")
            onDone()
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.preferences.setThemeMode(mode) }
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { repo.preferences.setLanguage(lang) }
    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { repo.preferences.setReduceMotion(enabled) }
    fun setHighContrast(enabled: Boolean) = viewModelScope.launch { repo.preferences.setHighContrast(enabled) }
    fun setCompactDensity(enabled: Boolean) = viewModelScope.launch { repo.preferences.setCompactDensity(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { repo.preferences.setFontScaleMultiplier(scale) }
    fun setAlarmOptions(vibration: Boolean, sound: Boolean) = viewModelScope.launch { repo.preferences.setAlarmOptions(vibration, sound) }
    fun setSmartDedup(enabled: Boolean) = viewModelScope.launch { repo.preferences.setSmartDeduplication(enabled) }
    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch { repo.preferences.setBiometricEnabled(enabled) }
    fun resetAppearanceDefaults() = viewModelScope.launch { repo.preferences.resetAppearanceDefaults() }

    fun triggerSecurityAlarm() {
        if (_isAlarmActive.value) return
        _isAlarmActive.value = true
        val prefs = preferences.value
        alarmJob = viewModelScope.launch(Dispatchers.Default) {
            if (prefs.alarmSoundEnabled) {
                runCatching {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 75)
                }
            }
            while (isActive && _isAlarmActive.value) {
                if (prefs.alarmVibrationEnabled) vibratePulse()
                if (prefs.alarmSoundEnabled) {
                    runCatching { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 260) }
                }
                delay(1200)
            }
        }
    }

    fun stopSecurityAlarm() {
        _isAlarmActive.value = false
        alarmJob?.cancel()
        alarmJob = null
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private fun vibratePulse() {
        val ctx = getApplication<Application>()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(280, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createOneShot(280, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    override fun onCleared() {
        stopSecurityAlarm()
        super.onCleared()
    }

    val lessons = AcademyContentProvider.lessons
    val simulations = AcademyContentProvider.simulations
}
