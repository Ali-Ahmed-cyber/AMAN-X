package com.amanx.app

import com.amanx.app.domain.engine.AmanXAnalysisEngine
import com.amanx.app.domain.engine.AmountExtractor
import com.amanx.app.domain.engine.BankWalletRegistry
import com.amanx.app.domain.engine.SecurityHealthCalculator
import com.amanx.app.domain.model.AdapterVerificationStatus
import com.amanx.app.domain.model.AnalysisInputChannel
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.FalsePositiveRule
import com.amanx.app.domain.model.IntegrityStatus
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.model.TransactionDirection
import com.amanx.app.domain.security.OtpRedactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmanXEngineTest {

    @Test
    fun otpRedactor_redactsArabicAndEnglishOtpCodes() {
        val arabic = OtpRedactor.redact("عزيزي العميل رمز التحقق: 482910 لا تشاركه مع أحد")
        assertTrue(arabic.containedOtpOrSecret)
        assertFalse(arabic.sanitizedText.contains("482910"))
        assertTrue(arabic.sanitizedText.contains("[OTP_REDACTED]"))

        val english = OtpRedactor.redact("Your verification code: 913245 for login")
        assertTrue(english.containedOtpOrSecret)
        assertFalse(english.sanitizedText.contains("913245"))
    }

    @Test
    fun amountExtractor_extractsSdgAndUsdWithArabicNumerals() {
        val text = "تم خصم مبلغ ١٥,٠٠٠ ج.س وتحويل 45.50 USD بنجاح"
        val extracted = AmountExtractor.extractAll(text, "com.test")
        val sdg = extracted.firstOrNull { it.currency == CurrencyCode.SDG }
        val usd = extracted.firstOrNull { it.currency == CurrencyCode.USD }

        assertEquals(1_500_000L, sdg?.amountMinorUnits)
        assertEquals(4_550L, usd?.amountMinorUnits)
        assertEquals(TransactionDirection.DEBIT, sdg?.direction)
    }

    @Test
    fun analysisEngine_neverFlagsCriticalOnSingleWeakKeyword() {
        val output = AmanXAnalysisEngine.analyze(
            rawTitle = "تنبيه",
            rawBody = "يرجى مراجعة التحديث فوراً",
            sourcePackage = "com.example",
            channel = AnalysisInputChannel.MANUAL_PASTE
        )
        assertNotEquals(RiskLevel.CRITICAL, output.assessment.riskLevel)
    }

    @Test
    fun analysisEngine_detectsCriticalOnMultiIndicatorOtpScamAndRedactsOtp() {
        val output = AmanXAnalysisEngine.analyze(
            rawTitle = "دعم البنك",
            rawBody = "معك خدمة العملاء، سيتم إيقاف حسابك فوراً، أرسل الرمز 654321 الآن لتأكيد الحساب",
            sourcePackage = "com.whatsapp",
            channel = AnalysisInputChannel.MANUAL_PASTE
        )
        assertEquals(RiskLevel.CRITICAL, output.assessment.riskLevel)
        assertTrue(output.containedRedactedOtp)
        assertFalse(output.sanitizedText.contains("654321"))
    }

    @Test
    fun analysisEngine_falsePositiveRuleDoesNotMuteHardCredentialRisk() {
        val probe = AmanXAnalysisEngine.analyze(
            rawTitle = "",
            rawBody = "أرسل الرمز الآن",
            sourcePackage = "com.test",
            channel = AnalysisInputChannel.MANUAL_PASTE
        )
        val fpRule = FalsePositiveRule(
            id = 1L,
            alertId = 1L,
            sourcePackage = "com.test",
            patternHash = probe.patternHash,
            note = "test",
            isActive = true
        )
        val retested = AmanXAnalysisEngine.analyze(
            rawTitle = "",
            rawBody = "أرسل الرمز الآن",
            sourcePackage = "com.test",
            channel = AnalysisInputChannel.MANUAL_PASTE,
            activeFalsePositiveRules = listOf(fpRule)
        )
        assertTrue(retested.assessment.riskLevel >= RiskLevel.HIGH)
    }

    @Test
    fun bankAdapters_doNotClaimVerifiedSupportBeforeRealTesting() {
        assertTrue(
            BankWalletRegistry.adapters.all {
                it.verificationStatus == AdapterVerificationStatus.UNVERIFIED_PATTERN
            }
        )
    }

    @Test
    fun securityHealthCalculator_computesRealReadinessScore() {
        val report = SecurityHealthCalculator.calculate(
            isAppLockConfigured = true,
            isNotificationListenerEnabled = true,
            isPostNotificationsGranted = true,
            isBatteryOptimizationIgnored = false,
            isKeystoreHealthy = true,
            hasRecentBackup = false,
            unreviewedCriticalCount = 0,
            processedNotificationsCount = 4,
            integrityStatus = IntegrityStatus.PASSED
        )
        assertEquals(80, report.scorePercent)
        assertEquals(6, report.readinessItems.size)
    }
}
