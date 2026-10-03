package com.salehtl.dubainownext.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Money in AED. Western digits always (Arabic too). */
object Money {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val two = DecimalFormat("#,##0.00", symbols)
    private val whole = DecimalFormat("#,##0", symbols)

    /** "1,012.60" (or "600" when whole and [trimWhole]). */
    fun figure(amount: BigDecimal, trimWhole: Boolean = false): String =
        if (trimWhole && amount.stripTrailingZeros().scale() <= 0) whole.format(amount) else two.format(amount.setScale(2, RoundingMode.HALF_UP))

    /** "AED 1,012.60" */
    fun aed(amount: BigDecimal, trimWhole: Boolean = false) = "AED " + figure(amount, trimWhole)

    fun spoken(amount: BigDecimal) = "${figure(amount)} dirhams"
}

/** Shorthand for literals: `412.60.aed` → BigDecimal("412.60"). */
val Double.aed: BigDecimal get() = BigDecimal(this.toString()).setScale(2, RoundingMode.HALF_UP)
val Int.aed: BigDecimal get() = BigDecimal(this).setScale(2)

/** Dates in the demo are relative to "now", so due dates stay true whenever the demo is opened. */
object DemoDate {
    /** A date `days` from today at a given time. */
    fun day(days: Int, hour: Int = 9, minute: Int = 0): LocalDateTime =
        LocalDate.now().plusDays(days.toLong()).atTime(hour, minute)

    /** Minutes from now. */
    fun minutes(m: Int): LocalDateTime = LocalDateTime.now().plusMinutes(m.toLong())

    /** A fixed calendar date (for things far away, like an Emirates ID expiry). */
    fun fixed(year: Int, month: Int, day: Int, hour: Int = 9): LocalDateTime = LocalDateTime.of(year, month, day, hour, 0)

    private val dShort = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val dLong = DateTimeFormatter.ofPattern("d MMMM", Locale.UK)
    private val dFull = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
    private val dMonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)
    private val dTime = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private val dWeekday = DateTimeFormatter.ofPattern("EEEE", Locale.UK)

    /** "24 Sep" */ fun short(d: LocalDateTime): String = d.format(dShort)
    /** "4 October" */ fun long(d: LocalDateTime): String = d.format(dLong)
    /** "14 March 2027" */ fun full(d: LocalDateTime): String = d.format(dFull)
    /** "March 2028" */ fun monthYear(d: LocalDateTime): String = d.format(dMonthYear)
    /** "08:14" */ fun time(d: LocalDateTime): String = d.format(dTime)
    /** "Thursday" */ fun weekday(d: LocalDateTime): String = d.format(dWeekday)

    /** Whole days from today to the date (negative in the past). */
    fun daysFromToday(d: LocalDateTime): Int = ChronoUnit.DAYS.between(LocalDate.now(), d.toLocalDate()).toInt()

    /** "Due today", "Due tomorrow", "Due in 3 days", "Due 13 Oct", "Overdue". */
    fun dueLabel(d: LocalDateTime): String = when (val n = daysFromToday(d)) {
        in Int.MIN_VALUE..-1 -> "Overdue"
        0 -> "Due today"
        1 -> "Due tomorrow"
        in 2..13 -> "Due in $n days"
        else -> "Due ${short(d)}"
    }

    /** "Today", "Yesterday", "3 days ago", "24 Sep" */
    fun relative(d: LocalDateTime): String = when (val n = daysFromToday(d)) {
        0 -> "Today"
        -1 -> "Yesterday"
        in -6..-2 -> "${-n} days ago"
        else -> short(d)
    }

    /** "in 2h 31m" */
    fun countdown(to: LocalDateTime, from: LocalDateTime = LocalDateTime.now()): String {
        val mins = maxOf(0L, Duration.between(from, to).toMinutes())
        val h = mins / 60; val m = mins % 60
        return if (h > 0) "in ${h}h ${m}m" else "in $m min"
    }
}
