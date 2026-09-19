package com.jayesh.cashcollect.data.repository

import com.jayesh.cashcollect.data.local.dao.CustomerDao
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.domain.model.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CustomerRepository(private val customerDao: CustomerDao) {

    fun getRecentCustomers(limit: Int = 10): Flow<List<Customer>> {
        return customerDao.getRecent(limit)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    /** One party, observed — the party ledger's header. */
    fun observeCustomer(id: Long): Flow<Customer?> {
        return customerDao.observeById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.Default)
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.search(query)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getCustomerById(id: Long): Customer? {
        return customerDao.getById(id)?.toDomain()
    }

    suspend fun addCustomer(name: String, alias: String?): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Customer name cannot be empty" }

        val existing = customerDao.findByName(trimmedName)
        if (existing != null) {
            customerDao.updateLastUsed(existing.id)
            return existing.id
        }

        val entity = CustomerEntity(
            name = trimmedName,
            alias = alias?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis()
        )
        return customerDao.insert(entity)
    }

    suspend fun touchLastUsed(customerId: Long) {
        customerDao.updateLastUsed(customerId, System.currentTimeMillis())
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        name = name,
        alias = alias,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
