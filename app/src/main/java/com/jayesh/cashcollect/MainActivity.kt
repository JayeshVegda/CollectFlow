package com.jayesh.cashcollect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.ui.add.AddCollectionScreen
import com.jayesh.cashcollect.ui.collections.CollectRoute
import com.jayesh.cashcollect.ui.collections.CollectViewModel
import com.jayesh.cashcollect.ui.detail.CollectionDetailScreen
import com.jayesh.cashcollect.ui.history.HistoryRoute
import com.jayesh.cashcollect.ui.history.HistoryViewModel
import com.jayesh.cashcollect.ui.insights.InsightsScreen
import com.jayesh.cashcollect.ui.settings.SettingsRoute
import com.jayesh.cashcollect.ui.settings.SettingsViewModel
import com.jayesh.cashcollect.ui.theme.CashCollectTheme
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingMuted
import androidx.compose.material.icons.filled.Dashboard
import com.jayesh.cashcollect.ui.dashboard.DashboardScreen
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Collections : Screen()
    object Insights : Screen()
    object Dashboard : Screen()
    object History : Screen()
    object Settings : Screen()
    object AddCollection : Screen()
    data class Detail(val collectionId: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    /** Emits each new intent so the Compose tree can react (widget quick-capture, notification tap). */
    private val deepLinkFlow = MutableStateFlow<Intent?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Notifications are disabled — receipt reminders and Telegram failure alerts will not appear.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkFlow.value = intent
    }

    /** Android 13+ requires an explicit runtime grant before any notification can be shown. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val app = application as CashCollectApplication
        val initialCollectionId = intent.getLongExtra("EXTRA_COLLECTION_ID", -1L)
        val openAddDirectly = intent.getBooleanExtra("EXTRA_OPEN_ADD", false)
        val openQuickCaptureDirectly = intent.getBooleanExtra("EXTRA_OPEN_QUICK_CAPTURE", false)

        setContent {
            CompositionLocalProvider(
                androidx.lifecycle.compose.LocalLifecycleOwner provides this,
                androidx.compose.ui.platform.LocalLifecycleOwner provides this
            ) {
                CashCollectTheme {
                val collectViewModel: CollectViewModel = viewModel(
                    factory = CollectViewModel.Factory(
                        collectionRepo = app.collectionRepository,
                        customerRepo = app.customerRepository,
                        settingsRepo = app.settingsRepository,
                        notificationManager = app.notificationManager,
                        telegramManager = app.telegramManager
                    )
                )

                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(
                        collectionRepo = app.collectionRepository,
                        csvExporter = app.csvExporter
                    )
                )

                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settingsRepo = app.settingsRepository,
                        telegramManager = app.telegramManager
                    )
                )

                var screenToken by rememberSaveable {
                    mutableStateOf(
                        when {
                            initialCollectionId > 0 -> "detail"
                            openAddDirectly -> "add"
                            else -> "collections"
                        }
                    )
                }
                var detailId by rememberSaveable { mutableStateOf(initialCollectionId) }
                val currentScreen: Screen = remember(screenToken, detailId) {
                    when (screenToken) {
                        "detail" -> Screen.Detail(detailId)
                        "add" -> Screen.AddCollection
                        "insights" -> Screen.Insights
                        "dashboard" -> Screen.Dashboard
                        "history" -> Screen.History
                        "settings" -> Screen.Settings
                        else -> Screen.Collections
                    }
                }
                val goTo: (Screen) -> Unit = { target ->
                    screenToken = when (target) {
                        is Screen.Detail -> {
                            detailId = target.collectionId
                            "detail"
                        }
                        is Screen.AddCollection -> "add"
                        is Screen.Insights -> "insights"
                        is Screen.Dashboard -> "dashboard"
                        is Screen.History -> "history"
                        is Screen.Settings -> "settings"
                        is Screen.Collections -> "collections"
                    }
                }

                var shouldOpenQuickCapture by rememberSaveable {
                    mutableStateOf(openQuickCaptureDirectly)
                }

                // Widget/notification taps arriving while the app is already alive
                // (singleTask launch mode) are delivered through onNewIntent.
                val deepLinkIntent by deepLinkFlow.collectAsStateWithLifecycle()
                LaunchedEffect(deepLinkIntent) {
                    val i = deepLinkIntent ?: return@LaunchedEffect
                    val collectionId = i.getLongExtra("EXTRA_COLLECTION_ID", -1L)
                    when {
                        collectionId > 0 -> goTo(Screen.Detail(collectionId))
                        i.getBooleanExtra("EXTRA_OPEN_QUICK_CAPTURE", false) -> {
                            goTo(Screen.Collections)
                            shouldOpenQuickCapture = true
                        }
                        i.getBooleanExtra("EXTRA_OPEN_ADD", false) -> goTo(Screen.AddCollection)
                    }
                    deepLinkFlow.value = null
                }


                BackHandler(enabled = currentScreen !is Screen.Collections) {
                    goTo(Screen.Collections)
                }

                val historyList by historyViewModel.historyList.collectAsStateWithLifecycle()
                val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
                val recentCustomers by app.customerRepository.getRecentCustomers(10).collectAsStateWithLifecycle(initialValue = emptyList())
                val customerSearchResults = remember { MutableStateFlow<List<Customer>>(emptyList()) }
                val searchResultsState by customerSearchResults.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                // One cancellable collector for customer search: a new query cancels the previous
                // Room Flow instead of leaking a collector per keystroke.
                var searchJob by remember { mutableStateOf<Job?>(null) }

                val isRootTab = currentScreen is Screen.Collections ||
                        currentScreen is Screen.Insights ||
                        currentScreen is Screen.Dashboard ||
                        currentScreen is Screen.History ||
                        currentScreen is Screen.Settings

                Scaffold(
                    containerColor = NothingBlack,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (isRootTab) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(width = 1.dp, color = NothingBorder, shape = RectangleShape)
                            ) {
                                NavigationBar(
                                    containerColor = NothingBlack,
                                    windowInsets = NavigationBarDefaults.windowInsets
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Collections,
                                        onClick = { goTo(Screen.Collections) },
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
                                        onClick = { goTo(Screen.Insights) },
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
                                        selected = currentScreen is Screen.Dashboard,
                                        onClick = { goTo(Screen.Dashboard) },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                        label = {
                                            Text(
                                                text = if (currentScreen is Screen.Dashboard) "[ DASHBOARD ]" else "DASHBOARD",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (currentScreen is Screen.Dashboard) FontWeight.Bold else FontWeight.Normal,
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
                                        onClick = { goTo(Screen.History) },
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
                                        onClick = { goTo(Screen.Settings) },
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
                                CollectRoute(
                                    viewModel = collectViewModel,
                                    initialOpenQuickCapture = shouldOpenQuickCapture,
                                    onQuickCaptureDismissed = {
                                        shouldOpenQuickCapture = false
                                        intent.removeExtra("EXTRA_OPEN_QUICK_CAPTURE")
                                    },
                                    onAddCollectionClick = { goTo(Screen.AddCollection) },
                                    onCollectionClick = { id -> goTo(Screen.Detail(id)) },
                                    onSettingsClick = { goTo(Screen.Settings) }
                                )
                            }

                            is Screen.Insights -> {
                                InsightsScreen(collections = historyList)
                            }

                            is Screen.Dashboard -> {
                                val currentSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
                                val authState by settingsViewModel.telegramAuthState.collectAsStateWithLifecycle()
                                DashboardScreen(
                                    collections = historyList,
                                    appSettings = currentSettings,
                                    telegramAuthState = authState,
                                    onQuickCaptureClick = {
                                        goTo(Screen.Collections)
                                        shouldOpenQuickCapture = true
                                    },
                                    onCollectionClick = { id -> goTo(Screen.Detail(id)) },
                                    onGoToHistory = { goTo(Screen.History) },
                                    onGoToSettings = { goTo(Screen.Settings) },
                                    onExportCsv = { historyViewModel.exportCsv(this@MainActivity) }
                                )
                            }

                            is Screen.AddCollection -> {
                                AddCollectionScreen(
                                    recentCustomers = recentCustomers,
                                    searchResults = searchResultsState,
                                    commissionRatePerThousand = appSettings.commissionRatePerThousand,
                                    onSearchCustomer = { query ->
                                        searchJob?.cancel()
                                        if (query.isBlank()) {
                                            customerSearchResults.value = emptyList()
                                        } else {
                                            searchJob = scope.launch {
                                                app.customerRepository.searchCustomers(query).collect {
                                                    customerSearchResults.value = it
                                                }
                                            }
                                        }
                                    },
                                    onAddNewCustomer = { name, alias ->
                                        app.customerRepository.addCustomer(name, alias)
                                    },
                                    onSaveCollection = { customerId, amountPaise, note ->
                                        scope.launch {
                                            app.collectionRepository.createPendingCollection(
                                                customerId = customerId,
                                                amountPaise = amountPaise,
                                                commissionRateSnapshot = appSettings.commissionRatePerThousand,
                                                note = note
                                            )
                                            CashCollectWidgetProvider.notifyDataChanged(this@MainActivity)
                                            goTo(Screen.Collections)
                                        }
                                    },
                                    onCheckDuplicate = { customerId, amountPaise ->
                                        app.collectionRepository.checkRecentDuplicate(customerId, amountPaise)
                                    },
                                    onBackClick = { goTo(Screen.Collections) }
                                )
                            }

                            is Screen.Detail -> {
                                val item = historyList.find { it.id == screen.collectionId }
                                CollectionDetailScreen(
                                    collection = item,
                                    onReceiveAndWhatsApp = { target ->
                                        collectViewModel.confirmReceiveAndOpenWhatsApp(this@MainActivity, target)
                                    },
                                    onOpenWhatsAppAgain = { target ->
                                        collectViewModel.openWhatsAppAgain(this@MainActivity, target)
                                    },
                                    onConfirmSent = { id ->
                                        collectViewModel.confirmSent(this@MainActivity, id)
                                    },
                                    onVoidAndReplace = { originalId, reason, newAmountPaise, note ->
                                        collectViewModel.voidAndReplace(
                                            context = this@MainActivity,
                                            originalId = originalId,
                                            reason = reason,
                                            newAmountPaise = newAmountPaise,
                                            note = note,
                                            onSuccess = {
                                                goTo(Screen.Collections)
                                            }
                                        )
                                    },
                                    onBackClick = { goTo(Screen.Collections) }
                                )
                            }

                            is Screen.History -> {
                                HistoryRoute(
                                    viewModel = historyViewModel,
                                    onItemClick = { id -> goTo(Screen.Detail(id)) }
                                )
                            }

                            is Screen.Settings -> {
                                SettingsRoute(
                                    viewModel = settingsViewModel,
                                    onBackupNow = {
                                        scope.launch {
                                            try {
                                                val backupFile = app.backupManager.createEncryptedBackup()
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
                                    onBackClick = { goTo(Screen.Collections) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
