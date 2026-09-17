package com.jayesh.cashcollect.data.local

import androidx.room.withTransaction
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.money.SmartInputParser
import com.jayesh.cashcollect.domain.state.CollectionStatus
import java.util.Calendar

/**
 * Pre-filled operating history.
 *
 * The operator asked for their existing five entries to be in the app from the start and to stay
 * there across updates, so they are seeded once into Room — which is what makes them survive a
 * new build: the database file is upgraded, not recreated. They are written as CONFIRMED
 * (received and reported) with historical dates, so they read as a real day's ledger and never
 * leak into today's work queue.
 *
 * The only guard is "the collections table is empty":
 *  - fresh install  -> seeded
 *  - after a database reset -> refilled, same as a fresh install
 *  - once the operator has any entry of their own -> never touched again, so deleted history
 *    cannot resurrect itself
 *
 * Amounts go through [SmartInputParser] so "3.5l" means exactly what it means everywhere else in
 * the app — 3,50,000 rupees, stored as integer paise. Commission is computed from the current
 * Settings rate.
 */
object SampleHistory {

    /** One seeded entry: party, amount in the app's own shorthand, and a resolved timestamp. */
    private data class Seed(
        val party: String,
        val amountToken: String,
        val timestamp: Long
    )

    /** Evening, so a seeded day reads like a working day rather than midnight. */
    private const val SEED_HOUR = 19

    suspend fun seedIfEmpty(database: AppDatabase, commissionRatePerThousand: Int) {
        if (database.collectionDao().countAll() > 0) return

        val seeds = listOf(
            Seed("Sambhu", "3.5l", yesterday()),
            Seed("Sambhu", "4.50l", lastTuesday()),
            Seed("Naveenbhai", "2.5l", lastTuesday()),
            Seed("Adeshbhai", "4l", september(11)),
            Seed("Sambhu", "6l", september(12))
        )

        database.withTransaction {
            for (seed in seeds) {
                val amountPaise = SmartInputParser.parseAmountToken(seed.amountToken) ?: continue
                database.collectionDao().insert(
                    CollectionEntity(
                        customerId = customerId(database, seed.party),
                        amountPaise = amountPaise,
                        commissionRateSnapshot = commissionRatePerThousand,
                        commissionPaise = CommissionCalculator.calculate(
                            amountPaise,
                            commissionRatePerThousand
                        ),
                        status = CollectionStatus.CONFIRMED.name,
                        createdAt = seed.timestamp,
                        receivedAt = seed.timestamp,
                        whatsappOpenedAt = seed.timestamp,
                        confirmedSentAt = seed.timestamp
                    )
                )
            }
        }
    }

    /** Reuses the party if it already exists, so seeding can never create a duplicate customer. */
    private suspend fun customerId(database: AppDatabase, name: String): Long {
        database.customerDao().findByName(name)?.let { return it.id }
        return database.customerDao().insert(CustomerEntity(name = name, alias = null))
    }

    /** Today's clock at the seeding hour. */
    private fun todayAtSeedHour(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, SEED_HOUR)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun yesterday(): Long = todayAtSeedHour().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    /**
     * The most recent Tuesday before today. The walk steps back before it looks, so on a Tuesday
     * this resolves to the previous week rather than to today.
     */
    private fun lastTuesday(): Long = todayAtSeedHour().apply {
        do {
            add(Calendar.DAY_OF_YEAR, -1)
        } while (get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY)
    }.timeInMillis

    /** The requested September day of this year, or of last year when that day is still ahead. */
    private fun september(day: Int): Long {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val thisYear = dateIn(currentYear, day)
        return if (thisYear <= System.currentTimeMillis()) thisYear else dateIn(currentYear - 1, day)
    }

    private fun dateIn(year: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, Calendar.SEPTEMBER, day, SEED_HOUR, 0, 0)
    }.timeInMillis
}