package com.amanx.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NotificationSourceEntity::class,
        AnalyzedRecordEntity::class,
        ExtractedAmountEntity::class,
        SecurityAlertEntity::class,
        FalsePositiveRuleEntity::class,
        ReceiptAnalysisEntity::class,
        IncidentEntity::class,
        IncidentRecoveryStepEntity::class,
        IncidentTimelineEntity::class,
        EvidenceItemEntity::class,
        ActivityLogEntity::class,
        AcademyProgressEntity::class,
        RetentionPolicyEntity::class,
        IntegrityCheckLogEntity::class,
        ExportLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AmanXDatabase : RoomDatabase() {
    abstract fun notificationSourceDao(): NotificationSourceDao
    abstract fun analyzedRecordDao(): AnalyzedRecordDao
    abstract fun extractedAmountDao(): ExtractedAmountDao
    abstract fun securityAlertDao(): SecurityAlertDao
    abstract fun falsePositiveRuleDao(): FalsePositiveRuleDao
    abstract fun receiptAnalysisDao(): ReceiptAnalysisDao
    abstract fun incidentDao(): IncidentDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun academyProgressDao(): AcademyProgressDao
    abstract fun governanceDao(): GovernanceDao

    companion object {
        @Volatile
        private var INSTANCE: AmanXDatabase? = null

        fun getInstance(context: Context): AmanXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AmanXDatabase::class.java,
                    "amanx_local.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): AmanXDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AmanXDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
