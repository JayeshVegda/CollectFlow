package com.jayesh.cashcollect.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import com.jayesh.cashcollect.ui.common.AppOutlinedButton
import com.jayesh.cashcollect.ui.common.AppPrimaryButton
import com.jayesh.cashcollect.ui.common.AppScreenTitle
import com.jayesh.cashcollect.ui.common.AppSectionLabel
import com.jayesh.cashcollect.ui.common.AppTextField
import com.jayesh.cashcollect.ui.common.GlassSurface
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingGlassBorder
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary
import com.jayesh.cashcollect.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBackupNow: () -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onBackClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        onSaveNumber = viewModel::saveBrotherNumber,
        onSaveTemplate = viewModel::saveMessageTemplate,
        onSaveRate = viewModel::saveCommissionRate,
        onSaveNotificationDelay = viewModel::saveNotificationDelay,
        onBackupNow = onBackupNow,
        onRestoreBackup = onRestoreBackup,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSaveNumber: (String) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onSaveRate: (Int) -> Unit,
    onSaveNotificationDelay: (Int) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onBackClick: () -> Unit
) {
    var brotherNumber by remember(settings.brotherWhatsAppNumber) {
        mutableStateOf(settings.brotherWhatsAppNumber)
    }
    var messageTemplate by remember(settings.messageTemplate) {
        mutableStateOf(if (settings.messageTemplate.isNotBlank()) settings.messageTemplate else MessageTemplateEngine.DEFAULT_TEMPLATE)
    }
    var commissionRateText by remember(settings.commissionRatePerThousand) {
        mutableStateOf(settings.commissionRatePerThousand.toString())
    }
    var notificationDelayText by remember(settings.notificationDelayMs) {
        mutableStateOf(settings.notificationDelayMs.toString())
    }
    var showSavedMessage by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    // The system file picker, not a hardcoded path: a `content://` grant is the only way to read a
    // backup the operator saved to Drive, Downloads or a chat, and it needs no runtime permission.
    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreBackup(uri) }

    val previewMessage = remember(messageTemplate) {
        MessageTemplateEngine.preview(messageTemplate)
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { AppScreenTitle("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsHeader("WHATSAPP", "Where each cash receipt is sent")

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = brotherNumber,
                        onValueChange = { brotherNumber = it },
                        label = "Recipient number",
                        placeholder = "+919510233829",
                        keyboardType = KeyboardType.Phone
                    )
                    AppPrimaryButton(
                        label = "Save",
                        icon = Icons.Default.Save,
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            onSaveNumber(brotherNumber)
                            showSavedMessage = true
                        }
                    )
                }
            }

            SettingsHeader("MESSAGE TEMPLATE", "Tap a tag to insert it")

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((tag, _) in MessageTemplateEngine.AVAILABLE_TAGS) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, NothingGlassBorder, RoundedCornerShape(6.dp))
                                    .clickable { messageTemplate += " $tag" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NothingWhite
                                )
                            }
                        }
                    }

                    AppTextField(
                        value = messageTemplate,
                        onValueChange = { messageTemplate = it },
                        label = "Message template",
                        singleLine = false,
                        minLines = 3
                    )

                    Text(
                        text = "PREVIEW",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = previewMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NothingWhite,
                            lineHeight = 18.sp
                        )
                    }

                    AppPrimaryButton(
                        label = "Save template",
                        icon = Icons.Default.Save,
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            onSaveTemplate(messageTemplate)
                            showSavedMessage = true
                        }
                    )
                }
            }

            SettingsHeader("COMMISSION", "Rate per thousand")

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = commissionRateText,
                        onValueChange = { commissionRateText = it },
                        label = "Rate per 1000",
                        keyboardType = KeyboardType.Number
                    )
                    AppPrimaryButton(
                        label = "Save rate",
                        icon = Icons.Default.Save,
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            onSaveRate(commissionRateText.toIntOrNull() ?: 3)
                            showSavedMessage = true
                        }
                    )
                }
            }

            SettingsHeader("NOTIFICATION DELAY", "When the \"Reported?\" prompt appears")

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Receiving cash opens WhatsApp immediately, so the prompt waits until the message has gone out. 3000–5000 ms suits a normal send. 0 posts it at once.",
                        fontSize = 11.sp,
                        color = NothingMuted,
                        lineHeight = 16.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (preset in listOf(0, 3000, 5000, 10000)) {
                            val selected = notificationDelayText.toIntOrNull() == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0x33FFFFFF) else Color(0x14FFFFFF))
                                    .border(
                                        1.dp,
                                        if (selected) NothingWhite else NothingGlassBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { notificationDelayText = preset.toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (preset == 0) "INSTANT" else "${preset / 1000}s",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NothingWhite
                                )
                            }
                        }
                    }

                    AppTextField(
                        value = notificationDelayText,
                        // Digits only: the field is milliseconds, so a stray sign or letter would
                        // otherwise reach the clamp as 0 and look like the save silently failed.
                        onValueChange = { input -> notificationDelayText = input.filter { it.isDigit() }.take(5) },
                        label = "Custom delay (ms)",
                        keyboardType = KeyboardType.Number
                    )

                    AppPrimaryButton(
                        label = "Save delay",
                        icon = Icons.Default.Save,
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            onSaveNotificationDelay(
                                notificationDelayText.toIntOrNull() ?: AppSettings.DEFAULT_NOTIFICATION_DELAY_MS
                            )
                            showSavedMessage = true
                        }
                    )
                }
            }

            SettingsHeader("DATA", "Export and encrypted backup")

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENCRYPTED BACKUP (AES-256)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            color = NothingWhite
                        )
                    }

                    Text(
                        text = "Last backup: ${settings.lastBackupAt?.let { dateFormat.format(Date(it)) } ?: "Never"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        AppPrimaryButton(
                            label = "Back up now",
                            onClick = onBackupNow,
                            modifier = Modifier.weight(1f)
                        )

                        AppOutlinedButton(
                            label = "Restore",
                            icon = Icons.Default.Restore,
                            accent = TextDisplay,
                            onClick = { showRestoreConfirm = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NothingAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "All data is stored offline on this phone only. The encrypted backup can be " +
                            "restored on this phone; it is locked to this phone's hardware key, so it " +
                            "cannot currently be moved to a different phone.",
                        fontSize = 12.sp,
                        color = NothingGray,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Restore replaces the whole ledger, so it asks first and spells out exactly what happens.
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, NothingRed, RoundedCornerShape(16.dp)),
            title = { Text("RESTORE FROM BACKUP", fontFamily = FontFamily.Monospace, color = NothingWhite) },
            text = {
                Text(
                    "Every entry currently in the app is replaced by the contents of the backup file. " +
                        "Nothing is merged and the current entries are not kept. Pick the .enc file you " +
                        "exported with BACK UP NOW.",
                    color = NothingGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restorePicker.launch(arrayOf("*/*"))
                }) {
                    Text("CHOOSE FILE", color = NothingRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("CANCEL", color = NothingGray)
                }
            }
        )
    }

    if (showSavedMessage) {
        AlertDialog(
            onDismissRequest = { showSavedMessage = false },
            containerColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(16.dp)),
            title = { Text("SAVED", fontFamily = FontFamily.Monospace, color = NothingWhite) },
            text = { Text("Settings updated.", color = NothingGray) },
            confirmButton = {
                TextButton(onClick = { showSavedMessage = false }) {
                    Text("OK", color = NothingRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(label: String, hint: String) {
    Column(modifier = Modifier.padding(top = Space.sm)) {
        AppSectionLabel(text = label, color = TextDisplay)
        // TextTertiary rather than NothingMuted: this hint is content the operator has to read, and
        // #666666 measures roughly 3.7:1 against the canvas — below the 4.5:1 AA floor.
        Text(text = hint, style = AppType.caption, color = TextTertiary)
    }
}