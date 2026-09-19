package com.jayesh.cashcollect.domain.model

data class AppSettings(
    val id: Long = 1L,
    val brotherWhatsAppNumber: String = "",
    val commissionRatePerThousand: Int = 3,
    val lastBackupAt: Long? = null,
    val messageTemplate: String = "",
    val notificationDelayMs: Int = DEFAULT_NOTIFICATION_DELAY_MS
) {
    companion object {
        /**
         * How long to wait after cash is committed before posting the "Reported?" nudge.
         *
         * Committing a receipt opens WhatsApp immediately, so a prompt posted at the same
         * instant lands on top of the app the operator is about to type in. A few seconds is
         * enough for the message to go out first, which is when the prompt becomes actionable.
         */
        const val DEFAULT_NOTIFICATION_DELAY_MS = 4_000

        /**
         * Upper bound accepted from Settings. Well past the useful 3–5 s range, but bounded so a
         * mistyped value cannot park the reminder an hour out, where it would read as a missed
         * notification rather than a prompt.
         */
        const val MAX_NOTIFICATION_DELAY_MS = 60_000
    }
}