package com.jayesh.cashcollect.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.common.GlassSurface
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGlassBorder
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HISTORY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                },
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search party or area...", color = NothingMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NothingGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelect(null) },
                    label = { Text("ALL", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
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
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { onStatusSelect(if (selectedStatus == status) null else status) },
                        label = { Text(status.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} records",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingMuted
                )
                Text(
                    text = sortOption.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingGray,
                    modifier = Modifier.clickable { onToggleSort() }
                )
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records match your filters.", fontFamily = FontFamily.Monospace, color = NothingMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        HistoryRow(
                            item = item,
                            onClick = { onItemClick(item.id) },
                            onPartyClick = { onPartyClick(item.customerId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: CollectionItem, onClick: () -> Unit, onPartyClick: () -> Unit) {
    val isVoided = item.status == CollectionStatus.VOIDED
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

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
                Text(
                    text = item.customerDisplayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NothingWhite,
                    textDecoration = if (isVoided) TextDecoration.LineThrough else null,
                    // Tapping the party name opens that party's ledger; the row still opens the entry.
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
                Text(
                    text = dateFormat.format(Date(item.createdAt)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingGray
                )
                Text(
                    text = Paise(item.amountPaise).toFormattedRupees(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isVoided) NothingGray else NothingWhite,
                    textDecoration = if (isVoided) TextDecoration.LineThrough else null
                )
            }

            if (!item.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Note: ${item.note}", fontSize = 12.sp, color = NothingGray)
            }

            if (isVoided && !item.voidReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Void: ${item.voidReason}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingRed
                )
            }
        }
    }
}