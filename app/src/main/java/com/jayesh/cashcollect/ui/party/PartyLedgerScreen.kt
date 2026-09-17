package com.jayesh.cashcollect.ui.party

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.party.PartyLedger
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppEmptyState
import com.jayesh.cashcollect.ui.common.AppSectionLabel
import com.jayesh.cashcollect.ui.common.AppSurface
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary
import com.jayesh.cashcollect.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * One party's page.
 *
 * The operator deals with roughly ten or twelve parties, repeatedly, so this is the view that
 * answers "what is going on with Sambhu?" without scrolling the whole ledger: what is still owed,
 * what has been paid over all time, the commission it has earned, this month's movement, and every
 * entry that party has ever had, newest first.
 *
 * Read-only by design — tapping an entry opens the entry, where every action already lives, so
 * there is exactly one place in the app that can change one. The single action here is the one a
 * repeat party needs: a new entry, pre-filled with the name.
 */
@Composable
fun PartyLedgerRoute(
    viewModel: PartyLedgerViewModel,
    onBackClick: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onNewEntryClick: (String) -> Unit
) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()

    PartyLedgerScreen(
        ledger = ledger,
        onBackClick = onBackClick,
        onEntryClick = onEntryClick,
        onNewEntryClick = onNewEntryClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    ledger: PartyLedger,
    onBackClick: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onNewEntryClick: (String) -> Unit
) {
    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ledger.displayName,
                        style = AppType.subheading,
                        color = TextDisplay,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDisplay
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Space.gutter),
            contentPadding = PaddingValues(top = Space.sm, bottom = Space.xxl)
        ) {
            item(key = "summary") {
                PartySummary(ledger = ledger)
            }

            item(key = "new_entry") {
                Button(
                    onClick = { onNewEntryClick(ledger.customer?.name.orEmpty()) },
                    enabled = !ledger.customer?.name.isNullOrBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Space.touchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(Radius.pill)
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Space.xs))
                    Text(text = "NEW ENTRY", style = AppType.labelMono, color = Color.Black)
                }
            }

            if (ledger.entries.isEmpty()) {
                item(key = "empty") {
                    AppEmptyState(title = "Nothing recorded yet.")
                }
            } else {
                item(key = "ledger_header") {
                    Spacer(modifier = Modifier.height(Space.md))
                    AppSectionLabel(
                        text = "Ledger",
                        count = ledger.entryCount,
                        color = TextSecondary
                    )
                }

                items(ledger.entries, key = { entry -> entry.id }) { entry ->
                    PartyEntryRow(entry = entry, onClick = { onEntryClick(entry.id) })
                }
            }
        }
    }
}
/**
 * The two numbers that matter for a party, plus the month's movement.
 *
 * TO COLLECT is money still owed by them — the reason to open this page. COLLECTED is money already
 * in hand over all time, with the commission it earned. The month line is what makes the page worth
 * revisiting for a party dealt with every week.
 */
@Composable
private fun PartySummary(ledger: PartyLedger) {
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
                    amountPaise = ledger.toCollectPaise,
                    style = AppType.amountLarge,
                    color = TextDisplay,
                    align = TextAlign.Start
                )
                Text(
                    text = openCaption(ledger),
                    style = AppType.caption,
                    color = TextTertiary,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "COLLECTED", style = AppType.labelMono, color = TextSecondary)
                Spacer(modifier = Modifier.height(Space.xs))
                AmountText(
                    amountPaise = ledger.collectedPaise,
                    style = AppType.amountLarge,
                    color = TextDisplay
                )
                Text(
                    text = "comm " + Paise(ledger.commissionPaise).toFormattedRupees(),
                    style = AppType.caption,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        if (ledger.monthCollectedPaise > 0L) {
            Text(
                text = "THIS MONTH   " +
                    Paise(ledger.monthCollectedPaise).toFormattedRupees() +
                    "   ·   comm " + Paise(ledger.monthCommissionPaise).toFormattedRupees(),
                style = AppType.labelMono,
                color = NothingGreen
            )
            Spacer(modifier = Modifier.height(Space.sm))
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
 * TO COLLECT's caption, in the same shape the queue uses: how many entries are open and what they
 * are worth in commission — or, when nothing is owed, whether cash is in hand waiting to be
 * reported, because that is the state that gets forgotten.
 */
private fun openCaption(ledger: PartyLedger): String {
    if (ledger.toCollectCount <= 0) {
        return if (ledger.inHandCount > 0) {
            ledger.inHandCount.toString() + " in hand to report"
        } else {
            "all clear"
        }
    }
    val open = if (ledger.toCollectCount == 1) {
        "1 entry"
    } else {
        ledger.toCollectCount.toString() + " entries"
    }
    return open + " · comm " + Paise(ledger.toCollectCommissionPaise).toFormattedRupees()
}
/**
 * One entry on a party's page. The party is already known from the page, so the row leads with the
 * day, then the amount and where the entry stands, with the commission underneath.
 */
@Composable
private fun PartyEntryRow(entry: CollectionItem, onClick: () -> Unit) {
    val isVoided = entry.status == CollectionStatus.VOIDED
    val isReported = entry.status == CollectionStatus.CONFIRMED

    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Space.listAdjacent)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.card)
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
                    text = dayLabel(entry),
                    style = AppType.subheading,
                    color = if (isVoided) TextSecondary else TextDisplay,
                    maxLines = 1,
                    textDecoration = if (isVoided) TextDecoration.LineThrough else null
                )
                Text(
                    text = "comm " + Paise(entry.commissionPaise).toFormattedRupees() +
                        noteSuffix(entry),
                    style = AppType.caption,
                    color = TextTertiary,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountPaise = entry.amountPaise,
                    style = AppType.amountLarge,
                    color = if (isVoided || isReported) TextSecondary else TextDisplay,
                    strikethrough = isVoided
                )
                Spacer(modifier = Modifier.height(Space.xs))
                StatusBadge(status = entry.status)
            }
        }
    }
}

/** The day an entry belongs to. Today / Yesterday read faster than a date for the common cases. */
private fun dayLabel(entry: CollectionItem): String {
    val timestamp = entry.receivedAt ?: entry.createdAt
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(day, today) -> "Today"
        sameDay(day, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun noteSuffix(entry: CollectionItem): String =
    entry.note?.takeIf { it.isNotBlank() }?.let { " · " + it } ?: ""