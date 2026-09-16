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
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppEmptyState
import com.jayesh.cashcollect.ui.common.AppSectionLabel
import com.jayesh.cashcollect.ui.common.AppSurface
import com.jayesh.cashcollect.ui.common.EditCollectionBottomSheet
import com.jayesh.cashcollect.ui.common.StatusBadge
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
import java.util.Date
import java.util.Locale
/*
 * TODAY — the daily loop
 * ----------------------
 * The operator's real job is a loop over ~10-15 parties:
 *
 *     brother says "collect Rs.X from Y"   ->  TO COLLECT   (PENDING)
 *     cash is handed over                  ->  TO REPORT    (RECEIPT_CONFIRMED)
 *     brother is told on WhatsApp          ->  DONE         (CONFIRMED)
 *
 * So this screen answers those three questions instead of leading with a generic
 * "today's total" KPI, and it leads with exactly one primary number.
 *
 * Previously the screen led with TODAY COLLECTED (a vanity metric for a tool used daily)
 * and listed PENDING/OUTSTANDING as two loose sections, which meant the one state that
 * actually causes a problem — cash collected but not yet reported, the thing the operator
 * said they "sometimes forget" — had no home of its own.
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
        onConfirmSent = { id -> viewModel.confirmSent(context, id) },
        onDeleteCollection = { id -> viewModel.deleteCollection(context, id) },
        onUpdateCollection = { id, amt, note -> viewModel.updateCollection(context, id, amt, note) },
        onVoidInstantly = { item -> viewModel.voidInstantly(context, item) },
        onBulkReceiveAndSend = { items -> viewModel.receiveAndSendAll(context, items) },
        onBulkMarkSent = { items -> viewModel.markAllSent(context, items) }
    )
}

/** The three questions this screen answers. */
private enum class TodayTab(val label: String) {
    TO_COLLECT("TO COLLECT"),
    TO_REPORT("TO REPORT"),
    DONE("DONE")
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
    onConfirmSent: (Long) -> Unit,
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

