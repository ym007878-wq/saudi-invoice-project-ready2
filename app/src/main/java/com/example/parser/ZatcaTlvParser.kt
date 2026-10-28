package com.example.parser

import android.util.Base64
import com.example.data.model.InvoiceSourceType
import com.example.data.model.ParsedInvoicePayload
import com.example.data.model.ReviewStatus
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ZatcaTlvParser {

    /**
     * Parses a base64 encoded TLV (Tag-Length-Value) string compliant with
     * the Saudi ZATCA (Fatoora) e-invoicing specification.
     */
    fun parse(rawInput: String, sourceType: InvoiceSourceType): ParsedInvoicePayload? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // Try decoding Base64 TLV first
        try {
            val bytes = Base64.decode(trimmed, Base64.DEFAULT)
            if (bytes != null && bytes.size >= 4) {
                val tlvResult = parseTlvBytes(bytes, trimmed, sourceType)
                if (tlvResult != null) {
                    return tlvResult
                }
            }
        } catch (_: Exception) {
            // Not a valid base64 or not TLV
        }

        // Fallback: check if the QR is key-value formatted text or JSON
        return parseFallbackText(trimmed, sourceType)
    }

    private fun parseTlvBytes(
        bytes: ByteArray,
        rawBase64: String,
        sourceType: InvoiceSourceType
    ): ParsedInvoicePayload? {
        var index = 0
        var sellerName = ""
        var vatNumber = ""
        var timestamp = ""
        var invoiceTotal = 0.0
        var vatTotal = 0.0

        var foundTagsCount = 0

        while (index + 1 < bytes.size) {
            val tag = bytes[index].toInt() and 0xFF
            index++

            val length = bytes[index].toInt() and 0xFF
            index++

            if (length < 0 || index + length > bytes.size) {
                break
            }

            val valueBytes = bytes.copyOfRange(index, index + length)
            val value = String(valueBytes, StandardCharsets.UTF_8).trim()
            index += length

            when (tag) {
                1 -> {
                    sellerName = value
                    foundTagsCount++
                }
                2 -> {
                    vatNumber = value
                    foundTagsCount++
                }
                3 -> {
                    timestamp = value
                    foundTagsCount++
                }
                4 -> {
                    invoiceTotal = value.toDoubleOrNull() ?: 0.0
                    foundTagsCount++
                }
                5 -> {
                    vatTotal = value.toDoubleOrNull() ?: 0.0
                    foundTagsCount++
                }
            }
        }

        // If at least seller or vat number or total is extracted, it's a valid TLV
        if (foundTagsCount >= 2 || (sellerName.isNotEmpty() && vatNumber.isNotEmpty())) {
            var datePart = ""
            var timePart = ""

            if (timestamp.isNotEmpty()) {
                val split = timestamp.replace("T", " ").split(" ")
                datePart = split.firstOrNull() ?: ""
                timePart = if (split.size > 1) split[1].replace("Z", "") else ""
            }

            if (datePart.isEmpty()) {
                datePart = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            }

            val taxableAmount = if (invoiceTotal > vatTotal) {
                String.format(Locale.US, "%.2f", invoiceTotal - vatTotal).toDoubleOrNull() ?: (invoiceTotal - vatTotal)
            } else {
                0.0
            }

            val vatRate = if (taxableAmount > 0) {
                val computedRate = (vatTotal / taxableAmount) * 100.0
                if (Math.abs(computedRate - 15.0) < 1.0) 15.0 else String.format(Locale.US, "%.1f", computedRate).toDoubleOrNull() ?: 15.0
            } else {
                15.0
            }

            // Generate an automatic reference number from timestamp if none present
            val defaultInvoiceNo = "ZATCA-${timestamp.replace(Regex("[^0-9]"), "").takeLast(8)}"

            return ParsedInvoicePayload(
                invoiceNumber = defaultInvoiceNo,
                invoiceDate = datePart,
                issueTime = timePart,
                supplierName = sellerName,
                supplierVatNumber = vatNumber,
                taxableAmount = taxableAmount,
                vatRate = vatRate,
                vatAmount = vatTotal,
                totalAmount = invoiceTotal,
                sourceType = sourceType,
                reviewStatus = ReviewStatus.NEEDS_REVIEW,
                notes = "تم استخراج البيانات آلياً من رمز QR (ZATCA TLV Phase 1/2)",
                rawQrData = rawBase64
            )
        }

        return null
    }

    private fun parseFallbackText(text: String, sourceType: InvoiceSourceType): ParsedInvoicePayload? {
        // Match 15-digit Saudi VAT numbers (starts and ends with 3)
        val vatRegex = Regex("\\b3\\d{13}3\\b")
        val vatMatch = vatRegex.find(text)?.value

        // Match amounts
        val amountRegex = Regex("(?i)(?:total|amount|المبلغ|الإجمالي)[:\\s]*([0-9]+(?:\\.[0-9]{1,2})?)")
        val amountMatch = amountRegex.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()

        if (vatMatch != null || amountMatch != null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            val total = amountMatch ?: 0.0
            val vat = String.format(Locale.US, "%.2f", total * 0.15 / 1.15).toDoubleOrNull() ?: 0.0
            val taxable = String.format(Locale.US, "%.2f", total - vat).toDoubleOrNull() ?: 0.0

            return ParsedInvoicePayload(
                invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
                invoiceDate = today,
                supplierName = "مورد غير مسمى",
                supplierVatNumber = vatMatch ?: "",
                taxableAmount = taxable,
                vatRate = 15.0,
                vatAmount = vat,
                totalAmount = total,
                sourceType = sourceType,
                reviewStatus = ReviewStatus.NEEDS_REVIEW,
                notes = "تم الاستخراج بالتعرف على الأنماط النصية",
                rawQrData = text
            )
        }

        return null
    }
}
