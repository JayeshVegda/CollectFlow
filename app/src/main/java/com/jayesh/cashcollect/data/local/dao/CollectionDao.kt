package com.jayesh.cashcollect.data.local.dao

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
        ORDER BY c.created_at DESC
    """)
    fun getAllHistory(): Flow<List<CollectionWithCustomer>>

    @Query("""
        SELECT c.*, cust.name AS customer_name, cust.alias AS customer_alias
        FROM collections c
        INNER JOIN customers cust ON c.customer_id = cust.id
        WHERE (:status IS NULL OR c.status = :status)
          AND (:fromTimestamp IS NULL OR c.created_at >= :fromTimestamp)
          AND (:toTimestamp IS NULL OR c.created_at <= :toTimestamp)
          AND (:searchQuery IS NULL OR cust.name LIKE '%' || :searchQuery || '%' OR (cust.alias IS NOT NULL AND cust.alias LIKE '%' || :searchQuery || '%'))
        ORDER BY c.created_at DESC
    """)
    fun filterHistory(
        status: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        searchQuery: String?
    ): Flow<List<CollectionWithCustomer>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}
