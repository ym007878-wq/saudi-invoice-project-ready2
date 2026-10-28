package com.example.data.model

enum class InvoiceSourceType(val displayNameAr: String) {
    QR_CAMERA("تصوير كاميرا"),
    QR_IMAGE("صورة من المعرض"),
    PDF_DOCUMENT("مستند PDF"),
    XML_FILE("ملف ZATCA XML"),
    MANUAL_ENTRY("إدخال يدوي")
}

enum class ReviewStatus(val displayNameAr: String) {
    NEEDS_REVIEW("تحتاج مراجعة وتدقيق"),
    REVIEWED("تمت المراجعة والاعتماد")
}

data class InvoiceLineItem(
    val id: Long = 0,
    val invoiceId: Long = 0,
    val itemIndex: Int = 1,
    val itemName: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val vatRate: Double = 15.0,
    val vatAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

data class ParsedInvoicePayload(
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val issueTime: String = "",
    val supplierName: String = "",
    val supplierVatNumber: String = "",
    val customerName: String = "",
    val customerVatNumber: String = "",
    val taxableAmount: Double = 0.0,
    val vatRate: Double = 15.0,
    val vatAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val sourceType: InvoiceSourceType = InvoiceSourceType.MANUAL_ENTRY,
    val reviewStatus: ReviewStatus = ReviewStatus.NEEDS_REVIEW,
    val notes: String = "",
    val rawQrData: String = "",
    val items: List<InvoiceLineItem> = emptyList()
)

data class TaxPeriodFilter(
    val title: String,
    val startDate: String? = null,
    val endDate: String? = null
)
