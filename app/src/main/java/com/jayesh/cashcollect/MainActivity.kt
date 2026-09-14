package com.jayesh.cashcollect

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.service.whatsapp.WhatsAppLauncher
import com.jayesh.cashcollect.ui.add.AddCollectionScreen
import com.jayesh.cashcollect.ui.collections.CollectionsScreen
import com.jayesh.cashcollect.ui.detail.CollectionDetailScreen
import com.jayesh.cashcollect.ui.history.HistoryScreen
import com.jayesh.cashcollect.ui.insights.InsightsScreen
import com.jayesh.cashcollect.ui.settings.SettingsScreen
import com.jayesh.cashcollect.ui.theme.CashCollectTheme
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Collections : Screen()
    object Insights : Screen()
    object History : Screen()
    object Settings : Screen()
    object AddCollection : Screen()
    data class Detail(val collectionId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Official Google Edge-to-Edge API (Android 15 / SDK 35 standard)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val app = application as CashCollectApplication
        val collectionRepo = app.collectionRepository
        val customerRepo = app.customerRepository
        val settingsRepo = app.settingsRepository
        val backupManager = app.backupManager
        val csvExporter = app.csvExporter
        val notificationManager = app.notificationManager

        val initialCollectionId = intent.getLongExtra("EXTRA_COLLECTION_ID", -1L)
        val openAddDirectly = intent.getBooleanExtra("EXTRA_OPEN_ADD", false)
        val openQuickCaptureDirectly = intent.getBooleanExtra("EXTRA_OPEN_QUICK_CAPTURE", false)

        setContent {
            CashCollectTheme {
                var currentScreen by remember {
                    mutableStateOf<Screen>(
                        when {
                            initialCollectionId > 0 -> Screen.Detail(initialCollectionId)
                            openAddDirectly -> Screen.AddCollection
                            else -> Screen.Collections
                        }
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

                // Bottom Nav is shown on root tabs
                val isRootTab = currentScreen is Screen.Collections ||
                        currentScreen is Screen.Insights ||
                        currentScreen is Screen.History ||
                        currentScreen is Screen.Settings

                Scaffold(
                    containerColor = NothingBlack,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0), // Lets inner TopAppBars handle status bars insets natively!
                    bottomBar = {
                        if (isRootTab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = NothingBorder,
                                        shape = androidx.compose.ui.graphics.RectangleShape
                                    )
                            ) {
                                NavigationBar(
                                    containerColor = NothingBlack,
                                    windowInsets = NavigationBarDefaults.windowInsets
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Collections,
                                        onClick = { currentScreen = Screen.Collections },
                                        icon = { Icon(Icons.Default.List, contentDescription = "Collections") },
                                        label = {
                                            Text(
                                                text = if (currentScreen is Screen.Collections) "[ COLLECT ]" else "COLLECT",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (currentScreen is Screen.Collections) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NothingWhite,
                                            selectedTextColor = NothingWhite,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = NothingMuted,
                                            unselectedTextColor = NothingMuted
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Insights,
                                        onClick = { currentScreen = Screen.Insights },
                                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Insights") },
                                        label = {
                                            Text(
                                                text = if (currentScreen is Screen.Insights) "[ INSIGHTS ]" else "INSIGHTS",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (currentScreen is Screen.Insights) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NothingWhite,
                                            selectedTextColor = NothingWhite,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = NothingMuted,
                                            unselectedTextColor = NothingMuted
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.History,
                                        onClick = { currentScreen = Screen.History },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = {
                                            Text(
                                                text = if (currentScreen is Screen.History) "[ HISTORY ]" else "HISTORY",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (currentScreen is Screen.History) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NothingWhite,
                                            selectedTextColor = NothingWhite,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = NothingMuted,
                                            unselectedTextColor = NothingMuted
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Settings,
                                        onClick = { currentScreen = Screen.Settings },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = {
                                            Text(
                                                text = if (currentScreen is Screen.Settings) "[ SETTINGS ]" else "SETTINGS",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (currentScreen is Screen.Settings) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NothingWhite,
                                            selectedTextColor = NothingWhite,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = NothingMuted,
                                            unselectedTextColor = NothingMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NothingBlack)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Collections -> {
                                CollectionsScreen(
                                    outstandingList = outstandingList,
                                    pendingList = pendingList,
                                    commissionRatePerThousand = appSettings.commissionRatePerThousand,
                                    initialOpenQuickCapture = openQuickCaptureDirectly,
                                    onAddCollectionClick = { currentScreen = Screen.AddCollection },
                                    onQuickCaptureSave = { name, amountPaise, note ->
                                        scope.launch {
                                            val customerId = customerRepo.addCustomer(name, null)
                                            collectionRepo.createPendingCollection(
                                                customerId = customerId,
                                                amountPaise = amountPaise,
                                                commissionRateSnapshot = appSettings.commissionRatePerThousand,
                                                note = note
                                            )
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            Toast.makeText(this@MainActivity, "Saved: $name", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCollectionClick = { id -> currentScreen = Screen.Detail(id) },
                                    onReceiveAndWhatsApp = { item ->
                                        scope.launch {
                                            // 1. Commit receipt before WhatsApp intent
                                            val committed = collectionRepo.markReceivedAndCommit(item.id)
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)

                                            // 2. Immediate notification
                                            notificationManager.showImmediateReceiptNotification(committed)

                                            // 3. Launch WhatsApp with customizable template
                                            val msg = WhatsAppLauncher.buildReceiptMessage(
                                                committed,
                                                appSettings.messageTemplate
                                            )
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
                                            val msg = WhatsAppLauncher.buildReceiptMessage(
                                                item,
                                                appSettings.messageTemplate
                                            )
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
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            Toast.makeText(this@MainActivity, "Confirmed Sent", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSettingsClick = { currentScreen = Screen.Settings }
                                )
                            }

                            is Screen.Insights -> {
                                InsightsScreen(collections = historyList)
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
                                    onSaveCollection = { customerId, amountPaise, note ->
                                        scope.launch {
                                            collectionRepo.createPendingCollection(
                                                customerId = customerId,
                                                amountPaise = amountPaise,
                                                commissionRateSnapshot = appSettings.commissionRatePerThousand,
                                                note = note
                                            )
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
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
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            notificationManager.showImmediateReceiptNotification(committed)

                                            val msg = WhatsAppLauncher.buildReceiptMessage(
                                                committed,
                                                appSettings.messageTemplate
                                            )
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
                                            val msg = WhatsAppLauncher.buildReceiptMessage(
                                                target,
                                                appSettings.messageTemplate
                                            )
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
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            Toast.makeText(this@MainActivity, "Confirmed Sent", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onVoidAndReplace = { originalId, reason, newAmountPaise, note ->
                                        scope.launch {
                                            val newId = collectionRepo.voidAndReplace(
                                                originalId,
                                                reason,
                                                newAmountPaise,
                                                appSettings.commissionRatePerThousand,
                                                note
                                            )
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            Toast.makeText(this@MainActivity, "Voided & replaced with #$newId", Toast.LENGTH_SHORT).show()
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
                                    onSaveTemplate = { template ->
                                        scope.launch {
                                            settingsRepo.updateMessageTemplate(template)
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
