package com.amanx.app.data.receipt

import android.content.Context
import android.net.Uri
import com.amanx.app.domain.engine.AmountExtractor
import com.amanx.app.domain.model.CurrencyCode
import com.amanx.app.domain.model.ReceiptAnalysis
import com.amanx.app.domain.security.OtpRedactor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Independent local OCR analyzer for receipts, invoices, and payment proof images.
 * Strictly separate from Bank & Wallet Notification summarization.
 * Never treats missing OCR fields as proof of fraud and never claims authenticity.
 */
class LocalReceiptOcrAnalyzer(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val dateRegex = Regex("""(\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4})""")
    private val refRegex = Regex("""(?i)(?:ref|reference|trx|tx|id|رقم\s*المرجع|المرجع)\s*[:#\-]?\s*([A-Z0-9\-]{4,24})""")

    suspend fun analyzeImageUri(uri: Uri, title: String): ReceiptAnalysis {
        val rawText = runLocalOcr(uri)
        return parseExtractedText(
            title = title,
            imageUriString = uri.toString(),
            rawOcrText = rawText
        )
    }

    fun parseExtractedText(
        title: String,
        imageUriString: String,
        rawOcrText: String
    ): ReceiptAnalysis {
        val sanitized = OtpRedactor.redact(rawOcrText).sanitizedText
        val amounts = AmountExtractor.extractAll(sanitized)
        val primaryAmount = amounts.maxByOrNull { it.amountMinorUnits }
        val extractedDate = dateRegex.find(sanitized)?.groupValues?.getOrNull(1)
        val referenceNumber = refRegex.find(sanitized)?.groupValues?.getOrNull(1)
        val merchantName = sanitized
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { line -> line.length in 3..45 && line.any { it.isLetter() } }

        val missingAr = mutableListOf<String>()
        val missingEn = mutableListOf<String>()
        if (primaryAmount == null) {
            missingAr += "المبلغ/العملة"
            missingEn += "Amount/Currency"
        }
        if (extractedDate == null) {
            missingAr += "التاريخ"
            missingEn += "Date"
        }
        if (merchantName == null) {
            missingAr += "اسم الجهة"
            missingEn += "Entity Name"
        }
        if (referenceNumber == null) {
            missingAr += "رقم المرجع"
            missingEn += "Reference Number"
        }

        val noteAr = if (missingAr.isEmpty()) {
            "تم استخراج الحقول الأساسية محلياً. يرجى مراجعتها وتذكر أن الفحص النصي لا يثبت أصالة المستند."
        } else {
            "تعذر استخراج: ${missingAr.joinToString("، ")}. غياب بعض الحقول أو ضعف وضوح الصورة أو النص العربي في OCR الأساسي لا يعني الاحتيال؛ يمكنك التعديل يدوياً."
        }

        val noteEn = if (missingEn.isEmpty()) {
            "Core fields were parsed locally. Please review them and note that OCR extraction does not verify document authenticity."
        } else {
            "Could not extract: ${missingEn.joinToString(", ")}. Missing fields (including non-Latin scripts in offline Latin OCR) do not indicate fraud; you can edit manually."
        }

        return ReceiptAnalysis(
            title = title.ifBlank { "Receipt" },
            imageUriString = imageUriString,
            rawExtractedText = sanitized,
            extractedAmountMinorUnits = primaryAmount?.amountMinorUnits,
            currency = primaryAmount?.currency,
            extractedDateText = extractedDate,
            merchantName = merchantName,
            referenceNumber = referenceNumber,
            missingFieldsNoteAr = noteAr,
            missingFieldsNoteEn = noteEn,
            isUserReviewed = false
        )
    }

    private suspend fun runLocalOcr(uri: Uri): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (cont.isActive) cont.resume(visionText.text ?: "")
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume("")
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }
}
