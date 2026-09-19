package com.jayesh.cashcollect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY last_used_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers 
        WHERE name LIKE '%' || :query || '%' OR (alias IS NOT NULL AND alias LIKE '%' || :query || '%')
        ORDER BY last_used_at DESC
    """)
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    /** The party ledger observes its party, so a rename shows up without leaving the page. */
    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET last_used_at = :timestamp WHERE id = :customerId")
    suspend fun updateLastUsed(customerId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers")
    suspend fun getAllSync(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
