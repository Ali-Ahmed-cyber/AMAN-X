package com.amanx.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSourceDao {
    @Query("SELECT * FROM notification_sources ORDER BY verificationStatus ASC, displayNameEn ASC")
    fun observeAll(): Flow<List<NotificationSourceEntity>>

    @Query("SELECT * FROM notification_sources WHERE packageName = :pkg LIMIT 1")
    suspend fun findByPackage(pkg: String): NotificationSourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(source: NotificationSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sources: List<NotificationSourceEntity>)

    @Query("UPDATE notification_sources SET isEnabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("UPDATE notification_sources SET sensitivityLevel = :level WHERE packageName = :pkg")
    suspend fun setSensitivity(pkg: String, level: Int)

    @Query("UPDATE notification_sources SET lastSeenAt = :seenAt WHERE packageName = :pkg")
    suspend fun updateLastSeen(pkg: String, seenAt: Long)

    @Query("DELETE FROM notification_sources")
    suspend fun deleteAll()
}

@Dao
interface AnalyzedRecordDao {
    @Query("SELECT * FROM analyzed_records ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<AnalyzedRecordEntity>>

    @Query("SELECT * FROM analyzed_records ORDER BY receivedAt DESC")
    suspend fun getAllSync(): List<AnalyzedRecordEntity>

    @Query("SELECT COUNT(*) FROM analyzed_records")
    fun observeCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM analyzed_records WHERE dedupHash = :hash AND receivedAt >= :sinceEpochMs)")
    suspend fun existsDuplicateSince(hash: String, sinceEpochMs: Long): Boolean

    @Insert
    suspend fun insert(record: AnalyzedRecordEntity): Long

    @Query("DELETE FROM analyzed_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM analyzed_records WHERE receivedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("DELETE FROM analyzed_records")
    suspend fun deleteAll()
}

@Dao
interface ExtractedAmountDao {
    @Query("SELECT * FROM extracted_amounts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExtractedAmountEntity>>

    @Query("SELECT * FROM extracted_amounts ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<ExtractedAmountEntity>

    @Insert
    suspend fun insertAll(items: List<ExtractedAmountEntity>)

    @Query("UPDATE extracted_amounts SET amountMinorUnits = :newMinorUnits, currencyCode = :newCurrency, isUserCorrected = 1 WHERE id = :id")
    suspend fun updateManualCorrection(id: Long, newMinorUnits: Long, newCurrency: String)

    @Query("DELETE FROM extracted_amounts")
    suspend fun deleteAll()
}

@Dao
interface SecurityAlertDao {
    @Query("SELECT * FROM security_alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SecurityAlertEntity>>

    @Query("SELECT * FROM security_alerts WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<SecurityAlertEntity?>

    @Query("SELECT * FROM security_alerts WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Long): SecurityAlertEntity?

    @Query("SELECT * FROM security_alerts ORDER BY createdAt DESC")
    suspend fun getAllSync(): List<SecurityAlertEntity>

    @Query("SELECT COUNT(*) FROM security_alerts WHERE riskLevel = 'CRITICAL' AND status = 'NEW'")
    fun observeUnreviewedCriticalCount(): Flow<Int>

    @Insert
    suspend fun insert(alert: SecurityAlertEntity): Long

    @Query("UPDATE security_alerts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM security_alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM security_alerts WHERE createdAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("DELETE FROM security_alerts")
    suspend fun deleteAll()
}

@Dao
interface FalsePositiveRuleDao {
    @Query("SELECT * FROM false_positive_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FalsePositiveRuleEntity>>

    @Query("SELECT * FROM false_positive_rules WHERE isActive = 1")
    suspend fun getActiveSync(): List<FalsePositiveRuleEntity>

    @Insert
    suspend fun insert(rule: FalsePositiveRuleEntity): Long

    @Query("UPDATE false_positive_rules SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM false_positive_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM false_positive_rules")
    suspend fun deleteAll()
}

@Dao
interface ReceiptAnalysisDao {
    @Query("SELECT * FROM receipt_analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReceiptAnalysisEntity>>

    @Insert
    suspend fun insert(receipt: ReceiptAnalysisEntity): Long

    @Query("UPDATE receipt_analyses SET extractedAmountMinorUnits = :amountMinorUnits, currencyCode = :currencyCode, merchantName = :merchant, extractedDateText = :dateText, referenceNumber = :refNum, isUserReviewed = 1 WHERE id = :id")
    suspend fun updateManualReview(
        id: Long,
        amountMinorUnits: Long?,
        currencyCode: String?,
        merchant: String?,
        dateText: String?,
        refNum: String?
    )

    @Query("DELETE FROM receipt_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM receipt_analyses")
    suspend fun deleteAll()
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents ORDER BY updatedAt DESC")
    suspend fun getAllSync(): List<IncidentEntity>

    @Insert
    suspend fun insert(incident: IncidentEntity): Long

    @Query("UPDATE incidents SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("SELECT * FROM incident_recovery_steps WHERE incidentId = :incidentId ORDER BY stepOrder ASC")
    fun observeRecoverySteps(incidentId: Long): Flow<List<IncidentRecoveryStepEntity>>

    @Insert
    suspend fun insertRecoverySteps(steps: List<IncidentRecoveryStepEntity>)

    @Query("UPDATE incident_recovery_steps SET isCompleted = :completed, completedAt = :completedAt WHERE id = :stepId")
    suspend fun updateRecoveryStep(stepId: Long, completed: Boolean, completedAt: Long?)

    @Query("SELECT * FROM incident_timeline_events WHERE incidentId = :incidentId ORDER BY occurredAt DESC")
    fun observeTimeline(incidentId: Long): Flow<List<IncidentTimelineEntity>>

    @Insert
    suspend fun insertTimelineEvent(event: IncidentTimelineEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun deleteAll()
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_items ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<EvidenceItemEntity>>

    @Query("SELECT * FROM evidence_items ORDER BY capturedAt DESC")
    suspend fun getAllSync(): List<EvidenceItemEntity>

    @Insert
    suspend fun insert(item: EvidenceItemEntity): Long

    @Query("UPDATE evidence_items SET integrityStatus = :status, lastVerifiedAt = :verifiedAt WHERE id = :id")
    suspend fun updateIntegrity(id: Long, status: String, verifiedAt: Long)

    @Query("DELETE FROM evidence_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM evidence_items")
    suspend fun deleteAll()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC LIMIT 500")
    fun observeAll(): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insert(entry: ActivityLogEntity)

    @Query("DELETE FROM activity_logs WHERE createdAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("DELETE FROM activity_logs")
    suspend fun deleteAll()
}

@Dao
interface AcademyProgressDao {
    @Query("SELECT * FROM academy_progress ORDER BY lastAttemptAt DESC")
    fun observeAll(): Flow<List<AcademyProgressEntity>>

    @Query("SELECT * FROM academy_progress")
    suspend fun getAllSync(): List<AcademyProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: AcademyProgressEntity)

    @Query("DELETE FROM academy_progress")
    suspend fun deleteAll()
}

@Dao
interface GovernanceDao {
    @Query("SELECT * FROM retention_policies")
    fun observeRetentionPolicies(): Flow<List<RetentionPolicyEntity>>

    @Query("SELECT * FROM retention_policies")
    suspend fun getRetentionPoliciesSync(): List<RetentionPolicyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRetentionPolicy(policy: RetentionPolicyEntity)

    @Query("SELECT * FROM integrity_check_logs ORDER BY checkedAt DESC LIMIT 20")
    fun observeIntegrityLogs(): Flow<List<IntegrityCheckLogEntity>>

    @Insert
    suspend fun insertIntegrityLog(log: IntegrityCheckLogEntity)

    @Query("SELECT * FROM export_logs ORDER BY exportedAt DESC LIMIT 30")
    fun observeExportLogs(): Flow<List<ExportLogEntity>>

    @Insert
    suspend fun insertExportLog(log: ExportLogEntity)

    @Query("DELETE FROM retention_policies")
    suspend fun deleteRetentionPolicies()

    @Query("DELETE FROM integrity_check_logs")
    suspend fun deleteIntegrityLogs()

    @Query("DELETE FROM export_logs")
    suspend fun deleteExportLogs()
}
