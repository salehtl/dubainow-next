package com.salehtl.dubainownext.design

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.pow

/**
 * DubaiNow motion (Paper C1 row 08 · "Three springs, one physics"), converted from the spec's
 * response/damping (SwiftUI terms) to Compose: stiffness = (2π / response)², dampingRatio = damping.
 */
object DNMotion {
    private fun stiffness(response: Double) = (2 * PI / response).pow(2).toFloat()

    /** Presses, toggles, chips, the tab selection. response 0.30 · damping 0.86 */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.86f, stiffness = stiffness(0.30))
    /** Sheets, pushes, the search morph, reflowing grids. response 0.40 · damping 0.90 */
    fun <T> smooth(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.90f, stiffness = stiffness(0.40))
    /** Moments of success only. response 0.50 · damping 0.70 */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.70f, stiffness = stiffness(0.50))

    /** 150 ms – press highlights, results updating as you type. */
    fun <T> fast(): FiniteAnimationSpec<T> = tween(150)
    /** 250 ms – cross-fades, tab content, Reduce Motion swaps. */
    fun <T> base(): FiniteAnimationSpec<T> = tween(250)
    /** 600 ms – the Home wash changing with the time of day. */
    fun <T> wash(): FiniteAnimationSpec<T> = tween(600)

    const val STAGGER_MS = 30L
    const val PRESS_SCALE = 0.97f
    const val HOLD_DELAY_MS = 450L
    const val HOLD_LIFT = 1.04f
    const val TOAST_HOLD_MS = 2500L
    const val TOAST_HOLD_UNDO_MS = 5000L
}

/** Haptics, used sparingly (Paper C1 · Haptics and fallbacks). Never for scrolling or pushes. */
object DNHaptics {
    /** Switching tabs, choosing a chip or a Happiness face. */
    fun selection(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    /** Removing a favorite or a widget. */
    fun light(view: View) = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    /** Entering edit mode, lifting a widget. */
    fun medium(view: View) = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    /** Crossing the pull threshold at the end of Home. */
    fun soft(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    /** Payment done, request sent, favorite added. */
    fun success(view: View) = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    /** Favorites full (8 of 8), parking about to end. */
    fun warning(view: View) = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    /** Payment failed, a form that cannot be sent. */
    fun error(view: View) = view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
}
