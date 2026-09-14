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

    companion object {
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
