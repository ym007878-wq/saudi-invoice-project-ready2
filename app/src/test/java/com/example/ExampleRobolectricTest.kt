package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.InvoiceSourceType
import com.example.parser.ZatcaTlvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Saudi Invoice Scanner", appName)
    }

    @Test
    fun `test Zatca TLV encoding and parsing`() {
        // Construct valid ZATCA TLV Base64
        val bos = ByteArrayOutputStream()
        fun writeTag(tag: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            bos.write(tag)
            bos.write(bytes.size)
            bos.write(bytes)
        }

        writeTag(1, "شركة التقنية المتقدمة المحدودة")
        writeTag(2, "310123456789013")
        writeTag(3, "2024-05-15T10:30:00Z")
        writeTag(4, "115.00")
        writeTag(5, "15.00")

        val base64 = android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
        val result = ZatcaTlvParser.parse(base64, InvoiceSourceType.QR_CAMERA)

        assertNotNull(result)
        assertEquals("شركة التقنية المتقدمة المحدودة", result?.supplierName)
        assertEquals("310123456789013", result?.supplierVatNumber)
        assertEquals(115.00, result?.totalAmount ?: 0.0, 0.01)
        assertEquals(15.00, result?.vatAmount ?: 0.0, 0.01)
        assertEquals(100.00, result?.taxableAmount ?: 0.0, 0.01)
    }
}
