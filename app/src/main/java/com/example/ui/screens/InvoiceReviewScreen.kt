package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceLineItem
import com.example.data.model.ReviewStatus
import com.example.ui.components.DuplicateWarningDialog
import com.example.ui.components.ZatcaOfficialDisclaimer
import com.example.ui.theme.SaudiGreenPrimary
import com.example.ui.theme.StatusNeedsReviewAmber
import com.example.ui.theme.StatusVerifiedGreen
import com.example.ui.viewmodel.InvoiceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceReviewScreen(
    viewModel: InvoiceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeReview()
        onBack()
    }

    val activePayload by viewModel.activeInvoiceToReview.collectAsStateWithLifecycle()
    val duplicateWarning by viewModel.duplicateWarning.collectAsStateWithLifecycle()
    val activeId by viewModel.activeInvoiceId.collectAsStateWithLifecycle()

    val payload = activePayload
    if (payload == null) {
        onBack()
        return
    }

    var showCustomerFields by remember {
        mutableStateOf(payload.customerName.isNotEmpty() || payload.customerVatNumber.isNotEmpty())
    }

    var showAddItemDialog by remember { mutableStateOf(false) }

    // Duplicate Warning Dialog
    duplicateWarning?.let { dup ->
        DuplicateWarningDialog(
            duplicateInvoice = dup,
            onDismiss = { viewModel.dismissDuplicateWarning() },
            onConfirmSaveAnyway = {
                viewModel.dismissDuplicateWarning()
                viewModel.saveActiveInvoice(forceSave = true)
                onBack()
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (activeId == null) "مراجعة وتأكيد الفاتورة" else "تعديل الفاتورة المسجلة",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeReview()
                            onBack()
                        },
                        modifier = Modifier.testTag("review_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SaudiGreenPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.closeReview()
                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_review_button")
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            viewModel.saveActiveInvoice(forceSave = false)
                            // If no duplicate caught, activeInvoiceToReview will become null and screen navigates back
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaudiGreenPrimary),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("save_invoice_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ الفاتورة في السجل")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Source & Automated Extraction Warning Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusNeedsReviewAmber.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusNeedsReviewAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مصدر الفاتورة: ${payload.sourceType.displayNameAr}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusNeedsReviewAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "البيانات أدناه مستخرجة آلياً. بصفتك المحاسب المسؤول، يُرجى تدقيق ومطابقة المبالغ والنسبة وتعديل أي حقل غير دقيق قبل الاعتماد والحفظ.",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Supplier Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "بيانات المورد (البائع)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = SaudiGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = payload.supplierName,
                            onValueChange = { newName ->
                                viewModel.updateActiveInvoice { it.copy(supplierName = newName) }
                            },
                            label = { Text("اسم المورد / المنشأة *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_supplier_name"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val vat = payload.supplierVatNumber
                        val isValidKsaVat = vat.length == 15 && vat.startsWith("3") && vat.endsWith("3")

                        OutlinedTextField(
                            value = vat,
                            onValueChange = { newVat ->
                                val filtered = newVat.filter { it.isDigit() }.take(15)
                                viewModel.updateActiveInvoice { it.copy(supplierVatNumber = filtered) }
                            },
                            label = { Text("الرقم الضريبي للمورد (15 رقماً) *") },
                            placeholder = { Text("3XXXXXXXXXXXX03") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = vat.isNotEmpty() && !isValidKsaVat,
                            supportingText = {
                                if (vat.isNotEmpty() && !isValidKsaVat) {
                                    Text("الرقم الضريبي السعودي يتكون من 15 خانة ويبدأ وينتهي بالرقم 3")
                                } else {
                                    Text("معرف الضريبة المسجل في ZATCA")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_supplier_vat"),
                            singleLine = true
                        )
                    }
                }
            }

            // Invoice Numbers & Dates Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "بيانات وتاريخ الفاتورة",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = SaudiGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = payload.invoiceNumber,
                                onValueChange = { newNum ->
                                    viewModel.updateActiveInvoice { it.copy(invoiceNumber = newNum) }
                                },
                                label = { Text("رقم الفاتورة *") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_invoice_number"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = payload.invoiceDate,
                                onValueChange = { newDate ->
                                    viewModel.updateActiveInvoice { it.copy(invoiceDate = newDate) }
                                },
                                label = { Text("تاريخ الفاتورة *") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_invoice_date"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = payload.issueTime,
                            onValueChange = { newTime ->
                                viewModel.updateActiveInvoice { it.copy(issueTime = newTime) }
                            },
                            label = { Text("وقت الإصدار (اختياري)") },
                            placeholder = { Text("HH:mm:ss") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_invoice_time"),
                            singleLine = true
                        )
                    }
                }
            }

            // Financial & Tax Amounts Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبالغ واحتساب الضريبة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = SaudiGreenPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = SaudiGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "الحساب الآلي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaudiGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = if (payload.taxableAmount > 0) payload.taxableAmount.toString() else "",
                            onValueChange = { text ->
                                val taxable = text.toDoubleOrNull() ?: 0.0
                                val vat = String.format(Locale.US, "%.2f", taxable * (payload.vatRate / 100.0)).toDoubleOrNull() ?: 0.0
                                val total = String.format(Locale.US, "%.2f", taxable + vat).toDoubleOrNull() ?: (taxable + vat)
                                viewModel.updateActiveInvoice {
                                    it.copy(
                                        taxableAmount = taxable,
                                        vatAmount = vat,
                                        totalAmount = total
                                    )
                                }
                            },
                            label = { Text("المبلغ الخاضع للضريبة (قبل الضريبة) *") },
                            suffix = { Text("ر.س") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_taxable_amount"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = payload.vatRate.toString(),
                                onValueChange = { rateStr ->
                                    val rate = rateStr.toDoubleOrNull() ?: 15.0
                                    val vat = String.format(Locale.US, "%.2f", payload.taxableAmount * (rate / 100.0)).toDoubleOrNull() ?: 0.0
                                    val total = String.format(Locale.US, "%.2f", payload.taxableAmount + vat).toDoubleOrNull() ?: (payload.taxableAmount + vat)
                                    viewModel.updateActiveInvoice {
                                        it.copy(
                                            vatRate = rate,
                                            vatAmount = vat,
                                            totalAmount = total
                                        )
                                    }
                                },
                                label = { Text("نسبة الضريبة %") },
                                suffix = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_vat_rate"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = if (payload.vatAmount > 0) payload.vatAmount.toString() else "",
                                onValueChange = { vatStr ->
                                    val vat = vatStr.toDoubleOrNull() ?: 0.0
                                    viewModel.updateActiveInvoice {
                                        it.copy(
                                            vatAmount = vat,
                                            totalAmount = it.taxableAmount + vat
                                        )
                                    }
                                },
                                label = { Text("مبلغ الضريبة 15%") },
                                suffix = { Text("ر.س") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("input_vat_amount"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = if (payload.totalAmount > 0) payload.totalAmount.toString() else "",
                            onValueChange = { totalStr ->
                                val total = totalStr.toDoubleOrNull() ?: 0.0
                                // Calculate backward from total
                                val vat = String.format(Locale.US, "%.2f", total * 0.15 / 1.15).toDoubleOrNull() ?: 0.0
                                val taxable = String.format(Locale.US, "%.2f", total - vat).toDoubleOrNull() ?: (total - vat)
                                viewModel.updateActiveInvoice {
                                    it.copy(
                                        totalAmount = total,
                                        taxableAmount = taxable,
                                        vatAmount = vat
                                    )
                                }
                            },
                            label = { Text("الإجمالي شامل الضريبة *") },
                            suffix = { Text("ر.س") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_total_amount"),
                            singleLine = true
                        )
                    }
                }
            }

            // Customer Details (Optional)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بيانات العميل (المشتري) - اختياري",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Switch(
                                checked = showCustomerFields,
                                onCheckedChange = { showCustomerFields = it }
                            )
                        }

                        AnimatedVisibility(visible = showCustomerFields) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = payload.customerName,
                                    onValueChange = { name ->
                                        viewModel.updateActiveInvoice { it.copy(customerName = name) }
                                    },
                                    label = { Text("اسم العميل") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_customer_name"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = payload.customerVatNumber,
                                    onValueChange = { cVat ->
                                        val filtered = cVat.filter { it.isDigit() }.take(15)
                                        viewModel.updateActiveInvoice { it.copy(customerVatNumber = filtered) }
                                    },
                                    label = { Text("الرقم الضريبي للعميل (15 رقماً)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_customer_vat"),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // Line Items Details Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "تفاصيل الأصناف والبنود (${payload.items.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "للإدراج في ورقة تفاصيل الأصناف بملف Excel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    val nextIndex = payload.items.size + 1
                                    val newItem = InvoiceLineItem(
                                        itemIndex = nextIndex,
                                        itemName = "صنف $nextIndex",
                                        quantity = 1.0,
                                        unitPrice = 0.0,
                                        vatRate = payload.vatRate,
                                        vatAmount = 0.0,
                                        totalAmount = 0.0
                                    )
                                    viewModel.updateActiveInvoice { it.copy(items = it.items + newItem) }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_item_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة صنف", fontSize = 12.sp)
                                }
                            }
                        }

                        if (payload.items.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد بنود مفصلة. سيتم إدراج بند إجمالي تلقائي للفاتورة في ملف Excel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // List of Items
            itemsIndexed(payload.items) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بند ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = {
                                    val updated = payload.items.toMutableList()
                                    updated.removeAt(index)
                                    viewModel.updateActiveInvoice { it.copy(items = updated) }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف البند",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = item.itemName,
                            onValueChange = { newDesc ->
                                val updated = payload.items.toMutableList()
                                updated[index] = item.copy(itemName = newDesc)
                                viewModel.updateActiveInvoice { it.copy(items = updated) }
                            },
                            label = { Text("بيان الصنف أو الخدمة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = item.quantity.toString(),
                                onValueChange = { qStr ->
                                    val q = qStr.toDoubleOrNull() ?: 1.0
                                    val subtotal = q * item.unitPrice
                                    val vat = subtotal * (item.vatRate / 100.0)
                                    val updated = payload.items.toMutableList()
                                    updated[index] = item.copy(quantity = q, vatAmount = vat, totalAmount = subtotal + vat)
                                    viewModel.updateActiveInvoice { it.copy(items = updated) }
                                },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.unitPrice.toString(),
                                onValueChange = { pStr ->
                                    val p = pStr.toDoubleOrNull() ?: 0.0
                                    val subtotal = item.quantity * p
                                    val vat = subtotal * (item.vatRate / 100.0)
                                    val updated = payload.items.toMutableList()
                                    updated[index] = item.copy(unitPrice = p, vatAmount = vat, totalAmount = subtotal + vat)
                                    viewModel.updateActiveInvoice { it.copy(items = updated) }
                                },
                                label = { Text("سعر الوحدة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = item.totalAmount.toString(),
                                onValueChange = { tStr ->
                                    val t = tStr.toDoubleOrNull() ?: 0.0
                                    val updated = payload.items.toMutableList()
                                    updated[index] = item.copy(totalAmount = t)
                                    viewModel.updateActiveInvoice { it.copy(items = updated) }
                                },
                                label = { Text("الإجمالي") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Accountant Notes & Review Verification Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ملاحظات المحاسب وتأكيد المراجعة",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = SaudiGreenPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = payload.notes,
                            onValueChange = { notes ->
                                viewModel.updateActiveInvoice { it.copy(notes = notes) }
                            },
                            label = { Text("ملاحظات محاسبية إضافية") },
                            placeholder = { Text("مثال: تم التأكد من الختم والضريبة للمورد...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_notes"),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val isReviewed = payload.reviewStatus == ReviewStatus.REVIEWED
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isReviewed) StatusVerifiedGreen.copy(alpha = 0.12f) else StatusNeedsReviewAmber.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isReviewed,
                                    onCheckedChange = { checked ->
                                        viewModel.updateActiveInvoice {
                                            it.copy(
                                                reviewStatus = if (checked) ReviewStatus.REVIEWED else ReviewStatus.NEEDS_REVIEW
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("checkbox_mark_reviewed")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "وضع علامة: تم التدقيق والمراجعة المحاسبية",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isReviewed) StatusVerifiedGreen else StatusNeedsReviewAmber
                                    )
                                    Text(
                                        text = if (isReviewed) "الفاتورة جاهزة للاعتماد في الإقرار وتصدير Excel" else "الفاتورة بحاجة لمراجعة محاسبية قبل الاعتماد",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Disclaimer Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ZatcaOfficialDisclaimer()
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
