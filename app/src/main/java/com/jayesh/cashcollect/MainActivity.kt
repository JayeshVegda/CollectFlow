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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import com.jayesh.cashcollect.ui.add.AddCollectionScreen
import com.jayesh.cashcollect.ui.collections.CollectRoute
import com.jayesh.cashcollect.ui.collections.CollectViewModel
import com.jayesh.cashcollect.ui.detail.CollectionDetailScreen
import com.jayesh.cashcollect.ui.history.HistoryRoute
import com.jayesh.cashcollect.ui.history.HistoryViewModel
import com.jayesh.cashcollect.ui.insights.InsightsScreen
import com.jayesh.cashcollect.ui.party.PartyLedgerRoute
import com.jayesh.cashcollect.ui.party.PartyLedgerViewModel
import com.jayesh.cashcollect.ui.settings.SettingsRoute
import com.jayesh.cashcollect.ui.settings.SettingsViewModel
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.CashCollectTheme
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Collections : Screen()
    object Insights : Screen()
    object History : Screen()
    object Settings : Screen()
    object AddCollection : Screen()
    data class Detail(val collectionId: Long) : Screen()

    /**
     * One party's ledger. Navigation carries the party id, so the page can be opened from any
     * screen that shows a party name.
     */
    data class PartyLedger(val customerId: Long) : Screen()
}

/*
 * Back-stack tokens
 * -----------------
 * The stack is held as plain strings so `rememberSaveable` can persist it with no custom `Saver`,
 * which is the same reason the old code used a token — the difference is that a *list* of tokens is
 * a stack, and a single token was not.
 *
 * [encode] is the only producer of these values; [decodeScreen] is the only consumer.
 */

private const val TOKEN_COLLECTIONS = "collections"
private const val TOKEN_INSIGHTS = "insights"
private const val TOKEN_HISTORY = "history"
private const val TOKEN_SETTINGS = "settings"
private const val TOKEN_ADD = "add"
private const val TOKEN_DETAIL_PREFIX = "detail:"
private const val TOKEN_PARTY_PREFIX = "party:"

/** The four tab destinations. Selecting one resets the stack; it is not a drill-in. */
private fun Screen.isTab(): Boolean =
    this is Screen.Collections || this is Screen.Insights ||
        this is Screen.History || this is Screen.Settings

private fun Screen.encode(): String {
    val screen = this
    return when (screen) {
        Screen.Collections -> TOKEN_COLLECTIONS
        Screen.Insights -> TOKEN_INSIGHTS
        Screen.History -> TOKEN_HISTORY
        Screen.Settings -> TOKEN_SETTINGS
        Screen.AddCollection -> TOKEN_ADD
        is Screen.Detail -> TOKEN_DETAIL_PREFIX + screen.collectionId
        is Screen.PartyLedger -> TOKEN_PARTY_PREFIX + screen.customerId
    }
}

/**
 * Decodes one token, falling back to the queue.
 *
 * Unrecognised or malformed tokens resolve to [Screen.Collections] rather than throwing. This runs
 * while restoring state written by an earlier process, so a token from an older build — or a
 * corrupted bundle — must never crash the app or land the operator on "Collection not found".
 */
private fun decodeScreen(token: String): Screen = when {
    token == TOKEN_INSIGHTS -> Screen.Insights
    token == TOKEN_HISTORY -> Screen.History
    token == TOKEN_SETTINGS -> Screen.Settings
    token == TOKEN_ADD -> Screen.AddCollection
    token.startsWith(TOKEN_DETAIL_PREFIX) ->
        parsePositiveId(token, TOKEN_DETAIL_PREFIX)?.let { Screen.Detail(it) } ?: Screen.Collections
    token.startsWith(TOKEN_PARTY_PREFIX) ->
        parsePositiveId(token, TOKEN_PARTY_PREFIX)?.let { Screen.PartyLedger(it) } ?: Screen.Collections
    else -> Screen.Collections
}

private fun parsePositiveId(token: String, prefix: String): Long? =
    token.removePrefix(prefix).toLongOrNull()?.takeIf { it > 0L }

class MainActivity : ComponentActivity() {

