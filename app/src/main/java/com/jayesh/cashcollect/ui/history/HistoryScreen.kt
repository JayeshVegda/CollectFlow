package com.jayesh.cashcollect.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppScreenTitle
import com.jayesh.cashcollect.ui.common.GlassSurface
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextPrimary
import com.jayesh.cashcollect.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DateGroup(
    val title: String,
    val totalPaise: Long,
    val commissionPaise: Long,
    val items: List<CollectionItem>
)

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onItemClick: (Long) -> Unit,
    onPartyClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    HistoryScreen(
        items = items,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        selectedStatus = selectedStatus,
        onStatusSelect = viewModel::onStatusSelect,
        sortOption = sortOption,
        onToggleSort = viewModel::toggleSort,
        onItemClick = onItemClick,
        onPartyClick = onPartyClick,
        onExportCsvClick = { viewModel.exportCsv(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    items: List<CollectionItem>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedStatus: CollectionStatus? = null,
    onStatusSelect: (CollectionStatus?) -> Unit = {},
    sortOption: SortOption = SortOption.NEWEST,
    onToggleSort: () -> Unit = {},
    onItemClick: (Long) -> Unit,
    onPartyClick: (Long) -> Unit = {},
    onExportCsvClick: () -> Unit
) {
    // Summary computations for the active filtered items
    val activeItems = remember(items) { items.filter { it.status != CollectionStatus.VOIDED } }
    val totalPaise = remember(activeItems) { activeItems.sumOf { it.amountPaise } }
    val totalCommission = remember(activeItems) { activeItems.sumOf { it.commissionPaise } }
    val activeCount = remember(activeItems) { activeItems.size }
    val isFiltered = searchQuery.isNotBlank() || selectedStatus != null

    // Date grouping for NEWEST / OLDEST sort
    val dateGroups = remember(items, sortOption) {
        if (sortOption == SortOption.HIGHEST_AMOUNT) {
            emptyList()
        } else {
            groupItemsByDay(items)
        }
    }

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { AppScreenTitle("History") },
                actions = {
                    IconButton(onClick = onToggleSort) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Sort", tint = NothingWhite)
                    }
                    IconButton(onClick = onExportCsvClick) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = NothingWhite)
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search party, note, or area...", color = NothingMuted, style = AppType.bodySm) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NothingGray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = NothingGray)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = AppType.bodySm,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelect(null) },
                    label = { Text("ALL", style = AppType.labelMono) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = NothingCard,
                        labelColor = NothingGray,
                        selectedContainerColor = NothingWhite,
                        selectedLabelColor = Color.Black
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedStatus == null,
                        borderColor = NothingBorderVisible,
                        selectedBorderColor = NothingWhite
                    )
                )

                CollectionStatus.values().forEach { status ->
                    val label = when (status) {
                        CollectionStatus.PENDING -> "PENDING"
                        CollectionStatus.RECEIPT_CONFIRMED -> "TO REPORT"
                        CollectionStatus.CONFIRMED -> "REPORTED"
                        CollectionStatus.VOIDED -> "VOIDED"
                    }
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelect(if (selectedStatus == status) null else status) },
                        label = { Text(label, style = AppType.labelMono) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NothingCard,
                            labelColor = NothingGray,
                            selectedContainerColor = NothingWhite,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedStatus == status,
                            borderColor = NothingBorderVisible,
                            selectedBorderColor = NothingWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Filter Summary Banner
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFiltered) "FILTERED VOLUME" else "TOTAL VOLUME",
                            style = AppType.labelMono,
                            color = if (isFiltered) NothingAmber else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AmountText(
                            amountPaise = totalPaise,
                            style = AppType.amountLarge.copy(fontSize = 18.sp),
                            color = TextDisplay
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "COMMISSION",
                            style = AppType.labelMono,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AmountText(
                            amountPaise = totalCommission,
                            style = AppType.amount,
                            color = NothingGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "COUNT",
                            style = AppType.labelMono,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$activeCount entries",
                            style = AppType.bodySm.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                }
            }

            // Sub-bar: Records count & Sort toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} records",
                    style = AppType.labelMono,
                    color = NothingMuted
                )
                Text(
                    text = "SORT: ${sortOption.label.uppercase()}",
                    style = AppType.labelMono,
                    color = NothingGray,
                    modifier = Modifier.clickable { onToggleSort() }
                )
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFiltered) "No records match your filters." else "No collection history yet.",
                        style = AppType.bodySm,
                        color = NothingMuted
                    )
                }
            } else if (sortOption == SortOption.HIGHEST_AMOUNT) {
                // Flat ranked list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        HistoryRow(
                            item = item,
                            showFullDate = true,
                            onClick = { onItemClick(item.id) },
                            onPartyClick = { onPartyClick(item.customerId) }
                        )
                    }
                }
            } else {
                // Date-grouped list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateGroups.forEach { group ->
                        item(key = "header_${group.title}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.title,
                                    style = AppType.labelMonoLarge,
                                    color = NothingWhite
                                )
                                Text(
                                    text = "${group.items.size} · ${Paise(group.totalPaise).toFormattedRupees()}",
                                    style = AppType.labelMono,
                                    color = NothingGray
                                )
                            }
                        }

                        items(group.items, key = { it.id }) { item ->
                            HistoryRow(
                                item = item,
                                showFullDate = false,
                                onClick = { onItemClick(item.id) },
                                onPartyClick = { onPartyClick(item.customerId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: CollectionItem,
    showFullDate: Boolean,
    onClick: () -> Unit,
    onPartyClick: () -> Unit
) {
    val isVoided = item.status == CollectionStatus.VOIDED
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.createdAt, showFullDate) {
        if (showFullDate) {
            fullDateFormat.format(Date(item.createdAt))
        } else {
            timeFormat.format(Date(item.createdAt))
        }
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Party name in Space Grotesk
                Text(
                    text = item.customerDisplayName,
                    style = AppType.subheading,
                    color = NothingWhite,
                    textDecoration = if (isVoided) TextDecoration.LineThrough else null,
                    modifier = Modifier.clickable(onClick = onPartyClick)
                )
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = AppType.labelMono,
                        color = NothingGray
                    )
                    if (!isVoided && item.commissionPaise > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${Paise(item.commissionPaise).toFormattedRupees()} comm",
                            style = AppType.labelMono,
                            color = NothingGreen
                        )
                    }
                }

                AmountText(
                    amountPaise = item.amountPaise,
                    style = AppType.amountLarge,
                    color = if (isVoided) NothingGray else NothingWhite,
                    strikethrough = isVoided
                )
            }

            if (!item.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${item.note}",
                    style = AppType.bodySm,
                    color = NothingGray
                )
            }

            if (isVoided && !item.voidReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Void: ${item.voidReason}",
                    style = AppType.labelMono,
                    color = NothingRed
                )
            }
        }
    }
}

