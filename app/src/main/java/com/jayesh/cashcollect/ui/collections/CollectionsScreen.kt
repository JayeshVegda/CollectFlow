package com.jayesh.cashcollect.ui.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppEmptyState
import com.jayesh.cashcollect.ui.common.AppSectionLabel
import com.jayesh.cashcollect.ui.common.AppSurface
import com.jayesh.cashcollect.ui.common.EditCollectionBottomSheet
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.SurfaceRaised
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary
import com.jayesh.cashcollect.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
/*
 * TODAY — plain, minimal, one list.
 *
 * Design decisions taken from direct operator feedback on the previous iteration:
 *
 *  - The swipe hint line was noise. Removed; the gesture is discoverable by using it.
 *  - The TO COLLECT / TO REPORT / DONE switcher was over-structuring. At three or four
 *    entries a day a single list is the simplest thing that can be correct, so the buckets
 *    are merged back into one list.
 *  - Time-of-day is not useful. What matters per entry is day, amount, name, commission.
 *  - The top board carries the summary numbers; the list carries entries.
 *
 * Interaction stays silent and unchanged: swipe right advances an entry (cash received,
 * then send on WhatsApp), swipe left cancels it, long-press selects for bulk actions.
 *
 * One 8dp dot per row carries status so no badge is needed:
 *   amber = cash received, not reported to the brother yet
 *   dim   = still to collect
 *   green = reported
 */

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
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val outstandingList by viewModel.outstandingList.collectAsStateWithLifecycle()
    val pendingList by viewModel.pendingList.collectAsStateWithLifecycle()
    val doneTodayList by viewModel.doneTodayList.collectAsStateWithLifecycle()
    val commissionRate by viewModel.commissionRate.collectAsStateWithLifecycle()

    CollectionsScreen(
        todayStats = todayStats,
        outstandingList = outstandingList,
        pendingList = pendingList,
        doneTodayList = doneTodayList,
        commissionRatePerThousand = commissionRate,
        initialOpenQuickCapture = initialOpenQuickCapture,
        onQuickCaptureDismissed = onQuickCaptureDismissed,
        onAddCollectionClick = onAddCollectionClick,
        onQuickCaptureSave = { name, amt, note -> viewModel.saveQuickCapture(context, name, amt, note) },
        onCollectionClick = onCollectionClick,
        onReceiveAndWhatsApp = { item -> viewModel.confirmReceive(context, item) },
        onOpenWhatsAppAgain = { item -> viewModel.openWhatsAppAgain(context, item) },
        onConfirmReported = { id -> viewModel.confirmSent(context, id) },
        onDeleteCollection = { id -> viewModel.deleteCollection(context, id) },
        onUpdateCollection = { id, amt, note -> viewModel.updateCollection(context, id, amt, note) },
        onVoidInstantly = { item -> viewModel.voidInstantly(context, item) },
        onBulkReceiveAndSend = { items -> viewModel.receiveAndSendAll(context, items) },
        onBulkMarkSent = { items -> viewModel.markAllSent(context, items) }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    todayStats: TodayStats = TodayStats(),
    outstandingList: List<CollectionItem>,
    pendingList: List<CollectionItem>,
    doneTodayList: List<CollectionItem>,
    commissionRatePerThousand: Int,
    initialOpenQuickCapture: Boolean = false,
    onQuickCaptureDismissed: () -> Unit = {},
    onAddCollectionClick: () -> Unit,
    onQuickCaptureSave: (customerName: String, amountPaise: Long, note: String?) -> Unit,
    onCollectionClick: (Long) -> Unit,
    onReceiveAndWhatsApp: (CollectionItem) -> Unit,
    onOpenWhatsAppAgain: (CollectionItem) -> Unit,
    onConfirmReported: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onUpdateCollection: (Long, Long, String?) -> Unit,
    onVoidInstantly: (CollectionItem) -> Unit = {},
    onBulkReceiveAndSend: (List<CollectionItem>) -> Unit = {},
    onBulkMarkSent: (List<CollectionItem>) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var isQuickCaptureOpen by remember { mutableStateOf(initialOpenQuickCapture) }
    val quickCaptureSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(initialOpenQuickCapture) {
        if (initialOpenQuickCapture) isQuickCaptureOpen = true
    }

    var itemToEdit by remember { mutableStateOf<CollectionItem?>(null) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // One merged list, most recently acted on first. No buckets and no switcher: with three
    // or four entries a day, a single list is the simplest thing that can be correct.
    val entries = remember(pendingList, outstandingList, doneTodayList) {
        (pendingList + outstandingList + doneTodayList)
            .sortedByDescending { it.receivedAt ?: it.createdAt }
    }

    // Drop selections that no longer exist (e.g. reported from another screen).
    LaunchedEffect(entries) {
        val stillPresent = entries.map { it.id }.toSet()
        selectedIds.removeAll { it !in stillPresent }
        if (selectedIds.isEmpty()) selectionMode = false
    }

    BackHandler(enabled = isQuickCaptureOpen) {
        isQuickCaptureOpen = false
        onQuickCaptureDismissed()
    }
    BackHandler(enabled = itemToEdit != null) { itemToEdit = null }
    BackHandler(enabled = selectionMode) {
        selectedIds.clear()
        selectionMode = false
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
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(Space.sm))
                        Text(
                            text = "COLLECTFLOW",
                            style = AppType.labelMonoLarge,
                            color = TextDisplay
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
            )
        },
        floatingActionButton = {
            Button(
                onClick = { isQuickCaptureOpen = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(Radius.pill),
                contentPadding = PaddingValues(horizontal = Space.md, vertical = Space.sm),
                modifier = Modifier
                    .height(Space.touchTarget)
                    .border(1.dp, SurfaceDivider, RoundedCornerShape(Radius.pill))
            ) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = NothingRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Space.xs))
                Text(text = "QUICK CAPTURE", style = AppType.labelMono, color = Color.Black)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Space.gutter),
            contentPadding = PaddingValues(top = Space.sm, bottom = 104.dp)
        ) {
            item(key = "board") {
                TodayBoard(
                    collectedPaise = todayStats.totalPaise,
                    commissionPaise = todayStats.commissionPaise,
                    entryCount = todayStats.count,
                    toCollectPaise = pendingList.sumOf { it.amountPaise },
                    toCollectCount = pendingList.size,
                    waitingToReport = outstandingList.size
                )
            }

            if (selectionMode) {
                item(key = "bulk") {
                    Spacer(modifier = Modifier.height(Space.md))
                    BulkActionBar(
                        selectedCount = selectedIds.size,
                        pendingCount = pendingList.count { it.id in selectedIds },
                        reportCount = outstandingList.count { it.id in selectedIds },
                        onReceive = {
                            onBulkReceiveAndSend(pendingList.filter { it.id in selectedIds })
                            selectedIds.clear()
                            selectionMode = false
                        },
                        onMarkReported = {
                            onBulkMarkSent(outstandingList.filter { it.id in selectedIds })
                            selectedIds.clear()
                            selectionMode = false
                        },
                        onClear = {
                            selectedIds.clear()
                            selectionMode = false
                        }
                    )
                }
            }

            if (entries.isEmpty()) {
                item(key = "empty") {
                    AppEmptyState(
                        title = "No entries yet.",
                        hint = "Tap QUICK CAPTURE to add one."
                    )
                }
            } else {
                items(entries, key = { it.id }) { item ->
                    CollectionRow(
                        item = item,
                        isSelected = item.id in selectedIds,
                        onLongClick = {
                            selectionMode = true
                            if (item.id !in selectedIds) selectedIds.add(item.id)
                        },
                        onTap = {
                            if (selectionMode) {
                                if (item.id in selectedIds) {
                                    selectedIds.remove(item.id)
                                } else {
                                    selectedIds.add(item.id)
                                }
                            } else {
                                onCollectionClick(item.id)
                            }
                        },
                        onReceive = { onReceiveAndWhatsApp(item) },
                        onOpenWhatsApp = { onOpenWhatsAppAgain(item) },
                        onMarkReported = { onConfirmReported(item.id) },
                        onVoid = { onVoidInstantly(item) }
                    )
                }
            }
        }
    }
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

    itemToEdit?.let { item ->
        EditCollectionBottomSheet(
            collection = item,
            sheetState = editSheetState,
            onDismiss = {
                scope.launch { editSheetState.hide() }.invokeOnCompletion { itemToEdit = null }
            },
            onSave = { amountPaise, note ->
                scope.launch { editSheetState.hide() }.invokeOnCompletion {
                    val id = item.id
                    itemToEdit = null
                    onUpdateCollection(id, amountPaise, note)
                }
            },
            onDelete = {
                scope.launch { editSheetState.hide() }.invokeOnCompletion {
                    val id = item.id
                    itemToEdit = null
                    onDeleteCollection(id)
                }
            }
        )
    }
}
/**
 * The top board: today's summary, and the only place numbers live.
 *
 * Per operator feedback this is where "the today thing" belongs - the hero number is money
 * actually collected today, with commission and entry count beside it. The pending total
 * sits below a hairline, and the single amber line calls out anything collected but not yet
 * reported to the brother, because that is the state that silently gets forgotten.
 */
