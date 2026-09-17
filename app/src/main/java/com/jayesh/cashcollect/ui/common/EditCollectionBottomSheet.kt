package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary
import com.jayesh.cashcollect.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Edit an entry: party, date, amount, note.
 *
 * The operator asked to be able to fix all four, because a wrong name or a wrong date is a data
 * problem, not a state problem — before this the only way to correct one was to void the entry
 * and re-enter it, which cost more taps than the correction was worth. Anything except a VOIDED
 * entry can be edited; a VOIDED entry is the audit trail of a correction that already happened,
 * so it stays frozen.
 *
 * DATE offers the two answers that come up constantly as one-tap chips and a real picker for
 * everything else, so backdating to "11 Sept" never means hunting a date spinner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionBottomSheet(
    collection: CollectionItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (customerName: String, amountPaise: Long, dateMillis: Long, note: String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(collection) { mutableStateOf(collection.customerName) }
    var rawDigits by remember(collection) {
        mutableStateOf((collection.amountPaise / 100).toString())
    }
    var dateMillis by remember(collection) {
        mutableStateOf(collection.receivedAt ?: collection.createdAt)
    }
    var note by remember(collection) { mutableStateOf(collection.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val amountPaise = (rawDigits.toLongOrNull() ?: 0L) * 100L
    val trimmedName = name.trim()
    val canSave = trimmedName.isNotBlank() && amountPaise > 0L

    // Hard delete is only legal for an entry that was never money in hand; a received entry is
    // corrected by voiding it (swipe left) so the audit trail survives. The button is hidden
    // rather than failing on tap.
    val canDelete = collection.status == CollectionStatus.PENDING ||
        collection.status == CollectionStatus.VOIDED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingCard,
        contentColor = TextDisplay,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = Space.md)
                    .width(40.dp)
                    .height(4.dp)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "EDIT ENTRY", style = AppType.labelMonoLarge, color = TextDisplay)
                StatusBadge(status = collection.status)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("PARTY", style = AppType.labelMono, color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(Radius.chip),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = TextDisplay,
                    unfocusedTextColor = TextDisplay
                )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "DATE", style = AppType.labelMono, color = TextSecondary)
                Spacer(modifier = Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    DateOption(
                        label = "TODAY",
                        selected = isSameLocalDay(dateMillis, todayMillis()),
                        onClick = { dateMillis = moveToLocalDate(dateMillis, todayMillis()) }
                    )
                    DateOption(
                        label = "YESTERDAY",
                        selected = isSameLocalDay(dateMillis, yesterdayMillis()),
                        onClick = { dateMillis = moveToLocalDate(dateMillis, yesterdayMillis()) }
                    )
                    DateOption(
                        label = "PICK DATE",
                        selected = false,
                        onClick = { showDatePicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(Space.xs))
                Text(text = dateLabel(dateMillis), style = AppType.caption, color = TextTertiary)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "AMOUNT", style = AppType.labelMono, color = TextSecondary)
                Spacer(modifier = Modifier.height(Space.xs))
                AmountText(
                    amountPaise = amountPaise,
                    style = AppType.amountLarge,
                    color = TextDisplay,
                    align = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(Space.sm))
                AmountKeypad(
                    onDigitClick = { digit ->
                        rawDigits = if (rawDigits == "0") digit else rawDigits + digit
                    },
                    onBackspaceClick = {
                        if (rawDigits.isNotEmpty()) rawDigits = rawDigits.dropLast(1)
                    },
                    onClearClick = { rawDigits = "" }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text("NOTE (OPTIONAL)", style = AppType.labelMono, color = TextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(Radius.chip),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingBlack,
                    unfocusedContainerColor = NothingBlack,
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorderVisible,
                    focusedTextColor = TextDisplay,
                    unfocusedTextColor = TextDisplay
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                if (canDelete) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(Space.touchTarget),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                        shape = RoundedCornerShape(Radius.pill),
                        border = BorderStroke(1.dp, NothingRed)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NothingRed
                        )
                        Spacer(modifier = Modifier.width(Space.xs))
                        Text(text = "DELETE", style = AppType.labelMono, color = NothingRed)
                    }
                }

                Button(
                    onClick = {
                        if (canSave) {
                            onSave(
                                trimmedName,
                                amountPaise,
                                dateMillis,
                                note.trim().ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(Space.touchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(Radius.pill),
                    enabled = canSave
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(Space.xs))
                    Text(text = "SAVE", style = AppType.labelMono, color = Color.Black)
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = pickerMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = NothingBlack),
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            dateMillis = moveToPickedDate(dateMillis, picked)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingWhite,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(Radius.pill)
                ) {
                    Text(text = "SET", style = AppType.labelMono, color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    shape = RoundedCornerShape(Radius.pill),
                    border = BorderStroke(1.dp, NothingBorderVisible)
                ) {
                    Text(text = "CANCEL", style = AppType.labelMono, color = TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NothingBlack,
                    titleContentColor = TextDisplay,
                    headlineContentColor = TextDisplay
                )
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NothingBlack,
            title = {
                Text(text = "DELETE ENTRY?", style = AppType.labelMonoLarge, color = TextDisplay)
            },
            text = {
                Text(
                    text = collection.customerDisplayName + " · " +
                        Paise(collection.amountPaise).toFormattedRupees(),
                    style = AppType.bodySm,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NothingRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(Radius.pill)
                ) {
                    Text(text = "DELETE", style = AppType.labelMono, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(Radius.pill),
                    border = BorderStroke(1.dp, NothingBorderVisible)
                ) {
                    Text(text = "CANCEL", style = AppType.labelMono, color = TextSecondary)
                }
            }
        )
    }
}
/**
 * A one-tap date chip. The selected chip is a filled pill, so which date will be saved is never
 * ambiguous — which matters most on the backdating path.
 */
@Composable
private fun DateOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (selected) NothingWhite else Color.Transparent)
            .clickable { onClick() }
            .border(
                1.dp,
                if (selected) NothingWhite else NothingBorderVisible,
                RoundedCornerShape(Radius.pill)
            )
            .padding(horizontal = Space.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = AppType.labelMono,
            color = if (selected) Color.Black else TextDisplay
        )
    }
}

