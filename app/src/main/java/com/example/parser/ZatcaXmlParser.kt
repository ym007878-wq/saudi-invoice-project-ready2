package com.example.parser

import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceSourceType
import com.example.data.model.ParsedInvoicePayload
import com.example.data.model.ReviewStatus
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

object ZatcaXmlParser {

    /**
     * Parses a standard Saudi ZATCA e-Invoice XML (UBL 2.1 format).
     */
    fun parse(inputStream: InputStream): ParsedInvoicePayload? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)
            document.documentElement.normalize()

            val rootName = document.documentElement.localName ?: document.documentElement.tagName
            if (!rootName.contains("Invoice", ignoreCase = true)) {
                return null
            }

            fun getFirstText(tagName: String): String {
                val list = document.getElementsByTagNameNS("*", tagName)
                if (list.length > 0) return list.item(0).textContent.trim()
                val fallbackList = document.getElementsByTagName(tagName)
                if (fallbackList.length > 0) return fallbackList.item(0).textContent.trim()
                return ""
            }

            // Header details
            val invoiceNumber = getFirstText("ID")
            val issueDate = getFirstText("IssueDate")
            val issueTime = getFirstText("IssueTime")

            // Supplier
            var supplierName = ""
            var supplierVat = ""
            val supplierList = document.getElementsByTagNameNS("*", "AccountingSupplierParty")
            if (supplierList.length > 0) {
                val supplierElem = supplierList.item(0) as Element
                supplierName = findChildText(supplierElem, "RegistrationName")
                    .ifEmpty { findChildText(supplierElem, "Name") }
                supplierVat = findChildText(supplierElem, "CompanyID")
            }

            // Customer
            var customerName = ""
            var customerVat = ""
            val customerList = document.getElementsByTagNameNS("*", "AccountingCustomerParty")
            if (customerList.length > 0) {
                val customerElem = customerList.item(0) as Element
                customerName = findChildText(customerElem, "RegistrationName")
                    .ifEmpty { findChildText(customerElem, "Name") }
                customerVat = findChildText(customerElem, "CompanyID")
            }

            // Monetary Totals
            var taxableAmount = 0.0
            var totalAmount = 0.0
            val monetaryList = document.getElementsByTagNameNS("*", "LegalMonetaryTotal")
            if (monetaryList.length > 0) {
                val monetaryElem = monetaryList.item(0) as Element
                val taxExcl = findChildText(monetaryElem, "TaxExclusiveAmount").toDoubleOrNull()
                val taxIncl = findChildText(monetaryElem, "TaxInclusiveAmount").toDoubleOrNull()
                val payable = findChildText(monetaryElem, "PayableAmount").toDoubleOrNull()

                totalAmount = taxIncl ?: payable ?: 0.0
                taxableAmount = taxExcl ?: 0.0
            }

            // Tax Total
            var vatAmount = 0.0
            var vatRate = 15.0
            val taxTotalList = document.getElementsByTagNameNS("*", "TaxTotal")
            if (taxTotalList.length > 0) {
                val taxTotalElem = taxTotalList.item(0) as Element
                vatAmount = findChildText(taxTotalElem, "TaxAmount").toDoubleOrNull() ?: 0.0
                val percentStr = findChildText(taxTotalElem, "Percent")
                if (percentStr.isNotEmpty()) {
                    vatRate = percentStr.toDoubleOrNull() ?: 15.0
                }
            }

            if (totalAmount <= 0.0 && taxableAmount > 0.0) {
                totalAmount = taxableAmount + vatAmount
            }
            if (taxableAmount <= 0.0 && totalAmount > 0.0) {
                taxableAmount = totalAmount - vatAmount
            }

            // Parse Line Items
            val items = mutableListOf<InvoiceLineItem>()
            val lineList: NodeList = document.getElementsByTagNameNS("*", "InvoiceLine")
            val count = lineList.length
            for (i in 0 until count) {
                val lineElem = lineList.item(i) as Element
                val lineId = findChildText(lineElem, "ID").toIntOrNull() ?: (i + 1)
                val qty = findChildText(lineElem, "InvoicedQuantity").toDoubleOrNull() ?: 1.0
                val lineExt = findChildText(lineElem, "LineExtensionAmount").toDoubleOrNull() ?: 0.0
                val itemName = findChildText(lineElem, "Name")
                val priceAmount = findChildText(lineElem, "PriceAmount").toDoubleOrNull() ?: (lineExt / (if (qty > 0) qty else 1.0))
                val itemVat = findChildText(lineElem, "TaxAmount").toDoubleOrNull() ?: (lineExt * (vatRate / 100.0))
                val lineTotal = lineExt + itemVat

                items.add(
                    InvoiceLineItem(
                        itemIndex = lineId,
                        itemName = itemName.ifEmpty { "صنف رقم $lineId" },
                        quantity = qty,
                        unitPrice = String.format(Locale.US, "%.2f", priceAmount).toDoubleOrNull() ?: priceAmount,
                        vatRate = vatRate,
                        vatAmount = String.format(Locale.US, "%.2f", itemVat).toDoubleOrNull() ?: itemVat,
                        totalAmount = String.format(Locale.US, "%.2f", lineTotal).toDoubleOrNull() ?: lineTotal
                    )
                )
            }

            ParsedInvoicePayload(
                invoiceNumber = invoiceNumber.ifEmpty { "XML-${System.currentTimeMillis().toString().takeLast(6)}" },
                invoiceDate = issueDate,
                issueTime = issueTime,
                supplierName = supplierName.ifEmpty { "مورد (ملف XML)" },
                supplierVatNumber = supplierVat,
                customerName = customerName,
                customerVatNumber = customerVat,
                taxableAmount = taxableAmount,
                vatRate = vatRate,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                sourceType = InvoiceSourceType.XML_FILE,
                reviewStatus = ReviewStatus.NEEDS_REVIEW,
                notes = "تم استيراد الفاتورة وقراءة الأصناف من ملف ZATCA XML UBL 2.1",
                items = items
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findChildText(element: Element, childTag: String): String {
        val list = element.getElementsByTagNameNS("*", childTag)
        if (list.length > 0) return list.item(0).textContent.trim()
        val list2 = element.getElementsByTagName(childTag)
        if (list2.length > 0) return list2.item(0).textContent.trim()
        return ""
    }
}
