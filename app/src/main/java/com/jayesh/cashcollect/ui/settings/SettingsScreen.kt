package com.jayesh.cashcollect.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.jayesh.cashcollect.service.telegram.TelegramAuthState
import com.jayesh.cashcollect.ui.theme.NothingGreen

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBackupNow: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telegramAuthState by viewModel.telegramAuthState.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        telegramAuthState = telegramAuthState,
        onSaveNumber = viewModel::saveBrotherNumber,
        onSaveTemplate = viewModel::saveMessageTemplate,
        onSaveRate = viewModel::saveCommissionRate,
        onSaveTelegram = { enabled, id, hash, recipient, fallback ->
            viewModel.saveTelegramSettings(enabled, id, hash, recipient, fallback)
        },
        onConnectTelegram = viewModel::connectTelegram,
        onCheckPassword = viewModel::checkTelegramPassword,
        onLogoutTelegram = viewModel::logoutTelegram,
        onTestSendTelegram = { viewModel.testSendTelegram(context) },
        onBackupNow = onBackupNow,
        onRestoreBackupClick = onRestoreBackupClick,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    telegramAuthState: TelegramAuthState = TelegramAuthState.Uninitialized,
    onSaveNumber: (String) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onSaveRate: (Int) -> Unit,
    onSaveTelegram: (enabled: Boolean, apiId: String, apiHash: String, recipient: String, fallbackWhatsApp: Boolean) -> Unit = { _, _, _, _, _ -> },
    onConnectTelegram: () -> Unit = {},
    onCheckPassword: (String) -> Unit = {},
    onLogoutTelegram: () -> Unit = {},
    onTestSendTelegram: () -> Unit = {},
    onBackupNow: () -> Unit,
    onRestoreBackupClick: () -> Unit,
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

    var tgEnabled by remember(settings.telegramEnabled) { mutableStateOf(settings.telegramEnabled) }
    var tgApiId by remember(settings.telegramApiId) { mutableStateOf(settings.telegramApiId) }
    var tgApiHash by remember(settings.telegramApiHash) { mutableStateOf(settings.telegramApiHash) }
    var tgRecipient by remember(settings.telegramRecipient) { mutableStateOf(settings.telegramRecipient) }
    var tgFallback by remember(settings.telegramFallbackWhatsApp) { mutableStateOf(settings.telegramFallbackWhatsApp) }
    var hashVisible by remember { mutableStateOf(false) }

    var showSavedMessage by remember { mutableStateOf(false) }

    val previewMessage = remember(messageTemplate) {
        MessageTemplateEngine.preview(messageTemplate)
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NothingWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NothingBlack
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. RECIPIENT WHATSAPP NUMBER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "WHATSAPP RECIPIENT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )
                    Text(
                        text = "Number that will receive the cash receipt (e.g. +919510233829):",
                        fontSize = 12.sp,
                        color = NothingGray
                    )

                    OutlinedTextField(
                        value = brotherNumber,
                        onValueChange = { brotherNumber = it },
                        placeholder = { Text("+919510233829", color = NothingMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    Button(
                        onClick = {
                            onSaveNumber(brotherNumber)
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingWhite,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE NUMBER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }

            // TELEGRAM DISPATCH (TDLIB)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TELEGRAM AUTO-SEND (TDLIB)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )

                        // Status pill
                        val (statusText, statusBg, statusFg) = when (telegramAuthState) {
                            is TelegramAuthState.Ready -> Triple("ONLINE", NothingGreen.copy(alpha = 0.2f), NothingGreen)
                            is TelegramAuthState.ShowingQr -> Triple("SCAN QR", NothingAmber.copy(alpha = 0.2f), NothingAmber)
                            is TelegramAuthState.WaitingPassword -> Triple("2FA REQ", NothingAmber.copy(alpha = 0.2f), NothingAmber)
                            is TelegramAuthState.WaitingParameters, is TelegramAuthState.Initializing -> Triple("CONNECTING", NothingAmber.copy(alpha = 0.2f), NothingAmber)
                            is TelegramAuthState.Error -> Triple("ERROR", NothingRed.copy(alpha = 0.2f), NothingRed)
                            is TelegramAuthState.Uninitialized, is TelegramAuthState.Closed -> Triple("OFFLINE", NothingCardRaised, NothingGray)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .border(1.dp, statusFg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = statusFg
                            )
                        }
                    }

                    Text(
                        text = "1-click automatic background dispatch from your personal Telegram account without opening the Telegram UI.",
                        fontSize = 12.sp,
                        color = NothingGray,
                        lineHeight = 16.sp
                    )

                    // Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Telegram Send", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NothingWhite)
                            Text("Auto-dispatch receipt via personal account", fontSize = 11.sp, color = NothingMuted)
                        }
                        Switch(
                            checked = tgEnabled,
                            onCheckedChange = { tgEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NothingWhite,
                                checkedTrackColor = NothingGreen,
                                uncheckedThumbColor = NothingGray,
                                uncheckedTrackColor = NothingBlack
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fallback to WhatsApp", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = NothingWhite)
                            Text("Open WhatsApp if Telegram fails or is offline", fontSize = 11.sp, color = NothingMuted)
                        }
                        Switch(
                            checked = tgFallback,
                            onCheckedChange = { tgFallback = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NothingWhite,
                                checkedTrackColor = NothingGreen,
                                uncheckedThumbColor = NothingGray,
                                uncheckedTrackColor = NothingBlack
                            )
                        )
                    }

                    // API ID
                    Text(
                        text = "API ID (from my.telegram.org):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingGray
                    )
                    OutlinedTextField(
                        value = tgApiId,
                        onValueChange = { tgApiId = it },
                        placeholder = { Text("e.g. 12345678", color = NothingMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    // API Hash
                    Text(
                        text = "API HASH (from my.telegram.org):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingGray
                    )
                    OutlinedTextField(
                        value = tgApiHash,
                        onValueChange = { tgApiHash = it },
                        placeholder = { Text("32-character hex hash", color = NothingMuted) },
                        visualTransformation = if (hashVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { hashVisible = !hashVisible }) {
                                Icon(
                                    if (hashVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = NothingGray
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    // Recipient
                    Text(
                        text = "RECIPIENT (Username @brother or phone number):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NothingGray
                    )
                    OutlinedTextField(
                        value = tgRecipient,
                        onValueChange = { tgRecipient = it },
                        placeholder = { Text("@brother or +91...", color = NothingMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveTelegram(tgEnabled, tgApiId, tgApiHash, tgRecipient, tgFallback)
                                showSavedMessage = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NothingWhite),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE CONFIG", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                        }

                        if (telegramAuthState !is TelegramAuthState.Ready) {
                            OutlinedButton(
                                onClick = onConnectTelegram,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SCAN QR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onTestSendTelegram,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TEST SEND", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                            }
                        }
                    }

                    if (telegramAuthState is TelegramAuthState.Ready) {
                        TextButton(
                            onClick = onLogoutTelegram,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("LOGOUT FROM TELEGRAM", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingRed)
                        }
                    }
                }
            }

            // 2. CUSTOMIZABLE MESSAGE TEMPLATE & LIVE PREVIEW
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MESSAGE FORMAT TEMPLATE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )

                    Text(
                        text = "Tap a tag below to insert it into your message format:",
                        fontSize = 12.sp,
                        color = NothingGray
                    )

                    // Clickable Tag Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((tag, _) in MessageTemplateEngine.AVAILABLE_TAGS) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NothingCardRaised)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
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

                    OutlinedTextField(
                        value = messageTemplate,
                        onValueChange = { messageTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    // LIVE PREVIEW BOX
                    Text(
                        text = "LIVE WHATSAPP PREVIEW",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NothingBlack, RoundedCornerShape(8.dp))
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
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

                    Button(
                        onClick = {
                            onSaveTemplate(messageTemplate)
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingWhite,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE TEMPLATE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }

            // 3. COMMISSION RATE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMMISSION RATE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )
                    Text(
                        text = "Rate per thousand (default: 3 = 0.3% / ₹3 per ₹1,000):",
                        fontSize = 12.sp,
                        color = NothingGray
                    )

                    OutlinedTextField(
                        value = commissionRateText,
                        onValueChange = { commissionRateText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )

                    Button(
                        onClick = {
                            val rate = commissionRateText.toIntOrNull() ?: 3
                            onSaveRate(rate)
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingWhite,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE RATE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    }
                }
            }

            // 4. ENCRYPTED BACKUP & RESTORE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = NothingWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENCRYPTED BACKUP (AES-256)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }

                    Text(
                        text = "Last successful backup: ${settings.lastBackupAt?.let { dateFormat.format(Date(it)) } ?: "Never"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBackupNow,
                            colors = ButtonDefaults.buttonColors(containerColor = NothingWhite),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("BACK UP NOW", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = onRestoreBackupClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = NothingWhite)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESTORE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingWhite)
                        }
                    }
                }
            }

            // 5. DATA RETENTION NOTICE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingAmberBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingAmberBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NothingAmber)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DATA RETENTION NOTICE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NothingAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All data is stored exclusively offline on your device. Take regular encrypted backups before uninstalling or updating your phone.",
                            fontSize = 12.sp,
                            color = NothingGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Generous bottom spacer ensures the entire last box is readable and accessible above any navigation bar
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showSavedMessage) {
        AlertDialog(
            onDismissRequest = { showSavedMessage = false },
            containerColor = NothingCardRaised,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(16.dp)),
            title = { Text("SETTINGS SAVED", fontFamily = FontFamily.Monospace, color = NothingWhite) },
            text = { Text("Settings have been updated successfully.", color = NothingGray) },
            confirmButton = {
                TextButton(onClick = { showSavedMessage = false }) {
                    Text("OK", color = NothingRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Telegram QR Dialog
    if (telegramAuthState is TelegramAuthState.ShowingQr) {
        val qrState = telegramAuthState as TelegramAuthState.ShowingQr
        AlertDialog(
            onDismissRequest = onLogoutTelegram,
            containerColor = NothingBlack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "LINK PERSONAL TELEGRAM",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrState.qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = "Telegram QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: androidx.compose.material3.CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "1. Open Telegram on your phone\n2. Go to Settings → Devices\n3. Tap 'Link Desktop Device'\n4. Scan this QR Code",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingGray,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onLogoutTelegram) {
                    Text("CANCEL", color = NothingRed, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // Telegram 2FA Password Dialog
    if (telegramAuthState is TelegramAuthState.WaitingPassword) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onLogoutTelegram,
            containerColor = NothingCardRaised,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, NothingBorderVisible, RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = "ENTER 2FA PASSWORD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Telegram account is protected by 2-Step Verification. Enter your cloud password to complete login:",
                        fontSize = 12.sp,
                        color = NothingGray
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onCheckPassword(passwordInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingWhite)
                ) {
                    Text("SUBMIT", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutTelegram) {
                    Text("CANCEL", color = NothingGray, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}