@Composable
private fun TodayBoard(
    collectedPaise: Long,
    commissionPaise: Long,
    entryCount: Int,
    toCollectPaise: Long,
    toCollectCount: Int,
    waitingToReport: Int
) {
    AppSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            AppSectionLabel(text = "Today collected", color = TextSecondary)

            AmountText(
                amountPaise = collectedPaise,
                style = AppType.displayMoney,
                color = TextDisplay,
                align = TextAlign.Start
            )

            Text(
                text = "COMM " + Paise(commissionPaise).toFormattedRupees() +
                    "   ·   " + entryCount + (if (entryCount == 1) " ENTRY" else " ENTRIES"),
                style = AppType.labelMono,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(Space.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceDivider)
            )
            Spacer(modifier = Modifier.height(Space.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "TO COLLECT", style = AppType.labelMono, color = TextSecondary)
                    Text(
                        text = Paise(toCollectPaise).toFormattedRupees(),
                        style = AppType.amount,
                        color = TextDisplay
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "PARTIES", style = AppType.labelMono, color = TextSecondary)
                    Text(
                        text = toCollectCount.toString(),
                        style = AppType.amount,
                        color = TextDisplay
                    )
                }
            }

            if (waitingToReport > 0) {
                Spacer(modifier = Modifier.height(Space.xs))
                Text(
                    text = waitingToReport.toString() + " WAITING TO BE REPORTED",
                    style = AppType.labelMono,
                    color = NothingAmber
                )
            }
        }
    }
}
/**
 * One entry. Exactly four facts, per operator feedback:
 *
 *     name + amount        (primary line)
 *     day  + commission    (secondary line)
 *
 * Status is a single 8dp dot instead of a badge, so the row stays quiet:
 *   amber = cash received, not reported yet  ·  dim = to collect  ·  green = reported
 *
 * Swipe handling follows the Material 3 rule strictly - the action fires once from the
 * settled value and the box is reset, never inside `confirmValueChange` (which can fire
 * repeatedly while a gesture settles). The database stays the single source of truth.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CollectionRow(
    item: CollectionItem,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onTap: () -> Unit,
    onReceive: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onMarkReported: () -> Unit,
    onVoid: () -> Unit
) {
    val shape = RoundedCornerShape(Radius.card)
    val isPending = item.status == CollectionStatus.PENDING
    val isReported = item.status == CollectionStatus.CONFIRMED

    val dismissState = rememberSwipeToDismissBoxState()

    // Swipe right advances the entry: take the cash, then send it on WhatsApp.
    val advance: () -> Unit = if (isPending) onReceive else onOpenWhatsApp

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                advance()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onVoid()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeBackdrop(
                startLabel = if (isPending) "RECEIVED" else "WHATSAPP",
                endLabel = "CANCEL"
            )
        },
        modifier = Modifier.padding(bottom = Space.listAdjacent)
    ) {
        AppSurface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) NothingRed else SurfaceDivider,
                    shape = shape
                )
                .combinedClickable(onClick = onTap, onLongClick = onLongClick),
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.md, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(
                            when {
                                isPending -> SurfaceDivider
                                isReported -> NothingGreen
                                else -> NothingAmber
                            }
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customerDisplayName,
                        style = AppType.subheading,
                        color = if (isReported) TextSecondary else TextDisplay,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(Space.xxs))
                    Text(
                        text = dayLabel(item.receivedAt ?: item.createdAt) +
                            "   ·   comm " + Paise(item.commissionPaise).toFormattedRupees(),
                        style = AppType.caption,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    AmountText(
                        amountPaise = item.amountPaise,
                        style = AppType.amountLarge,
                        color = if (isReported) TextSecondary else TextDisplay
                    )
                    // The only extra control on a row: a small SENT to close the loop once
                    // WhatsApp has actually gone out. Shown only while awaiting reporting.
                    if (!isPending && !isReported) {
                        Spacer(modifier = Modifier.height(Space.xs))
                        OutlinedButton(
                            onClick = onMarkReported,
                            shape = RoundedCornerShape(Radius.pill),
                            border = BorderStroke(1.dp, NothingAmber),
                            contentPadding = PaddingValues(horizontal = Space.sm, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(text = "SENT", style = AppType.labelMono, color = NothingAmber)
                        }
                    }
                }
            }
        }
    }
}
/** Revealed behind a row while it is being swiped. Colour is on the label only. */
@Composable
private fun SwipeBackdrop(startLabel: String, endLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Radius.card))
            .background(SurfaceRaised),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = Space.md),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = startLabel, style = AppType.labelMono, color = NothingGreen)
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = Space.md),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(text = endLabel, style = AppType.labelMono, color = NothingRed)
        }
    }
}
/** Bulk bar shown while rows are multi-selected. */
@Composable
private fun BulkActionBar(
    selectedCount: Int,
    pendingCount: Int,
    reportCount: Int,
    onReceive: () -> Unit,
    onMarkReported: () -> Unit,
    onClear: () -> Unit
) {
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NothingRed, RoundedCornerShape(Radius.card)),
        shape = RoundedCornerShape(Radius.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCount.toString() + " SELECTED",
                    style = AppType.labelMonoLarge,
                    color = TextDisplay
                )
                OutlinedButton(
                    onClick = onClear,
                    shape = RoundedCornerShape(Radius.pill),
                    border = BorderStroke(1.dp, SurfaceDivider)
                ) {
                    Text(text = "CANCEL", style = AppType.labelMono, color = TextSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (pendingCount > 0) {
                    Button(
                        onClick = onReceive,
                        modifier = Modifier
                            .weight(1f)
                            .height(Space.touchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingWhite,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(Radius.pill)
                    ) {
                        Text(
                            text = "RECEIVED " + pendingCount,
                            style = AppType.labelMono,
                            color = Color.Black
                        )
                    }
                }
                if (reportCount > 0) {
                    Button(
                        onClick = onMarkReported,
                        modifier = Modifier
                            .weight(1f)
                            .height(Space.touchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(Radius.pill)
                    ) {
                        Text(
                            text = "REPORTED " + reportCount,
                            style = AppType.labelMono,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
/**
 * Day only. The operator explicitly does not care about the time of day - only the day,
 * amount, name and commission matter - and "Today" / "Yesterday" read faster than a date
 * for the two cases that come up constantly.
 */
private fun dayLabel(timestamp: Long): String {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val oneDayMs = 24L * 60L * 60L * 1000L

    return when {
        timestamp >= startOfToday -> "Today"
        timestamp >= startOfToday - oneDayMs -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
