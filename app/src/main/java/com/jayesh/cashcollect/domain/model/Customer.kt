package com.jayesh.cashcollect.domain.model

data class Customer(
    val id: Long = 0L,
    val name: String,
    val alias: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (alias.isNullOrBlank()) name else "$name ($alias)"
}
