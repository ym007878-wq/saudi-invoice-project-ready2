package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoiceNumber", "supplierVatNumber"]),
        Index(value = ["invoiceDate"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val invoiceDate: String,
    val issueTime: String? = null,
    val supplierName: String,
    val supplierVatNumber: String,
    val customerName: String? = null,
    val customerVatNumber: String? = null,
    val taxableAmount: Double,
    val vatRate: Double = 15.0,
    val vatAmount: Double,
    val totalAmount: Double,
    val sourceType: String, // from InvoiceSourceType.name
    val reviewStatus: String, // from ReviewStatus.name
    val notes: String? = null,
    val rawQrData: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
