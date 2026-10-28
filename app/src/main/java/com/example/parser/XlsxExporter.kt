package com.example.parser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.InvoiceWithItems
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {

    fun generateInvoicesWorkbook(context: Context, invoices: List<InvoiceWithItems>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val xlsxFile = File(exportDir, "Saudi_Invoices_$timestamp.xlsx")
        FileOutputStream(xlsxFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypes())
                writeZipEntry(zos, "_rels/.rels", buildRootRels())
                writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRels())
                writeZipEntry(zos, "xl/workbook.xml", buildWorkbook())
                writeZipEntry(zos, "xl/styles.xml", buildStyles())
                writeZipEntry(zos, "xl/worksheets/sheet1.xml", buildSheet1(invoices))
                writeZipEntry(zos, "xl/worksheets/sheet2.xml", buildSheet2(invoices))
            }
        }
        return xlsxFile
    }

    fun getShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تصدير فواتير ضريبية - قارئ الفاتورة الإلكترونية السعودية")
            putExtra(Intent.EXTRA_TEXT, "مرفق ملف Excel (XLSX) يحتوي على ملخص الفواتير الضريبية وتفاصيل الأصناف وفق متطلبات هيئة الزكاة والضريبة والجمارك (ZATCA).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(input: String?): String {
        if (input == null) return ""
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypes(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
    }

    private fun buildRootRels(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookRels(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildWorkbook(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="ملخص الفواتير" sheetId="1" r:id="rId1"/>
    <sheet name="تفاصيل الأصناف" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""
    }

    private fun buildStyles(): String {
        // Defines font, fills, borders, cell formats:
        // Style 0: Normal
        // Style 1: Header (Bold, white text, Saudi dark emerald #0D5C3A fill)
        // Style 2: Number with 2 decimals
        // Style 3: Bold total row
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
  </numFmts>
  <fonts count="3">
    <font><name val="Calibri"/><sz val="11"/><color theme="1"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/><color rgb="FFFFFFFF"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/><color rgb="FF000000"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0D5C3A"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEBF5EF"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D0D0"/></left>
      <right style="thin"><color rgb="FFD0D0D0"/></right>
      <top style="thin"><color rgb="FFD0D0D0"/></top>
      <bottom style="thin"><color rgb="FFD0D0D0"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="5">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="164" fontId="2" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
    }

    private fun buildSheet1(invoices: List<InvoiceWithItems>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1"/>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="10" customWidth="1"/>
    <col min="5" max="5" width="28" customWidth="1"/>
    <col min="6" max="6" width="20" customWidth="1"/>
    <col min="7" max="7" width="22" customWidth="1"/>
    <col min="8" max="8" width="18" customWidth="1"/>
    <col min="9" max="9" width="16" customWidth="1"/>
    <col min="10" max="10" width="12" customWidth="1"/>
    <col min="11" max="11" width="14" customWidth="1"/>
    <col min="12" max="12" width="18" customWidth="1"/>
    <col min="13" max="13" width="16" customWidth="1"/>
    <col min="14" max="14" width="14" customWidth="1"/>
    <col min="15" max="15" width="24" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header Row (Row 1)
        val headers = listOf(
            "م", "رقم الفاتورة", "تاريخ الفاتورة", "الوقت", "اسم المورد",
            "الرقم الضريبي للمورد", "اسم العميل", "الرقم الضريبي للعميل",
            "المبلغ قبل الضريبة", "نسبة الضريبة %", "مبلغ الضريبة", "الإجمالي شامل الضريبة",
            "حالة المراجعة", "طريقة الإدخال", "ملاحظات"
        )
        sb.append("    <row r=\"1\" customHeight=\"1\" ht=\"28\">\n")
        headers.forEachIndexed { idx, h ->
            val colLetter = getColumnLetter(idx + 1)
            sb.append("      <c r=\"$colLetter\"1\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        var totalTaxable = 0.0
        var totalVat = 0.0
        var totalGross = 0.0

        invoices.forEachIndexed { i, invWithItems ->
            val inv = invWithItems.invoice
            val rowNum = i + 2
            totalTaxable += inv.taxableAmount
            totalVat += inv.vatAmount
            totalGross += inv.totalAmount

            val reviewLabel = if (inv.reviewStatus == "REVIEWED") "معتمدة ومراجعة" else "قراءة آلية (تحتاج مراجعة)"
            val sourceLabel = when (inv.sourceType) {
                "QR_CAMERA" -> "تصوير كاميرا"
                "QR_IMAGE" -> "صورة معرض"
                "PDF_DOCUMENT" -> "مستند PDF"
                "XML_FILE" -> "ملف XML"
                else -> "إدخال يدوي"
            }

            sb.append("    <row r=\"$rowNum\">\n")
            sb.append("      <c r=\"A$rowNum\" s=\"0\"><v>${i + 1}</v></c>\n")
            sb.append("      <c r=\"B$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceNumber)}</t></is></c>\n")
            sb.append("      <c r=\"C$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceDate)}</t></is></c>\n")
            sb.append("      <c r=\"D$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.issueTime ?: "")}</t></is></c>\n")
            sb.append("      <c r=\"E$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierName)}</t></is></c>\n")
            sb.append("      <c r=\"F$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierVatNumber)}</t></is></c>\n")
            sb.append("      <c r=\"G$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.customerName ?: "")}</t></is></c>\n")
            sb.append("      <c r=\"H$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.customerVatNumber ?: "")}</t></is></c>\n")
            sb.append("      <c r=\"I$rowNum\" s=\"2\"><v>${inv.taxableAmount}</v></c>\n")
            sb.append("      <c r=\"J$rowNum\" s=\"2\"><v>${inv.vatRate}</v></c>\n")
            sb.append("      <c r=\"K$rowNum\" s=\"2\"><v>${inv.vatAmount}</v></c>\n")
            sb.append("      <c r=\"L$rowNum\" s=\"2\"><v>${inv.totalAmount}</v></c>\n")
            sb.append("      <c r=\"M$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(reviewLabel)}</t></is></c>\n")
            sb.append("      <c r=\"N$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(sourceLabel)}</t></is></c>\n")
            sb.append("      <c r=\"O$rowNum\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.notes ?: "")}</t></is></c>\n")
            sb.append("    </row>\n")
        }

        // Summary Total Row
        val totalRowNum = invoices.size + 2
        sb.append("    <row r=\"$totalRowNum\" customHeight=\"1\" ht=\"22\">\n")
        sb.append("      <c r=\"A$totalRowNum\" s=\"4\" t=\"inlineStr\"><is><t>الإجمالي</t></is></c>\n")
        sb.append("      <c r=\"B$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"C$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"D$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"E$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"F$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"G$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"H$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"I$totalRowNum\" s=\"3\"><v>${String.format(Locale.US, "%.2f", totalTaxable)}</v></c>\n")
        sb.append("      <c r=\"J$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"K$totalRowNum\" s=\"3\"><v>${String.format(Locale.US, "%.2f", totalVat)}</v></c>\n")
        sb.append("      <c r=\"L$totalRowNum\" s=\"3\"><v>${String.format(Locale.US, "%.2f", totalGross)}</v></c>\n")
        sb.append("      <c r=\"M$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"N$totalRowNum\" s=\"4\"/>\n")
        sb.append("      <c r=\"O$totalRowNum\" s=\"4\"/>\n")
        sb.append("    </row>\n")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun buildSheet2(invoices: List<InvoiceWithItems>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView rightToLeft="1" workbookViewId="0"/>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="28" customWidth="1"/>
    <col min="5" max="5" width="20" customWidth="1"/>
    <col min="6" max="6" width="10" customWidth="1"/>
    <col min="7" max="7" width="30" customWidth="1"/>
    <col min="8" max="8" width="12" customWidth="1"/>
    <col min="9" max="9" width="14" customWidth="1"/>
    <col min="10" max="10" width="12" customWidth="1"/>
    <col min="11" max="11" width="14" customWidth="1"/>
    <col min="12" max="12" width="18" customWidth="1"/>
  </cols>
  <sheetData>
""")

        // Header Row
        val headers = listOf(
            "م", "رقم الفاتورة", "تاريخ الفاتورة", "اسم المورد", "الرقم الضريبي للمورد",
            "بند رقم", "اسم الصنف / البيان", "الكمية", "سعر الوحدة",
            "نسبة الضريبة %", "مبلغ الضريبة", "الإجمالي شامل الضريبة"
        )
        sb.append("    <row r=\"1\" customHeight=\"1\" ht=\"28\">\n")
        headers.forEachIndexed { idx, h ->
            val colLetter = getColumnLetter(idx + 1)
            sb.append("      <c r=\"$colLetter\"1\" s=\"1\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        var rowIndex = 2
        var itemSeq = 1
        var totalItemsVat = 0.0
        var totalItemsGross = 0.0

        invoices.forEach { invWithItems ->
            val inv = invWithItems.invoice
            if (invWithItems.items.isNotEmpty()) {
                invWithItems.items.forEach { item ->
                    totalItemsVat += item.vatAmount
                    totalItemsGross += item.totalAmount

                    sb.append("    <row r=\"$rowIndex\">\n")
                    sb.append("      <c r=\"A$rowIndex\" s=\"0\"><v>$itemSeq</v></c>\n")
                    sb.append("      <c r=\"B$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceNumber)}</t></is></c>\n")
                    sb.append("      <c r=\"C$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceDate)}</t></is></c>\n")
                    sb.append("      <c r=\"D$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierName)}</t></is></c>\n")
                    sb.append("      <c r=\"E$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierVatNumber)}</t></is></c>\n")
                    sb.append("      <c r=\"F$rowIndex\" s=\"0\"><v>${item.itemIndex}</v></c>\n")
                    sb.append("      <c r=\"G$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(item.itemName)}</t></is></c>\n")
                    sb.append("      <c r=\"H$rowIndex\" s=\"2\"><v>${item.quantity}</v></c>\n")
                    sb.append("      <c r=\"I$rowIndex\" s=\"2\"><v>${item.unitPrice}</v></c>\n")
                    sb.append("      <c r=\"J$rowIndex\" s=\"2\"><v>${item.vatRate}</v></c>\n")
                    sb.append("      <c r=\"K$rowIndex\" s=\"2\"><v>${item.vatAmount}</v></c>\n")
                    sb.append("      <c r=\"L$rowIndex\" s=\"2\"><v>${item.totalAmount}</v></c>\n")
                    sb.append("    </row>\n")
                    rowIndex++
                    itemSeq++
                }
            } else {
                // If invoice doesn't have detailed items, output 1 summary line
                totalItemsVat += inv.vatAmount
                totalItemsGross += inv.totalAmount

                sb.append("    <row r=\"$rowIndex\">\n")
                sb.append("      <c r=\"A$rowIndex\" s=\"0\"><v>$itemSeq</v></c>\n")
                sb.append("      <c r=\"B$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceNumber)}</t></is></c>\n")
                sb.append("      <c r=\"C$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.invoiceDate)}</t></is></c>\n")
                sb.append("      <c r=\"D$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierName)}</t></is></c>\n")
                sb.append("      <c r=\"E$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>${escapeXml(inv.supplierVatNumber)}</t></is></c>\n")
                sb.append("      <c r=\"F$rowIndex\" s=\"0\"><v>1</v></c>\n")
                sb.append("      <c r=\"G$rowIndex\" s=\"0\" t=\"inlineStr\"><is><t>إجمالي الفاتورة (بدون تفصيل)</t></is></c>\n")
                sb.append("      <c r=\"H$rowIndex\" s=\"2\"><v>1.0</v></c>\n")
                sb.append("      <c r=\"I$rowIndex\" s=\"2\"><v>${inv.taxableAmount}</v></c>\n")
                sb.append("      <c r=\"J$rowIndex\" s=\"2\"><v>${inv.vatRate}</v></c>\n")
                sb.append("      <c r=\"K$rowIndex\" s=\"2\"><v>${inv.vatAmount}</v></c>\n")
                sb.append("      <c r=\"L$rowIndex\" s=\"2\"><v>${inv.totalAmount}</v></c>\n")
                sb.append("    </row>\n")
                rowIndex++
                itemSeq++
            }
        }

        // Summary Total Row
        sb.append("    <row r=\"$rowIndex\" customHeight=\"1\" ht=\"22\">\n")
        sb.append("      <c r=\"A$rowIndex\" s=\"4\" t=\"inlineStr\"><is><t>الإجمالي</t></is></c>\n")
        sb.append("      <c r=\"B$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"C$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"D$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"E$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"F$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"G$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"H$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"I$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"J$rowIndex\" s=\"4\"/>\n")
        sb.append("      <c r=\"K$rowIndex\" s=\"3\"><v>${String.format(Locale.US, "%.2f", totalItemsVat)}</v></c>\n")
        sb.append("      <c r=\"L$rowIndex\" s=\"3\"><v>${String.format(Locale.US, "%.2f", totalItemsGross)}</v></c>\n")
        sb.append("    </row>\n")

        sb.append("""  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        var letter = ""
        while (temp > 0) {
            val mod = (temp - 1) % 26
            letter = ('A'.code + mod).toChar() + letter
            temp = (temp - mod) / 26
        }
        return letter
    }
}
