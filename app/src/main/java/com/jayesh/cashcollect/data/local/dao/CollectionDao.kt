package com.jayesh.cashcollect.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CollectionWithCustomer
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE c.status = 'RECEIPT_CONFIRMED'
        ORDER BY c.received_at DESC
    """)
    fun getOutstandingConfirmations(): Flow<List<CollectionWithCustomer>>

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE c.status = 'PENDING'
        ORDER BY c.created_at DESC
    """)
    fun getPendingCollections(): Flow<List<CollectionWithCustomer>>

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE c.customer_id = :customerId
        ORDER BY c.created_at DESC
    """)
    fun getByCustomer(customerId: Long): Flow<List<CollectionWithCustomer>>

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        ORDER BY c.created_at DESC
    """)
    fun getAllHistory(): Flow<List<CollectionWithCustomer>>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CollectionEntity?

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE c.id = :id LIMIT 1
    """)
    suspend fun getWithCustomerById(id: Long): CollectionWithCustomer?

    @Query("""
        SELECT COUNT(*) FROM collections
        WHERE customer_id = :customerId
          AND amount_paise = :amountPaise
          AND status != 'VOIDED'
          AND created_at >= :sinceTimestamp
    """)
    suspend fun countRecentDuplicates(
        customerId: Long,
        amountPaise: Long,
        sinceTimestamp: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity): Long

    @Update
    suspend fun update(collection: CollectionEntity)

    @Query("UPDATE collections SET whatsapp_opened_at = :timestamp WHERE id = :id")
    suspend fun updateWhatsAppOpenedAt(id: Long, timestamp: Long)

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE c.status = 'RECEIPT_CONFIRMED'
          AND c.received_at <= :olderThanTimestamp
    """)
    suspend fun getUnconfirmedOlderThan(olderThanTimestamp: Long): List<CollectionWithCustomer>

    @Query("SELECT * FROM collections")
    suspend fun getAllSync(): List<CollectionEntity>

    /**
     * Cheap emptiness probe. Used as the guard before pre-filling the sample history, so the
     * seeder costs one indexed count instead of loading every row on every launch.
     */
    @Query("SELECT COUNT(*) FROM collections")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>)

    /**
     * One day's realized figures, totalled inside SQLite.
     *
     * The three home-screen widgets used to pull the entire `collections` table with `getAllSync()`
     * and total it in Kotlin on every data change — every row of history, as cursors, entities and
     * objects, to produce three numbers.
     */
    @Query("""
        SELECT
            COALESCE(SUM(amount_paise), 0) AS total_paise,
            COALESCE(SUM(commission_paise), 0) AS commission_paise,
            COUNT(*) AS entry_count
        FROM collections
        WHERE status IN ('RECEIPT_CONFIRMED', 'CONFIRMED')
          AND COALESCE(received_at, created_at) >= :startOfToday
    """)
    suspend fun realizedTotalsSince(startOfToday: Long): DayTotals

    /** Cash already in hand that the brother has not been told about yet. */
    @Query("SELECT COUNT(*) FROM collections WHERE status = 'RECEIPT_CONFIRMED'")
    suspend fun countAwaitingReport(): Int

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}

/** Aggregate row returned by [CollectionDao.realizedTotalsSince]. */
data class DayTotals(
    @ColumnInfo(name = "total_paise") val totalPaise: Long,
    @ColumnInfo(name = "commission_paise") val commissionPaise: Long,
    @ColumnInfo(name = "entry_count") val entryCount: Int
)
