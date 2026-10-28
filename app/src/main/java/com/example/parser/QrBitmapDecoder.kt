package com.example.parser

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

object QrBitmapDecoder {

    /**
     * Decodes a QR code from a given Bitmap.
     * Uses both HybridBinarizer and GlobalHistogramBinarizer for robust detection.
     */
    fun decodeQrCode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)

        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )

        val reader = MultiFormatReader()
        reader.setHints(hints)

        // Try HybridBinarizer first
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            if (!result.text.isNullOrEmpty()) {
                return result.text
            }
        } catch (_: Exception) {
            // Try fallback
        } finally {
            reader.reset()
        }

        // Try GlobalHistogramBinarizer fallback
        try {
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            if (!result.text.isNullOrEmpty()) {
                return result.text
            }
        } catch (_: Exception) {
            // Not found
        } finally {
            reader.reset()
        }

        // If high resolution, try downscaling or cropping center 80%
        if (width > 800 || height > 800) {
            try {
                val scaled = Bitmap.createScaledBitmap(bitmap, (width * 0.7).toInt(), (height * 0.7).toInt(), true)
                val scaledPixels = IntArray(scaled.width * scaled.height)
                scaled.getPixels(scaledPixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                val scaledSource = RGBLuminanceSource(scaled.width, scaled.height, scaledPixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(scaledSource))
                val result = reader.decodeWithState(binaryBitmap)
                if (!result.text.isNullOrEmpty()) {
                    return result.text
                }
            } catch (_: Exception) {
                // Ignore
            } finally {
                reader.reset()
            }
        }

        return null
    }
}
