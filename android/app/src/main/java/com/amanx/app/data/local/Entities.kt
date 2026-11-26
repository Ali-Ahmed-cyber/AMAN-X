package com.amanx.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notification_sources")
data class NotificationSourceEntity(
    @PrimaryKey val packageName: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val category: String,
    val verificationStatus: String,
    val isEnabled: Boolean,
    val sensitivityLevel: Int,
    val lastSeenAt: Long
)

@Entity(
    tableName = "analyzed_records",
    indices = [
        Index(value = ["dedupHash", "receivedAt"]),
        Index(value = ["sourcePackage"])
    ]
)
data class AnalyzedRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourcePackage: String,
    val inputChannel: String,
    val dedupHash: String,
    val encryptedSanitizedText: String,
    val containedRedactedOtp: Boolean,
    val adapterId: String,
    val receivedAt: Long,
    val retentionExpiresAt: Long?
)

@Entity(
    tableName = "extracted_amounts",
    foreignKeys = [
        ForeignKey(
            entity = AnalyzedRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"]), Index(value = ["currencyCode"])]
)
data class ExtractedAmountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordId: Long,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val direction: String,
    val isUserCorrected: Boolean,
    val originalAmountMinorUnits: Long,
    val sourcePackage: String,
    val createdAt: Long
)

@Entity(
    tableName = "security_alerts",
    foreignKeys = [
        ForeignKey(
            entity = AnalyzedRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"]), Index(value = ["status", "riskLevel"])]
)
data class SecurityAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordId: Long,
    val category: String,
    val riskLevel: String,
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
    val patternHash: String,
    val sourcePackage: String,
    val status: String,
    val createdAt: Long
)

@Entity(tableName = "false_positive_rules")
data class FalsePositiveRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val alertId: Long,
    val sourcePackage: String,
    val patternHash: String,
    val note: String,
    val isActive: Boolean,
    val createdAt: Long
)

@Entity(tableName = "receipt_analyses")
data class ReceiptAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val imageUriString: String,
    val encryptedExtractedText: String,
    val extractedAmountMinorUnits: Long?,
    val currencyCode: String?,
    val extractedDateText: String?,
    val merchantName: String?,
    val referenceNumber: String?,
    val missingFieldsNoteAr: String,
    val missingFieldsNoteEn: String,
    val isUserReviewed: Boolean,
    val createdAt: Long
)

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val incidentType: String,
    val severity: String,
    val status: String,
    val encryptedNotes: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "incident_recovery_steps",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incidentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["incidentId"])]
)
data class IncidentRecoveryStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val incidentId: Long,
    val stepOrder: Int,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val isCompleted: Boolean,
    val completedAt: Long?
)

@Entity(
    tableName = "incident_timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incidentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["incidentId"])]
)
data class IncidentTimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val incidentId: Long,
    val eventType: String,
    val summaryAr: String,
    val summaryEn: String,
    val occurredAt: Long
)

@Entity(
    tableName = "evidence_items",
    indices = [Index(value = ["incidentId"])]
)
data class EvidenceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val incidentId: Long?,
    val title: String,
    val evidenceType: String,
    val encryptedSummary: String,
    val encryptedPayloadPath: String?,
    val sha256Digest: String,
    val encryptedNotes: String,
    val capturedAt: Long,
    val lastVerifiedAt: Long?,
    val integrityStatus: String
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventType: String,
    val titleAr: String,
    val titleEn: String,
    val detailsAr: String,
    val detailsEn: String,
    val createdAt: Long
)

@Entity(tableName = "academy_progress")
data class AcademyProgressEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val isCompleted: Boolean,
    val bestScore: Int,
    val attemptsCount: Int,
    val lastAttemptAt: Long
)

@Entity(tableName = "retention_policies")
data class RetentionPolicyEntity(
    @PrimaryKey val dataType: String,
    val retentionDays: Int?, // null = manual only
    val lastCleanupAt: Long
)

@Entity(tableName = "integrity_check_logs")
data class IntegrityCheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val scope: String,
    val status: String,
    val summaryAr: String,
    val summaryEn: String,
    val checkedAt: Long
)

@Entity(tableName = "export_logs")
data class ExportLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val exportType: String,
    val isEncrypted: Boolean,
    val sha256Digest: String,
    val exportedAt: Long
)
