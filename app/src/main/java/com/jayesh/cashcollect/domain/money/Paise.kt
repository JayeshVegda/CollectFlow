package com.jayesh.cashcollect.domain.money

/**
 * Value representation of currency in integer paise.
 * Floats and Doubles are strictly forbidden for currency in this codebase.
 * 1 Rupee = 100 Paise.
 */
@JvmInline
value class Paise(val value: Long) : Comparable<Paise> {

    init {
        require(value >= 0L) { "Amount in paise cannot be negative: $value" }
    }

    operator fun plus(other: Paise): Paise = Paise(value + other.value)
    operator fun minus(other: Paise): Paise {
        require(value >= other.value) { "Subtracting $other from $value results in negative paise" }
        return Paise(value - other.value)
    }

    override fun compareTo(other: Paise): Int = value.compareTo(other.value)

    /**
     * Formats paise into Indian currency format (e.g. ₹4,00,000 or ₹1,200.50).
     */
    fun toFormattedRupees(includeSymbol: Boolean = true, includePaiseIfZero: Boolean = false): String {
        val rupeesPart = value / 100L
        val paisePart = value % 100L

        val formattedRupees = formatIndianNumber(rupeesPart)
        val symbol = if (includeSymbol) "₹" else ""

        return if (paisePart == 0L && !includePaiseIfZero) {
            "$symbol$formattedRupees"
        } else {
            val paiseFormatted = if (paisePart < 10) "0$paisePart" else "$paisePart"
            "$symbol$formattedRupees.$paiseFormatted"
        }
    }

    override fun toString(): String = toFormattedRupees()

    /**
     * Compact Indian-market form for tight slots: `₹45L`, `₹1.5Cr`, `₹45,000`.
     *
     * Used where three figures must share one phone-width row and a full `₹45,00,000` cannot fit
     * beside the other two. Below one lakh the exact figure is already short enough, so rounding
     * only happens where it actually buys space.
     *
     * `L` (lakh = 10^5) and `Cr` (crore = 10^7) are the units this operator already speaks in —
     * Quick Capture's parser accepts "3.5l" for the same reason.
     *
     * DISPLAY ONLY: the result never feeds arithmetic, which is what makes the rounding safe.
     * Value formatting elsewhere in the app stays exact.
     */
    fun toCompactRupees(includeSymbol: Boolean = true): String {
        val symbol = if (includeSymbol) "₹" else ""
        val rupees = value / 100L
        return when {
            rupees >= CRORE -> "$symbol${compact(rupees, CRORE)}Cr"
            rupees >= LAKH -> "$symbol${compact(rupees, LAKH)}L"
            else -> "$symbol${formatIndianNumber(rupees)}"
        }
    }

    companion object {
        private const val LAKH = 100_000L
        private const val CRORE = 10_000_000L

        /**
         * One decimal place of `rupees / unit`, in integer arithmetic — floats are forbidden for
         * currency in this codebase, and this is a rounding step, so the rule applies here too.
         * The final digit is truncated, never rounded up, so a compact figure can only ever
         * understate: ₹4,99,999 reads as `₹49.9L`, not `₹50L` for money that is not there.
         */
        private fun compact(rupees: Long, unit: Long): String {
            val tenths = (rupees * 10L) / unit
            val whole = tenths / 10L
            val fraction = tenths % 10L
            return if (fraction == 0L) whole.toString() else "$whole.$fraction"
        }

        val ZERO = Paise(0L)

        fun fromRupees(rupees: Long): Paise = Paise(rupees * 100L)
        fun fromPaise(paise: Long): Paise = Paise(paise)

        /**
         * Formats an integer using the Indian numbering system (e.g., 400000 -> "4,00,000").
         */
        fun formatIndianNumber(number: Long): String {
            if (number < 1000) return number.toString()

            val str = number.toString()
            val lastThree = str.substring(str.length - 3)
            val rest = str.substring(0, str.length - 3)

            val builder = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                builder.insert(0, rest[i])
                count++
                if (count == 2 && i != 0) {
                    builder.insert(0, ',')
                    count = 0
                }
            }
            return "$builder,$lastThree"
        }
    }
}
