package com.salehtl.dubainownext.data.personas

import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.MetroDeparture
import com.salehtl.dubainownext.data.MetroStation
import com.salehtl.dubainownext.data.Persona
import com.salehtl.dubainownext.data.PersonaState
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.Hue
import java.time.LocalDateTime

/** The demo residents you can sign in as (persona picker behind "Sign in with UAE PASS"). */
object Personas {
    val all: List<Persona> get() = listOf(sara, khalid, faisal, wei)

    fun persona(id: String): Persona? = all.firstOrNull { it.id == id }

    /** Guest: no account. Services, Search and public widgets only. */
    fun guestState() = PersonaState(
        favorites = listOf(
            FavoriteItem("fav-parking", "Pay parking", "p.square.fill", Hue.Sky, FavoriteItem.Target.Service("pay-for-parking")),
            FavoriteItem("fav-donate", "Donate", "heart.fill", Hue.Lilac, FavoriteItem.Target.Service("donate-to-charity")),
        ),
        widgets = listOf(
            HomeWidget("g-prayer", WidgetKind.Prayer, WidgetSize.Small),
            HomeWidget("g-weather", WidgetKind.Weather, WidgetSize.Small),
            HomeWidget("g-metro", WidgetKind.Metro, WidgetSize.Medium),
        ),
        metro = MetroStation("Business Bay", "Red Line", listOf(MetroDeparture("Centrepoint", 3, 9), MetroDeparture("Expo 2020", 6, 14))),
    )
}

/** Dubai prayer times for early October (used by the prayer widget). Static timetable. */
object PrayerTimes {
    data class Prayer(val name: String, val hour: Int, val minute: Int)

    val today = listOf(
        Prayer("Fajr", 4, 58), Prayer("Sunrise", 6, 12), Prayer("Dhuhr", 12, 12),
        Prayer("Asr", 15, 34), Prayer("Maghrib", 18, 5), Prayer("Isha", 19, 19),
    )

    /** The next prayer after [now] (wraps to tomorrow's Fajr). */
    fun next(now: LocalDateTime): Pair<Prayer, LocalDateTime> {
        for (p in today) {
            if (p.name == "Sunrise") continue
            val d = now.withHour(p.hour).withMinute(p.minute).withSecond(0)
            if (d.isAfter(now)) return p to d
        }
        val f = today[0]
        return f to now.plusDays(1).withHour(f.hour).withMinute(f.minute).withSecond(0)
    }
}
