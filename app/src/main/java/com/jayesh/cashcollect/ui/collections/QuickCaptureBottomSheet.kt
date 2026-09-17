package com.jayesh.cashcollect.ui.collections

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.money.SmartInputParser
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextTertiary

/*
 * QUICK CAPTURE — the fastest path to a new entry.
 *
 * One free-text field ("sambhu 400"), two pill rows, one preview line, save. The pills exist
 * because a repeat party is the common case: the PARTY row is the customers this operator
 * actually deals with, most-recently-used first, so the first pill is usually the right tap.
 * Both pill rows route through [mergeCaptureInput], so tapping a party keeps an amount that is
 * already typed and tapping an amount keeps the party — either order, nothing clobbered.
 *
 * Deliberately not here: a written explanation of the shorthand syntax (the two pill rows
 * demonstrate both halves of it), and the old three-row breakdown of what was parsed, which
 * only restated the input. The preview line below the pills replaces it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureBottomSheet(
    sheetState: SheetState,
    commissionRatePerThousand: Int,
    recentCustomers: List<Customer> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmSave: (customerName: String, amountPaise: Long, note: String?) -> Unit,
    onOpenFullForm: () -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    val parsed = remember(rawInput) { SmartInputParser.parse(rawInput) }

    // The field takes focus the moment the sheet opens, so the keyboard is already up. The sheet
    // is opened in order to type; making the operator tap the field first buys nothing.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // A parse counts only once it has both halves. The sheet previews and saves this one value
    // rather than re-parsing at each call site, so the preview can never disagree with what
    // actually gets stored.
    val preview = parsed?.takeIf {
        it.customerName.isNotBlank() && it.amountPaise > 0L
    }
    val computedCommission = preview?.let {
        CommissionCalculator.calculate(it.amountPaise, commissionRatePerThousand)
    } ?: 0L

    fun submit() {
        val entry = preview ?: return
        onConfirmSave(entry.customerName, entry.amountPaise, noteInput.ifBlank { null })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingCard,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = Space.md)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(NothingBorderVisible)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md)
                .padding(bottom = Space.lg)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(Space.sm))
                    Text(
                        text = "QUICK CAPTURE",
                        style = AppType.labelMonoLarge,
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

            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "party + amount",
                        style = AppType.bodySm,
                        color = NothingMuted
                    )
                },
                trailingIcon = {
                    if (rawInput.isNotBlank()) {
                        IconButton(onClick = { rawInput = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = NothingGray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(Radius.chip),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = if (preview != null) NothingGreen else NothingBorderVisible,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )

            if (recentCustomers.isNotEmpty()) {
                PillRow(
                    label = "PARTY",
                    values = recentCustomers.map { it.name },
                    onPick = { name -> rawInput = mergeCaptureInput(rawInput, name = name) }
                )
            }

            PillRow(
                label = "AMOUNT",
                values = AMOUNT_PRESETS,
                onPick = { amount -> rawInput = mergeCaptureInput(rawInput, amount = amount) }
            )

            if (preview != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = preview.customerName,
                        style = AppType.subheading,
                        color = TextDisplay,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    AmountText(
                        amountPaise = preview.amountPaise,
                        style = AppType.amountLarge,
                        color = TextDisplay
                    )
                    Spacer(modifier = Modifier.width(Space.sm))
                    Text(
                        text = "comm " + Paise(computedCommission).toFormattedRupees(),
                        style = AppType.labelMono,
                        color = NothingGreen
                    )
                }
            }

            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.md),
                placeholder = {
                    Text(
                        text = "note (optional)",
                        style = AppType.bodySm,
                        color = NothingMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(Radius.chip),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = NothingBorderVisible,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                )
            )

            Spacer(modifier = Modifier.height(Space.lg))

            Button(
                onClick = { submit() },
                enabled = preview != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Space.touchTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = Color.Black,
                    disabledContainerColor = NothingBorderVisible,
                    disabledContentColor = NothingMuted
                ),
                shape = RoundedCornerShape(Radius.pill)
            ) {
                Text(
                    text = "CONFIRM & SAVE",
                    style = AppType.labelMono,
                    color = if (preview != null) Color.Black else NothingMuted
                )
            }

            Spacer(modifier = Modifier.height(Space.sm))

            OutlinedButton(
                onClick = onOpenFullForm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Space.touchTarget),
                shape = RoundedCornerShape(Radius.pill),
                border = BorderStroke(1.dp, NothingBorderVisible)
            ) {
                Text(
                    text = "OPEN FULL FORM",
                    style = AppType.labelMono,
                    color = NothingGray
                )
            }
        }
    }
}

/** Amount shorthands, in the Indian business form the operator already types. */
private val AMOUNT_PRESETS = listOf("50k", "1L", "2L", "4L", "5L", "10L", "20L", "50L")

/**
 * A horizontally scrolling row of tappable pills with a small mono label above it.
 *
 * Pills are 36dp tall rather than 48dp: two of these rows sit above the keyboard on a small
 * screen, the pills are wide enough to hit comfortably, and height is the only thing that
 * stops the sheet pushing CONFIRM & SAVE off-screen.
 */
@Composable
private fun PillRow(
    label: String,
    values: List<String>,
    onPick: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.md)
    ) {
        Text(text = label, style = AppType.labelMono, color = TextTertiary)
        Spacer(modifier = Modifier.height(Space.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            for (value in values) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPick(value)
                        }
                        .border(1.dp, NothingBorderVisible, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = Space.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = value, style = AppType.bodySm, color = TextDisplay)
                }
            }
        }
    }
}

/**
 * Merges a tapped pill into the capture input without clobbering the other half.
 *
 * Quick Capture is a single text field, so a pill must never destroy what is already typed:
 * tapping "Mahesh" over "4L" has to leave "Mahesh 4L", and tapping "4L" over "Mahesh" has to
 * leave "Mahesh 4L". The last token that parses as an amount is the amount and everything else
 * is the party name — the same rule [SmartInputParser] applies when saving, so the field always
 * reads back the way it will be stored.
 */
private fun mergeCaptureInput(
    current: String,
    name: String? = null,
    amount: String? = null
): String {
    val tokens = current.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val typedAmount = tokens.lastOrNull { SmartInputParser.parseAmountToken(it) != null }
    val typedName = tokens
        .filter { SmartInputParser.parseAmountToken(it) == null }
        .joinToString(" ")
    val nextName = name ?: typedName
    val nextAmount = amount ?: typedAmount
    return listOfNotNull(nextName.takeIf { it.isNotBlank() }, nextAmount).joinToString(" ")
}