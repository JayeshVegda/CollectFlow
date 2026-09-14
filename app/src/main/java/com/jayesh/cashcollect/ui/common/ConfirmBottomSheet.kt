package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmBottomSheet(
    collection: CollectionItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirmReceiveAndWhatsApp: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CONFIRM CASH RECEIPT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = NothingGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NothingCardRaised),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Customer", color = NothingGray, fontSize = 13.sp)
                        Text(
                            text = collection.customerDisplayName,
                            fontWeight = FontWeight.SemiBold,
                            color = NothingWhite
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Cash Amount", color = NothingGray, fontSize = 13.sp)
                        Text(
                            text = Paise(collection.amountPaise).toFormattedRupees(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = NothingWhite
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Commission to Pay", color = NothingGray, fontSize = 13.sp)
                        Text(
                            text = Paise(collection.commissionPaise).toFormattedRupees(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = NothingRed
                        )
                    }

                    if (!collection.note.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Note", color = NothingGray, fontSize = 13.sp)
                            Text(
                                text = collection.note,
                                color = NothingWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Receipt will be recorded locally and WhatsApp will open.",
                fontSize = 12.sp,
                color = NothingGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirmReceiveAndWhatsApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "RECEIVE & OPEN WHATSAPP",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NothingBorderVisible)
            ) {
                Text("CANCEL", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingGray)
            }
        }
    }
}
