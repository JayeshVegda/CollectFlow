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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppEmptyState
import com.jayesh.cashcollect.ui.common.AppSectionLabel
import com.jayesh.cashcollect.ui.common.AppSurface
import com.jayesh.cashcollect.ui.common.EditCollectionBottomSheet
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.Motion
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceCard
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
 * COLLECT — the daily work queue.
 *
 * Why the page is shaped this way:
 *
 *  - The queue is the page. Rows are grouped by the three states the operator moves through
 *    (TO COLLECT -> TO REPORT -> REPORTED TODAY), so state is communicated by POSITION. That
 *    is what removed the per-row status dot, badge and repeated labels the earlier version
 *    carried: they were compensating for an ungrouped list.
 *  - The summary is two numbers — what is still out, and what came in today. Party count and
 *    commission ride inline as small suffixes; every other aggregate was dashboard furniture
 *    on a work queue, and it pushed the queue itself off the first screen.
 *  - A row states three facts: party, amount, commission. Metadata the operator does not act
 *    on is gone, except the day on entries that are not from today, which keeps a stale open
 *    item from being mistaken for today's work.
 *
 * Interaction is unchanged: swipe right advances an entry (cash received, then WhatsApp),
 * swipe left cancels it, long-press selects for bulk actions, QUICK CAPTURE stays primary.
 */

