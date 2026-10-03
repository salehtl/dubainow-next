package com.salehtl.dubainownext.features.welcome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.design.Hue

/** The six heroes, each drawn from Canvas objects (see WelcomeHeroes.kt). */
internal enum class WelcomeHeroKind { Orbit, NeedsYou, Widgets, Journey, Search, Poster }

/** Who a story is from: DubaiNow's own tour, or an entity's announcement. */
internal sealed interface WelcomeSource {
    data object DubaiNow : WelcomeSource
    data class Entity(val name: String, val icon: ImageVector, val age: String) : WelcomeSource
}

/**
 * One story in the pre-login player (Paper C1 · "10 · Welcome", English stories 1–6).
 * Each story takes one Canvas hue for its wash, kicker and shadows.
 */
internal data class WelcomeStory(
    val id: Int,
    val hero: WelcomeHeroKind,
    val hue: Hue,
    val source: WelcomeSource = WelcomeSource.DubaiNow,
    val kicker: String,
    /** One line at the default text size (34/40 bold). */
    val headline: String,
    /** Up to two lines. */
    val body: String,
) {
    val sourceKey: String
        get() = when (source) {
            WelcomeSource.DubaiNow -> "dubaiNow"
            is WelcomeSource.Entity -> source.name
        }

    /** What TalkBack reads after the label: who it's from, the kicker and the body. */
    val accessibilityValue: String
        get() {
            val text = "$kicker. $body"
            return if (source is WelcomeSource.Entity) "From ${source.name}. $text" else text
        }

    companion object {
        /**
         * The tour (stories 1–5) plus one sample announcement (story 6). The rules board says later visits
         * show announcements only; the demo always plays all six so the whole tour is seen.
         */
        val all: List<WelcomeStory> = listOf(
            WelcomeStory(
                id = 0, hero = WelcomeHeroKind.Orbit, hue = Hue.Sky,
                kicker = "Welcome to DubaiNow",
                headline = "Dubai, in one app.",
                body = "Pay, renew, apply and track government services. One sign-in, with UAE PASS.",
            ),
            WelcomeStory(
                id = 1, hero = WelcomeHeroKind.NeedsYou, hue = Hue.Peach,
                kicker = "Needs you",
                headline = "What needs you, first.",
                body = "Bills, fines and renewals show up before they’re late. Pay them all in one go.",
            ),
            WelcomeStory(
                id = 2, hero = WelcomeHeroKind.Widgets, hue = Hue.Lilac,
                kicker = "Your widgets",
                headline = "Your city, live.",
                body = "Parking time, Salik balance, prayer times and the metro, on a Home you arrange.",
            ),
            WelcomeStory(
                id = 3, hero = WelcomeHeroKind.Journey, hue = Hue.Sand,
                kicker = "Start to finish",
                headline = "One step at a time.",
                body = "Building a house or moving home? We line up every service you need, in order.",
            ),
            WelcomeStory(
                id = 4, hero = WelcomeHeroKind.Search, hue = Hue.Mint,
                kicker = "Search",
                headline = "Just ask.",
                body = "Local names, typos and questions like “how much do I owe” all get a straight answer.",
            ),
            WelcomeStory(
                id = 5, hero = WelcomeHeroKind.Poster, hue = Hue.Rose,
                source = WelcomeSource.Entity("Dubai Fitness Challenge", Icons.AutoMirrored.Rounded.DirectionsRun, "2h"),
                kicker = "Announcement",
                headline = "Dubai gets moving.",
                body = "Free runs, rides and classes across the city, from 1 to 30 November.",
            ),
        )
    }
}

/**
 * The story clock: 6 s per story, the segment fills in a straight line, pauses keep the elapsed time.
 * It never loops on its own: after the last story it stops with every segment full.
 * The player's frame loop adds to [elapsedMs] while it runs (see WelcomeScreen).
 */
internal class WelcomeStoryClock(val count: Int, startAt: Int = 0) {
    var index by mutableIntStateOf(startAt.coerceIn(0, maxOf(count - 1, 0)))
        private set

    /** The last story has played to the end; every segment is full. */
    var isFinished by mutableStateOf(false)
        private set

    /** Time played on the current story. Read it in draw code only (it changes every frame). */
    var elapsedMs by mutableFloatStateOf(0f)

    /** How much of the current story has played, 0…1. */
    val fraction: Float get() = if (isFinished) 1f else (elapsedMs / DURATION_MS).coerceIn(0f, 1f)

    /** Jump to a story and start it from the beginning. */
    fun go(to: Int) {
        index = to.coerceIn(0, count - 1)
        elapsedMs = 0f
        isFinished = false
    }

    /** Next story. From the last story (finished or not) it starts again from story 1. */
    fun next() = go(if (index < count - 1) index + 1 else 0)

    /** Previous story. On story 1 it restarts story 1. */
    fun previous() = go(index - 1)

    /** The timer ran out: move on, or stop on the last story with every segment full. */
    fun timerEnded() {
        if (index < count - 1) {
            go(index + 1)
        } else {
            elapsedMs = DURATION_MS
            isFinished = true
        }
    }

    companion object {
        const val DURATION_MS = 6000f
    }
}

/** The sheets Welcome presents itself (it sits outside the main scaffold and its router). */
internal enum class WelcomeSheet { SignIn, CreateAccount, Events }

/**
 * Debug launch extras for screenshots, applied once per launch (signing out later starts clean):
 * `welcomeStory 1…6`, `welcomeSheet signIn|handoff|picker|verifying|verified|createAccount|events`,
 * `welcomeAutoSignIn true` (with a sign-in sheet: picks the first resident on its own).
 */
internal object WelcomeLaunch {
    private var consumed = false

    data class Values(val story: Int, val sheet: WelcomeSheet?, val signInStage: String?, val autoSignIn: Boolean)

    fun consume(): Values {
        if (consumed) return Values(0, null, null, false)
        consumed = true
        val extras = LaunchOptions.extras
        val story = ((extras["welcomeStory"]?.toIntOrNull() ?: 1) - 1).coerceIn(0, WelcomeStory.all.size - 1)
        val raw = extras["welcomeSheet"]
        val sheet = when (raw) {
            "signIn", "handoff", "picker", "verifying", "verified" -> WelcomeSheet.SignIn
            "createAccount" -> WelcomeSheet.CreateAccount
            "events" -> WelcomeSheet.Events
            else -> null
        }
        val stage = raw.takeIf { sheet == WelcomeSheet.SignIn && it != "signIn" }
        return Values(story, sheet, stage, LaunchOptions.flag("welcomeAutoSignIn"))
    }
}
