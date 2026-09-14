package com.jayesh.cashcollect.domain.state

/**
 * Lifecycle states of a Cash Collection entry.
 */
enum class CollectionStatus {
    /**
     * Newly created collection record awaiting cash handover.
     */
    PENDING,

    /**
     * Cash has been received and committed to local database;
     * WhatsApp receipt intent dispatched, awaiting explicit user confirmation of message send.
     */
    RECEIPT_CONFIRMED,

    /**
     * Fully confirmed: cash received and WhatsApp message confirmed sent.
     */
    CONFIRMED,

    /**
     * Terminal state: Voided due to mistake or correction.
     * Original entry is preserved in history; replacements link to this via replaces_id.
     */
    VOIDED
}
