package com.example.data.repository

import com.example.data.local.InvoiceDao
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val dao: InvoiceDao) {

    val allInvoicesWithItems: Flow<List<InvoiceWithItems>> = dao.getAllInvoicesWithItems()

    val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()

    fun getInvoiceById(id: Long): Flow<InvoiceWithItems?> = dao.getInvoiceWithItemsById(id)

    suspend fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
        val invoiceId = dao.insertInvoice(invoice)
        if (items.isNotEmpty()) {
            val itemsWithInvoiceId = items.mapIndexed { index, item ->
                item.copy(invoiceId = invoiceId, itemIndex = index + 1)
            }
            dao.insertItems(itemsWithInvoiceId)
        }
        return invoiceId
    }

    suspend fun updateInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        dao.updateInvoice(invoice)
        dao.deleteItemsByInvoiceId(invoice.id)
        if (items.isNotEmpty()) {
            val itemsWithInvoiceId = items.mapIndexed { index, item ->
                item.copy(invoiceId = invoice.id, itemIndex = index + 1)
            }
            dao.insertItems(itemsWithInvoiceId)
        }
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        dao.deleteInvoice(invoice)
    }

    suspend fun checkDuplicate(
        invoiceNumber: String,
        supplierVat: String,
        totalAmount: Double,
        invoiceDate: String
    ): List<InvoiceEntity> {
        return dao.checkDuplicate(
            invoiceNumber = invoiceNumber.trim(),
            supplierVat = supplierVat.trim(),
            totalAmount = totalAmount,
            invoiceDate = invoiceDate.trim()
        )
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
