package com.jayesh.cashcollect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["replaces_id"]),
        Index(value = ["replaced_by_id"])
    ]
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,
    @ColumnInfo(name = "commission_rate_snapshot")
    val commissionRateSnapshot: Int,
    @ColumnInfo(name = "commission_paise")
    val commissionPaise: Long,
    val status: String, // PENDING, RECEIPT_CONFIRMED, CONFIRMED, VOIDED
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "received_at")
    val receivedAt: Long? = null,
    @ColumnInfo(name = "whatsapp_opened_at")
    val whatsappOpenedAt: Long? = null,
    @ColumnInfo(name = "confirmed_sent_at")
    val confirmedSentAt: Long? = null,
    @ColumnInfo(name = "voided_at")
    val voidedAt: Long? = null,
    @ColumnInfo(name = "void_reason")
    val voidReason: String? = null,
    @ColumnInfo(name = "replaced_by_id")
    val replacedById: Long? = null,
    @ColumnInfo(name = "replaces_id")
    val replacesId: Long? = null,
    @ColumnInfo(name = "note")
    val note: String? = null
)
