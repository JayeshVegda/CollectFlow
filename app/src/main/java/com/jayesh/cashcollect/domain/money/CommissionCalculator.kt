package com.jayesh.cashcollect.domain.money

/**
 * Pure Kotlin commission calculator using exact integer paise math.
 *
 * Non-negotiable specification:
 * Rate is represented as rate_per_thousand (e.g. 3 = 0.003).
 * Rounding: Round-half-up integer division:
 * commission_paise = (amount_paise * rate_per_thousand + 500) / 1000
 */
object CommissionCalculator {

    const val DEFAULT_RATE_PER_THOUSAND: Int = 3

    /**
     * Computes the commission in paise.
     *
     * @param amountPaise The collection amount in integer paise.
     * @param ratePerThousand The commission rate per thousand (e.g., 3 for 0.3%).
     * @return Calculated commission in integer paise, rounded half-up.
     */
    fun calculate(amountPaise: Long, ratePerThousand: Int = DEFAULT_RATE_PER_THOUSAND): Long {
        require(amountPaise >= 0L) { "Amount paise cannot be negative: $amountPaise" }
        require(ratePerThousand >= 0) { "Rate per thousand cannot be negative: $ratePerThousand" }

        if (amountPaise == 0L || ratePerThousand == 0) return 0L

        // Round-half-up: add 500 before dividing by 1000
        return (amountPaise * ratePerThousand.toLong() + 500L) / 1000L
    }

    /**
     * Type-safe overload with Paise value class.
     */
    fun calculate(amount: Paise, ratePerThousand: Int = DEFAULT_RATE_PER_THOUSAND): Paise {
        return Paise(calculate(amount.value, ratePerThousand))
    }
}
