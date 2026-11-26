package com.amanx.app.domain.model

enum class RiskLevel(val severityOrder: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4)
}

enum class ConfidenceLevel {
    LOW,
    MODERATE,
    HIGH
}

enum class AnalysisInputChannel {
    NOTIFICATION,
    MANUAL_PASTE,
    SHARE_SHEET
}

enum class CurrencyCode(val symbolEn: String, val symbolAr: String, val minorUnitsFactor: Int = 100) {
    SDG("SDG", "ج.س", 100),
    USD("USD", "دولار", 100)
}

enum class TransactionDirection {
    DEBIT,
    CREDIT,
    UNKNOWN
}

enum class AlertCategory {
    SECURITY_WARNING,
    FINANCIAL_NOTIFICATION,
    AWARENESS_TIP
}

enum class AlertStatus {
    NEW,
    REVIEWED,
    ARCHIVED,
    FALSE_POSITIVE
}

enum class AdapterVerificationStatus {
    UNVERIFIED_PATTERN,
    TESTED_VERIFIED
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppLanguage(val tag: String) {
    AR("ar"),
    EN("en")
}

enum class IncidentStatus {
    OPEN,
    RECOVERING,
    RESOLVED
}

enum class IncidentType {
    SUSPECTED_TRANSFER_SCAM,
    IMPERSONATION_CALL_OR_MSG,
    CREDENTIAL_OR_OTP_REQUEST,
    UNVERIFIED_WALLET_ALERT,
    OTHER
}

enum class EvidenceType {
    NOTIFICATION_SNAPSHOT,
    MANUAL_NOTE,
    RECEIPT_DOCUMENT,
    USER_TEXT_EXCERPT
}

enum class IntegrityStatus {
    PASSED,
    WARNING,
    FAILED
}

data class LocalizedText(
    val ar: String,
    val en: String
) {
    fun get(language: AppLanguage): String = if (language == AppLanguage.AR) ar else en
}

data class ExtractedAmount(
    val id: Long = 0L,
    val recordId: Long = 0L,
    val amountMinorUnits: Long,
    val currency: CurrencyCode,
    val direction: TransactionDirection,
    val isUserCorrected: Boolean = false,
    val originalAmountMinorUnits: Long = amountMinorUnits,
    val sourcePackage: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formatted(language: AppLanguage): String {
        val major = amountMinorUnits / currency.minorUnitsFactor
        val minor = kotlin.math.abs(amountMinorUnits % currency.minorUnitsFactor)
        val numberStr = if (minor == 0L) {
            String.format(java.util.Locale.US, "%,d", major)
        } else {
            String.format(java.util.Locale.US, "%,d.%02d", major, minor)
        }
        val unit = if (language == AppLanguage.AR) currency.symbolAr else currency.symbolEn
        return "$numberStr $unit"
    }
}

data class RiskAssessment(
    val riskLevel: RiskLevel,
    val confidenceScore: Int, // 0..100
    val confidenceLevel: ConfidenceLevel,
    val matchedIndicators: List<LocalizedText>,
    val whatHappened: LocalizedText,
    val whyFlagged: LocalizedText,
    val evidenceBasis: LocalizedText,
    val recommendations: List<LocalizedText>,
    val suggestedAction: LocalizedText,
    val ruleVersion: String = "1.0.0-local"
)

data class SecurityAlert(
    val id: Long = 0L,
    val recordId: Long,
    val category: AlertCategory,
    val riskLevel: RiskLevel,
    val confidenceScore: Int,
    val titleAr: String,
    val titleEn: String,
    val summaryAr: String,
    val summaryEn: String,
    val whatHappenedAr: String,
    val whatHappenedEn: String,
    val whyFlaggedAr: String,
    val whyFlaggedEn: String,
    val evidenceBasisAr: String,
    val evidenceBasisEn: String,
    val recommendationsAr: String,
    val recommendationsEn: String,
    val suggestedActionAr: String,
    val suggestedActionEn: String,
    val sourcePackage: String,
    val status: AlertStatus,
    val createdAt: Long
)

data class NotificationSource(
    val packageName: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val category: String,
    val verificationStatus: AdapterVerificationStatus,
    val isEnabled: Boolean,
    val sensitivityLevel: Int = 2, // 1=Low, 2=Standard, 3=High
    val lastSeenAt: Long = 0L
)

data class FalsePositiveRule(
    val id: Long = 0L,
    val alertId: Long,
    val sourcePackage: String,
    val patternHash: String,
    val note: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ReceiptAnalysis(
    val id: Long = 0L,
    val title: String,
    val imageUriString: String,
    val rawExtractedText: String,
    val extractedAmountMinorUnits: Long?,
    val currency: CurrencyCode?,
    val extractedDateText: String?,
    val merchantName: String?,
    val referenceNumber: String?,
    val missingFieldsNoteAr: String,
    val missingFieldsNoteEn: String,
    val isUserReviewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Incident(
    val id: Long = 0L,
    val title: String,
    val incidentType: IncidentType,
    val severity: RiskLevel,
    val status: IncidentStatus,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class IncidentRecoveryStep(
    val id: Long = 0L,
    val incidentId: Long,
    val stepOrder: Int,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val isCompleted: Boolean,
    val completedAt: Long?
)

data class EvidenceItem(
    val id: Long = 0L,
    val incidentId: Long?,
    val title: String,
    val evidenceType: EvidenceType,
    val sanitizedSummary: String,
    val encryptedPayloadPath: String?,
    val sha256Digest: String,
    val notes: String,
    val capturedAt: Long,
    val lastVerifiedAt: Long?,
    val integrityStatus: IntegrityStatus
)

data class ActivityLogEntry(
    val id: Long = 0L,
    val eventType: String,
    val titleAr: String,
    val titleEn: String,
    val detailsAr: String,
    val detailsEn: String,
    val createdAt: Long
)

data class ReadinessCheckItem(
    val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val isReady: Boolean,
    val isRequiredForCore: Boolean,
    val actionRoute: String
)

data class SecurityHealthReport(
    val scorePercent: Int,
    val statusLabel: LocalizedText,
    val readinessItems: List<ReadinessCheckItem>,
    val unreviewedCriticalCount: Int,
    val processedNotificationsCount: Int,
    val integrityStatus: IntegrityStatus
)

data class AcademyLesson(
    val id: String,
    val title: LocalizedText,
    val summary: LocalizedText,
    val body: LocalizedText,
    val keyTakeaways: List<LocalizedText>,
    val quizQuestion: LocalizedText,
    val quizOptions: List<LocalizedText>,
    val correctOptionIndex: Int
)

data class FraudSimulationStep(
    val prompt: LocalizedText,
    val choices: List<FraudSimulationChoice>
)

data class FraudSimulationChoice(
    val label: LocalizedText,
    val isSafeChoice: Boolean,
    val feedback: LocalizedText
)

data class FraudSimulationScenario(
    val id: String,
    val title: LocalizedText,
    val contextLabel: LocalizedText,
    val redFlags: List<LocalizedText>,
    val steps: List<FraudSimulationStep>
)
