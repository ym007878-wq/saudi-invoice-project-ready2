package com.example.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.model.InvoiceSourceType
import com.example.data.model.ParsedInvoicePayload
import java.io.File
import java.io.FileOutputStream

data class PdfParseResult(
    val payload: ParsedInvoicePayload?,
    val previewBitmap: Bitmap?,
    val rawQr: String?
)

object PdfInvoiceHelper {

    fun processPdf(context: Context, pdfUri: Uri): PdfParseResult {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        try {
            // Copy uri stream to a temp file because PdfRenderer requires a seekable FileDescriptor
            tempFile = File.createTempFile("invoice_pdf_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)

            if (renderer.pageCount == 0) {
                return PdfParseResult(null, null, null)
            }

            // Render first page at good resolution (e.g. 1500 x 2100)
            val pageCountToScan = minOf(renderer.pageCount, 3)
            var foundQr: String? = null
            var firstPageBitmap: Bitmap? = null

            for (pageIndex in 0 until pageCountToScan) {
                val page = renderer.openPage(pageIndex)
                val width = page.width * 2
                val height = page.height * 2

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                if (pageIndex == 0) {
                    firstPageBitmap = bitmap
                }

                val qrText = QrBitmapDecoder.decodeQrCode(bitmap)
                if (!qrText.isNullOrEmpty()) {
                    foundQr = qrText
                    break
                }
            }

            if (!foundQr.isNullOrEmpty()) {
                val payload = ZatcaTlvParser.parse(foundQr, InvoiceSourceType.PDF_DOCUMENT)
                return PdfParseResult(payload, firstPageBitmap, foundQr)
            }

            return PdfParseResult(null, firstPageBitmap, null)
        } catch (_: Exception) {
            return PdfParseResult(null, null, null)
        } finally {
            try {
                renderer?.close()
                fileDescriptor?.close()
                tempFile?.delete()
            } catch (_: Exception) {
            }
        }
    }
}
