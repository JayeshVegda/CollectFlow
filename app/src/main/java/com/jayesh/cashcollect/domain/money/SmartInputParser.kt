package com.jayesh.cashcollect.domain.money

import java.util.Locale

data class SmartParseResult(
    val customerName: String,
    val amountPaise: Long,
    val amountRupees: Long,
    val formattedRupees: String,
    val rawAmountToken: String
)

object SmartInputParser {

    /**
     * Parses a string like "sambhu 400" or "mahesh 1.5L" or "rajesh 50k" or "ramesh 20000".
     */
    fun parse(input: String): SmartParseResult? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val tokens = trimmed.split("\\s+".toRegex())
        if (tokens.isEmpty()) return null

        // Last token is candidate amount
        val lastToken = tokens.last()
        val parsedAmount = parseAmountToken(lastToken)

        return if (parsedAmount != null) {
            val nameTokens = tokens.dropLast(1)
            val name = nameTokens.joinToString(" ").trim()
            val rupees = parsedAmount / 100L
            SmartParseResult(
                customerName = name,
                amountPaise = parsedAmount,
                amountRupees = rupees,
                formattedRupees = Paise(parsedAmount).toFormattedRupees(),
                rawAmountToken = lastToken
            )
        } else {
            // Check if entire input is just an amount
            val wholeAmount = parseAmountToken(trimmed)
            if (wholeAmount != null) {
                SmartParseResult(
                    customerName = "",
                    amountPaise = wholeAmount,
                    amountRupees = wholeAmount / 100L,
                    formattedRupees = Paise(wholeAmount).toFormattedRupees(),
                    rawAmountToken = trimmed
                )
            } else {
                null
            }
        }
    }

    /**
     * Evaluates an amount token into integer paise.
     */
    fun parseAmountToken(token: String): Long? {
        val clean = token.lowercase(Locale.ROOT).trim().replace(",", "")
        if (clean.isEmpty()) return null

        // 1. Suffix 'l' or 'lakh' (e.g. 1l, 1.5l, 50lakh)
        if (clean.endsWith("l") || clean.endsWith("lakh")) {
            val numStr = clean.removeSuffix("lakh").removeSuffix("l").trim()
            val num = numStr.toDoubleOrNull() ?: return null
            if (num <= 0) return null
            // 1 Lakh = 100,000 rupees = 10,000,000 paise
            return (num * 10_000_000L).toLong()
        }

        // 2. Suffix 'k' or 'thousand' (e.g. 20k, 50k)
        if (clean.endsWith("k") || clean.endsWith("thousand")) {
            val numStr = clean.removeSuffix("thousand").removeSuffix("k").trim()
            val num = numStr.toDoubleOrNull() ?: return null
            if (num <= 0) return null
            // 1k = 1,000 rupees = 100,000 paise
            return (num * 100_000L).toLong()
        }

        // 3. Suffix 'cr' or 'crore'
        if (clean.endsWith("cr") || clean.endsWith("crore")) {
            val numStr = clean.removeSuffix("crore").removeSuffix("cr").trim()
            val num = numStr.toDoubleOrNull() ?: return null
            if (num <= 0) return null
            return (num * 1_000_000_000L).toLong()
        }

        // 4. Naked numbers:
        val num = clean.toLongOrNull() ?: return null
        if (num <= 0) return null

        return when {
            // Already full rupees >= 10,000 (e.g. 20000, 50000, 400000)
            num >= 10_000 -> num * 100L

            // Indian business shorthand: Every 100 = 1 Lakh (e.g. 100 -> 1L, 400 -> 4L, 500 -> 5L, 5000 -> 50L)
            // Equivalent to: num * 1,000 rupees
            num in 100..5000 -> (num * 1000L) * 100L

            // Suffixless tens (30..99) as thousands shorthand (e.g. 30 -> 30k, 50 -> 50k)
            num in 30..99 -> (num * 1000L) * 100L

            // Exact rupees
            else -> num * 100L
        }
    }
}
