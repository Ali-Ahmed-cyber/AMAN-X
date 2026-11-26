package com.amanx.app.domain.engine

import com.amanx.app.domain.model.AdapterVerificationStatus
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.NotificationSource

/**
 * Extensible Bank & Wallet Adapter Architecture.
 * All Sudanese bank/wallet adapters start explicitly as UNVERIFIED_PATTERN until tested
 * against real device notification payloads from those apps.
 */
interface BankWalletAdapter {
    val id: String
    val packageNames: Set<String>
    val displayNameAr: String
    val displayNameEn: String
    val verificationStatus: AdapterVerificationStatus
    fun matchesNotification(packageName: String, title: String, body: String): Boolean
    fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount>
}

object BankWalletRegistry {

    private val bankakAdapter = object : BankWalletAdapter {
        override val id = "sd_bok_bankak"
        override val packageNames = setOf("com.bok.mobilebanking")
        override val displayNameAr = "بنك الخرطوم (بنكك) — نمط قيد الاختبار"
        override val displayNameEn = "Bank of Khartoum (Bankak) — Pattern Pending Real Test"
        override val verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN

        override fun matchesNotification(packageName: String, title: String, body: String): Boolean {
            if (packageName in packageNames) return true
            val combined = "$title $body".lowercase()
            return combined.contains("بنكك") || combined.contains("bankak") || combined.contains("بنك الخرطوم")
        }

        override fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount> =
            AmountExtractor.extractAll("$title $body", sourcePackage)
    }

    private val fawryAdapter = object : BankWalletAdapter {
        override val id = "sd_fib_fawry"
        override val packageNames = setOf("sd.faisalbank.fawry")
        override val displayNameAr = "بنك فيصل الإسلامي (فوري) — نمط قيد الاختبار"
        override val displayNameEn = "Faisal Islamic Bank (Fawry) — Pattern Pending Real Test"
        override val verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN

        override fun matchesNotification(packageName: String, title: String, body: String): Boolean {
            if (packageName in packageNames) return true
            val combined = "$title $body".lowercase()
            return combined.contains("بنك فيصل") || combined.contains("faisal") || combined.contains("فوري")
        }

        override fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount> =
            AmountExtractor.extractAll("$title $body", sourcePackage)
    }

    private val ocashAdapter = object : BankWalletAdapter {
        override val id = "sd_onb_ocash"
        override val packageNames = setOf("com.onb.ocash")
        override val displayNameAr = "بنك أم درمان الوطني (أوكاش) — نمط قيد الاختبار"
        override val displayNameEn = "Omdurman National Bank (O-Cash) — Pattern Pending Real Test"
        override val verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN

        override fun matchesNotification(packageName: String, title: String, body: String): Boolean {
            if (packageName in packageNames) return true
            val combined = "$title $body".lowercase()
            return combined.contains("أوكاش") || combined.contains("o-cash") || combined.contains("ocash") || combined.contains("أم درمان الوطني")
        }

        override fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount> =
            AmountExtractor.extractAll("$title $body", sourcePackage)
    }

    private val mycashiAdapter = object : BankWalletAdapter {
        override val id = "sd_mycashi"
        override val packageNames = setOf("sd.mycashi.app")
        override val displayNameAr = "محفظة ماي كاشي — نمط قيد الاختبار"
        override val displayNameEn = "MyCashi Wallet — Pattern Pending Real Test"
        override val verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN

        override fun matchesNotification(packageName: String, title: String, body: String): Boolean {
            if (packageName in packageNames) return true
            val combined = "$title $body".lowercase()
            return combined.contains("ماي كاشي") || combined.contains("mycashi")
        }

        override fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount> =
            AmountExtractor.extractAll("$title $body", sourcePackage)
    }

    private val genericAdapter = object : BankWalletAdapter {
        override val id = "generic_financial"
        override val packageNames = emptySet<String>()
        override val displayNameAr = "محلل مالي عام (غير مخصص لبنك محدد)"
        override val displayNameEn = "Generic Financial Notification Parser"
        override val verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN

        override fun matchesNotification(packageName: String, title: String, body: String): Boolean = true

        override fun parseAmounts(title: String, body: String, sourcePackage: String): List<ExtractedAmount> =
            AmountExtractor.extractAll("$title $body", sourcePackage)
    }

    val adapters: List<BankWalletAdapter> = listOf(
        bankakAdapter,
        fawryAdapter,
        ocashAdapter,
        mycashiAdapter
    )

    fun resolveAdapter(packageName: String, title: String, body: String): BankWalletAdapter {
        return adapters.firstOrNull { it.matchesNotification(packageName, title, body) } ?: genericAdapter
    }

    fun defaultKnownSources(): List<NotificationSource> = listOf(
        NotificationSource(
            packageName = "com.bok.mobilebanking",
            displayNameAr = "بنكك (يتطلب اختبار إشعارات فعلي)",
            displayNameEn = "Bankak (Requires Real Notification Test)",
            category = "BANK_WALLET_PATTERN",
            verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN,
            isEnabled = true
        ),
        NotificationSource(
            packageName = "sd.faisalbank.fawry",
            displayNameAr = "فوري - فيصل (يتطلب اختبار إشعارات فعلي)",
            displayNameEn = "Fawry - Faisal (Requires Real Notification Test)",
            category = "BANK_WALLET_PATTERN",
            verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN,
            isEnabled = true
        ),
        NotificationSource(
            packageName = "com.onb.ocash",
            displayNameAr = "أوكاش (يتطلب اختبار إشعارات فعلي)",
            displayNameEn = "O-Cash (Requires Real Notification Test)",
            category = "BANK_WALLET_PATTERN",
            verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN,
            isEnabled = true
        ),
        NotificationSource(
            packageName = "sd.mycashi.app",
            displayNameAr = "ماي كاشي (يتطلب اختبار إشعارات فعلي)",
            displayNameEn = "MyCashi (Requires Real Notification Test)",
            category = "BANK_WALLET_PATTERN",
            verificationStatus = AdapterVerificationStatus.UNVERIFIED_PATTERN,
            isEnabled = true
        ),
        NotificationSource(
            packageName = "com.whatsapp",
            displayNameAr = "واتساب (إشعارات الرسائل المصرح بها)",
            displayNameEn = "WhatsApp (Authorized Notifications)",
            category = "MESSAGING",
            verificationStatus = AdapterVerificationStatus.TESTED_VERIFIED,
            isEnabled = true
        ),
        NotificationSource(
            packageName = "com.google.android.apps.messaging",
            displayNameAr = "الرسائل النصية (عبر الإشعارات فقط)",
            displayNameEn = "Messages (Via Notifications Only)",
            category = "MESSAGING",
            verificationStatus = AdapterVerificationStatus.TESTED_VERIFIED,
            isEnabled = true
        )
    )
}
