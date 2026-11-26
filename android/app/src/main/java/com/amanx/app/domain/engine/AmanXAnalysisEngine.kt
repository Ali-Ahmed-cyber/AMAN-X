package com.amanx.app.domain.engine

import com.amanx.app.domain.model.AlertCategory
import com.amanx.app.domain.model.AnalysisInputChannel
import com.amanx.app.domain.model.ConfidenceLevel
import com.amanx.app.domain.model.ExtractedAmount
import com.amanx.app.domain.model.FalsePositiveRule
import com.amanx.app.domain.model.LocalizedText
import com.amanx.app.domain.model.RiskAssessment
import com.amanx.app.domain.model.RiskLevel
import com.amanx.app.domain.security.OtpRedactor
import java.security.MessageDigest

/**
 * AMAN X SECURITY RESPONSE — Hybrid local multi-indicator analysis engine.
 * Strictly avoids standalone URL/link scanning (excluded from scope) and never issues
 * CRITICAL conclusions from a single isolated keyword.
 */
object AmanXAnalysisEngine {

    private const val MAX_ANALYZED_LENGTH = 4000

    data class IndicatorRule(
        val id: String,
        val weight: Int,
        val isHardCredentialRisk: Boolean,
        val patterns: List<String>,
        val label: LocalizedText
    )

    data class AnalysisOutput(
        val sanitizedText: String,
        val containedRedactedOtp: Boolean,
        val dedupHash: String,
        val patternHash: String,
        val adapter: BankWalletAdapter,
        val extractedAmounts: List<ExtractedAmount>,
        val assessment: RiskAssessment,
        val category: AlertCategory
    )

    private val indicatorRules = listOf(
        IndicatorRule(
            id = "OTP_SHARE_REQUEST",
            weight = 45,
            isHardCredentialRisk = true,
            patterns = listOf(
                "أرسل الرمز", "ارسل الكود", "زودنا بالرمز", "شارك رمز التحقق", "اعطني الكود",
                "send otp", "share your code", "share the otp", "tell me the code", "send verification code"
            ),
            label = LocalizedText(
                ar = "طلب مشاركة رمز التحقق (OTP) أو كود سري",
                en = "Request to share an OTP or verification code"
            )
        ),
        IndicatorRule(
            id = "CREDENTIAL_REQUEST",
            weight = 45,
            isHardCredentialRisk = true,
            patterns = listOf(
                "كلمة المرور", "الرقم السري", "رقم البطاقة", "رمز pin", "تحديث بيانات البطاقة",
                "enter your pin", "bank password", "wallet pin", "card pin", "cvv"
            ),
            label = LocalizedText(
                ar = "طلب بيانات سرية أو أرقام دخول مالية",
                en = "Request for financial PIN, password, or secret credentials"
            )
        ),
        IndicatorRule(
            id = "URGENCY_PRESSURE",
            weight = 22,
            isHardCredentialRisk = false,
            patterns = listOf(
                "فوراً", "خلال دقائق", "سيتم إيقاف حسابك", "تجميد الحساب", "إجراء عاجل", "آخر تحذير",
                "immediately", "account suspended", "urgent action", "account will be blocked", "within minutes"
            ),
            label = LocalizedText(
                ar = "أسلوب استعجال وضغط نفسي لإرباك المستخدم",
                en = "Urgency or account-suspension pressure tactic"
            )
        ),
        IndicatorRule(
            id = "IMPERSONATION_CLAIM",
            weight = 25,
            isHardCredentialRisk = false,
            patterns = listOf(
                "خدمة العملاء", "الدعم الفني للبنك", "إدارة البنك", "فريق الحماية", "مندوب البنك",
                "customer support", "bank security team", "official representative", "technical support"
            ),
            label = LocalizedText(
                ar = "ادعاء تمثيل دعم فني أو جهة مصرفية",
                en = "Claim of representing bank support or official staff"
            )
        ),
        IndicatorRule(
            id = "PRIZE_OR_FAKE_REFUND",
            weight = 25,
            isHardCredentialRisk = false,
            patterns = listOf(
                "ربحت جائزة", "مبروك الفوز", "استرداد مالي", "حوالة بالخطأ", "أعد التحويل",
                "you won", "prize winner", "transferred by mistake", "send it back", "claim reward"
            ),
            label = LocalizedText(
                ar = "ادعاء جائزة أو تحويل بالخطأ مع طلب إعادة إرسال",
                en = "Prize claim or mistaken-transfer return request"
            )
        ),
        IndicatorRule(
            id = "UNUSUAL_TRANSFER_INSTRUCTION",
            weight = 20,
            isHardCredentialRisk = false,
            patterns = listOf(
                "حول المبلغ إلى", "دفع رسوم التفعيل", "رسوم تأكيد", "إثبات الرصيد",
                "transfer fee", "activation fee", "send to verify"
            ),
            label = LocalizedText(
                ar = "طلب تحويل رسوم تفعيل أو تأكيد غير معتاد",
                en = "Request to transfer an activation or verification fee"
            )
        )
    )