@Composable
fun CollectRoute(
    viewModel: CollectViewModel,
    initialOpenQuickCapture: Boolean,
    quickCapturePrefill: String,
    onQuickCaptureDismissed: () -> Unit,
    onAddCollectionClick: () -> Unit,
    onCollectionClick: (Long) -> Unit,
    onPartyClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val outstandingList by viewModel.outstandingList.collectAsStateWithLifecycle()
    val pendingList by viewModel.pendingList.collectAsStateWithLifecycle()
    val doneTodayList by viewModel.doneTodayList.collectAsStateWithLifecycle()
    val commissionRate by viewModel.commissionRate.collectAsStateWithLifecycle()
    val recentCustomers by viewModel.recentCustomers.collectAsStateWithLifecycle()

    CollectionsScreen(
        todayStats = todayStats,
        outstandingList = outstandingList,
        pendingList = pendingList,
        doneTodayList = doneTodayList,
        commissionRatePerThousand = commissionRate,
        recentCustomers = recentCustomers,
        initialOpenQuickCapture = initialOpenQuickCapture,
        quickCapturePrefill = quickCapturePrefill,
        onQuickCaptureDismissed = onQuickCaptureDismissed,
        onAddCollectionClick = onAddCollectionClick,
        onQuickCaptureSave = { name, amt, note -> viewModel.saveQuickCapture(context, name, amt, note) },
        onCollectionClick = onCollectionClick,
        onPartyClick = onPartyClick,
        onReceiveAndWhatsApp = { item -> viewModel.confirmReceive(context, item) },
        onOpenWhatsAppAgain = { item -> viewModel.openWhatsAppAgain(context, item) },
        onConfirmReported = { id -> viewModel.confirmSent(context, id) },
        onDeleteCollection = { id -> viewModel.deleteCollection(context, id) },
        onUpdateCollection = { id, name, amt, date, note ->
            viewModel.updateCollection(context, id, name, amt, date, note)
        },
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
    recentCustomers: List<Customer> = emptyList(),
    quickCapturePrefill: String = "",
    initialOpenQuickCapture: Boolean = false,
    onQuickCaptureDismissed: () -> Unit = {},
    onAddCollectionClick: () -> Unit,
    onQuickCaptureSave: (customerName: String, amountPaise: Long, note: String?) -> Unit,
    onCollectionClick: (Long) -> Unit,
    onPartyClick: (Long) -> Unit = {},
    onReceiveAndWhatsApp: (CollectionItem) -> Unit,
    onOpenWhatsAppAgain: (CollectionItem) -> Unit,
    onConfirmReported: (Long) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onUpdateCollection: (Long, String, Long, Long, String?) -> Unit,
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

    // Every live entry in one place: the three groups partition it, so selection cleanup and
    // the empty check stay single-sourced.
    val allEntries = remember(pendingList, outstandingList, doneTodayList) {
        pendingList + outstandingList + doneTodayList
    }

    LaunchedEffect(allEntries) {
        val stillPresent = allEntries.map { it.id }.toSet()
        selectedIds.removeAll { it !in stillPresent }
        if (selectedIds.isEmpty()) selectionMode = false
    }

    val haptics = LocalHapticFeedback.current

    // Shared row callbacks, so each group stays a short invocation.
    val handleLongClick: (CollectionItem) -> Unit = { item ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        selectionMode = true
        if (item.id !in selectedIds) selectedIds.add(item.id)
    }
    val handleTap: (CollectionItem) -> Unit = { item ->
        if (selectionMode) {
            if (item.id in selectedIds) {
                selectedIds.remove(item.id)
            } else {
                selectedIds.add(item.id)
            }
        } else {
            onCollectionClick(item.id)
        }
    }
    val handleReceive: (CollectionItem) -> Unit = { item -> onReceiveAndWhatsApp(item) }
    val handleOpenWhatsApp: (CollectionItem) -> Unit = { item -> onOpenWhatsAppAgain(item) }
    val handleMarkReported: (CollectionItem) -> Unit = { item -> onConfirmReported(item.id) }
    val handleVoid: (CollectionItem) -> Unit = { item -> onVoidInstantly(item) }

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
                modifier = Modifier.height(Space.touchTarget)
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
            item(key = "summary") {
                SummaryStrip(
                    toCollectPaise = pendingList.sumOf { it.amountPaise },
                    toCollectCount = pendingList.size,
                    toCollectCommissionPaise = pendingList.sumOf { it.commissionPaise },
                    collectedPaise = todayStats.totalPaise,
                    commissionPaise = todayStats.commissionPaise
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
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBulkReceiveAndSend(pendingList.filter { it.id in selectedIds })
                            selectedIds.clear()
                            selectionMode = false
                        },
                        onMarkReported = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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

            if (allEntries.isEmpty()) {
                item(key = "empty") {
                    AppEmptyState(
                        title = "No entries yet.",
                        hint = "Tap QUICK CAPTURE to add one."
                    )
                }
            }

            entryGroup(
                key = "pending",
                title = "TO COLLECT",
                labelColor = TextSecondary,
                totalColor = TextDisplay,
                topPadding = Space.md,
                rows = pendingList,
                selectedIds = selectedIds,
                onLongClick = handleLongClick,
                onTap = handleTap,
                onReceive = handleReceive,
                onOpenWhatsApp = handleOpenWhatsApp,
                onMarkReported = handleMarkReported,
                onPartyClick = onPartyClick,
                onVoid = handleVoid
            )

            // The one group that silently gets forgotten: cash in hand, brother not told yet.
            entryGroup(
                key = "report",
                title = "TO REPORT",
                labelColor = NothingAmber,
                totalColor = NothingAmber,
                topPadding = Space.lg,
                rows = outstandingList,
                selectedIds = selectedIds,
                onLongClick = handleLongClick,
                onTap = handleTap,
                onReceive = handleReceive,
                onOpenWhatsApp = handleOpenWhatsApp,
                onMarkReported = handleMarkReported,
                onPartyClick = onPartyClick,
                onVoid = handleVoid
            )

            entryGroup(
                key = "reported",
                title = "REPORTED TODAY",
                labelColor = TextTertiary,
                totalColor = TextTertiary,
                topPadding = Space.lg,
                rows = doneTodayList,
                selectedIds = selectedIds,
                onLongClick = handleLongClick,
                onTap = handleTap,
                onReceive = handleReceive,
                onOpenWhatsApp = handleOpenWhatsApp,
                onMarkReported = handleMarkReported,
                onPartyClick = onPartyClick,
                onVoid = handleVoid
            )
        }
    }

    if (isQuickCaptureOpen) {
        QuickCaptureBottomSheet(
            sheetState = quickCaptureSheetState,
            commissionRatePerThousand = commissionRatePerThousand,
            recentCustomers = recentCustomers,
            initialInput = quickCapturePrefill,
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
            onSave = { name, amountPaise, dateMillis, note ->
                scope.launch { editSheetState.hide() }.invokeOnCompletion {
                    val id = item.id
                    itemToEdit = null
                    onUpdateCollection(id, name, amountPaise, dateMillis, note)
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
 * One workflow group: a header that names the group and totals it, then its rows.
 *
 * The header is the only place a state name appears, which is what lets the row itself stay
 * down to three facts. Groups are ordered by what needs doing next, so "what is next" is
 * answered by scroll position instead of by scanning a badge on every card.
 */
private fun LazyListScope.entryGroup(
    key: String,
    title: String,
    labelColor: Color,
    totalColor: Color,
    topPadding: Dp,
    rows: List<CollectionItem>,
    selectedIds: List<Long>,
    onLongClick: (CollectionItem) -> Unit,
    onTap: (CollectionItem) -> Unit,
    onReceive: (CollectionItem) -> Unit,
    onOpenWhatsApp: (CollectionItem) -> Unit,
    onMarkReported: (CollectionItem) -> Unit,
    onPartyClick: (Long) -> Unit,
    onVoid: (CollectionItem) -> Unit
) {
    if (rows.isEmpty()) return

    item(key = "header_" + key) {
        GroupHeader(
            modifier = Modifier.animateItemPlacement(Motion.standard()),
            title = title,
            count = rows.size,
            totalPaise = rows.sumOf { it.amountPaise },
            labelColor = labelColor,
            totalColor = totalColor,
            topPadding = topPadding
        )
    }

    // Keyed by the entry id alone rather than by group, so an entry that changes state keeps its
    // identity: LazyColumn animates it to its new group instead of cutting it out and pasting it
    // back in. That movement is the feedback — the row visibly goes where the work went.
    items(rows, key = { row -> row.id }) { row ->
        CollectionRow(
            item = row,
            modifier = Modifier.animateItemPlacement(Motion.standard()),
            isSelected = row.id in selectedIds,
            onLongClick = { onLongClick(row) },
            onTap = { onTap(row) },
            onReceive = { onReceive(row) },
            onOpenWhatsApp = { onOpenWhatsApp(row) },
            onMarkReported = { onMarkReported(row) },
            onPartyClick = { onPartyClick(row.customerId) },
            onVoid = { onVoid(row) }
        )
    }
}

/**
 * Group label and group total on one line. The total is worth showing here because it is the
 * only per-group number the operator needs, and it replaces the separate "waiting to be
 * reported" line the old board used to carry.
 */
@Composable
private fun GroupHeader(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    totalPaise: Long,
    labelColor: Color,
    totalColor: Color,
    topPadding: Dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppSectionLabel(text = title, count = count, color = labelColor)
        AmountText(amountPaise = totalPaise, style = AppType.amount, color = totalColor)
    }
}
/**
 * The summary: two numbers, one line each, no card.
 *
 * TO COLLECT leads because it is the question the app gets opened to answer — the money still
 * out, how many parties it is spread across, and the commission riding on it, so the operator
 * knows what the outstanding work is worth before touching anything. COLLECTED TODAY sits beside
 * it with today's commission, so the day's progress needs no second glance. Both captions are
 * stacked under their number rather than inline so a large amount can never squeeze its own
 * caption off the row. The
 * previous board also carried an entry count, an inner divider and a "waiting to be reported"
 * line, all of which are either re-stated by the labelled, counted groups below or were never
 * acted on; it cost roughly 200dp above the first row of the queue.
 */
@Composable
private fun SummaryStrip(
    toCollectPaise: Long,
    toCollectCount: Int,
    toCollectCommissionPaise: Long,
    collectedPaise: Long,
    commissionPaise: Long
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.md, bottom = Space.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "TO COLLECT", style = AppType.labelMono, color = TextSecondary)
                Spacer(modifier = Modifier.height(Space.xs))
                AmountText(
                    amountPaise = toCollectPaise,
                    style = AppType.amountLarge,
                    color = TextDisplay,
                    align = TextAlign.Start
                )
                Text(
                    text = toCollectCaption(toCollectCount, toCollectCommissionPaise),
                    style = AppType.caption,
                    color = TextTertiary,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "COLLECTED TODAY",
                    style = AppType.labelMono,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(Space.xs))
                AmountText(
                    amountPaise = collectedPaise,
                    style = AppType.amountLarge,
                    color = TextDisplay
                )
                Text(
                    text = "comm " + Paise(commissionPaise).toFormattedRupees(),
                    style = AppType.caption,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SurfaceDivider)
        )
    }
}
/**
 * One entry. Three facts only — party, amount, commission — plus the single control that closes
 * the loop, and that only on a row that is still waiting to be reported.
 *
 * There is no status dot any more: the group a row sits in *is* the status, so repeating it on
 * every card only added noise. Rows awaiting reporting sit one surface step brighter, which is
 * the quietest way to say "act on me" without another label.
 *
 * Swipe handling follows the Material 3 rule strictly — the action fires once from the settled
 * value and the box is reset, never inside `confirmValueChange` (which can fire repeatedly
 * while a gesture settles). The database stays the single source of truth.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CollectionRow(
    item: CollectionItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onTap: () -> Unit,
    onReceive: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onMarkReported: () -> Unit,
    onPartyClick: () -> Unit,
    onVoid: () -> Unit
) {
    val shape = RoundedCornerShape(Radius.card)
    val isPending = item.status == CollectionStatus.PENDING
    val isReported = item.status == CollectionStatus.CONFIRMED
    val awaitingReport = !isPending && !isReported
    val haptics = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState()

    // Swipe right advances the entry: take the cash, then send it on WhatsApp.
    val advance: () -> Unit = if (isPending) onReceive else onOpenWhatsApp

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                // The buzz is the acknowledgement: the gesture fired.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                advance()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
        modifier = modifier.padding(bottom = Space.listAdjacent)
    ) {
        AppSurface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) NothingRed else SurfaceDivider,
                    shape = shape
                )
                .combinedClickable(onClick = onTap, onLongClick = onLongClick),
            shape = shape,
            fill = if (awaitingReport) SurfaceRaised else SurfaceCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.md, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customerDisplayName,
                        style = AppType.subheading,
                        color = if (isReported) TextSecondary else TextDisplay,
                        maxLines = 1,
                        // The name carries its own target: it opens that party's ledger, while the
                        // rest of the row still opens the entry. Long-press selects from either.
                        modifier = Modifier.combinedClickable(
                            onClick = onPartyClick,
                            onLongClick = onLongClick
                        )
                    )
                    Text(
                        text = metaLine(item),
                        style = AppType.caption,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }

                AmountText(
                    amountPaise = item.amountPaise,
                    style = AppType.amountLarge,
                    color = if (isReported) TextSecondary else TextDisplay
                )

                if (awaitingReport) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMarkReported()
                        },
                        shape = RoundedCornerShape(Radius.pill),
                        border = BorderStroke(1.dp, NothingAmber),
                        contentPadding = PaddingValues(horizontal = Space.md, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "SENT", style = AppType.labelMono, color = NothingAmber)
                    }
                }
            }
        }
    }
}
/**
 * The second line of a row: commission, plus the day when the entry is not from today.
 *
 * Commission earns the line because it is the figure the operator is actually paid on. The day
 * is the only timestamp kept anywhere in the queue, because a stale open item that looks like
 * today's is the one mistake this list can cause. Time of day, entry counts and bare "ENTRY"
 * wording are gone — nothing is done differently because of them.
 */
private fun metaLine(item: CollectionItem): String {
    val commission = "comm " + Paise(item.commissionPaise).toFormattedRupees()
    val day = staleDayLabel(item.receivedAt ?: item.createdAt)
    return if (day == null) commission else commission + " · " + day
}

/**
 * Day label for entries that are NOT from today; null for today, so the common case carries no
 * date at all.
 */
private fun staleDayLabel(timestamp: Long): String? {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val oneDayMs = 24L * 60L * 60L * 1000L

    return when {
        timestamp >= startOfToday -> null
        timestamp >= startOfToday - oneDayMs -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Party count in plain words. "all clear" is the state worth saying out loud; a bare zero is
 * not.
 */
private fun partyCountLabel(count: Int): String = when {
    count <= 0 -> "all clear"
    count == 1 -> "1 party"
    else -> count.toString() + " parties"
}

/**
 * The TO COLLECT caption: how many parties the outstanding money is spread across, and the
 * commission riding on it. When there is nothing left to collect it says so and stops — a zero
 * count with a zero commission is not worth spelling out.
 */
private fun toCollectCaption(partyCount: Int, commissionPaise: Long): String {
    if (partyCount <= 0) return "all clear"
    return partyCountLabel(partyCount) + " · comm " + Paise(commissionPaise).toFormattedRupees()
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