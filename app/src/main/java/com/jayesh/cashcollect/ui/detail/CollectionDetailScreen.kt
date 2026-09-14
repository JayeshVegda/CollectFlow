package com.jayesh.cashcollect.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.ConfirmBottomSheet
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.AmberWarning
import com.jayesh.cashcollect.ui.theme.AmberWarningBg
import com.jayesh.cashcollect.ui.theme.ErrorRed
import com.jayesh.cashcollect.ui.theme.GreenPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collection: CollectionItem?,
    onReceiveAndWhatsApp: (CollectionItem) -> Unit,
    onOpenWhatsAppAgain: (CollectionItem) -> Unit,
    onConfirmSent: (Long) -> Unit,
    onVoidAndReplace: (originalId: Long, reason: String, newAmountPaise: Long) -> Unit,
    onBackClick: () -> Unit
) {
    if (collection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Collection not found")
        }
        return
    }

    var showConfirmSheet by remember { mutableStateOf(false) }
    var showVoidDialog by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("") }
    var newAmountRupees by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection #${collection.id}") },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STATUS BADGE
            StatusBadge(status = collection.status)

            // 1. CUSTOMER NAME (Large)
            Text(
                text = collection.customerDisplayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // 2. AMOUNT (Largest on screen)
            Text(
                text = Paise(collection.amountPaise).toFormattedRupees(),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary
            )

            // 3. COMMISSION TO PAY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Commission to Pay", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            text = "Rate snapshotted: ${collection.commissionRateSnapshot}/1000",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    Text(
                        text = Paise(collection.commissionPaise).toFormattedRupees(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // TIMELINE / AUDIT INFO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Audit Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    DetailRow(label = "Created:", value = dateFormat.format(Date(collection.createdAt)))

                    collection.receivedAt?.let {
                        DetailRow(label = "Cash Received:", value = dateFormat.format(Date(it)))
                    }

                    collection.whatsappOpenedAt?.let {
                        DetailRow(label = "WhatsApp Opened:", value = dateFormat.format(Date(it)))
                    }

                    collection.confirmedSentAt?.let {
                        DetailRow(label = "Confirmed Sent:", value = dateFormat.format(Date(it)))
                    }

                    if (collection.status == CollectionStatus.VOIDED) {
                        DetailRow(label = "Void Reason:", value = collection.voidReason.orEmpty(), isError = true)
                        collection.replacesId?.let { DetailRow(label = "Replaced Old Item:", value = "#$it") }
                        collection.replacedById?.let { DetailRow(label = "Replaced By New Item:", value = "#$it") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. STATUS-DEPENDENT PRIMARY ACTION
            when (collection.status) {
                CollectionStatus.PENDING -> {
                    Button(
                        onClick = { showConfirmSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Receive & WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                CollectionStatus.RECEIPT_CONFIRMED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AmberWarningBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⚠️ Confirmation Outstanding",
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text(
                                text = "Cash is already recorded on your phone. Did you tap Send in WhatsApp?",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenWhatsAppAgain(collection) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open WA Again", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onConfirmSent(collection.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yes, Sent", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                CollectionStatus.CONFIRMED -> {
                    Text(
                        text = "✓ This collection has been completely confirmed and archived.",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                CollectionStatus.VOIDED -> {
                    Text(
                        text = "✕ This collection is voided and cannot be modified.",
                        color = ErrorRed,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // 5. "CORRECT THIS ENTRY" ACTION (Void & Replace flow)
            if (collection.status != CollectionStatus.VOIDED) {
                OutlinedButton(
                    onClick = {
                        newAmountRupees = (collection.amountPaise / 100L).toString()
                        voidReason = ""
                        showVoidDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Correct This Entry (Void & Replace)")
                }
            }
        }
    }

    // Confirm Bottom Sheet
    if (showConfirmSheet) {
        ConfirmBottomSheet(
            collection = collection,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showConfirmSheet = false
                }
            },
            onConfirmReceiveAndWhatsApp = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showConfirmSheet = false
                    onReceiveAndWhatsApp(collection)
                }
            }
        )
    }

    // Dialog: Void & Replace
    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text("Correct Entry #${collection.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Original record will be preserved in history as VOIDED. A linked replacement record will be created.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for Correction *") },
                        placeholder = { Text("e.g. Wrong amount entered") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAmountRupees,
                        onValueChange = { newAmountRupees = it },
                        label = { Text("Correct Amount (₹) *") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = newAmountRupees.toLongOrNull() ?: 0L
                        if (voidReason.isNotBlank() && newAmount > 0L) {
                            showVoidDialog = false
                            onVoidAndReplace(collection.id, voidReason, newAmount * 100L)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Void & Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isError) ErrorRed else Color.Black
        )
    }
}
