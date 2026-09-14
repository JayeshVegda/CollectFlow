package com.jayesh.cashcollect.ui.detail

import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.ConfirmBottomSheet
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
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
    onVoidAndReplace: (originalId: Long, reason: String, newAmountPaise: Long, note: String?) -> Unit,
    onBackClick: () -> Unit
) {
    if (collection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Collection not found", color = NothingGray)
        }
        return
    }

    var showConfirmSheet by remember { mutableStateOf(false) }
    var showVoidDialog by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("") }
    var newAmountRupees by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf(collection.note ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "COLLECTION #${collection.id}",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
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
            StatusBadge(status = collection.status)

            // Customer Name
            Text(
                text = collection.customerDisplayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )

            // Amount
            Text(
                text = Paise(collection.amountPaise).toFormattedRupees(),
                fontFamily = FontFamily.Monospace,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )

            // Commission Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMMISSION TO PAY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NothingGray
                        )
                        Text(
                            text = "Rate snapshotted: ${collection.commissionRateSnapshot}/1000",
                            fontSize = 12.sp,
                            color = NothingGray
                        )
                    }
                    Text(
                        text = Paise(collection.commissionPaise).toFormattedRupees(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingGreen
                    )
                }
            }

            // Note card if present
            if (!collection.note.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = NothingCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "NOTE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NothingGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = collection.note, color = NothingWhite, fontSize = 14.sp)
                    }
                }
            }

            // Timeline / Audit
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AUDIT TRAIL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )

                    DetailRow(label = "Created", value = dateFormat.format(Date(collection.createdAt)))
                    collection.receivedAt?.let { DetailRow(label = "Cash Received", value = dateFormat.format(Date(it))) }
                    collection.whatsappOpenedAt?.let { DetailRow(label = "WhatsApp Opened", value = dateFormat.format(Date(it))) }
                    collection.confirmedSentAt?.let { DetailRow(label = "Confirmed Sent", value = dateFormat.format(Date(it))) }

                    if (collection.status == CollectionStatus.VOIDED) {
                        DetailRow(label = "Void Reason", value = collection.voidReason.orEmpty(), isError = true)
                        collection.replacesId?.let { DetailRow(label = "Replaced Old Item", value = "#$it") }
                        collection.replacedById?.let { DetailRow(label = "Replaced By New Item", value = "#$it") }
                    }
                }
            }

            // Contextual Status Actions
            when (collection.status) {
                CollectionStatus.PENDING -> {
                    Button(
                        onClick = { showConfirmSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "RECEIVE & OPEN WHATSAPP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                CollectionStatus.RECEIPT_CONFIRMED -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NothingAmberBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = NothingAmberBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "⚠️ CONFIRMATION OUTSTANDING",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NothingAmber,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Did you tap Send in WhatsApp?",
                                fontSize = 13.sp,
                                color = NothingGray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenWhatsAppAgain(collection) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NothingWhite)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OPEN WA AGAIN", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                                }

                                Button(
                                    onClick = { onConfirmSent(collection.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NothingGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("YES, SENT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                CollectionStatus.CONFIRMED -> {
                    Text(
                        text = "✓ This collection is confirmed and archived.",
                        fontFamily = FontFamily.Monospace,
                        color = NothingGreen,
                        fontSize = 13.sp
                    )
                }

                CollectionStatus.VOIDED -> {
                    Text(
                        text = "✕ This collection is voided.",
                        fontFamily = FontFamily.Monospace,
                        color = NothingRed,
                        fontSize = 13.sp
                    )
                }
            }

            // Void / Correct Button
            if (collection.status != CollectionStatus.VOIDED) {
                OutlinedButton(
                    onClick = {
                        newAmountRupees = (collection.amountPaise / 100L).toString()
                        voidReason = ""
                        showVoidDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = NothingGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CORRECT ENTRY (VOID & REPLACE)", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NothingGray)
                }
            }
        }
    }

    if (showConfirmSheet) {
        ConfirmBottomSheet(
            collection = collection,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showConfirmSheet = false }
            },
            onConfirmReceiveAndWhatsApp = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showConfirmSheet = false
                    onReceiveAndWhatsApp(collection)
                }
            }
        )
    }

    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text("CORRECT ENTRY #${collection.id}", fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Original will be voided. A linked replacement will be created.",
                        fontSize = 12.sp,
                        color = NothingGray
                    )
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for Correction *") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )
                    OutlinedTextField(
                        value = newAmountRupees,
                        onValueChange = { newAmountRupees = it },
                        label = { Text("Correct Amount (₹) *") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("Note (Optional)") },
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
                        val newAmt = newAmountRupees.toLongOrNull() ?: 0L
                        if (voidReason.isNotBlank() && newAmt > 0L) {
                            showVoidDialog = false
                            onVoidAndReplace(collection.id, voidReason, newAmt * 100L, newNote)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text("VOID & REPLACE", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text("CANCEL", color = NothingGray)
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
        Text(text = label, color = NothingGray, fontSize = 13.sp)
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isError) NothingRed else NothingWhite
        )
    }
}
