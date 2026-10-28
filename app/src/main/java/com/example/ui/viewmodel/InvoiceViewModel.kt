package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceSourceType
import com.example.data.model.ParsedInvoicePayload
import com.example.data.model.ReviewStatus
import com.example.data.repository.InvoiceRepository
import com.example.parser.PdfInvoiceHelper
import com.example.parser.QrBitmapDecoder
import com.example.parser.XlsxExporter
import com.example.parser.ZatcaTlvParser
import com.example.parser.ZatcaXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardStats(
    val totalCount: Int = 0,
    val totalTaxable: Double = 0.0,
    val totalVat: Double = 0.0,
    val totalGross: Double = 0.0,
    val needsReviewCount: Int = 0,
    val reviewedCount: Int = 0
)

class InvoiceViewModel(private val repository: InvoiceRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // ALL, CURRENT_MONTH, CURRENT_QUARTER, NEEDS_REVIEW, REVIEWED
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _activeInvoiceToReview = MutableStateFlow<ParsedInvoicePayload?>(null)
    val activeInvoiceToReview = _activeInvoiceToReview.asStateFlow()

    private val _activeInvoiceId = MutableStateFlow<Long?>(null) // null if new, > 0 if editing existing
    val activeInvoiceId = _activeInvoiceId.asStateFlow()

    private val _duplicateWarning = MutableStateFlow<InvoiceEntity?>(null)
    val duplicateWarning = _duplicateWarning.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile = _exportedFile.asStateFlow()

    val allInvoicesWithItems: StateFlow<List<InvoiceWithItems>> = repository.allInvoicesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(
        allInvoicesWithItems,
        _searchQuery,
        _selectedFilter
    ) { invoices, query, filter ->
        invoices.filter { item ->
            val inv = item.invoice
            val matchesQuery = query.isBlank() ||
                    inv.supplierName.contains(query, ignoreCase = true) ||
                    inv.supplierVatNumber.contains(query) ||
                    inv.invoiceNumber.contains(query, ignoreCase = true) ||
                    (inv.customerName?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                "NEEDS_REVIEW" -> inv.reviewStatus == ReviewStatus.NEEDS_REVIEW.name
                "REVIEWED" -> inv.reviewStatus == ReviewStatus.REVIEWED.name
                "CURRENT_MONTH" -> isDateInCurrentMonth(inv.invoiceDate)
                "CURRENT_QUARTER" -> isDateInCurrentQuarter(inv.invoiceDate)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = filteredInvoices.combine(_selectedFilter) { list, _ ->
        var taxable = 0.0
        var vat = 0.0
        var gross = 0.0
        var needsReview = 0
        var reviewed = 0

        list.forEach {
            taxable += it.invoice.taxableAmount
            vat += it.invoice.vatAmount
            gross += it.invoice.totalAmount
            if (it.invoice.reviewStatus == ReviewStatus.REVIEWED.name) {
                reviewed++
            } else {
                needsReview++
            }
        }

        DashboardStats(
            totalCount = list.size,
            totalTaxable = taxable,
            totalVat = vat,
            totalGross = gross,
            needsReviewCount = needsReview,
            reviewedCount = reviewed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissDuplicateWarning() {
        _duplicateWarning.value = null
    }

    fun closeReview() {
        _activeInvoiceToReview.value = null
        _activeInvoiceId.value = null
        _duplicateWarning.value = null
    }

    fun startManualEntry() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val defaultInvNo = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
        _activeInvoiceId.value = null
        _activeInvoiceToReview.value = ParsedInvoicePayload(
            invoiceNumber = defaultInvNo,
            invoiceDate = today,
            supplierName = "",
            supplierVatNumber = "",
            taxableAmount = 0.0,
            vatRate = 15.0,
            vatAmount = 0.0,
            totalAmount = 0.0,
            sourceType = InvoiceSourceType.MANUAL_ENTRY,
            reviewStatus = ReviewStatus.NEEDS_REVIEW,
            notes = "",
            items = listOf(
                InvoiceLineItem(
                    itemIndex = 1,
                    itemName = "مشتريات خاضعة للضريبة الأساسية",
                    quantity = 1.0,
                    unitPrice = 0.0,
                    vatRate = 15.0,
                    vatAmount = 0.0,
                    totalAmount = 0.0
                )
            )
        )
    }

    fun editInvoice(invoiceWithItems: InvoiceWithItems) {
        val inv = invoiceWithItems.invoice
        _activeInvoiceId.value = inv.id
        _activeInvoiceToReview.value = ParsedInvoicePayload(
            invoiceNumber = inv.invoiceNumber,
            invoiceDate = inv.invoiceDate,
            issueTime = inv.issueTime ?: "",
            supplierName = inv.supplierName,
            supplierVatNumber = inv.supplierVatNumber,
            customerName = inv.customerName ?: "",
            customerVatNumber = inv.customerVatNumber ?: "",
            taxableAmount = inv.taxableAmount,
            vatRate = inv.vatRate,
            vatAmount = inv.vatAmount,
            totalAmount = inv.totalAmount,
            sourceType = try { InvoiceSourceType.valueOf(inv.sourceType) } catch (_: Exception) { InvoiceSourceType.MANUAL_ENTRY },
            reviewStatus = try { ReviewStatus.valueOf(inv.reviewStatus) } catch (_: Exception) { ReviewStatus.REVIEWED },
            notes = inv.notes ?: "",
            rawQrData = inv.rawQrData ?: "",
            items = invoiceWithItems.items.map {
                InvoiceLineItem(
                    id = it.id,
                    invoiceId = it.invoiceId,
                    itemIndex = it.itemIndex,
                    itemName = it.itemName,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    vatRate = it.vatRate,
                    vatAmount = it.vatAmount,
                    totalAmount = it.totalAmount
                )
            }
        )
    }

    fun updateActiveInvoice(transform: (ParsedInvoicePayload) -> ParsedInvoicePayload) {
        _activeInvoiceToReview.value?.let { current ->
            _activeInvoiceToReview.value = transform(current)
        }
    }

    fun processImageUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val qrText = QrBitmapDecoder.decodeQrCode(bitmap)
                        if (!qrText.isNullOrEmpty()) {
                            val parsed = ZatcaTlvParser.parse(qrText, InvoiceSourceType.QR_IMAGE)
                            if (parsed != null) {
                                _activeInvoiceId.value = null
                                _activeInvoiceToReview.value = parsed
                                _statusMessage.value = "تم قراءة وتفكيك رمز الفاتورة بنجاح"
                            } else {
                                _statusMessage.value = "تم مسح الرمز ولكن لم يتم التعرف على بنية ZATCA TLV"
                            }
                        } else {
                            _statusMessage.value = "لم يتم العثور على رمز استجابة سريع (QR) واضح في الصورة"
                        }
                    } else {
                        _statusMessage.value = "تعذر تحميل ملف الصورة"
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء معالجة الصورة: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val qrText = QrBitmapDecoder.decodeQrCode(bitmap)
                    if (!qrText.isNullOrEmpty()) {
                        val parsed = ZatcaTlvParser.parse(qrText, InvoiceSourceType.QR_CAMERA)
                        if (parsed != null) {
                            _activeInvoiceId.value = null
                            _activeInvoiceToReview.value = parsed
                            _statusMessage.value = "تم التقاط وقراءة رمز الفاتورة بنجاح"
                        } else {
                            _statusMessage.value = "تم مسح الرمز ولكن لا يتوافق مع بنية الفاتورة الإلكترونية"
                        }
                    } else {
                        _statusMessage.value = "تعذر قراءة الرمز من الصورة الملتقطة. يُرجى التقريب وتحسين الإضاءة."
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء معالجة الصورة: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processPdfUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val result = PdfInvoiceHelper.processPdf(context, uri)
                    if (result.payload != null) {
                        _activeInvoiceId.value = null
                        _activeInvoiceToReview.value = result.payload
                        _statusMessage.value = "تم مسح وقراءة الفاتورة من ملف PDF بنجاح"
                    } else {
                        _statusMessage.value = "تم فحص ملف PDF ولكن لم يُعثر على رمز QR ضريبي معتمد"
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء معالجة ملف PDF: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processXmlUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val parsed = ZatcaXmlParser.parse(stream)
                        if (parsed != null) {
                            _activeInvoiceId.value = null
                            _activeInvoiceToReview.value = parsed
                            _statusMessage.value = "تم استيراد بيانات الفاتورة والأصناف من ملف XML بنجاح"
                        } else {
                            _statusMessage.value = "الملف ليس بتنسيق ZATCA UBL 2.1 صالح"
                        }
                    }
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء قراءة ملف XML: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveActiveInvoice(forceSave: Boolean = false) {
        val payload = _activeInvoiceToReview.value ?: return
        val currentId = _activeInvoiceId.value

        viewModelScope.launch {
            try {
                // If not forced and new invoice, check for duplicates
                if (!forceSave && currentId == null) {
                    val duplicates = repository.checkDuplicate(
                        invoiceNumber = payload.invoiceNumber,
                        supplierVat = payload.supplierVatNumber,
                        totalAmount = payload.totalAmount,
                        invoiceDate = payload.invoiceDate
                    )
                    if (duplicates.isNotEmpty()) {
                        _duplicateWarning.value = duplicates.first()
                        return@launch
                    }
                }

                val invoiceEntity = InvoiceEntity(
                    id = currentId ?: 0,
                    invoiceNumber = payload.invoiceNumber.ifBlank { "INV-${System.currentTimeMillis().toString().takeLast(6)}" },
                    invoiceDate = payload.invoiceDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) },
                    issueTime = payload.issueTime.ifBlank { null },
                    supplierName = payload.supplierName.ifBlank { "مورد غير محدد" },
                    supplierVatNumber = payload.supplierVatNumber,
                    customerName = payload.customerName.ifBlank { null },
                    customerVatNumber = payload.customerVatNumber.ifBlank { null },
                    taxableAmount = payload.taxableAmount,
                    vatRate = payload.vatRate,
                    vatAmount = payload.vatAmount,
                    totalAmount = payload.totalAmount,
                    sourceType = payload.sourceType.name,
                    reviewStatus = payload.reviewStatus.name,
                    notes = payload.notes.ifBlank { null },
                    rawQrData = payload.rawQrData.ifBlank { null }
                )

                val itemsEntities = payload.items.mapIndexed { index, item ->
                    InvoiceItemEntity(
                        id = item.id,
                        invoiceId = currentId ?: 0,
                        itemIndex = index + 1,
                        itemName = item.itemName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        vatRate = item.vatRate,
                        vatAmount = item.vatAmount,
                        totalAmount = item.totalAmount
                    )
                }

                if (currentId == null || currentId == 0L) {
                    repository.saveInvoice(invoiceEntity, itemsEntities)
                    _statusMessage.value = "تم حفظ الفاتورة بنجاح في السجل المحلي"
                } else {
                    repository.updateInvoice(invoiceEntity, itemsEntities)
                    _statusMessage.value = "تم تحديث بيانات الفاتورة بنجاح"
                }

                closeReview()
            } catch (e: Exception) {
                _statusMessage.value = "تعذر حفظ الفاتورة: ${e.localizedMessage}"
            }
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            try {
                repository.deleteInvoice(invoice)
                _statusMessage.value = "تم حذف الفاتورة رقم ${invoice.invoiceNumber}"
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء الحذف: ${e.localizedMessage}"
            }
        }
    }

    fun toggleReviewStatus(invoice: InvoiceEntity) {
        viewModelScope.launch {
            try {
                val newStatus = if (invoice.reviewStatus == ReviewStatus.REVIEWED.name) {
                    ReviewStatus.NEEDS_REVIEW.name
                } else {
                    ReviewStatus.REVIEWED.name
                }
                repository.updateInvoice(invoice.copy(reviewStatus = newStatus), emptyList())
                _statusMessage.value = if (newStatus == ReviewStatus.REVIEWED.name) "تم اعتماد ومراجعة الفاتورة" else "تم إعادة الفاتورة للمراجعة"
            } catch (e: Exception) {
                _statusMessage.value = "تعذر تحديث الحالة: ${e.localizedMessage}"
            }
        }
    }

    fun exportToExcel(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val invoices = filteredInvoices.value
                if (invoices.isEmpty()) {
                    _statusMessage.value = "لا توجد فواتير لتصديرها وفق التصفية الحالية"
                    return@launch
                }
                val file = withContext(Dispatchers.IO) {
                    XlsxExporter.generateInvoicesWorkbook(context, invoices)
                }
                _exportedFile.value = file
                _statusMessage.value = "تم إنشاء ملف Excel (XLSX) بنجاح"
                onReady(file)
            } catch (e: Exception) {
                _statusMessage.value = "خطأ أثناء تصدير Excel: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun isDateInCurrentMonth(dateStr: String): Boolean {
        return try {
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1

            val parts = dateStr.split("-", "/")
            if (parts.size >= 2) {
                val year = parts[0].toIntOrNull() ?: return false
                val month = parts[1].toIntOrNull() ?: return false
                year == currentYear && month == currentMonth
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun isDateInCurrentQuarter(dateStr: String): Boolean {
        return try {
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentQuarter = (currentMonth - 1) / 3 + 1

            val parts = dateStr.split("-", "/")
            if (parts.size >= 2) {
                val year = parts[0].toIntOrNull() ?: return false
                val month = parts[1].toIntOrNull() ?: return false
                val dateQuarter = (month - 1) / 3 + 1
                year == currentYear && dateQuarter == currentQuarter
            } else false
        } catch (_: Exception) {
            false
        }
    }
}

class InvoiceViewModelFactory(private val repository: InvoiceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
