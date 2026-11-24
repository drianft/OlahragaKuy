package com.example.olahragakuy.pages.steptracker

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private const val CARD_WIDTH_PX = 1080
private const val CARD_HEIGHT_PX = 1350
private const val SHARE_CACHE_SUBDIR = "step_share"
private const val MAX_CACHED_SHARE_FILES = 6

private const val COLOR_BG_TOP = 0xFF0A1A2C.toInt()
private const val COLOR_BG_MID = 0xFF102642.toInt()
private const val COLOR_BG_BOTTOM = 0xFF0B192C.toInt()
private const val COLOR_PANEL_SURFACE = 0xCC132944.toInt()
private const val COLOR_PANEL_BORDER = 0xFF234266.toInt()
private const val COLOR_ACCENT_ORANGE = 0xFFFF6500.toInt()
private const val COLOR_ACCENT_ORANGE_GLOW = 0x4DFF6500
private const val COLOR_START_DOT = 0xFF43D166.toInt()
private const val COLOR_TEXT_PRIMARY = 0xFFF2F5FA.toInt()
private const val COLOR_TEXT_SECONDARY = 0xFFA4B8CF.toInt()
private const val COLOR_STICKER_SHADOW = 0xCC000000.toInt()
private const val COLOR_STICKER_CASING = 0x990A1A2C.toInt()

private val INDONESIAN_MONTHS_SHORT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/**
 * Mengembalikan authority `FileProvider` yang selaras dengan deklarasi manifest
 * (`${applicationId}.fileprovider`).
 */
fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

/**
 * Memformat timestamp sesi ke format tanggal lokal Indonesia yang ringkas, mis. `24 Nov 2026 • 08:45`.
 */
fun formatSessionDateIndonesian(epochMillis: Long): String {
    val safeMillis = if (epochMillis > 0L) epochMillis else System.currentTimeMillis()
    val zoned = Instant.ofEpochMilli(safeMillis).atZone(ZoneId.systemDefault())
    val monthLabel = INDONESIAN_MONTHS_SHORT.getOrElse(zoned.monthValue - 1) { "Jan" }
    return String.format(
        Locale.US,
        "%02d %s %d • %02d:%02d",
        zoned.dayOfMonth,
        monthLabel,
        zoned.year,
        zoned.hour,
        zoned.minute,
    )
}

/**
 * Merender kartu aktivitas siap bagikan ala Strava ke [Bitmap] ARGB_8888 murni via [Canvas].
 *
 * - Tanpa peta dasar (hanya polyline rute oranye) demi menjaga privasi lokasi rumah/rute pengguna.
 * - Mendukung dua tampilan: [ShareCardStyle.DARK_NAVY] dan [ShareCardStyle.TRANSPARENT_STICKER].
 */
fun renderShareCardBitmap(
    record: StepSessionRecord,
    style: ShareCardStyle,
): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val isTransparent = style == ShareCardStyle.TRANSPARENT_STICKER

    if (isTransparent) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    } else {
        drawDarkNavyCardBackground(canvas)
    }

    drawCardHeader(canvas, record, isTransparent)

    val routeArea = RectF(96f, 224f, CARD_WIDTH_PX - 96f, 770f)
    if (!isTransparent) {
        drawRoutePanelContainer(canvas, routeArea)
    }

    val normalizedRoute = normalizeRouteToCanvas(record.routePoints, paddingFraction = 0.14f)
    if (normalizedRoute.size >= 2) {
        drawRoutePolyline(canvas, routeArea, normalizedRoute, isTransparent)
    } else {
        drawMinimalFootstepsFallback(canvas, routeArea, isTransparent)
    }

    val metricsArea = RectF(72f, 816f, CARD_WIDTH_PX - 72f, 1218f)
    drawMetricsSection(canvas, metricsArea, record, isTransparent)
    drawFooterWatermark(canvas, isTransparent)

    return bitmap
}

/**
 * Menyimpan kartu share PNG ke direktori cache (`cacheDir/step_share/`) dan membuka
 * share sheet Android (`Intent.ACTION_SEND`, `image/png`) melalui `FileProvider`.
 *
 * Mengembalikan `true` jika share sheet berhasil dibuka, atau `false` bila terjadi kendala.
 */
