package com.amanx.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amanx.app.data.local.AmanXDatabase
import com.amanx.app.data.local.AnalyzedRecordEntity
import com.amanx.app.data.local.ExtractedAmountEntity
import com.amanx.app.data.local.IncidentEntity
import com.amanx.app.data.local.IncidentRecoveryStepEntity
import com.amanx.app.data.receipt.LocalReceiptOcrAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseAndReceiptTest {

    private lateinit var context: Context
    private lateinit var db: AmanXDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = AmanXDatabase.createInMemory(context)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun roomDatabase_insertsDeduplicatesAndCorrectsAmounts() = runTest {
        val now = System.currentTimeMillis()
        val recordId = db.analyzedRecordDao().insert(
            AnalyzedRecordEntity(
                sourcePackage = "com.bok.mobilebanking",
                inputChannel = "NOTIFICATION",
                dedupHash = "abc123",
                encryptedSanitizedText = "enc",
                containedRedactedOtp = false,
                adapterId = "sd_bok_bankak",
                receivedAt = now,
                retentionExpiresAt = null
            )
        )

        assertTrue(db.analyzedRecordDao().existsDuplicateSince("abc123", now - 60_000L))

        db.extractedAmountDao().insertAll(
            listOf(
                ExtractedAmountEntity(
                    recordId = recordId,
                    amountMinorUnits = 500_000L,
                    currencyCode = "SDG",
                    direction = "DEBIT",
                    isUserCorrected = false,
                    originalAmountMinorUnits = 500_000L,
                    sourcePackage = "com.bok.mobilebanking",
                    createdAt = now
                )
            )
        )

        val inserted = db.extractedAmountDao().getAllSync().first()
        db.extractedAmountDao().updateManualCorrection(inserted.id, 600_000L, "SDG")
        val updated = db.extractedAmountDao().getAllSync().first()
        assertEquals(600_000L, updated.amountMinorUnits)
        assertTrue(updated.isUserCorrected)
        assertEquals(500_000L, updated.originalAmountMinorUnits)
    }

    @Test
    fun incidentRecovery_persistsStepsAndCompletion() = runTest {
        val now = System.currentTimeMillis()
        val incId = db.incidentDao().insert(
            IncidentEntity(
                title = "Test Incident",
                incidentType = "SUSPECTED_TRANSFER_SCAM",
                severity = "HIGH",
                status = "OPEN",
                encryptedNotes = "",
                createdAt = now,
                updatedAt = now
            )
        )
        db.incidentDao().insertRecoverySteps(
            listOf(
                IncidentRecoveryStepEntity(
                    incidentId = incId,
                    stepOrder = 1,
                    titleAr = "خطوة 1",
                    titleEn = "Step 1",
                    descriptionAr = "وصف",
                    descriptionEn = "Desc",
                    isCompleted = false,
                    completedAt = null
                )
            )
        )

        val step = db.incidentDao().observeRecoverySteps(incId).first().first()
        assertFalse(step.isCompleted)
        db.incidentDao().updateRecoveryStep(step.id, true, now)
        val completedStep = db.incidentDao().observeRecoverySteps(incId).first().first()
        assertTrue(completedStep.isCompleted)
    }

    @Test
    fun receiptAnalyzer_parsesTextAndDoesNotTreatMissingFieldsAsFraud() {
        val analyzer = LocalReceiptOcrAnalyzer(context)
        val parsed = analyzer.parseExtractedText(
            title = "Sample Receipt",
            imageUriString = "content://test/1",
            rawOcrText = "Khartoum Market\nTotal: 25.00 USD\nRef: TX9912"
        )
        assertEquals(2500L, parsed.extractedAmountMinorUnits)
        assertEquals("TX9912", parsed.referenceNumber)
        assertTrue(parsed.missingFieldsNoteEn.contains("Date"))
        assertTrue(parsed.missingFieldsNoteEn.contains("do not indicate fraud"))
    }
}
