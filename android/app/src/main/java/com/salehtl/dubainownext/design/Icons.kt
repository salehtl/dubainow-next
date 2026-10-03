package com.salehtl.dubainownext.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Android uses Material Symbols Rounded (Paper: --font-icon-android), never SF Symbols.
 * The shared demo data keeps the iOS symbol names; this maps them to Material icons.
 * Features can also use `Icons.Rounded.*` directly.
 */
object DNIcons {
    /**
     * Salik's toll gantry (Paper 9U5; Material has no such glyph): a beam on two posts with the
     * reader in the middle, the same rounded rects as the Salik widget's glyph, on a 24 grid.
     */
    private val SalikGantry: ImageVector = ImageVector.Builder("Rounded.SalikGantry", 24.dp, 24.dp, 24f, 24f).apply {
        listOf(
            floatArrayOf(0.96f, 2.4f, 22.08f, 4.08f),   // beam
            floatArrayOf(2.88f, 4.8f, 3.6f, 17.76f),    // posts
            floatArrayOf(17.52f, 4.8f, 3.6f, 17.76f),
            floatArrayOf(9.6f, 7.2f, 4.8f, 5.28f),      // reader
        ).forEach { (x, y, w, h) -> path(fill = SolidColor(Color.Black)) { roundRect(x, y, w, h, 1.44f) } }
    }.build()

