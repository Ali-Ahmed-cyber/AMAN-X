package com.amanx.app.domain.security

/**
 * Deterministic pre-storage filter that removes OTP codes, PIN digits, and password secrets
 * before any notification or pasted text is persisted to disk or displayed in logs/evidence.
 */
object OtpRedactor {

    private val arabicEasternDigits = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val otpContextRegex = Regex(
        pattern = "(?i)(otp|verification\\s*code|passcode|pin|password|cvv|رمز\\s*التحقق|رمز\\s*التفعيل|كود\\s*التحقق|الرقم\\s*السري|كلمة\\s*المرور|كلمة\\s*السر|(?:أرسل|ارسل|ابعث|شارك|زوّدني|زودني|أدخل|ادخل|اكتب)\\s+(?:لي\\s+)?(?:ال)?(?:رمز|كود)(?:\\s+(?:التحقق|التفعيل))?)\\s*[:\\-]?\\s*([0-9٠-٩]{4,8})"
    )

    private val reverseOtpContextRegex = Regex(
        pattern = "(?i)([0-9٠-٩]{4,8})\\s+(?:is\\s+your\\s+(?:otp|code|pin)|هو\\s+(?:رمز|كود)\\s+(?:التحقق|التفعيل))"
    )

    private val pinPasswordFieldRegex = Regex(
        pattern = "(?i)(password|passcode|pin\\s*code|secret\\s*pin|كلمة\\s*المرور|الرقم\\s*السري)\\s*[:=]\\s*\\S+"
    )

    data class RedactionResult(
        val sanitizedText: String,
        val containedOtpOrSecret: Boolean
    )

    fun normalizeDigits(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(arabicEasternDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun redact(rawInput: String): RedactionResult {
        if (rawInput.isBlank()) return RedactionResult(rawInput, false)
        var sanitized = rawInput
        var redacted = false

        sanitized = otpContextRegex.replace(sanitized) { match ->
            redacted = true
            "${match.groupValues[1]}: [OTP_REDACTED]"
        }

        sanitized = reverseOtpContextRegex.replace(sanitized) { match ->
            redacted = true
            val tail = match.value.removePrefix(match.groupValues[1])
            "[OTP_REDACTED]$tail"
        }

        sanitized = pinPasswordFieldRegex.replace(sanitized) { match ->
            redacted = true
            "${match.groupValues[1]}: [SECRET_REDACTED]"
        }

        return RedactionResult(
            sanitizedText = sanitized,
            containedOtpOrSecret = redacted
        )
    }
}
