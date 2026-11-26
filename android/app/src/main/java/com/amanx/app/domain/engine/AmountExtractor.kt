package com.amanx.app.domain.engine

import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.TransactionDirection
import com.amanx.app.domain.security.OtpRedactor
import java.math.BigDecimal

/**
 * Extracts SDG and USD amounts cleanly from notification/message texts
 * supporting both Arabic and English wording and numerals.
 */
object AmountExtractor {

    private val sdgAfterRegex = Regex(
        pattern = "(?i)([0-9][0-9,\\.]{0,14})\\s*(?:sdg|ج\\.\\s*س|جنيه\\s*سوداني|جنيهات|جنيه|ج\\s*س)(?=$|\\s|[,.;:!؟\\)\\]])"
    )
    private val sdgBeforeRegex = Regex(
        pattern = "(?i)(?:^|(?<=\\s))(?:sdg|ج\\.\\s*س|جنيه)\\s*[:\\-]?\\s*([0-9][0-9,\\.]{0,14})"
    )

    private val usdAfterRegex = Regex(
        pattern = "(?i)([0-9][0-9,\\.]{0,14})\\s*(?:usd|us\\$|\\$|دولار\\s*أمريكي|دولار)"
    )
    private val usdBeforeRegex = Regex(
        pattern = "(?i)(?:usd|us\\$|\\$)\\s*[:\\-]?\\s*([0-9][0-9,\\.]{0,14})"
    )

    private val debitMarkers = listOf(
        "خصم", "سحب", "تم تحويل", "دفع", "مشتريات", "شراء", "مدفوع",
        "debited", "withdrawn", "sent", "paid", "transfer out", "purchase", "deducted"
    )

    private val creditMarkers = listOf(
        "إيداع", "استلام", "تم استلام", "وارد", "تحويل وارد", "إضافة",
        "credited", "received", "deposited", "incoming", "refund"
    )

    fun extractAll(text: String, sourcePackage: String = ""): List<ExtractedAmount> {
        if (text.isBlank()) return emptyList()
        val normalized = OtpRedactor.normalizeDigits(text)
        val direction = detectDirection(normalized)
        val results = mutableListOf<ExtractedAmount>()
        val seenKeys = mutableSetOf<String>()

        fun collect(regex: Regex, currency: CurrencyCode) {
            regex.findAll(normalized).forEach { match ->
                val rawNum = match.groupValues[1]
                val minorUnits = parseToMinorUnits(rawNum, currency.minorUnitsFactor) ?: return@forEach
                if (minorUnits <= 0L) return@forEach
                val dedup = "${currency.name}:$minorUnits"
                if (seenKeys.add(dedup)) {
                    results += ExtractedAmount(
                        amountMinorUnits = minorUnits,
                        currency = currency,
                        direction = direction,
                        sourcePackage = sourcePackage
                    )
                }
            }
        }

        collect(sdgAfterRegex, CurrencyCode.SDG)
        collect(sdgBeforeRegex, CurrencyCode.SDG)
        collect(usdAfterRegex, CurrencyCode.USD)
        collect(usdBeforeRegex, CurrencyCode.USD)

        return results
    }

    fun detectDirection(text: String): TransactionDirection {
        val lower = text.lowercase()
        val hasDebit = debitMarkers.any { lower.contains(it) }
        val hasCredit = creditMarkers.any { lower.contains(it) }
        return when {
            hasDebit && !hasCredit -> TransactionDirection.DEBIT
            hasCredit && !hasDebit -> TransactionDirection.CREDIT
            else -> TransactionDirection.UNKNOWN
        }
    }

    private fun parseToMinorUnits(raw: String, factor: Int): Long? {
        val cleaned = raw.trim().trimEnd('.', ',')
        if (cleaned.isEmpty()) return null
        // Handle thousands commas (e.g. 15,000.50 or 15,000) vs single decimal
        val normalizedNum = if (cleaned.contains(',') && cleaned.contains('.')) {
            cleaned.replace(",", "")
        } else if (cleaned.contains(',')) {
            val parts = cleaned.split(',')
            if (parts.last().length == 2) {
                cleaned.replace(",", ".")
            } else {
                cleaned.replace(",", "")
            }
        } else {
            cleaned
        }
        return try {
            val bd = BigDecimal(normalizedNum)
            if (bd > BigDecimal("1000000000")) return null
            bd.multiply(BigDecimal(factor)).toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }
}
