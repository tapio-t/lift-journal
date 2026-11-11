package com.example.liftjournal.domain.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

private const val WEIGHT_DECIMALS = 2
private const val THOUSAND = 1_000.0
private const val TEN_THOUSAND = 10_000.0
private const val MILLION = 1_000_000.0

/** Weights use a dot as the decimal separator, as in the entry format. */
private val GROUPED_INTEGER = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

/** `82.5`, `100`, or `bw` for zero (prototype `fmtW`). At most two decimals, no trailing zeros. */
fun formatWeight(kg: Double): String {
    if (kg == 0.0) return "bw"
    return BigDecimal.valueOf(kg).setScale(WEIGHT_DECIMALS, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

/** `41,230`: rounded, with thousands separators (prototype `fmtN`). */
fun formatWholeKg(kg: Double): String = GROUPED_INTEGER.format(kg.roundToLong())

/** `1.2M`, `41k`, `1.5k`, `950`: short numbers for chart labels and stats (prototype `compact`). */
fun formatCompact(value: Double): String = when {
    value >= MILLION -> oneDecimal(value / MILLION) + "M"
    value >= TEN_THOUSAND -> (value / THOUSAND).roundToLong().toString() + "k"
    value >= THOUSAND -> oneDecimal(value / THOUSAND) + "k"
    else -> value.roundToLong().toString()
}

/** Rounds to one decimal place, e.g. for averages (prototype `r1`). */
fun roundToOneDecimal(value: Double): Double = (value * 10).roundToLong() / 10.0

private fun oneDecimal(value: Double): String =
    BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