fun shareSessionCard(
    context: Context,
    record: StepSessionRecord,
    style: ShareCardStyle,
): Boolean {
    val bitmap = renderShareCardBitmap(record, style)
    return try {
        val shareDir = File(context.cacheDir, SHARE_CACHE_SUBDIR).apply { mkdirs() }
        pruneOldShareFiles(shareDir)

        val safeId = record.id.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank {
            System.currentTimeMillis().toString()
        }
        val fileName = "olahragakuy_step_${safeId}_${style.name.lowercase(Locale.ROOT)}.png"
        val outputFile = File(shareDir, fileName)

        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        val uri = FileProvider.getUriForFile(
            context,
            fileProviderAuthority(context),
            outputFile,
        )

        val shareCaption = buildString {
            append("Aktivitas Pelacak Langkah di OlahragaKuy: ")
            append("${formatStepCount(record.steps)} langkah • ")
            append("${formatDistance(record.distanceMeters)} • ")
            append(formatDuration(record.durationSeconds))
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareCaption)
            clipData = ClipData.newRawUri("Aktivitas OlahragaKuy", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Bagikan Aktivitas").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
        true
    } catch (_: Exception) {
        false
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private fun pruneOldShareFiles(shareDir: File) {
    runCatching {
        val files = shareDir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= MAX_CACHED_SHARE_FILES) return
        files.sortedByDescending { it.lastModified() }
            .drop(MAX_CACHED_SHARE_FILES)
            .forEach { it.delete() }
    }
}

private fun drawDarkNavyCardBackground(canvas: Canvas) {
    val fullRect = RectF(0f, 0f, CARD_WIDTH_PX.toFloat(), CARD_HEIGHT_PX.toFloat())
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            CARD_HEIGHT_PX.toFloat(),
            intArrayOf(COLOR_BG_TOP, COLOR_BG_MID, COLOR_BG_BOTTOM),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(fullRect, bgPaint)

    // Cahaya radial oranye lembut di belakang area jalur rute.
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            CARD_WIDTH_PX * 0.5f,
            500f,
            460f,
            0x28FF6500,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(CARD_WIDTH_PX * 0.5f, 500f, 460f, glowPaint)

    // Bingkai luar halus agar rapi saat dibagikan.
    val borderRect = RectF(24f, 24f, CARD_WIDTH_PX - 24f, CARD_HEIGHT_PX - 24f)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = COLOR_PANEL_BORDER
    }
    canvas.drawRoundRect(borderRect, 44f, 44f, borderPaint)
}

private fun drawCardHeader(
    canvas: Canvas,
    record: StepSessionRecord,
    isTransparent: Boolean,
) {
    val brandWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 54f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(12f, 0f, 3f, COLOR_STICKER_SHADOW)
        }
    }
    val brandOrangePaint = Paint(brandWhitePaint).apply {
        color = COLOR_ACCENT_ORANGE
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) 0xFFE3ECF7.toInt() else COLOR_TEXT_SECONDARY
        textSize = 26f
        letterSpacing = 0.08f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(10f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }
    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) COLOR_TEXT_PRIMARY else COLOR_TEXT_SECONDARY
        textSize = 28f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        if (isTransparent) {
            setShadowLayer(10f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }

    val startX = 76f
    val titleY = 122f
    val prefix = "Olahraga"
    canvas.drawText(prefix, startX, titleY, brandWhitePaint)
    val prefixWidth = brandWhitePaint.measureText(prefix)
    canvas.drawText("KUY", startX + prefixWidth, titleY, brandOrangePaint)

    canvas.drawText("PELACAK LANGKAH", startX, titleY + 42f, subtitlePaint)
    canvas.drawText(
        formatSessionDateIndonesian(record.startedAtMillis),
        CARD_WIDTH_PX - 76f,
        titleY - 4f,
        datePaint,
    )
}

private fun drawRoutePanelContainer(canvas: Canvas, rect: RectF) {
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PANEL_SURFACE
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PANEL_BORDER
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    canvas.drawRoundRect(rect, 36f, 36f, fillPaint)
    canvas.drawRoundRect(rect, 36f, 36f, strokePaint)
}

private fun drawRoutePolyline(
    canvas: Canvas,
    area: RectF,
    normalizedPoints: List<NormalizedRoutePoint>,
    isTransparent: Boolean,
) {
    val path = Path()
    val mapped = normalizedPoints.map { pt ->
        (area.left + pt.x * area.width()) to (area.top + pt.y * area.height())
    }
    mapped.forEachIndexed { index, (x, y) ->
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // Lapisan glow / casing agar polyline oranye menonjol baik di latar gelap maupun foto IG Story.
    val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = if (isTransparent) 26f else 30f
        color = if (isTransparent) COLOR_STICKER_CASING else COLOR_ACCENT_ORANGE_GLOW
    }
    canvas.drawPath(path, casingPaint)

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 14f
        color = COLOR_ACCENT_ORANGE
        if (isTransparent) {
            setShadowLayer(12f, 0f, 3f, COLOR_STICKER_SHADOW)
        }
    }
    canvas.drawPath(path, linePaint)

    // Titik awal (hijau) & titik akhir (oranye) dengan ring putih.
    val (startX, startY) = mapped.first()
    val (endX, endY) = mapped.last()

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        if (isTransparent) {
            setShadowLayer(8f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }
    val startFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_START_DOT
        style = Paint.Style.FILL
    }
    val endFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_ACCENT_ORANGE
        style = Paint.Style.FILL
    }

    canvas.drawCircle(startX, startY, 16f, ringPaint)
    canvas.drawCircle(startX, startY, 10f, startFillPaint)

    canvas.drawCircle(endX, endY, 18f, ringPaint)
    canvas.drawCircle(endX, endY, 11f, endFillPaint)
}

