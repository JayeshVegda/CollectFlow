package com.jayesh.cashcollect.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.AmountKeypad
import com.jayesh.cashcollect.ui.theme.AmberWarning
import com.jayesh.cashcollect.ui.theme.AmberWarningBg
import com.jayesh.cashcollect.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCollectionScreen(
    recentCustomers: List<Customer>,
    searchResults: List<Customer>,
    commissionRatePerThousand: Int,
    onSearchCustomer: (String) -> Unit,
    onAddNewCustomer: (name: String, alias: String?) -> Unit,
    onSaveCollection: (customerId: Long, amountPaise: Long) -> Unit,
    onCheckDuplicate: suspend (customerId: Long, amountPaise: Long) -> Boolean,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amountDigits by remember { mutableStateOf("") } // Digits in rupees
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerAlias by remember { mutableStateOf("") }

    var showDuplicateWarningDialog by remember { mutableStateOf(false) }
    var pendingSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Parse amount in rupees -> paise
    val amountRupees = amountDigits.toLongOrNull() ?: 0L
    val amountPaise = amountRupees * 100L
    val computedCommissionPaise = CommissionCalculator.calculate(amountPaise, commissionRatePerThousand)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Cash Collection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. CUSTOMER SELECTOR
            Text(
                text = "1. Customer",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            if (selectedCustomer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedCustomer!!.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(text = "Selected Customer", fontSize = 12.sp, color = GreenPrimary)
                        }
                        TextButton(onClick = { selectedCustomer = null }) {
                            Text("Change")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchCustomer(it)
                    },
                    label = { Text("Search customer by name or area...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        for (customer in searchResults.take(5)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = customer
                                        searchQuery = ""
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(customer.displayName, fontWeight = FontWeight.Medium)
                                Text("Select", color = GreenPrimary, fontSize = 13.sp)
                            }
                        }

                        // Inline Add New Customer option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    newCustomerName = searchQuery
                                    showNewCustomerDialog = true
                                }
                                .padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add \"$searchQuery\" as new customer",
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (recentCustomers.isNotEmpty()) {
                    Text(text = "Recent Customers:", fontSize = 12.sp, color = Color.Gray)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (customer in recentCustomers.take(6)) {
                            Card(
                                modifier = Modifier.clickable { selectedCustomer = customer },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = customer.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. AMOUNT & COMMISSION DISPLAY
            Text(
                text = "2. Amount & Commission",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Collection Amount", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = if (amountRupees > 0) Paise.fromRupees(amountRupees).toFormattedRupees() else "₹0",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (amountRupees > 0) GreenPrimary else Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Live Commission (${commissionRatePerThousand}/1000):",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = Paise(computedCommissionPaise).toFormattedRupees(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // 3. NUMERIC KEYPAD
            AmountKeypad(
                onDigitClick = { digit ->
                    if (amountDigits.length < 9) { // Max ₹99,99,99,999
                        amountDigits += digit
                    }
                },
                onBackspaceClick = {
                    if (amountDigits.isNotEmpty()) {
                        amountDigits = amountDigits.dropLast(1)
                    }
                },
                onClearClick = {
                    amountDigits = ""
                }
            )

            // 4. SAVE ACTION
            Button(
                onClick = {
                    val customer = selectedCustomer ?: return@Button
                    if (amountPaise <= 0L) return@Button

                    val doSave = {
                        onSaveCollection(customer.id, amountPaise)
                    }

                    // Soft duplicate warning check
                    coroutineScope.launch {
                        val isDup = onCheckDuplicate(customer.id, amountPaise)
                        if (isDup) {
                            pendingSaveAction = doSave
                            showDuplicateWarningDialog = true
                        } else {
                            doSave()
                        }
                    }
                },
                enabled = selectedCustomer != null && amountPaise > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Pending Collection", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal: Add New Customer
    if (showNewCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            title = { Text("Add Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Customer Name *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustomerAlias,
                        onValueChange = { newCustomerAlias = it },
                        label = { Text("Area / Shop Alias (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomerName.isNotBlank()) {
                            onAddNewCustomer(newCustomerName, newCustomerAlias)
                            showNewCustomerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Duplicate Entry Warning
    if (showDuplicateWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning) },
            title = { Text("Possible Duplicate Collection") },
            text = {
                Text("A collection for ${selectedCustomer?.displayName} of ${Paise(amountPaise).toFormattedRupees()} was already recorded within the last 30 minutes.\n\nDo you want to record it again?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateWarningDialog = false
                        pendingSaveAction?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                ) {
                    Text("Yes, Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
