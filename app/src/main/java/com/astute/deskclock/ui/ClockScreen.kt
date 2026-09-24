package com.astute.deskclock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import android.graphics.Typeface

@Composable
fun DeskClockApp() {
    // Store epoch seconds — state only changes when the SECOND changes,
    // so Canvas/text recomposition happens exactly once per second. No 120Hz churn.
    var nowEpochSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowEpochSeconds = System.currentTimeMillis() / 1000
        }
    }

    val dateTime = remember(nowEpochSeconds) {
        Instant.ofEpochSecond(nowEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val timeText = DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
        val dateText = DateTimeFormatter.ofPattern("EEE, MMM d").format(dateTime)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBoundaryTicks(size)
        }

        CenterClockContent(
            timeText = timeText,
            dateText = dateText,
            widthPx = widthPx,
            heightPx = heightPx
        )
    }
}

@Composable
private fun CenterClockContent(
    timeText: String,
    dateText: String,
    widthPx: Float,
    heightPx: Float
) {
    val density = LocalDensity.current.density

    // ---- Responsive typography sizing (density-correct px -> sp) ----
    // In landscape, height drives the size so text never clips vertically.
    val basePx = min(widthPx * 0.42f / 5f, heightPx * 0.45f) // ~5 chars of time text
    val timeSp = with(LocalDensity.current) { (basePx / density).toInt().coerceIn(48, 220).sp }
    val dateSp = (timeSp.value * 0.14f).coerceIn(12f, 28f).sp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dateText.uppercase(),
                fontSize = dateSp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(timeSp * 0.10f))

            Text(
                text = timeText,
                fontSize = timeSp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Stadium-boundary tick dial.
 * 60 ticks distributed along the rectangular perimeter of ANY aspect ratio,
 * pointing inward toward the center. Major (every 5th) = white/long/thick,
 * minor = dim grey/short/thin.
 */
private fun DrawScope.drawBoundaryTicks(size: Size) {
    val w = size.width
    val h = size.height

    val insetPx = 10.dp.toPx()
    val margin = 16.dp.toPx()          // gap between screen edge and tick base

    val majorLen = h.coerceAtMost(w * 0.25f) * 0.09f
    val minorLen = majorLen * 0.55f
    val majorStroke = 3.dp.toPx()
    val minorStroke = 1.5.dp.toPx()

    val majorColor = Color(0xFFFFFFFF)
    val minorColor = Color(0x33FFFFFF)

    val totalTicks = 60
    val majorEvery = 5

    val left = insetPx + margin
    val right = w - insetPx - margin
    val top = insetPx + margin
    val bottom = h - insetPx - margin

    // Perimeter of usable rect, split so each side gets a proportional
    // number of ticks — wide sides get more ticks (adaptive to aspect ratio)
    val sideW = right - left
    val sideH = bottom - top
    val perim = 2 * (sideW + sideH)

    val nHoriz = ((totalTicks / 2f) * (sideW / (perim / 2f))).toInt().coerceAtLeast(4)
    val nVert = (totalTicks / 2) - nHoriz

    fun tick(x: Float, y: Float, dirX: Float, dirY: Float, index: Int) {
        val isMajor = index % majorEvery == 0
        val len = if (isMajor) majorLen else minorLen
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(x, y),
            end = Offset(x + dirX * len, y + dirY * len),
            strokeWidth = if (isMajor) majorStroke else minorStroke
        )
    }

    var i = 0

    // TOP edge — pointing down
    repeat(nHoriz) { j ->
        val x = left + sideW * j / (nHoriz - 1)
        tick(x, top, 0f, 1f, i++)
    }

    // RIGHT edge — pointing left
    repeat(nVert) { j ->
        val y = top + sideH * j / (nVert - 1)
        tick(right, y, -1f, 0f, i++)
    }

    // BOTTOM edge — pointing up
    repeat(nHoriz) { j ->
        val x = right - sideW * j / (nHoriz - 1)
        tick(x, bottom, 0f, -1f, i++)
    }

    // LEFT edge — pointing right
    repeat(nVert) { j ->
        val y = bottom - sideH * j / (nVert - 1)
        tick(left, y, 1f, 0f, i++)
    }
}