    fun analyze(
        rawTitle: String,
        rawBody: String,
        sourcePackage: String,
        channel: AnalysisInputChannel,
        activeFalsePositiveRules: List<FalsePositiveRule> = emptyList(),
        sensitivityLevel: Int = 2
    ): AnalysisOutput {
        val clippedTitle = rawTitle.take(500)
        val clippedBody = rawBody.take(MAX_ANALYZED_LENGTH)
        val combinedRaw = listOf(clippedTitle, clippedBody).filter { it.isNotBlank() }.joinToString("\n")

        val redaction = OtpRedactor.redact(combinedRaw)
        val normalizedLower = OtpRedactor.normalizeDigits(redaction.sanitizedText).lowercase()

        val adapter = BankWalletRegistry.resolveAdapter(sourcePackage, clippedTitle, clippedBody)
        val extractedAmounts = adapter.parseAmounts(clippedTitle, clippedBody, sourcePackage)

        val matched = indicatorRules.filter { rule ->
            rule.patterns.any { pattern -> normalizedLower.contains(pattern) }
        }

        val hasHardCredentialRisk = matched.any { it.isHardCredentialRisk }
        val rawScore = matched.sumOf { it.weight }
        val sensitivityBoost = when (sensitivityLevel) {
            3 -> 10
            1 -> -10
            else -> 0
        }

        val patternHash = sha256(
            sourcePackage + "|" + matched.sortedBy { it.id }.joinToString(",") { it.id }
        )

        val suppressedByFalsePositive = !hasHardCredentialRisk && activeFalsePositiveRules.any {
            it.isActive && (it.patternHash == patternHash || (it.sourcePackage == sourcePackage && matched.size <= 1))
        }

        var adjustedScore = (rawScore + sensitivityBoost).coerceIn(0, 100)
        if (suppressedByFalsePositive) {
            adjustedScore = (adjustedScore - 25).coerceAtLeast(0)
        }

        // Never jump to CRITICAL on a single weak/isolated indicator
        val riskLevel = when {
            adjustedScore >= 70 && matched.size >= 2 -> RiskLevel.CRITICAL
            adjustedScore >= 45 || hasHardCredentialRisk -> RiskLevel.HIGH
            adjustedScore >= 20 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val confidenceScore = when {
            matched.size >= 3 -> 88
            matched.size == 2 -> 74
            matched.size == 1 -> 52
            extractedAmounts.isNotEmpty() -> 70
            else -> 40
        }

        val confidenceLevel = when {
            confidenceScore >= 75 -> ConfidenceLevel.HIGH
            confidenceScore >= 55 -> ConfidenceLevel.MODERATE
            else -> ConfidenceLevel.LOW
        }

        val isFinancialNotification = channel == AnalysisInputChannel.NOTIFICATION &&
            extractedAmounts.isNotEmpty() &&
            riskLevel == RiskLevel.LOW

        val category = if (isFinancialNotification) {
            AlertCategory.FINANCIAL_NOTIFICATION
        } else {
            AlertCategory.SECURITY_WARNING
        }

        val dedupBucket = extractedAmounts.firstOrNull()?.let { "${it.currency.name}:${it.amountMinorUnits}" } ?: "NO_AMT"
        val dedupHash = sha256("$sourcePackage|$dedupBucket|${normalizedLower.replace(Regex("\\s+"), " ").trim()}")

        val assessment = buildAssessment(
            riskLevel = riskLevel,
            confidenceScore = confidenceScore,
            confidenceLevel = confidenceLevel,
            matched = matched,
            extractedAmounts = extractedAmounts,
            adapter = adapter,
            containedRedactedOtp = redaction.containedOtpOrSecret,
            suppressedByFalsePositive = suppressedByFalsePositive
        )

        return AnalysisOutput(
            sanitizedText = redaction.sanitizedText,
            containedRedactedOtp = redaction.containedOtpOrSecret,
            dedupHash = dedupHash,
            patternHash = patternHash,
            adapter = adapter,
            extractedAmounts = extractedAmounts,
            assessment = assessment,
            category = category
        )
    }

    private fun buildAssessment(
        riskLevel: RiskLevel,
        confidenceScore: Int,
        confidenceLevel: ConfidenceLevel,
        matched: List<IndicatorRule>,
        extractedAmounts: List<ExtractedAmount>,
        adapter: BankWalletAdapter,
        containedRedactedOtp: Boolean,
        suppressedByFalsePositive: Boolean
    ): RiskAssessment {
        val indicatorLabels = matched.map { it.label }

        val whatHappened = when {
            riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH -> LocalizedText(
                ar = "تم رصد عدة مؤشرات اشتباه في النص قد ترتبط بمحاولة هندسة اجتماعية أو احتيال مالي.",
                en = "Multiple suspicious indicators were detected that may align with social engineering or financial fraud."
            )
            riskLevel == RiskLevel.MEDIUM -> LocalizedText(
                ar = "توجد إشارات تستحق المراجعة والحذر قبل أي تصرف مالي.",
                en = "Noticeable warning cues were found; review carefully before taking any financial action."
            )
            extractedAmounts.isNotEmpty() -> LocalizedText(
                ar = "تم رصد إشعار يتضمن مبلغاً مالياً دون مؤشرات احتيال ظاهرة.",
                en = "A notification containing a financial amount was parsed with no clear fraud indicators."
            )
            else -> LocalizedText(
                ar = "لم تظهر مؤشرات احتيال واضحة في النص المتاح، لكن التحقق الشخصي يظل أساسياً.",
                en = "No obvious fraud indicators appeared in the available text; personal verification remains essential."
            )
        }

        val whyFlagged = if (matched.isEmpty()) {
            LocalizedText(
                ar = "لم تتطابق أنماط الخطر المحلية مع النص؛ التحليل يعتمد على المعلومات النصية المتاحة فقط.",
                en = "No local risk rules matched the text; analysis relies strictly on available text."
            )
        } else {
            val arList = matched.joinToString(" • ") { it.label.ar }
            val enList = matched.joinToString(" • ") { it.label.en }
            val fpNoteAr = if (suppressedByFalsePositive) " (تم خفض الترجيح وفق قاعدة استثناء محلية)." else ""
            val fpNoteEn = if (suppressedByFalsePositive) " (Score adjusted by local false-positive rule)." else ""
            LocalizedText(
                ar = "المؤشرات المرصودة: $arList$fpNoteAr",
                en = "Matched indicators: $enList$fpNoteEn"
            )
        }

        val otpNoteAr = if (containedRedactedOtp) " تم طمس رمز تحقق أو رقم سري لحماية خصوصيتك." else ""
        val otpNoteEn = if (containedRedactedOtp) " Sensitive OTP/PIN digits were redacted for your privacy." else ""

        val evidenceBasis = LocalizedText(
            ar = "استند التقييم إلى ${matched.size} مؤشر(ات) محلية والمحلل (${adapter.displayNameAr})، مع ${extractedAmounts.size} مبلغ مستخرج.$otpNoteAr",
            en = "Assessment used ${matched.size} local rule indicator(s) and parser (${adapter.displayNameEn}) with ${extractedAmounts.size} extracted amount(s).$otpNoteEn"
        )

        val recommendations = buildList {
            if (riskLevel >= RiskLevel.HIGH) {
                add(LocalizedText(
                    ar = "لا تشارك أي رمز تحقق (OTP) أو رقم سري أو كلمة مرور مع أي شخص حتى لو ادعى أنه من البنك.",
                    en = "Never share any OTP, PIN, or password with anyone, even if they claim to be bank support."
                ))
                add(LocalizedText(
                    ar = "تواصل مع الجهة الرسمية فقط عبر الرقم المعتمد على بطاقتك أو موقعها الرسمي، وليس عبر رقم المرسل.",
                    en = "Verify only via your bank's official number on your card or official app, never via the sender's contact."
                ))
                add(LocalizedText(
                    ar = "احفظ لقطة من هذه الرسالة داخل خزنة الأدلة في AMAN X إذا شعرت بتعرضك لمحاولة استهداف.",
                    en = "Save this message snapshot into the AMAN X Evidence Vault if you believe you are being targeted."
                ))
            } else if (riskLevel == RiskLevel.MEDIUM) {
                add(LocalizedText(
                    ar = "تحقق من هوية المرسل قبل الرد أو تنفيذ أي تحويل مالي.",
                    en = "Verify the sender's identity independently before replying or transferring funds."
                ))
            } else {
                add(LocalizedText(
                    ar = "تذكير: هذا التحليل استرشادي من النص المتاح ولا يُغني عن مراجعة تطبيق البنك الرسمي.",
                    en = "Reminder: This analysis reflects available text only and does not replace your official banking app."
                ))
            }
        }

        val suggestedAction = when (riskLevel) {
            RiskLevel.CRITICAL -> LocalizedText(
                ar = "افتح مركز الطوارئ واحفظ الدليل فوراً، وتوقف عن أي تحويل.",
                en = "Open Emergency Center, preserve evidence immediately, and pause any transfer."
            )
            RiskLevel.HIGH -> LocalizedText(
                ar = "راجع التفاصيل وتجاهل الطلب أو أنشئ سجلاً في الأدلة.",
                en = "Review details, ignore the request, or log it in Evidence Vault."
            )
            RiskLevel.MEDIUM -> LocalizedText(
                ar = "تحقق عبر قناة رسمية أو أبلغ إذا كان التنبيه خاطئاً.",
                en = "Verify via an official channel or report if this is a false positive."
            )
            RiskLevel.LOW -> LocalizedText(
                ar = "يمكنك مراجعة المبلغ وتصحيحه يدوياً عند الحاجة.",
                en = "Review or manually correct any extracted amount if needed."
            )
        }

        return RiskAssessment(
            riskLevel = riskLevel,
            confidenceScore = confidenceScore,
            confidenceLevel = confidenceLevel,
            matchedIndicators = indicatorLabels,
            whatHappened = whatHappened,
            whyFlagged = whyFlagged,
            evidenceBasis = evidenceBasis,
            recommendations = recommendations,
            suggestedAction = suggestedAction
        )
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