    var tab by remember { mutableStateOf(TodayTab.TO_COLLECT) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Drop selections that no longer exist (e.g. reported from another screen).
    LaunchedEffect(pendingList, outstandingList) {
        val stillPresent = (pendingList + outstandingList).map { it.id }.toSet()
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

    val toCollectTotal = pendingList.sumOf { it.amountPaise }
    val toReportTotal = outstandingList.sumOf { it.amountPaise }

    val activeList = when (tab) {
        TodayTab.TO_COLLECT -> pendingList
        TodayTab.TO_REPORT -> outstandingList
        TodayTab.DONE -> doneTodayList
    }
    val ledger = remember(activeList) { buildLedger(activeList) }

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
            item(key = "hero") {
                TodayHero(
                    toCollectPaise = toCollectTotal,
                    toCollectCount = pendingList.size,
                    toReportPaise = toReportTotal,
                    toReportCount = outstandingList.size,
                    todayCollectedPaise = todayStats.totalPaise,
                    todayCommissionPaise = todayStats.commissionPaise,
                    onAttentionClick = { tab = TodayTab.TO_REPORT }
                )
            }

            item(key = "tabs") {
                Spacer(modifier = Modifier.height(Space.md))
                TodayTabs(
                    selected = tab,
                    counts = mapOf(
                        TodayTab.TO_COLLECT to pendingList.size,
                        TodayTab.TO_REPORT to outstandingList.size,
                        TodayTab.DONE to doneTodayList.size
                    ),
                    onSelect = { tab = it }
                )
            }

            if (selectionMode) {
                item(key = "bulk") {
                    Spacer(modifier = Modifier.height(Space.sm))
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

            item(key = "hint") {
                Spacer(modifier = Modifier.height(Space.md))
                Text(
                    text = when (tab) {
                        TodayTab.TO_COLLECT -> "Swipe right for cash received  ·  swipe left to cancel  ·  hold to select"
                        TodayTab.TO_REPORT -> "Tap REPORT to send on WhatsApp, then mark it reported"
                        TodayTab.DONE -> "Reported to your brother today"
                    },
                    style = AppType.caption,
                    color = TextTertiary
                )
            }

            if (ledger.isEmpty()) {
                item(key = "empty") {
                    AppEmptyState(
                        title = when (tab) {
                            TodayTab.TO_COLLECT -> "Nothing to collect."
                            TodayTab.TO_REPORT -> "Nothing waiting to be reported."
                            TodayTab.DONE -> "Nothing finished yet today."
                        },
                        hint = when (tab) {
                            TodayTab.TO_COLLECT -> "Tap QUICK CAPTURE when your brother gives you a collection."
                            TodayTab.TO_REPORT -> "Mark cash received and it waits here until reported."
                            TodayTab.DONE -> "Entries you have reported appear here."
                        }
                    )
                }
            } else {
                items(ledger, key = { it.key }) { entry ->
                    LedgerEntryView(
                        entry = entry,
                        tab = tab,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        onToggleSelect = { id ->
                            if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
                        },
                        onLongPress = { id ->
                            selectionMode = true
                            if (id !in selectedIds) selectedIds.add(id)
                        },
                        onRowClick = onCollectionClick,
                        onReceive = onReceiveAndWhatsApp,
                        onReport = onOpenWhatsAppAgain,
                        onVoid = onVoidInstantly
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
 * The hero: exactly ONE primary number, plus the one urgent secondary if it exists.
 *
 * Today's collected total is deliberately demoted to a quiet footer line - it is metadata
 * about what already happened, not a to-do. The primary is what still has to happen.
 */
@Composable
private fun TodayHero(
    toCollectPaise: Long,
    toCollectCount: Int,
    toReportPaise: Long,
    toReportCount: Int,
    todayCollectedPaise: Long,
    todayCommissionPaise: Long,
    onAttentionClick: () -> Unit
) {
    AppSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            AppSectionLabel(text = "To collect", color = TextSecondary)

            AmountText(
                amountPaise = toCollectPaise,
                style = AppType.displayMoney,
                color = TextDisplay,
                align = TextAlign.Start
            )

            Text(
                text = if (toCollectCount == 1) "1 PARTY" else "$toCollectCount PARTIES",
                style = AppType.labelMono,
                color = TextTertiary
            )

            if (toReportCount > 0) {
                Spacer(modifier = Modifier.height(Space.sm))
                AttentionRow(
                    text = "$toReportCount COLLECTED, NOT REPORTED YET - TAP TO REVIEW",
                    onClick = onAttentionClick
                )
            }

            Spacer(modifier = Modifier.height(Space.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceDivider)
            )
            Spacer(modifier = Modifier.height(Space.xs))

            Text(
                text = "TODAY " + Paise(todayCollectedPaise).toFormattedRupees() +
                    " COLLECTED   ·   " + Paise(todayCommissionPaise).toFormattedRupees() + " COMMISSION",
                style = AppType.caption,
                color = TextTertiary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttentionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.chip))
            .background(SurfaceRaised)
            .border(1.dp, NothingAmber, RoundedCornerShape(Radius.chip))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = Space.sm, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // weight(1f) + maxLines = 2 so the label wraps instead of pushing the arrow off the
        // right edge (it was being clipped at 11sp monospace tracking).
        Text(
            text = text,
            style = AppType.labelMono,
            color = NothingAmber,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(Space.xs))
        Text(
            text = "->",
            style = AppType.labelMono,
            color = NothingAmber,
            maxLines = 1
        )
    }
}

/**
 * The three-question switcher. A plain row of pills rather than a Material segmented
 * control, because the labels are monospace and must stay on the 8dp rhythm.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayTabs(
    selected: TodayTab,
    counts: Map<TodayTab, Int>,
    onSelect: (TodayTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        for (tab in TodayTab.values()) {
            val isSelected = tab == selected
            val count = counts[tab] ?: 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Space.touchTarget)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(if (isSelected) SurfaceRaised else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) NothingWhite else SurfaceDivider,
                        shape = RoundedCornerShape(Radius.pill)
                    )
                    .combinedClickable(onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 0) tab.label + " " + count else tab.label,
                    style = AppType.labelMono,
                    color = when {
                        isSelected -> TextDisplay
                        tab == TodayTab.TO_REPORT && count > 0 -> NothingAmber
                        else -> TextSecondary
                    }
                )
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

/**
 * A party heading, emitted only when one party has more than one entry in the current
 * bucket. See [buildLedger] for why.
 */
@Composable
private fun PartyHeader(header: LedgerEntry.Header) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (header.first) Space.sm else Space.xl,
                bottom = Space.sm
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header.name,
                    style = AppType.subheading,
                    color = TextDisplay,
                    maxLines = 1
                )
                if (!header.alias.isNullOrBlank()) {
                    Text(
                        text = header.alias,
                        style = AppType.caption,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = if (header.count == 1) "1 ENTRY" else header.count.toString() + " ENTRIES",
                style = AppType.labelMono,
                color = TextTertiary
            )
        }
    }
}

/** Bulk action bar shown while rows are multi-selected. */
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
                    text = "$selectedCount SELECTED",
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

/** One rendered line in the ledger: either a party heading or a collection row. */
private sealed interface LedgerEntry {
    val key: String

    data class Header(
        val customerId: Long,
        val name: String,
        val alias: String?,
        val count: Int,
        val first: Boolean
    ) : LedgerEntry {
        override val key: String get() = "h" + customerId
    }

    data class Row(
        val item: CollectionItem,
        val showPartyName: Boolean
    ) : LedgerEntry {
        override val key: String get() = "i" + item.id
    }
}

/**
 * Builds the visible list for the active bucket.
 *
 * Density decision: this operator has ~10-15 parties and usually one open item each, so
 * emitting a heading for every party would burn a third of the screen on repetition. A
 * heading is therefore only emitted when a party has MORE THAN ONE entry in the bucket,
 * where it genuinely clusters information. Single-entry parties stay a plain row, so the
 * common case stays dense and the grouped case still reads as structure.
 *
 * (Per-party totals and running balances belong on the PARTIES screen, where they are the
 * whole point rather than a side effect of the list.)
 */
private fun buildLedger(items: List<CollectionItem>): List<LedgerEntry> {
    val out = mutableListOf<LedgerEntry>()
    var emittedAny = false
    items.groupBy { it.customerId }.values.forEach { group ->
        if (group.size == 1) {
            out += LedgerEntry.Row(group.first(), showPartyName = true)
            emittedAny = true
        } else {
            val first = group.first()
            out += LedgerEntry.Header(
                customerId = first.customerId,
                name = first.customerName,
                alias = first.customerAlias,
                count = group.size,
                first = !emittedAny
            )
            emittedAny = true
            group.forEach { out += LedgerEntry.Row(it, showPartyName = false) }
        }
    }
    return out
}

/**
 * Meta line for a TO REPORT row.
 *
 * When WhatsApp was opened but the entry was never marked reported, say so plainly. That
 * "handed off but unconfirmed" gap is exactly how a collected payment gets forgotten, which
 * is the problem this app exists to solve.
 */
private fun reportMeta(item: CollectionItem): String {
    val received = "received " + formatTimeAgo(item.receivedAt ?: item.createdAt)
    val openedAt = item.whatsappOpenedAt
    return if (openedAt != null) {
        received + "  ·  WhatsApp opened " + formatTimeAgo(openedAt) + ", not marked reported"
    } else {
        received
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / (1000 * 60)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> mins.toString() + "m ago"
        mins < 1440 -> (mins / 60).toString() + "h ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * One collection row.
 *
 * Swipe handling follows the Material 3 rule strictly: the state change is never performed
 * inside `confirmValueChange` (which can fire repeatedly while a gesture settles). Instead
 * the action fires once from the settled value and the box is reset, so the row stays in
 * the list and the database remains the single source of truth.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun LedgerRow(
    item: CollectionItem,
    showPartyName: Boolean,
    isSelected: Boolean,
    meta: String,
    startLabel: String,
    endLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeStartToEnd: () -> Unit,
    onSwipeEndToStart: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(Radius.card)
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onSwipeStartToEnd()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onSwipeEndToStart()
                dismissState.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeBackdrop(startLabel, endLabel) },
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
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (showPartyName) {
                            Text(
                                text = item.customerDisplayName,
                                style = AppType.subheading,
                                color = TextDisplay,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(Space.xxs))
                        }
                        Text(
                            text = meta,
                            style = AppType.caption,
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        AmountText(
                            amountPaise = item.amountPaise,
                            style = AppType.amountLarge,
                            color = TextDisplay
                        )
                        Spacer(modifier = Modifier.height(Space.xs))
                        StatusBadge(status = item.status)
                    }
                }
                trailing()
            }
        }
    }
}
/** Renders one ledger line, choosing row content by the active bucket. */
@Composable
private fun LedgerEntryView(
    entry: LedgerEntry,
    tab: TodayTab,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onLongPress: (Long) -> Unit,
    onRowClick: (Long) -> Unit,
    onReceive: (CollectionItem) -> Unit,
    onReport: (CollectionItem) -> Unit,
    onVoid: (CollectionItem) -> Unit
) {
    when (entry) {
        is LedgerEntry.Header -> PartyHeader(entry)
        is LedgerEntry.Row -> {
            val item = entry.item
            val handleClick: () -> Unit =
                if (selectionMode) {
                    { onToggleSelect(item.id) }
                } else {
                    { onRowClick(item.id) }
                }
            val handleLongClick: () -> Unit = { onLongPress(item.id) }

            when (tab) {
                TodayTab.TO_COLLECT -> LedgerRow(
                    item = item,
                    showPartyName = entry.showPartyName,
                    isSelected = item.id in selectedIds,
                    meta = "promised " + formatTimeAgo(item.createdAt),
                    startLabel = "RECEIVED",
                    endLabel = "CANCEL",
                    onClick = handleClick,
                    onLongClick = handleLongClick,
                    onSwipeStartToEnd = { onReceive(item) },
                    onSwipeEndToStart = { onVoid(item) }
                )

                TodayTab.TO_REPORT -> LedgerRow(
                    item = item,
                    showPartyName = entry.showPartyName,
                    isSelected = item.id in selectedIds,
                    meta = reportMeta(item),
                    startLabel = "WHATSAPP",
                    endLabel = "CANCEL",
                    onClick = handleClick,
                    onLongClick = handleLongClick,
                    onSwipeStartToEnd = { onReport(item) },
                    onSwipeEndToStart = { onVoid(item) },
                    trailing = {
                        Button(
                            onClick = { onReport(item) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Space.touchTarget),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NothingGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(Radius.pill)
                        ) {
                            Text(
                                text = "REPORT ON WHATSAPP",
                                style = AppType.labelMono,
                                color = Color.Black
                            )
                        }
                    }
                )

                TodayTab.DONE -> LedgerRow(
                    item = item,
                    showPartyName = entry.showPartyName,
                    isSelected = item.id in selectedIds,
                    meta = "reported " + formatTimeAgo(
                        item.confirmedSentAt ?: item.receivedAt ?: item.createdAt
                    ),
                    startLabel = "WHATSAPP",
                    endLabel = "CANCEL",
                    onClick = handleClick,
                    onLongClick = handleLongClick,
                    onSwipeStartToEnd = { onReport(item) },
                    onSwipeEndToStart = { onVoid(item) }
                )
            }
        }
    }
}
