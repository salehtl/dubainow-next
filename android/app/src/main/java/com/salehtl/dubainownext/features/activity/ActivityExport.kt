package com.salehtl.dubainownext.features.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.salehtl.dubainownext.R
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.design.DNColor
import java.io.OutputStream

/**
 * Files the Activity feature shares: the History CSV ("Export") and a receipt PDF
 * ("Download receipt (PDF)"). Nothing leaves the device unless the resident shares it.
 *
 * The app has no FileProvider yet (see NOTES.md), so the PDF is saved to Downloads through
 * MediaStore (Android 10+) and shared from there; the CSV is shared as text.
 */
internal object ActivityExport {
    // MARK: CSV

    fun csv(receipts: List<Receipt>): String {
        val lines = mutableListOf("Date,What,Paid to,Amount (AED),Paid with,Reference")
        receipts.sortedByDescending { it.date }.forEach { r ->
            val amount = r.amount?.let { Money.figure(it).replace(",", "") } ?: ""
            val count = r.count?.takeIf { it > 1 }?.let { " (×$it)" } ?: ""
            lines += listOf(ActivityFormat.csvDate(r.date), r.title + count, r.entity, amount, r.method?.label ?: "", r.reference)
                .joinToString(",") { escape(it) }
        }
        lines += ""
        lines += escape("DubaiNow demo export. No real payments were made.")
        return lines.joinToString("\n")
    }

    private fun escape(field: String): String =
        if (field.contains(',') || field.contains('"') || field.contains('\n')) "\"" + field.replace("\"", "\"\"") + "\"" else field

    /** Opens the share sheet with the History as CSV text. */
    fun shareCsv(context: Context, receipts: List<Receipt>) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DubaiNow history.csv")
            putExtra(Intent.EXTRA_TITLE, "DubaiNow history.csv")
            putExtra(Intent.EXTRA_TEXT, csv(receipts))
        }
        start(context, Intent.createChooser(send, "Export history"))
    }

    // MARK: Text

    /** Plain-text summary (the share button, and the PDF fallback before Android 10). */
    fun summary(r: Receipt): String {
        val lines = mutableListOf(r.title, r.entity)
        r.amount?.let { lines += Money.aed(it) }
        lines += ActivityFormat.whenLong(r.date)
        r.method?.let { lines += "Paid with ${it.label}" }
        lines += r.rows.map { "${it.label}: ${it.value}" }
        lines += "Reference ${r.reference}"
        lines += "DubaiNow demo · no money moved"
        return lines.joinToString("\n")
    }

    fun shareText(context: Context, text: String, subject: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(context, Intent.createChooser(send, chooserTitle))
    }

    /**
     * Copies [text] and returns whether the app should confirm it with a toast. Android 13+ shows its
     * own clipboard confirmation, so a toast there would only repeat it.
     */
    fun copy(context: Context, label: String, text: String): Boolean {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    // MARK: PDF

    private val pdfCache = mutableMapOf<String, Uri>()

    /** True where a real PDF can be saved and shared (Android 10+ without a FileProvider). */
    val canSavePdf: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Saves the receipt PDF to Downloads once per session and returns it. Call off the main thread. */
    fun receiptPdf(context: Context, receipt: Receipt): Uri? {
        if (!canSavePdf) return null
        pdfCache[receipt.id]?.let { return it }
        val name = "DubaiNow receipt ${receipt.reference.replace("/", "-")}.pdf"
        val uri = saveToDownloads(context, name, "application/pdf") { out -> writePdf(context, receipt, out) }
        if (uri != null) pdfCache[receipt.id] = uri
        return uri
    }

    fun sharePdf(context: Context, uri: Uri, title: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(context, Intent.createChooser(send, "Receipt (PDF)"))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloads(context: Context, name: String, mime: String, write: (OutputStream) -> Unit): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = runCatching { resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) }.getOrNull() ?: return null
        return try {
            val stream = resolver.openOutputStream(uri) ?: throw IllegalStateException("No stream")
            stream.use(write)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    /** The printable receipt (A4, 595 × 842 pt). Says clearly that it's a demo document. */
    private fun writePdf(context: Context, r: Receipt, out: OutputStream) {
        val inter = runCatching { ResourcesCompat.getFont(context, R.font.inter_variable) }.getOrNull() ?: Typeface.DEFAULT
        fun paint(size: Float, weight: Int, color: Int, mono: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (mono) Typeface.MONOSPACE else inter
            textSize = size
            this.color = color
            if (!mono) setFontVariationSettings("'wght' $weight")
        }
        val primary = DNColor.textPrimary.toArgb()
        val secondary = DNColor.textSecondary.toArgb()
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        val left = 48f
        val right = 595f - 48f
        val width = (right - left).toInt()

        // Header: logo, name, demo badge
        ContextCompat.getDrawable(context, R.drawable.dn_mark)?.let { d -> d.setBounds(48, 44, 48 + 36, 44 + 34); d.draw(c) }
        c.drawText("DubaiNow", 94f, 68f, paint(20f, 700, primary))
        val badgeText = "DEMO · NOT A REAL RECEIPT"
        val badgePaint = paint(10f, 700, DNColor.statusWarningForeground.toArgb()).apply { letterSpacing = 0.08f }
        val bw = badgePaint.measureText(badgeText) + 20f
        val badge = RectF(right - bw, 50f, right, 72f)
        c.drawRoundRect(badge, 11f, 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DNColor.statusWarningSurface.toArgb() })
        c.drawText(badgeText, badge.left + 10f, 65f, badgePaint)

        // Title block
        var y = 124f
        c.drawText(if (r.amount == null) "CONFIRMATION" else "RECEIPT", left, y, paint(13f, 600, secondary).apply { letterSpacing = 0.06f })
        y += 12f
        val titleLayout = StaticLayout.Builder.obtain(r.title, 0, r.title.length, paint(24f, 700, primary), width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.1f).build()
        c.save(); c.translate(left, y); titleLayout.draw(c); c.restore()
        y += titleLayout.height + 8f
        r.amount?.let {
            y += 42f
            c.drawText(Money.aed(it), left, y, paint(40f, 700, primary).apply { fontFeatureSettings = "tnum" })
        }
        y += 28f

        // Rows
        val rows = buildList {
            add(Triple("Paid to", r.entity, false))
            add(Triple("When", ActivityFormat.whenLong(r.date), false))
            r.method?.let { add(Triple("Paid with", it.label, false)) }
            r.rows.forEach { add(Triple(it.label, it.value, false)) }
            add(Triple("Reference", r.reference, true))
        }
        val rule = Paint().apply { color = DNColor.borderSubtle.toArgb(); strokeWidth = 1f }
        val labelPaint = paint(13f, 400, secondary)
        rows.forEach { (label, value, mono) ->
            y += 26f
            c.drawText(label, left, y, labelPaint)
            val vp = paint(13f, 600, primary, mono)
            c.drawText(value, right - vp.measureText(value), y, vp)
            y += 12f
            c.drawLine(left, y, right, y, rule)
        }

        // Footer
        y += 36f
        val note = "This document was made by the DubaiNow demo app. No money moved and no government system was contacted."
        val noteLayout = StaticLayout.Builder.obtain(note, 0, note.length, paint(11f, 400, secondary), width).build()
        c.save(); c.translate(left, y); noteLayout.draw(c); c.restore()

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    // MARK: Intents

    private fun start(context: Context, intent: Intent) {
        val activity = context.activityFindActivity()
        if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { (activity ?: context).startActivity(intent) }
    }
}