private fun drawMinimalFootstepsFallback(
    canvas: Canvas,
    area: RectF,
    isTransparent: Boolean,
) {
    val cx = area.centerX()
    val cy = area.centerY() - 24f

    // Lengkungan gerak tipis di belakang jejak kaki.
    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
        color = if (isTransparent) 0xCCFF6500.toInt() else 0x55FF6500
    }
    val arcPath = Path().apply {
        moveTo(cx - 170f, cy + 75f)
        quadTo(cx - 25f, cy - 125f, cx + 170f, cy - 45f)
    }
    canvas.drawPath(arcPath, arcPaint)

    val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_ACCENT_ORANGE
        style = Paint.Style.FILL
        if (isTransparent) {
            setShadowLayer(12f, 0f, 3f, COLOR_STICKER_SHADOW)
        }
    }

    // Jejak kiri.
    drawSingleFootprint(canvas, cx - 72f, cy + 16f, -14f, footPaint)
    // Jejak kanan lebih maju.
    drawSingleFootprint(canvas, cx + 70f, cy - 46f, 12f, footPaint)

    val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) COLOR_TEXT_PRIMARY else COLOR_TEXT_SECONDARY
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(10f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }
    canvas.drawText("Jejak Langkah Aktif", cx, area.bottom - 52f, captionPaint)
}

private fun drawSingleFootprint(
    canvas: Canvas,
    centerX: Float,
    centerY: Float,
    rotationDegrees: Float,
    paint: Paint,
) {
    canvas.save()
    canvas.rotate(rotationDegrees, centerX, centerY)
    // Bagian depan telapak kaki.
    canvas.drawOval(
        RectF(centerX - 32f, centerY - 62f, centerX + 32f, centerY + 12f),
        paint,
    )
    // Bagian tumit.
    canvas.drawOval(
        RectF(centerX - 22f, centerY + 22f, centerX + 22f, centerY + 72f),
        paint,
    )
    canvas.restore()
}

private fun drawMetricsSection(
    canvas: Canvas,
    area: RectF,
    record: StepSessionRecord,
    isTransparent: Boolean,
) {
    if (!isTransparent) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PANEL_SURFACE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PANEL_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(area, 36f, 36f, fillPaint)
        canvas.drawRoundRect(area, 36f, 36f, borderPaint)
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) 0xFFE3ECF7.toInt() else COLOR_TEXT_SECONDARY
        textSize = 26f
        letterSpacing = 0.06f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(10f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }

    val heroValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 68f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(14f, 0f, 4f, COLOR_STICKER_SHADOW)
        }
    }

    val heroAccentPaint = Paint(heroValuePaint).apply {
        color = COLOR_ACCENT_ORANGE
    }

    val subValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 44f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (isTransparent) {
            setShadowLayer(12f, 0f, 3f, COLOR_STICKER_SHADOW)
        }
    }

    // Baris 1: 2 metrik utama (Jarak & Langkah).
    val col2Left = area.left + area.width() * 0.27f
    val col2Right = area.left + area.width() * 0.73f
    val row1LabelY = area.top + 68f
    val row1ValueY = area.top + 148f

    canvas.drawText("JARAK", col2Left, row1LabelY, labelPaint)
    canvas.drawText(formatDistance(record.distanceMeters), col2Left, row1ValueY, heroAccentPaint)

    canvas.drawText("LANGKAH", col2Right, row1LabelY, labelPaint)
    canvas.drawText(formatStepCount(record.steps), col2Right, row1ValueY, heroValuePaint)

    // Garis pemisah horizontal halus.
    val dividerY = area.top + 204f
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) 0x66FFFFFF else COLOR_PANEL_BORDER
        strokeWidth = 2f
    }
    canvas.drawLine(area.left + 44f, dividerY, area.right - 44f, dividerY, dividerPaint)

    // Baris 2: 3 metrik sekunder (Waktu, Pace, Kalori).
    val col3Left = area.left + area.width() * (1f / 6f)
    val col3Mid = area.centerX()
    val col3Right = area.left + area.width() * (5f / 6f)
    val row2LabelY = dividerY + 62f
    val row2ValueY = dividerY + 124f

    canvas.drawText("WAKTU", col3Left, row2LabelY, labelPaint)
    canvas.drawText(formatDuration(record.durationSeconds), col3Left, row2ValueY, subValuePaint)

    canvas.drawText("PACE", col3Mid, row2LabelY, labelPaint)
    canvas.drawText("${formatPace(record.paceSecPerKm)} /km", col3Mid, row2ValueY, subValuePaint)

    canvas.drawText("KALORI", col3Right, row2LabelY, labelPaint)
    canvas.drawText("${record.caloriesKcal} kkal", col3Right, row2ValueY, subValuePaint)
}

private fun drawFooterWatermark(
    canvas: Canvas,
    isTransparent: Boolean,
) {
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTransparent) 0xE6FFFFFF.toInt() else COLOR_TEXT_SECONDARY
        textSize = 25f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        if (isTransparent) {
            setShadowLayer(10f, 0f, 2f, COLOR_STICKER_SHADOW)
        }
    }
    canvas.drawText(
        "OlahragaKuy • Bagikan Progres Tanpa Tag Lokasi",
        CARD_WIDTH_PX * 0.5f,
        CARD_HEIGHT_PX - 62f,
        footerPaint,
    )
}
