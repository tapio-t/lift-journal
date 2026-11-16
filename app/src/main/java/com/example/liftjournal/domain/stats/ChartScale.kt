package com.example.liftjournal.domain.stats

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * A value axis: gridlines from [low] to [high] every [step].
 * When [low] > 0 the axis is zoomed: a short stub at the bottom stands for 0 → [low] (SPEC §5.3).
 */
data class AxisScale(val low: Double, val high: Double, val step: Double) {
    val isZoomed: Boolean get() = low > 0

    /** Gridline values from [low] to [high]. */
    fun gridlines(): List<Double> {
        val count = ((high - low) / step + ROUNDING_SLACK).toInt()
        return (0..count).map { low + it * step }
    }
}

/** Load-off weeks lighter than this share of the median top set don't stretch the zoomed scale. */
const val LOAD_OFF_THRESHOLD = 0.8

/** About one gridline label per this many pixels of chart height. */
const val PIXELS_PER_GRIDLINE = 14f

private const val MIN_GRIDLINES = 2
private const val ROUNDING_SLACK = 1e-9
private val ZOOM_STEPS_KG = listOf(2.5, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 250.0)
private val NICE_MULTIPLIERS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
private const val LINEAR_TARGET_GRIDLINES = 4
private const val GOAL_HEADROOM = 1.05

/**
 * The zoomed weight scale of the Top set chart (prototype `barSvg`, `trunc`).
 * [low] is one step below the lightest "normal" week (at least 80% of the median top set);
 * the step is the smallest of 2.5…250 kg that fits [zoomHeightPx] at about one label per 14 px.
 */
fun zoomedScale(values: List<Double>, zoomHeightPx: Float): AxisScale {
    val positive = values.filter { it > 0 }.sorted()
    if (positive.isEmpty()) return linearScale(values)
    val median = positive[positive.size / 2]
    val lightestNormal = positive.filter { it >= median * LOAD_OFF_THRESHOLD }.min()
    val heaviest = positive.last()
    val maxGridlines = maxOf(MIN_GRIDLINES, floor(zoomHeightPx / PIXELS_PER_GRIDLINE).toInt())

    fun lowFor(step: Double): Double {
        var low = floor(lightestNormal / step) * step
        if (low >= lightestNormal) low -= step
        return maxOf(0.0, low)
    }

    val step = ZOOM_STEPS_KG.firstOrNull { (ceil(heaviest / it) * it - lowFor(it)) / it <= maxGridlines }
        ?: ZOOM_STEPS_KG.last()
    val low = lowFor(step)
    return AxisScale(low, maxOf(low + step, ceil(heaviest / step) * step), step)
}

/** A zero-based scale with about four gridlines (month and year bars). */
fun linearScale(values: List<Double>): AxisScale {
    val max = values.maxOrNull() ?: 0.0
    val step = niceStep(max / LINEAR_TARGET_GRIDLINES)
    return AxisScale(0.0, maxOf(step, ceil(max / step) * step), step)
}

/** The goal chart's zero-based scale, with 5% headroom above the larger of now and goal. */
fun goalScale(currentKg: Double, targetKg: Double): AxisScale {
    val max = maxOf(currentKg, targetKg)
    val step = niceStep(max / LINEAR_TARGET_GRIDLINES)
    return AxisScale(0.0, maxOf(step, ceil(max * GOAL_HEADROOM / step) * step), step)
}

/** Rounds up to 1, 2, 2.5 or 5 × a power of ten (prototype `tickStep`). */
fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val power = 10.0.pow(floor(log10(raw)))
    return NICE_MULTIPLIERS.map { it * power }.firstOrNull { it >= raw } ?: (10 * power)
}