private fun groupItemsByDay(items: List<CollectionItem>): List<DateGroup> {
    if (items.isEmpty()) return emptyList()

    val groups = LinkedHashMap<String, MutableList<CollectionItem>>()
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    for (item in items) {
        val key = keyFormat.format(Date(item.createdAt))
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }

    val calToday = Calendar.getInstance()
    val todayYear = calToday.get(Calendar.YEAR)
    val todayDay = calToday.get(Calendar.DAY_OF_YEAR)

    return groups.map { (_, groupItems) ->
        val firstItemTime = groupItems.first().createdAt
        val calItem = Calendar.getInstance().apply { timeInMillis = firstItemTime }
        val itemYear = calItem.get(Calendar.YEAR)
        val itemDay = calItem.get(Calendar.DAY_OF_YEAR)

        val title = if (todayYear == itemYear) {
            when (todayDay - itemDay) {
                0 -> "TODAY"
                1 -> "YESTERDAY"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(firstItemTime)).uppercase()
            }
        } else {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(firstItemTime)).uppercase()
        }

        val active = groupItems.filter { it.status != CollectionStatus.VOIDED }
        val totalPaise = active.sumOf { it.amountPaise }
        val commPaise = active.sumOf { it.commissionPaise }

        DateGroup(
            title = title,
            totalPaise = totalPaise,
            commissionPaise = commPaise,
            items = groupItems
        )
    }
}