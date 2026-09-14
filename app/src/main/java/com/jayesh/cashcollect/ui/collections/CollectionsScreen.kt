package com.jayesh.cashcollect.ui.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.money.SmartInputParser
import com.jayesh.cashcollect.ui.common.ConfirmBottomSheet
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    outstandingList: List<CollectionItem>,
    pendingList: List<CollectionItem>,
    commissionRatePerThousand: Int,
    onAddCollectionClick: () -> Unit,
    onQuickCaptureSave: (customerName: String, amountPaise: Long) -> Unit,
    onCollectionClick: (Long) -> Unit,
    onReceiveAndWhatsApp: (CollectionItem) -> Unit,
    onOpenWhatsAppAgain: (CollectionItem) -> Unit,
    onConfirmSent: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    var quickInputText by remember { mutableStateOf("") }
    val parsedResult = remember(quickInputText) { SmartInputParser.parse(quickInputText) }

    var selectedItemForConfirm by remember { mutableStateOf<CollectionItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "COLLECTFLOW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 18.sp,
                        color = NothingWhite
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NothingGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NothingBlack
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCollectionClick,
                containerColor = NothingRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Collection")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SMART QUICK-CAPTURE BAR
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (parsedResult != null) NothingRed else NothingBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = NothingCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (parsedResult != null) NothingRed else NothingGray,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "SMART QUICK CAPTURE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingGray,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = quickInputText,
                            onValueChange = { quickInputText = it },
                            placeholder = {
                                Text(
                                    "e.g. \"sambhu 400\" or \"mahesh 1.5L\"",
                                    fontSize = 13.sp,
                                    color = NothingMuted
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = NothingWhite,
                                unfocusedTextColor = NothingWhite
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                parsedResult?.let { res ->
                                    if (res.customerName.isNotBlank() && res.amountPaise > 0L) {
                                        onQuickCaptureSave(res.customerName, res.amountPaise)
                                        quickInputText = ""
                                    }
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NothingBlack, RoundedCornerShape(10.dp))
                                .padding(horizontal = 4.dp)
                        )

                        // Live Parsed Tokens Preview
                        AnimatedVisibility(visible = parsedResult != null) {
                            parsedResult?.let { res ->
                                val commission = CommissionCalculator.calculate(res.amountPaise, commissionRatePerThousand)
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (res.customerName.isNotBlank()) res.customerName else "Enter party name...",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = NothingWhite
                                            )
                                            Text(
                                                text = "${res.formattedRupees} • Comm: ${Paise(commission).toFormattedRupees()}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = NothingGreen
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (res.customerName.isNotBlank() && res.amountPaise > 0L) {
                                                    onQuickCaptureSave(res.customerName, res.amountPaise)
                                                    quickInputText = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "ADD",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. OUTSTANDING CONFIRMATIONS (Warning Amber)
            if (outstandingList.isNotEmpty()) {
                item {
                    Text(
                        text = "OUTSTANDING CONFIRMATIONS (${outstandingList.size})",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = NothingAmber
                    )
                }

                items(outstandingList, key = { it.id }) { item ->
                    OutstandingRow(
                        item = item,
                        onClick = { onCollectionClick(item.id) },
                        onOpenWhatsAppAgain = { onOpenWhatsAppAgain(item) },
                        onConfirmSent = { onConfirmSent(item.id) }
                    )
                }
            }

            // 3. PENDING COLLECTIONS
            item {
                Text(
                    text = "PENDING COLLECTIONS (${pendingList.size})",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = NothingGray
                )
            }

            if (pendingList.isEmpty() && outstandingList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending collections.\nUse Quick Capture above or tap + to add.",
                            fontFamily = FontFamily.Monospace,
                            color = NothingMuted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(pendingList, key = { it.id }) { item ->
                    PendingRow(
                        item = item,
                        onClick = { onCollectionClick(item.id) },
                        onReceiveClick = { selectedItemForConfirm = item }
                    )
                }
            }
        }
    }

    // Confirmation Bottom Sheet before committing & launching WhatsApp
    selectedItemForConfirm?.let { item ->
        ConfirmBottomSheet(
            collection = item,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedItemForConfirm = null
                }
            },
            onConfirmReceiveAndWhatsApp = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    val target = selectedItemForConfirm
                    selectedItemForConfirm = null
                    target?.let { onReceiveAndWhatsApp(it) }
                }
            }
        )
    }
}

@Composable
private fun OutstandingRow(
    item: CollectionItem,
    onClick: () -> Unit,
    onOpenWhatsAppAgain: () -> Unit,
    onConfirmSent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NothingAmberBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NothingAmberBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ CASH RECEIVED (UNSENT)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingAmber
                )
                val timeAgo = formatTimeAgo(item.receivedAt ?: item.createdAt)
                Text(
                    text = timeAgo,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.customerDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NothingWhite
                    )
                    Text(
                        text = "Comm: ${Paise(item.commissionPaise).toFormattedRupees()}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingGray
                    )
                }
                Text(
                    text = Paise(item.amountPaise).toFormattedRupees(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = NothingAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWhatsAppAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NothingWhite)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REOPEN WA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                }

                Button(
                    onClick = onConfirmSent,
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

@Composable
private fun PendingRow(
    item: CollectionItem,
    onClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.customerDisplayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NothingWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Paise(item.amountPaise).toFormattedRupees(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = NothingWhite
                )
                Text(
                    text = "Comm: ${Paise(item.commissionPaise).toFormattedRupees()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NothingGray
                )
            }

            Button(
                onClick = onReceiveClick,
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RECEIVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (60 * 1000)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
