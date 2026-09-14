package com.jayesh.cashcollect.ui.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.ConfirmBottomSheet
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
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

@Composable
fun CollectRoute(
    viewModel: CollectViewModel,
    initialOpenQuickCapture: Boolean,
    onQuickCaptureDismissed: () -> Unit,
    onAddCollectionClick: () -> Unit,
    onCollectionClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val outstandingList by viewModel.outstandingList.collectAsStateWithLifecycle()
    val pendingList by viewModel.pendingList.collectAsStateWithLifecycle()
    val commissionRate by viewModel.commissionRate.collectAsStateWithLifecycle()

    CollectionsScreen(
        outstandingList = outstandingList,
        pendingList = pendingList,
        commissionRatePerThousand = commissionRate,
        initialOpenQuickCapture = initialOpenQuickCapture,
        onQuickCaptureDismissed = onQuickCaptureDismissed,
        onAddCollectionClick = onAddCollectionClick,
        onQuickCaptureSave = { name, amt, note -> viewModel.saveQuickCapture(context, name, amt, note) },
        onCollectionClick = onCollectionClick,
        onReceiveAndWhatsApp = { item -> viewModel.confirmReceiveAndOpenWhatsApp(context, item) },
        onOpenWhatsAppAgain = { item -> viewModel.openWhatsAppAgain(context, item) },
        onConfirmSent = { id -> viewModel.confirmSent(context, id) },
        onSettingsClick = onSettingsClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    outstandingList: List<CollectionItem>,
    pendingList: List<CollectionItem>,
    commissionRatePerThousand: Int,
    initialOpenQuickCapture: Boolean = false,
    onQuickCaptureDismissed: () -> Unit = {},
    onAddCollectionClick: () -> Unit,
    onQuickCaptureSave: (customerName: String, amountPaise: Long, note: String?) -> Unit,
    onCollectionClick: (Long) -> Unit,
    onReceiveAndWhatsApp: (CollectionItem) -> Unit,
    onOpenWhatsAppAgain: (CollectionItem) -> Unit,
    onConfirmSent: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    var isQuickCaptureOpen by remember { mutableStateOf(initialOpenQuickCapture) }
    val quickCaptureSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialOpenQuickCapture) {
        if (initialOpenQuickCapture) {
            isQuickCaptureOpen = true
        }
    }

    var selectedItemForConfirm by remember { mutableStateOf<CollectionItem?>(null) }
    val confirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // BackHandler ensures pressing Back closes sheets cleanly without exiting or triggering navigation loops
    BackHandler(enabled = isQuickCaptureOpen) {
        isQuickCaptureOpen = false
        onQuickCaptureDismissed()
    }

    BackHandler(enabled = selectedItemForConfirm != null) {
        selectedItemForConfirm = null
    }

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COLLECTFLOW",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 17.sp,
                            color = NothingWhite
                        )
                    }
                },
                actions = {
                    // Quick Action pill in top bar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, NothingBorderVisible, RoundedCornerShape(999.dp))
                            .clickable { isQuickCaptureOpen = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ QUICK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = NothingWhite
                        )
                    }

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
            ExtendedFloatingActionButton(
                onClick = { isQuickCaptureOpen = true },
                containerColor = NothingWhite,
                contentColor = Color.Black,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(999.dp))
            ) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "QUICK CAPTURE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = Color.Black
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 2. OUTSTANDING CONFIRMATIONS (Warning Amber)
            if (outstandingList.isNotEmpty()) {
                item {
                    Text(
                        text = "OUTSTANDING CONFIRMATIONS (${outstandingList.size})",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
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
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = NothingGray
                )
            }

            if (pendingList.isEmpty() && outstandingList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending collections.\nTap \"+ QUICK CAPTURE\" to add.",
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

    // Quick Capture Popup BottomSheet
    if (isQuickCaptureOpen) {
        QuickCaptureBottomSheet(
            sheetState = quickCaptureSheetState,
            commissionRatePerThousand = commissionRatePerThousand,
            onDismiss = {
                isQuickCaptureOpen = false
                onQuickCaptureDismissed()
            },
            onConfirmSave = { name, amountPaise, note ->
                isQuickCaptureOpen = false
                onQuickCaptureDismissed()
                onQuickCaptureSave(name, amountPaise, note)
            },
            onOpenFullForm = {
                isQuickCaptureOpen = false
                onQuickCaptureDismissed()
                onAddCollectionClick()
            }
        )
    }

    // Confirmation Bottom Sheet before committing & launching WhatsApp
    selectedItemForConfirm?.let { item ->
        ConfirmBottomSheet(
            collection = item,
            sheetState = confirmSheetState,
            onDismiss = {
                scope.launch { confirmSheetState.hide() }.invokeOnCompletion {
                    selectedItemForConfirm = null
                }
            },
            onConfirmReceiveAndWhatsApp = {
                scope.launch { confirmSheetState.hide() }.invokeOnCompletion {
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
                    if (!item.note.isNullOrBlank()) {
                        Text(
                            text = "Note: ${item.note}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NothingMuted
                        )
                    }
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
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorderVisible)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REOPEN WA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                }

                Button(
                    onClick = onConfirmSent,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NothingGreen),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
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
            .border(1.dp, NothingBorderVisible, RoundedCornerShape(14.dp))
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
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = "Note: ${item.note}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingMuted
                    )
                }
            }

            Button(
                onClick = onReceiveClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "RECEIVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.Black
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
