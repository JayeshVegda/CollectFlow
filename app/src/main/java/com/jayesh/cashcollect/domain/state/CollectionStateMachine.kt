package com.jayesh.cashcollect.domain.state

/**
 * State machine transition validator.
 * Enforces business rules for collection lifecycle.
 */
object CollectionStateMachine {

    /**
     * Checks if a transition from [current] to [target] is valid.
     */
    fun canTransition(current: CollectionStatus, target: CollectionStatus): Boolean {
        return when (current) {
            CollectionStatus.PENDING -> target == CollectionStatus.RECEIPT_CONFIRMED || target == CollectionStatus.VOIDED
            CollectionStatus.RECEIPT_CONFIRMED -> target == CollectionStatus.CONFIRMED || target == CollectionStatus.VOIDED
            CollectionStatus.CONFIRMED -> target == CollectionStatus.VOIDED
            CollectionStatus.VOIDED -> false // Terminal
        }
    }

    /**
     * Validates transition or throws [IllegalStateException].
     */
    fun assertValidTransition(current: CollectionStatus, target: CollectionStatus) {
        if (!canTransition(current, target)) {
            throw IllegalStateException("Illegal state transition from $current to $target")
        }
    }

    /**
     * Reopening WhatsApp should only be permitted in RECEIPT_CONFIRMED or PENDING,
     * but does NOT alter the collection status.
     */
    fun canReopenWhatsApp(current: CollectionStatus): Boolean {
        return current == CollectionStatus.RECEIPT_CONFIRMED || current == CollectionStatus.CONFIRMED
    }

    /**
     * A record can only be voided if a non-blank reason is supplied.
     */
    fun validateVoidReason(reason: String?): String {
        val trimmed = reason?.trim().orEmpty()
        require(trimmed.isNotEmpty()) { "Void reason is mandatory to correct or void a collection record" }
        return trimmed
    }
}