    private fun PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y); lineTo(x + w - r, y); arcTo(r, r, 0f, false, true, x + w, y + r)
        lineTo(x + w, y + h - r); arcTo(r, r, 0f, false, true, x + w - r, y + h)
        lineTo(x + r, y + h); arcTo(r, r, 0f, false, true, x, y + h - r)
        lineTo(x, y + r); arcTo(r, r, 0f, false, true, x + r, y); close()
    }

    private val map: Map<String, ImageVector> = mapOf(
        "airplane" to Icons.Rounded.Flight,
        "arrow.up" to Icons.Rounded.ArrowUpward,
        "arrow.up.forward.square" to Icons.AutoMirrored.Rounded.OpenInNew,
        "backpack.fill" to Icons.Rounded.Backpack,
        "banknote" to Icons.Rounded.Payments,
        "banknote.fill" to Icons.Rounded.Payments,
        "bell" to Icons.Rounded.NotificationsNone,
        "bell.fill" to Icons.Rounded.Notifications,
        "bolt.fill" to Icons.Rounded.Bolt,
        "book.closed.fill" to Icons.Rounded.Book,
        "book.fill" to Icons.AutoMirrored.Rounded.MenuBook,
        "briefcase.fill" to Icons.Rounded.Work,
        "building.2.fill" to Icons.Rounded.Apartment,
        "building.columns" to Icons.Rounded.AccountBalance,
        "building.columns.fill" to Icons.Rounded.AccountBalance,
        "calendar" to Icons.Rounded.CalendarToday,
        "camera" to Icons.Rounded.PhotoCamera,
        "camera.fill" to Icons.Rounded.PhotoCamera,
        "car" to Icons.Rounded.DirectionsCar,
        "car.fill" to Icons.Rounded.DirectionsCar,
        "character.bubble" to Icons.Rounded.Translate,
        "checklist" to Icons.Rounded.Checklist,
        "checkmark" to Icons.Rounded.Check,
        "checkmark.circle.fill" to Icons.Rounded.CheckCircle,
        "checkmark.seal.fill" to Icons.Rounded.Verified,
        "checkmark.shield.fill" to Icons.Rounded.VerifiedUser,
        "chevron.forward" to Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        "chevron.right" to Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        "clock" to Icons.Rounded.Schedule,
        "clock.arrow.circlepath" to Icons.Rounded.History,
        "creditcard.fill" to Icons.Rounded.CreditCard,
        "cross.case.fill" to Icons.Rounded.MedicalServices,
        "doc.badge.clock" to Icons.Rounded.PendingActions,
        "doc.fill" to Icons.Rounded.Description,
        "doc.on.doc.fill" to Icons.Rounded.FileCopy,
        "doc.text" to Icons.Rounded.Description,
        "doc.text.fill" to Icons.Rounded.Description,
        "envelope.fill" to Icons.Rounded.Email,
        "exclamationmark.bubble" to Icons.Rounded.Feedback,
        "exclamationmark.bubble.fill" to Icons.Rounded.Feedback,
        "exclamationmark.circle.fill" to Icons.Rounded.Error,
        "exclamationmark.triangle.fill" to Icons.Rounded.Warning,
        "eye.fill" to Icons.Rounded.Visibility,
        "face.smiling" to Icons.Rounded.SentimentSatisfied,
        "faceid" to Icons.Rounded.Face,
        "figure.2.and.child.holdinghands" to Icons.Rounded.FamilyRestroom,
        "figure.roll" to Icons.AutoMirrored.Rounded.Accessible,
        "figure.run" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "figure.walk" to Icons.AutoMirrored.Rounded.DirectionsWalk,
        "fuelpump.fill" to Icons.Rounded.LocalGasStation,
        "gauge.with.dots.needle.67percent" to Icons.Rounded.Speed,
        "globe" to Icons.Rounded.Language,
        "graduationcap.fill" to Icons.Rounded.School,
        "hammer.fill" to Icons.Rounded.Construction,
        "heart.circle.fill" to Icons.Rounded.Favorite,
        "heart.fill" to Icons.Rounded.Favorite,
        "house" to Icons.Rounded.Home,
        "house.fill" to Icons.Rounded.Home,
        "house.lodge.fill" to Icons.Rounded.Cottage,
        "info.circle" to Icons.Outlined.Info,
        "key.fill" to Icons.Rounded.Key,
        "leaf.fill" to Icons.Rounded.Eco,
        "line.3.horizontal.decrease" to Icons.Rounded.FilterList,
        "link" to Icons.Rounded.Link,
        "list.bullet" to Icons.AutoMirrored.Rounded.FormatListBulleted,
        "list.number" to Icons.Rounded.FormatListNumbered,
        "lock.fill" to Icons.Rounded.Lock,
        "magnifyingglass" to Icons.Rounded.Search,
        "map.fill" to Icons.Rounded.Map,
        "mappin.and.ellipse" to Icons.Rounded.LocationOn,
        "mic.fill" to Icons.Rounded.Mic,
        "moon.stars" to Icons.Rounded.NightsStay,
        "moon.stars.fill" to Icons.Rounded.NightsStay,
        "number.square.fill" to Icons.Rounded.Pin,
        "p.square" to Icons.Rounded.LocalParking,
        "p.square.fill" to Icons.Rounded.LocalParking,
        "pawprint.fill" to Icons.Rounded.Pets,
        "person" to Icons.Rounded.PersonOutline,
        "person.2" to Icons.Rounded.People,
        "person.2.fill" to Icons.Rounded.People,
        "person.3.fill" to Icons.Rounded.Groups,
        "person.crop.circle" to Icons.Rounded.AccountCircle,
        "person.crop.circle.badge.plus" to Icons.Rounded.PersonAdd,
        "person.crop.rectangle" to Icons.Rounded.Badge,
        "person.text.rectangle" to Icons.Rounded.Badge,
        "person.text.rectangle.fill" to Icons.Rounded.Badge,
        "phone.fill" to Icons.Rounded.Phone,
        "point.topleft.down.to.point.bottomright.curvepath" to Icons.Rounded.Route,
        "qrcode" to Icons.Rounded.QrCode,
        "questionmark.circle.fill" to Icons.AutoMirrored.Rounded.Help,
        "road.lanes" to SalikGantry,
        "shield.fill" to Icons.Rounded.Shield,
        "shippingbox.fill" to Icons.Rounded.LocalShipping,
        "sparkles" to Icons.Rounded.AutoAwesome,
        "speedometer" to Icons.Rounded.Speed,
        "sportscourt.fill" to Icons.Rounded.SportsSoccer,
        "square.and.arrow.up" to Icons.Rounded.Share,
        "square.grid.2x2.fill" to Icons.Rounded.GridView,
        "star" to Icons.Rounded.StarOutline,
        "star.fill" to Icons.Rounded.Star,
        "staroflife.fill" to Icons.Rounded.LocalHospital,
        "steeringwheel" to Icons.Rounded.DirectionsCar,
        "stethoscope" to Icons.Rounded.MedicalServices,
        "stop.fill" to Icons.Rounded.Stop,
        "storefront.fill" to Icons.Rounded.Storefront,
        "stroller.fill" to Icons.Rounded.ChildFriendly,
        "sun.max.fill" to Icons.Rounded.WbSunny,
        "tag.fill" to Icons.Rounded.LocalOffer,
        "text.bubble.fill" to Icons.AutoMirrored.Rounded.Chat,
        "theatermasks.fill" to Icons.Rounded.TheaterComedy,
        "ticket.fill" to Icons.Rounded.ConfirmationNumber,
        "touchid" to Icons.Rounded.Fingerprint,
        "tram.fill" to Icons.Rounded.Tram,
        "trash.fill" to Icons.Rounded.Delete,
        "tree.fill" to Icons.Rounded.Park,
        "wifi" to Icons.Rounded.Wifi,
        "wrench.and.screwdriver" to Icons.Rounded.Build,
        "xmark" to Icons.Rounded.Close,
    )

    /** The Material icon for an iOS symbol name used in the shared data (falls back to a grid). */
    fun forSymbol(name: String): ImageVector = map[name] ?: Icons.Rounded.GridView

    fun has(name: String) = map.containsKey(name)
}
