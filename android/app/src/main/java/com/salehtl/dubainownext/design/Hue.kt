package com.salehtl.dubainownext.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * The six Canvas hues. A service's hue belongs to its record and defaults to its topic.
 * Each hue has a start and end (for gradients) and an ink (for glyphs and text on the hue).
 */
enum class Hue(val start: Color, val end: Color, val ink: Color, private val s: Int, private val e: Int) {
    Sky(DNColor.hueSkyStart, DNColor.hueSkyEnd, DNColor.hueSkyInk, 0xE4EEFF, 0xCFDDFB),
    Sand(DNColor.hueSandStart, DNColor.hueSandEnd, DNColor.hueSandInk, 0xFFF0DC, 0xFBDDB8),
    Rose(DNColor.hueRoseStart, DNColor.hueRoseEnd, DNColor.hueRoseInk, 0xFBE6E4, 0xF4CBC6),
    Lilac(DNColor.hueLilacStart, DNColor.hueLilacEnd, DNColor.hueLilacInk, 0xEFE7FA, 0xDCCFF3),
    Mint(DNColor.hueMintStart, DNColor.hueMintEnd, DNColor.hueMintInk, 0xE1F5EA, 0xC2E7D6),
    Peach(DNColor.huePeachStart, DNColor.huePeachEnd, DNColor.huePeachInk, 0xFFE9D9, 0xFFCEB5);

    /** Paper: `linear-gradient(in oklab 160deg, start, end)`; stops computed in OKLab. */
    val gradient: Brush by lazy { cssGradient(160.0, OKLab.stops(s, e, 6)) }
}

/** A CSS-style angled gradient (0° = to top, clockwise) whose line reaches the box corners. */
fun cssGradient(degrees: Double, stops: List<Pair<Float, Color>>): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val r = Math.toRadians(degrees)
        val dx = sin(r).toFloat(); val dy = (-cos(r)).toFloat()
        val half = (abs(size.width * sin(r)) + abs(size.height * cos(r))).toFloat() / 2f
        val c = Offset(size.width / 2, size.height / 2)
        return LinearGradientShader(
            from = c - Offset(dx * half, dy * half),
            to = c + Offset(dx * half, dy * half),
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
    }
}

/** Minimal OKLab maths (Björn Ottosson) for perceptual gradient stops. */
object OKLab {
    fun stops(from: Int, to: Int, count: Int): List<Pair<Float, Color>> {
        val a = toLab(from); val b = toLab(to)
        return (0 until count).map { i ->
            val t = i / (count - 1).toDouble()
            val l = DoubleArray(3) { k -> a[k] + (b[k] - a[k]) * t }
            val rgb = toRgb(l)
            t.toFloat() to Color(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
        }
    }

    private fun lin(c: Int): Double { val v = c / 255.0; return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4) }
    private fun gam(v: Double): Double { val c = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055; return c.coerceIn(0.0, 1.0) }

    private fun toLab(rgb: Int): DoubleArray {
        val r = lin(rgb shr 16 and 0xFF); val g = lin(rgb shr 8 and 0xFF); val b = lin(rgb and 0xFF)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun toRgb(lab: DoubleArray): DoubleArray {
        val l_ = lab[0] + 0.3963377774 * lab[1] + 0.2158037573 * lab[2]
        val m_ = lab[0] - 0.1055613458 * lab[1] - 0.0638541728 * lab[2]
        val s_ = lab[0] - 0.0894841775 * lab[1] - 1.2914855480 * lab[2]
        val l = l_ * l_ * l_; val m = m_ * m_ * m_; val s = s_ * s_ * s_
        return doubleArrayOf(
            gam(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
            gam(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
            gam(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s),
        )
    }
}

/** The Home wash changes with the time of day: morning 05–11 · midday 11–16 · evening 16–20 · night. */
enum class TimeOfDay(val wash: Color, val greeting: String) {
    Morning(DNColor.washMorning, "Good morning"),
    Midday(DNColor.washMidday, "Good afternoon"),
    Evening(DNColor.washEvening, "Good evening"),
    Night(DNColor.washNight, "Good evening");

    companion object {
        fun of(time: LocalDateTime): TimeOfDay = when (time.hour) {
            in 5..10 -> Morning
            in 11..15 -> Midday
            in 16..19 -> Evening
            else -> Night
        }
    }
}

/** The ambient wash behind the top of Home (and other tab roots). 420 dp, fades into the canvas. */
@Composable
fun AmbientWash(timeOfDay: TimeOfDay, modifier: Modifier = Modifier, height: Dp = 420.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(listOf(timeOfDay.wash, timeOfDay.wash.copy(alpha = 0.55f), DNColor.surfaceSecondary.copy(alpha = 0f))))
    )
}