private fun todayMillis(): Long = System.currentTimeMillis()

private fun yesterdayMillis(): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, -1)
}.timeInMillis

private fun isSameLocalDay(timestamp: Long, other: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = timestamp }
    val b = Calendar.getInstance().apply { timeInMillis = other }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/** Human date for the chip row: Today / Yesterday, else the calendar date. */
private fun dateLabel(timestamp: Long): String = when {
    isSameLocalDay(timestamp, todayMillis()) -> "Today"
    isSameLocalDay(timestamp, yesterdayMillis()) -> "Yesterday"
    else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

/** Moves only the calendar date, keeping the entry's own time of day. */
private fun moveToLocalDate(original: Long, localDateSource: Long): Long {
    val source = Calendar.getInstance().apply { timeInMillis = localDateSource }
    return mergeDate(
        original,
        source.get(Calendar.YEAR),
        source.get(Calendar.MONTH),
        source.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * Material's date picker reports the picked day as UTC midnight, so it is read back through a UTC
 * calendar before being merged — reading it as a local instant lands the entry a day early west
 * of Greenwich.
 */
private fun moveToPickedDate(original: Long, utcMidnight: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMidnight
    }
    return mergeDate(
        original,
        picked.get(Calendar.YEAR),
        picked.get(Calendar.MONTH),
        picked.get(Calendar.DAY_OF_MONTH)
    )
}

/** The picker shows a day, so it is seeded with UTC midnight of the entry's local date. */
private fun pickerMillis(timestamp: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = timestamp }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}

private fun mergeDate(original: Long, year: Int, month: Int, dayOfMonth: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = original
        val hour = get(Calendar.HOUR_OF_DAY)
        val minute = get(Calendar.MINUTE)
        val second = get(Calendar.SECOND)
        set(year, month, dayOfMonth, hour, minute, second)
    }.timeInMillis