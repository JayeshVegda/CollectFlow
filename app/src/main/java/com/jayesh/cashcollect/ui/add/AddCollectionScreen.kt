package com.jayesh.cashcollect.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.AmountKeypad
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCollectionScreen(
    recentCustomers: List<Customer>,
    searchResults: List<Customer>,
    commissionRatePerThousand: Int,
    onSearchCustomer: (String) -> Unit,
    onAddNewCustomer: (name: String, alias: String?) -> Unit,
    onSaveCollection: (customerId: Long, amountPaise: Long, note: String?) -> Unit,
    onCheckDuplicate: suspend (customerId: Long, amountPaise: Long) -> Boolean,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amountDigits by remember { mutableStateOf("") } // Digits in rupees
    var noteText by remember { mutableStateOf("") }

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
                title = {
                    Text(
                        text = "NEW COLLECTION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NothingWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NothingBlack
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
                text = "1. PARTY / CUSTOMER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = NothingGray
            )

            if (selectedCustomer != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NothingRed, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = NothingCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedCustomer!!.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = NothingWhite
                            )
                            Text(
                                text = "SELECTED PARTY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NothingRed
                            )
                        }
                        TextButton(onClick = { selectedCustomer = null }) {
                            Text("CHANGE", fontFamily = FontFamily.Monospace, color = NothingGray)
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
                    placeholder = { Text("Search party by name or area...", color = NothingMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite
                    )
                )

                if (searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NothingCard, RoundedCornerShape(12.dp))
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
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
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(customer.displayName, fontWeight = FontWeight.Medium, color = NothingWhite)
                                Text("SELECT", fontFamily = FontFamily.Monospace, color = NothingRed, fontSize = 12.sp)
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
                                .padding(top = 8.dp, bottom = 4.dp, start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NothingRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add \"$searchQuery\" as new party",
                                color = NothingRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (recentCustomers.isNotEmpty()) {
                    Text(
                        text = "RECENT PARTIES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingGray
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (customer in recentCustomers.take(6)) {
                            Card(
                                modifier = Modifier
                                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedCustomer = customer },
                                colors = CardDefaults.cardColors(containerColor = NothingCardRaised),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = customer.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NothingWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. AMOUNT & COMMISSION DISPLAY
            Text(
                text = "2. AMOUNT & COMMISSION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = NothingGray
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CASH AMOUNT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NothingGray
                    )

                    Text(
                        text = if (amountRupees > 0) Paise.fromRupees(amountRupees).toFormattedRupees() else "₹0",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (amountRupees > 0) NothingWhite else NothingMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Commission (${commissionRatePerThousand}/1000):",
                            fontSize = 13.sp,
                            color = NothingGray
                        )
                        Text(
                            text = Paise(computedCommissionPaise).toFormattedRupees(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingGreen
                        )
                    }
                }
            }

            // Quick Shorthand Preset Chips (1-tap amounts!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "50k" to 50000L,
                    "1L" to 100000L,
                    "2L" to 200000L,
                    "4L" to 400000L,
                    "5L" to 500000L,
                    "10L" to 1000000L
                )
                for ((label, valRupees) in presets) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingCardRaised)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable { amountDigits = valRupees.toString() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NothingWhite
                        )
                    }
                }
            }

            // 3. OPTIONAL NOTE FIELD
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Optional note (e.g. 500 bundles, counter slip)...", color = NothingMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    unfocusedBorderColor = NothingBorder,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                )
            )

            // 4. NUMERIC KEYPAD
            AmountKeypad(
                onDigitClick = { digit ->
                    if (amountDigits.length < 9) {
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

            // 5. SAVE ACTION BUTTON
            Button(
                onClick = {
                    val customer = selectedCustomer ?: return@Button
                    if (amountPaise <= 0L) return@Button

                    val doSave = {
                        onSaveCollection(customer.id, amountPaise, noteText)
                    }

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
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SAVE PENDING COLLECTION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }
    }

    // Modal: Add New Customer
    if (showNewCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            title = { Text("NEW PARTY", fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = { newCustomerName = it },
                        label = { Text("Party Name *") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )
                    OutlinedTextField(
                        value = newCustomerAlias,
                        onValueChange = { newCustomerAlias = it },
                        label = { Text("Area / Shop Alias") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
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
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text("SAVE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomerDialog = false }) {
                    Text("CANCEL", color = NothingGray)
                }
            }
        )
    }

    // Modal: Duplicate Entry Warning
    if (showDuplicateWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = NothingAmber) },
            title = { Text("DUPLICATE WARNING", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "A collection for ${selectedCustomer?.displayName} of ${Paise(amountPaise).toFormattedRupees()} was already recorded within the last 30 minutes.\n\nSave anyway?",
                    color = NothingGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateWarningDialog = false
                        pendingSaveAction?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingAmber)
                ) {
                    Text("SAVE ANYWAY", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarningDialog = false }) {
                    Text("CANCEL", color = NothingGray)
                }
            }
        )
    }
}
