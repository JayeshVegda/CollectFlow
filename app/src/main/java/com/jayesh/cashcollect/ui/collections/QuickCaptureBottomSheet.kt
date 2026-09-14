package com.jayesh.cashcollect.ui.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.money.SmartInputParser
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingTextPrimary
import com.jayesh.cashcollect.ui.theme.NothingWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureBottomSheet(
    sheetState: SheetState,
    commissionRatePerThousand: Int,
    onDismiss: () -> Unit,
    onConfirmSave: (customerName: String, amountPaise: Long, note: String?) -> Unit,
    onOpenFullForm: () -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    val parsedResult = remember(rawInput) { SmartInputParser.parse(rawInput) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Automatically request focus for keyboard on open
        kotlinx.coroutines.delay(100)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    val isValid = parsedResult != null &&
            parsedResult.customerName.isNotBlank() &&
            parsedResult.amountPaise > 0L

    val computedCommission = if (isValid) {
        CommissionCalculator.calculate(parsedResult!!.amountPaise, commissionRatePerThousand)
    } else 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingCard,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(NothingBorderVisible)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header (Nothing OS Minimalist)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUICK CAPTURE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingWhite
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NothingGray
                    )
                }
            }

            Text(
                text = "Enter party & shorthand (e.g. \"sambhu 400\" or \"mahesh 1.5L\")",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = NothingGray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2. Main Input Field
            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        "party name + amount...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = NothingMuted
                    )
                },
                trailingIcon = {
                    if (rawInput.isNotBlank()) {
                        IconButton(onClick = { rawInput = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = NothingGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = if (isValid) NothingGreen else NothingBorderVisible,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isValid) {
                            onConfirmSave(
                                parsedResult!!.customerName,
                                parsedResult.amountPaise,
                                noteInput.ifBlank { null }
                            )
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Shorthand Presets Bar (1-tap helper)
            Text(
                text = "PRESET SHORTHAND AMOUNTS",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = NothingMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            val presets = listOf("50k", "1L", "2L", "4L", "5L", "10L", "20L", "50L")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (preset in presets) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, NothingBorderVisible, RoundedCornerShape(999.dp))
                            .clickable {
                                // If user already has a name typed, replace/append amount
                                val tokens = rawInput.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                                rawInput = if (tokens.isEmpty()) {
                                    preset
                                } else {
                                    val nameTokens = tokens.filter {
                                        SmartInputParser.parseAmountToken(it) == null
                                    }
                                    if (nameTokens.isEmpty()) {
                                        preset
                                    } else {
                                        "${nameTokens.joinToString(" ")} $preset"
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Live Parsed Token Card
            AnimatedVisibility(visible = parsedResult != null) {
                parsedResult?.let { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NothingBorderVisible, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = NothingCardRaised),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PARSED PARTY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NothingGray,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (res.customerName.isNotBlank()) res.customerName else "—",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NothingWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CASH AMOUNT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NothingGray,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = res.formattedRupees,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COMMISSION (${commissionRatePerThousand}/1000)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NothingGray,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = Paise(computedCommission).toFormattedRupees(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Optional Note
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Note (optional, e.g. Bag #1, Angadia)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = NothingBorderVisible,
                    unfocusedBorderColor = NothingBorder,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Action Buttons (Nothing OS Primary Pill + Secondary Outlined)
            Button(
                onClick = {
                    if (isValid) {
                        onConfirmSave(
                            parsedResult!!.customerName,
                            parsedResult.amountPaise,
                            noteInput.ifBlank { null }
                        )
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = Color.Black,
                    disabledContainerColor = NothingBorderVisible,
                    disabledContentColor = NothingMuted
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "CONFIRM & SAVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onOpenFullForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorderVisible)
            ) {
                Text(
                    text = "OPEN FULL FORM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = NothingGray
                )
            }
        }
    }
}
