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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSaveNumber: (String) -> Unit,
    onSaveTemplate: (String) -> Unit,
    onSaveRate: (Int) -> Unit,
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
    var showSavedMessage by remember { mutableStateOf(false) }

    val previewMessage = remember(messageTemplate) {
        MessageTemplateEngine.preview(messageTemplate)
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
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
                .padding(16.dp),
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
                            focusedBorderColor = NothingRed,
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
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE NUMBER", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
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
                            focusedBorderColor = NothingRed,
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
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE TEMPLATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
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
                            focusedBorderColor = NothingRed,
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
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE RATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
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
        }
    }

    if (showSavedMessage) {
        AlertDialog(
            onDismissRequest = { showSavedMessage = false },
            title = { Text("SETTINGS SAVED", fontFamily = FontFamily.Monospace) },
            text = { Text("Settings have been updated successfully.", color = NothingGray) },
            confirmButton = {
                TextButton(onClick = { showSavedMessage = false }) {
                    Text("OK", color = NothingRed)
                }
            }
        )
    }
}
