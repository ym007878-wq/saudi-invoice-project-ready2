package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceWithItemsById(id: Long): Flow<InvoiceWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsByInvoiceId(invoiceId: Long)

    @Query("""
        SELECT * FROM invoices 
        WHERE (invoiceNumber = :invoiceNumber AND supplierVatNumber = :supplierVat)
           OR (supplierVatNumber = :supplierVat AND totalAmount = :totalAmount AND invoiceDate = :invoiceDate)
        LIMIT 5
    """)
    suspend fun checkDuplicate(
        invoiceNumber: String,
        supplierVat: String,
        totalAmount: Double,
        invoiceDate: String
    ): List<InvoiceEntity>

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}
