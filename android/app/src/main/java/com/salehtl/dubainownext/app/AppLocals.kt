package com.salehtl.dubainownext.app

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.design.components.ToastCenter

/** App-wide objects, provided once in MainActivity. Read with `LocalStore.current` etc. */
val LocalStore = staticCompositionLocalOf<DemoStore> { error("DemoStore not provided") }
val LocalRouter = staticCompositionLocalOf<Router> { error("Router not provided") }
val LocalToasts = staticCompositionLocalOf<ToastCenter> { error("ToastCenter not provided") }

/** True where a widget is drawn as a preview (gallery, Edit Home, suggestions): no taps, menus or timers. */
val LocalWidgetPreview = compositionLocalOf { false }
