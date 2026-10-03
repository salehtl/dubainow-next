package com.salehtl.dubainownext

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.RootScreen
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTypography
import com.salehtl.dubainownext.design.components.ToastCenter

/**
 * Launch extras for demos and screenshots (adb shell am start -n …/.MainActivity -e persona sara …):
 *   persona sara|khalid|faisal|wei|guest · tab home|services|activity|search · route "topic:x,service:y"
 *   sheet profile|happiness|… · assistant true · hour 9 · welcome true
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Catalogue.load(this)
        val options = LaunchOptions.from(intent)
        setContent {
            val store = remember { DemoStore() }
            val router = remember { Router() }
            val toasts = remember { ToastCenter() }
            remember { options.apply(store, router); true }
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DNColor.actionAccent, onPrimary = DNColor.textOnAccent,
                    primaryContainer = DNColor.actionAccentSubtle, onPrimaryContainer = DNColor.textLink,
                    background = DNColor.surfaceSecondary, surface = DNColor.surfacePrimary,
                    surfaceContainerLow = DNColor.surfacePrimary, surfaceContainer = DNColor.surfaceContainer,
                    surfaceContainerHigh = DNColor.surfacePrimary, onSurface = DNColor.textPrimary,
                    onSurfaceVariant = DNColor.textSecondary, outline = DNColor.borderControl,
                    outlineVariant = DNColor.borderSubtle, error = DNColor.actionDestructive,
                ),
                typography = DNTypography,
            ) {
                CompositionLocalProvider(
                    LocalStore provides store, LocalRouter provides router, LocalToasts provides toasts,
                    LocalLayoutDirection provides LayoutDirection.Ltr, // English-only demo: LTR even on an Arabic phone
                ) {
                    RootScreen()
                }
            }
        }
    }
}