    /**
     * Deep links arrive as one-shot events, not as state.
     *
     * This used to be a `MutableStateFlow<Intent?>`, and `StateFlow` de-duplicates by `equals`. Two
     * taps on the same notification action produce *equal* `Intent`s, so the second tap was
     * swallowed: the entry stayed unreported and the action looked broken. A buffered channel
     * delivers every intent exactly once.
     */
    private val deepLinkEvents = Channel<Intent>(Channel.BUFFERED)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Notifications are disabled — receipt reminders will not appear.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkEvents.trySend(intent)
    }

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
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val app = application as CashCollectApplication
        val initialCollectionId = intent.getLongExtra("EXTRA_COLLECTION_ID", -1L)
        val openAddDirectly = intent.getBooleanExtra("EXTRA_OPEN_ADD", false)
        val openQuickCaptureDirectly = intent.getBooleanExtra("EXTRA_OPEN_QUICK_CAPTURE", false)

        // The launch intent goes through the same path as a warm one, so a notification action
        // behaves identically whether the app was already open or is starting cold.
        //
        // A genuine cold start only: on a configuration change the back stack is restored by
        // `rememberSaveable`, and replaying the launch intent would push the same screen a second
        // time or reopen the capture sheet the operator had already dismissed.
        if (savedInstanceState == null) {
            deepLinkEvents.trySend(intent)
        }

        setContent {
            CompositionLocalProvider(
                androidx.lifecycle.compose.LocalLifecycleOwner provides this@MainActivity,
                androidx.compose.ui.platform.LocalLifecycleOwner provides this@MainActivity
            ) {
                LaunchedEffect(Unit) {
                    requestNotificationPermissionIfNeeded()
                }
                CashCollectTheme {
                    val collectViewModel: CollectViewModel = viewModel(
                    factory = CollectViewModel.Factory(
                        collectionRepo = app.collectionRepository,
                        customerRepo = app.customerRepository,
                        settingsRepo = app.settingsRepository,
                        notificationManager = app.notificationManager
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
                        collectionRepo = app.collectionRepository,
                        csvExporter = app.csvExporter,
                        csvImporter = app.csvImporter
                    )
                )

                // ---------------------------------------------------------------------------
                // Navigation: one app-owned back stack.
                //
                // This replaces a `screenToken` string plus two loose Longs. That shape could not
                // express "where did I come from", so every back action was hardcoded to the queue
                // and a Detail -> Party -> back sequence jumped two levels at once. It also could
                // not survive process death *as a stack*, because nothing recorded the order.
                //
                // This is the same principle Navigation 3 is built on — the back stack is app-owned
                // state that the UI renders — implemented without adding a dependency.
                // ---------------------------------------------------------------------------
                var backStack by rememberSaveable {
                    mutableStateOf(
                        listOf(
                            when {
                                initialCollectionId > 0L -> Screen.Detail(initialCollectionId).encode()
                                openAddDirectly -> Screen.AddCollection.encode()
                                else -> Screen.Collections.encode()
                            }
                        )
                    )
                }
                val currentScreen: Screen = remember(backStack) { decodeScreen(backStack.last()) }

                val goTo: (Screen) -> Unit = { target ->
                    backStack = if (target.isTab()) {
                        // Switching tabs starts that tab cleanly: backing out of a tab should leave
                        // the app, not replay a trail of previously visited tabs.
                        listOf(target.encode())
                    } else {
                        // A drill-in stacks, so back returns to whatever opened it.
                        backStack + target.encode()
                    }
                }

                // Reads only `backStack`, so it stays correct even when captured by a long-lived
                // effect rather than being recreated on every recomposition.
                val goBack: () -> Unit = {
                    backStack = when {
                        backStack.size > 1 -> backStack.dropLast(1)
                        decodeScreen(backStack.last()) !is Screen.Collections ->
                            listOf(Screen.Collections.encode())
                        else -> backStack
                    }
                }

                var shouldOpenQuickCapture by rememberSaveable {
                    mutableStateOf(openQuickCaptureDirectly)
                }
                // A party page hands the capture sheet the party's name, so a repeat party is one
                // tap on the ledger and then straight to the amount.
                var quickCapturePrefill by rememberSaveable { mutableStateOf("") }

                // Collected as events, in order, once each. `LaunchedEffect(Unit)` is correct here
                // precisely because the channel — not this composition — owns the queue of intents.
                LaunchedEffect(Unit) {
                    deepLinkEvents.receiveAsFlow().collect { i ->
                        val collectionId = i.getLongExtra("EXTRA_COLLECTION_ID", -1L)
                        val markReportedId = i.getLongExtra(
                            AppNotificationManager.EXTRA_MARK_REPORTED_ID,
                            -1L
                        )
                        when {
                            // One tap on the notification's "YES, REPORTED" action closes the loop:
                            // the entry moves to REPORTED and the queue is re-shown.
                            markReportedId > 0L -> {
                                collectViewModel.confirmSent(this@MainActivity, markReportedId)
                                goTo(Screen.Collections)
                            }
                            collectionId > 0L -> goTo(Screen.Detail(collectionId))
                            i.getBooleanExtra("EXTRA_OPEN_QUICK_CAPTURE", false) -> {
                                goTo(Screen.Collections)
                                shouldOpenQuickCapture = true
                            }
                            i.getBooleanExtra("EXTRA_OPEN_ADD", false) -> goTo(Screen.AddCollection)
                        }
                    }
                }

                // Back pops the stack. At the root of a tab it returns to the queue, and on the
                // queue itself the handler is disabled so back leaves the app — the Android
                // convention, and previously impossible: every screen intercepted back, so the app
                // could never be exited with the gesture at all.
                BackHandler(enabled = backStack.size > 1 || currentScreen !is Screen.Collections) {
                    goBack()
                }

                val historyList by historyViewModel.historyList.collectAsStateWithLifecycle()
                val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
                val recentCustomersFlow = remember(app) { app.customerRepository.getRecentCustomers(10) }
                val recentCustomers by recentCustomersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val customerSearchResults = remember { MutableStateFlow<List<Customer>>(emptyList()) }
                val searchResultsState by customerSearchResults.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var searchJob by remember { mutableStateOf<Job?>(null) }

                // A party page is a browsing surface rather than a drill-in, so the tab bar stays
                // available and there is always a one-tap way back to the queue.
                val isRootTab = currentScreen is Screen.Collections ||
                        currentScreen is Screen.Insights ||
                        currentScreen is Screen.History ||
                        currentScreen is Screen.Settings ||
                        currentScreen is Screen.PartyLedger

                Scaffold(
                    containerColor = NothingBlack,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (isRootTab) {
                            AppleBottomNavigationBar(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> goTo(screen) }
                            )
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
                                    quickCapturePrefill = quickCapturePrefill,
                                    onQuickCaptureDismissed = {
                                        shouldOpenQuickCapture = false
                                        quickCapturePrefill = ""
                                        intent.removeExtra("EXTRA_OPEN_QUICK_CAPTURE")
                                    },
                                    onAddCollectionClick = { goTo(Screen.AddCollection) },
                                    onCollectionClick = { id -> goTo(Screen.Detail(id)) },
                                    onPartyClick = { customerId -> goTo(Screen.PartyLedger(customerId)) },
                                    onSettingsClick = { goTo(Screen.Settings) }
                                )
                            }

                            is Screen.Insights -> {
                                InsightsScreen(
                                    collections = historyList,
                                    onPartyClick = { customerId -> goTo(Screen.PartyLedger(customerId)) }
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
                                    onBackClick = goBack
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
                                            onSuccess = { goTo(Screen.Collections) }
                                        )
                                    },
                                    onSaveEdit = { name, amountPaise, dateMillis, note ->
                                        // `screen` is smart-cast to Screen.Detail here, so its id is
                                        // the non-null one being displayed; `item` above is the
                                        // nullable result of find {}.
                                        collectViewModel.updateCollection(
                                            this@MainActivity,
                                            screen.collectionId,
                                            name,
                                            amountPaise,
                                            dateMillis,
                                            note
                                        )
                                    },
                                    onDeleteEntry = { id ->
                                        collectViewModel.deleteCollection(this@MainActivity, id)
                                        // The entry no longer exists, so staying here would render
                                        // "Collection not found". The queue is the right place to
                                        // land: the entry that was deleted is not somewhere to go
                                        // back to.
                                        goTo(Screen.Collections)
                                    },
                                    onPartyClick = { customerId ->
                                        goTo(Screen.PartyLedger(customerId))
                                    },
                                    onBackClick = goBack
                                )
                            }

                            is Screen.PartyLedger -> {
                                val partyViewModel: PartyLedgerViewModel = viewModel(
                                    key = "party_" + screen.customerId,
                                    factory = PartyLedgerViewModel.Factory(
                                        customerId = screen.customerId,
                                        collectionRepo = app.collectionRepository,
                                        customerRepo = app.customerRepository
                                    )
                                )
                                PartyLedgerRoute(
                                    viewModel = partyViewModel,
                                    onBackClick = goBack,
                                    onEntryClick = { id -> goTo(Screen.Detail(id)) },
                                    onNewEntryClick = { name ->
                                        quickCapturePrefill = name
                                        goTo(Screen.Collections)
                                        shouldOpenQuickCapture = true
                                    }
                                )
                            }

                            is Screen.History -> {
                                HistoryRoute(
                                    viewModel = historyViewModel,
                                    onItemClick = { id -> goTo(Screen.Detail(id)) },
                                    onPartyClick = { customerId -> goTo(Screen.PartyLedger(customerId)) }
                                )
                            }

                            is Screen.Settings -> {
                                SettingsRoute(
                                    viewModel = settingsViewModel,
                                    onBackupNow = {
                                        settingsViewModel.exportCsvBackup(this@MainActivity)
                                    },
                                    onRestoreBackup = { uri ->
                                        settingsViewModel.restoreFromCsv(this@MainActivity, uri)
                                    },
                                    onBackClick = goBack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    override fun onResume() {
        super.onResume()
        CashCollectWidgetProvider.notifyDataChanged(this)
    }
}


@Composable
private fun AppleBottomNavigationBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xF8121215),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF26262C))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleTabItem(
                    selected = currentScreen is Screen.Collections,
                    label = "Collect",
                    icon = Icons.Default.ReceiptLong,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(Screen.Collections)
                    }
                )
                AppleTabItem(
                    selected = currentScreen is Screen.Insights,
                    label = "Insights",
                    icon = Icons.Default.Analytics,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(Screen.Insights)
                    }
                )
                AppleTabItem(
                    selected = currentScreen is Screen.History,
                    label = "History",
                    icon = Icons.Default.History,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(Screen.History)
                    }
                )
                AppleTabItem(
                    selected = currentScreen is Screen.Settings,
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(Screen.Settings)
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.AppleTabItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )
    val contentColor = if (selected) NothingWhite else Color(0xFF8E8E93)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Color(0xFF222228) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = AppType.labelSm,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 11.5.sp,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}