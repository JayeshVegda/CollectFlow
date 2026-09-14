package com.jayesh.cashcollect.ui.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.backup.CsvExporter
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val collectionRepo: CollectionRepository,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<CollectionStatus?>(null)
    val selectedStatus: StateFlow<CollectionStatus?> = _selectedStatus.asStateFlow()

    val historyList: StateFlow<List<CollectionItem>> = collectionRepo.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredItems: StateFlow<List<CollectionItem>> = combine(
        historyList,
        _searchQuery,
        _selectedStatus
    ) { items, query, status ->
        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.customerDisplayName.contains(query, ignoreCase = true) ||
                    (item.note != null && item.note.contains(query, ignoreCase = true))
            val matchesStatus = status == null || item.status == status
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusSelect(status: CollectionStatus?) {
        _selectedStatus.value = status
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val csvFile = csvExporter.exportHistoryToCsv(filteredItems.value)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share History CSV"))
            } catch (e: Exception) {
                Toast.makeText(context, "CSV export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class Factory(
        private val collectionRepo: CollectionRepository,
        private val csvExporter: CsvExporter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(collectionRepo, csvExporter) as T
        }
    }
}
