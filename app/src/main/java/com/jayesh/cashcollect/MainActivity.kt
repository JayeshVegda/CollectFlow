package com.jayesh.cashcollect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.service.whatsapp.WhatsAppLauncher
import com.jayesh.cashcollect.ui.add.AddCollectionScreen
import com.jayesh.cashcollect.ui.collections.CollectionsScreen
import com.jayesh.cashcollect.ui.detail.CollectionDetailScreen
import com.jayesh.cashcollect.ui.history.HistoryScreen
import com.jayesh.cashcollect.ui.settings.SettingsScreen
import com.jayesh.cashcollect.ui.theme.CashCollectTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Collections : Screen()
    object History : Screen()
    object AddCollection : Screen()
    data class Detail(val collectionId: Long) : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as CashCollectApplication
        val collectionRepo = app.collectionRepository
        val customerRepo = app.customerRepository
        val settingsRepo = app.settingsRepository
        val backupManager = app.backupManager
        val csvExporter = app.csvExporter
        val notificationManager = app.notificationManager

        val initialCollectionId = intent.getLongExtra("EXTRA_COLLECTION_ID", -1L)

        setContent {
            CashCollectTheme {
                var currentScreen by remember {
                    mutableStateOf<Screen>(
                        if (initialCollectionId > 0) Screen.Detail(initialCollectionId) else Screen.Collections
                    )
                }

                val outstandingList by collectionRepo.getOutstandingConfirmations()
                    .collectAsState(initial = emptyList())
                val pendingList by collectionRepo.getPendingCollections()
                    .collectAsState(initial = emptyList())
                val historyList by collectionRepo.getAllHistory()
                    .collectAsState(initial = emptyList())
                val recentCustomers by customerRepo.getRecentCustomers(10)
                    .collectAsState(initial = emptyList())
                val appSettings by settingsRepo.getSettings()
                    .collectAsState(initial = com.jayesh.cashcollect.domain.model.AppSettings())

                val customerSearchResults = remember { MutableStateFlow<List<Customer>>(emptyList()) }
                val searchResultsState by customerSearchResults.collectAsState()

                val scope = rememberCoroutineScope()

                // Bottom Nav is shown only on root tabs (Collections, History)
                val isRootTab = currentScreen is Screen.Collections || currentScreen is Screen.History

                Scaffold(
                    bottomBar = {
                        if (isRootTab) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Collections,
                                    onClick = { currentScreen = Screen.Collections },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Collections") },
                                    label = { Text("Collections") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.History,
                                    onClick = { currentScreen = Screen.History },
                                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                    label = { Text("History") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Collections -> {
                                CollectionsScreen(
                                    outstandingList = outstandingList,
                                    pendingList = pendingList,
                                    onAddCollectionClick = { currentScreen = Screen.AddCollection },
                                    onCollectionClick = { id -> currentScreen = Screen.Detail(id) },
                                    onReceiveAndWhatsApp = { item ->
                                        scope.launch {
                                            // 1. NON-NEGOTIABLE: Persist receipt to Room BEFORE intent
                                            val committed = collectionRepo.markReceivedAndCommit(item.id)

                                            // 2. Fire immediate local notification
                                            notificationManager.showImmediateReceiptNotification(committed)

                                            // 3. Launch WhatsApp intent
                                            val msg = WhatsAppLauncher.buildReceiptMessage(committed)
                                            val intent = WhatsAppLauncher.createSendIntent(
                                                this@MainActivity,
                                                appSettings.brotherWhatsAppNumber,
                                                msg
                                            )
                                            collectionRepo.logWhatsAppOpened(item.id)

                                            try {
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Could not open WhatsApp. Receipt is saved.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    onOpenWhatsAppAgain = { item ->
                                        scope.launch {
                                            collectionRepo.logWhatsAppOpened(item.id)
                                            val msg = WhatsAppLauncher.buildReceiptMessage(item)
                                            val intent = WhatsAppLauncher.createSendIntent(
                                                this@MainActivity,
                                                appSettings.brotherWhatsAppNumber,
                                                msg
                                            )
                                            try {
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onConfirmSent = { id ->
                                        scope.launch {
                                            collectionRepo.confirmSent(id)
                                            Toast.makeText(this@MainActivity, "Marked as Confirmed Sent", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSettingsClick = { currentScreen = Screen.Settings }
                                )
                            }

                            is Screen.AddCollection -> {
                                AddCollectionScreen(
                                    recentCustomers = recentCustomers,
                                    searchResults = searchResultsState,
                                    commissionRatePerThousand = appSettings.commissionRatePerThousand,
                                    onSearchCustomer = { query ->
                                        scope.launch {
                                            customerRepo.searchCustomers(query).collect {
                                                customerSearchResults.value = it
                                            }
                                        }
                                    },
                                    onAddNewCustomer = { name, alias ->
                                        scope.launch {
                                            customerRepo.addCustomer(name, alias)
                                        }
                                    },
                                    onSaveCollection = { customerId, amountPaise ->
                                        scope.launch {
                                            collectionRepo.createPendingCollection(
                                                customerId = customerId,
                                                amountPaise = amountPaise,
                                                commissionRateSnapshot = appSettings.commissionRatePerThousand
                                            )
                                            currentScreen = Screen.Collections
                                        }
                                    },
                                    onCheckDuplicate = { customerId, amountPaise ->
                                        collectionRepo.checkRecentDuplicate(customerId, amountPaise)
                                    },
                                    onBackClick = { currentScreen = Screen.Collections }
                                )
                            }

                            is Screen.Detail -> {
                                val item = historyList.find { it.id == screen.collectionId }
                                CollectionDetailScreen(
                                    collection = item,
                                    onReceiveAndWhatsApp = { target ->
                                        scope.launch {
                                            val committed = collectionRepo.markReceivedAndCommit(target.id)
                                            notificationManager.showImmediateReceiptNotification(committed)
                                            val msg = WhatsAppLauncher.buildReceiptMessage(committed)
                                            val intent = WhatsAppLauncher.createSendIntent(
                                                this@MainActivity,
                                                appSettings.brotherWhatsAppNumber,
                                                msg
                                            )
                                            collectionRepo.logWhatsAppOpened(target.id)
                                            try {
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onOpenWhatsAppAgain = { target ->
                                        scope.launch {
                                            collectionRepo.logWhatsAppOpened(target.id)
                                            val msg = WhatsAppLauncher.buildReceiptMessage(target)
                                            val intent = WhatsAppLauncher.createSendIntent(
                                                this@MainActivity,
                                                appSettings.brotherWhatsAppNumber,
                                                msg
                                            )
                                            try {
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onConfirmSent = { id ->
                                        scope.launch {
                                            collectionRepo.confirmSent(id)
                                            Toast.makeText(this@MainActivity, "Confirmed Sent", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onVoidAndReplace = { originalId, reason, newAmountPaise ->
                                        scope.launch {
                                            val newId = collectionRepo.voidAndReplace(
                                                originalId,
                                                reason,
                                                newAmountPaise,
                                                appSettings.commissionRatePerThousand
                                            )
                                            Toast.makeText(this@MainActivity, "Voided and created replacement #$newId", Toast.LENGTH_SHORT).show()
                                            currentScreen = Screen.Collections
                                        }
                                    },
                                    onBackClick = { currentScreen = Screen.Collections }
                                )
                            }

                            is Screen.History -> {
                                HistoryScreen(
                                    items = historyList,
                                    onItemClick = { id -> currentScreen = Screen.Detail(id) },
                                    onExportCsvClick = {
                                        scope.launch {
                                            try {
                                                val csvFile = csvExporter.exportHistoryToCsv(historyList)
                                                val uri = FileProvider.getUriForFile(
                                                    this@MainActivity,
                                                    "${packageName}.fileprovider",
                                                    csvFile
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                startActivity(Intent.createChooser(shareIntent, "Share History CSV"))
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "CSV export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }

                            is Screen.Settings -> {
                                SettingsScreen(
                                    settings = appSettings,
                                    onSaveNumber = { number ->
                                        scope.launch {
                                            settingsRepo.updateBrotherWhatsAppNumber(number)
                                        }
                                    },
                                    onSaveRate = { rate ->
                                        scope.launch {
                                            settingsRepo.updateCommissionRate(rate)
                                        }
                                    },
                                    onBackupNow = {
                                        scope.launch {
                                            try {
                                                val backupFile = backupManager.createEncryptedBackup()
                                                val uri = FileProvider.getUriForFile(
                                                    this@MainActivity,
                                                    "${packageName}.fileprovider",
                                                    backupFile
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/octet-stream"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                startActivity(Intent.createChooser(shareIntent, "Export Encrypted Backup"))
                                            } catch (e: Exception) {
                                                Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onRestoreBackupClick = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "To restore, copy backup file to backups folder or import via file manager.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onBackClick = { currentScreen = Screen.Collections }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
