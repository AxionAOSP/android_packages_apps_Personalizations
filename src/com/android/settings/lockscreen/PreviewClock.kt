/*
 * Copyright (C) 2025 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.lockscreen

import android.content.Context
import android.graphics.Typeface
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import com.android.settings.R
import kotlinx.coroutines.*
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

val Context.scaleRatio: Float
    get() {
        val displayMetrics = resources.displayMetrics
        val sw = minOf(displayMetrics.widthPixels, displayMetrics.heightPixels) / displayMetrics.density
        val ratio = sw / 420f
        return ratio
    }

enum class DateAlignment {
    START, CENTER, END
}

private fun Context.getDigitDrawables(prefix: String): Array<Int> {
    return (0..9).map { digit ->
        val resourceName = "${prefix}_$digit"
        val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
        if (resourceId == 0) {
            throw IllegalArgumentException("Drawable not found: $resourceName")
        }
        resourceId
    }.toTypedArray()
}

@Composable
fun PreviewClock() {
    val context = LocalContext.current
    val scale = context.scaleRatio
    var isLoadedFromSettings by remember { mutableStateOf(false) }
    
    val clocks = listOf(
        NtypeClock(),
        NDotClock(),
        GraphicClock(),
        GeneralClock(),
        LondonUGClock(),
        QuickLookClock(),
        SpaceAgeClock(),
        PolylineClock()
    )

    val pagerState = rememberPagerState { clocks.size }
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val json = Settings.Secure.getString(
                    context.contentResolver,
                    "lock_screen_custom_clock_face"
                )

                if (!json.isNullOrEmpty()) {
                    val clockId = JSONObject(json).optString("clockId")
                    val index = clocks.indexOfFirst { it.name == clockId }
                    if (index >= 0) {
                        withContext(Dispatchers.Main) {
                            pagerState.scrollToPage(index)
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                isLoadedFromSettings = true
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, isLoadedFromSettings) {
        if (isLoadedFromSettings) {
            val selectedClock = clocks[pagerState.currentPage]
            val timestamp = System.currentTimeMillis()
            val json = JSONObject().apply {
                put("clockId", selectedClock.name)
                put("metadata", JSONObject().apply {
                    put("appliedTimestamp", timestamp)
                })
                put("axes", org.json.JSONArray())
            }.toString()

            withContext(Dispatchers.IO) {
                try {
                    Settings.Secure.putString(
                        context.contentResolver,
                        "lock_screen_custom_clock_face",
                        json
                    )
                } catch (e: Exception) {
                }
            }
        }
    }

    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .animateContentSize()
    ) {
        val currentClock = clocks[pagerState.currentPage]
        val targetAlignment = currentClock.dateAlignment

        AnimatedVisibility(
            visible = targetAlignment != null,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                contentAlignment = when (targetAlignment) {
                    DateAlignment.START -> Alignment.CenterStart
                    DateAlignment.CENTER -> Alignment.Center
                    DateAlignment.END -> Alignment.CenterEnd
                    null -> Alignment.Center
                }
            ) {
                Text(
                    text = dateFormat.format(currentTime),
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                clocks[page].Render(currentTime)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClockIndicator(pageCount = clocks.size, currentPage = pagerState.currentPage)
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ClockIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        animateColorAsState(
                            targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                            animationSpec = tween(durationMillis = 300)
                        ).value
                    )
            )
        }
    }
}

sealed class ClockItem {
    abstract val name: String
    abstract val dateAlignment: DateAlignment?
    
    @Composable
    abstract fun Render(currentTime: Date)

    class GeneralClock(
        override val name: String,
        val showDate: Boolean = true
    ) : ClockItem() {
        override val dateAlignment: DateAlignment? = null

        @Composable
        override fun Render(currentTime: Date) {
            val context = LocalContext.current
            val is24Hour = DateFormat.is24HourFormat(context)
            val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

            val scale = context.scaleRatio
            val digitSpacing = 4.dp
            val dotSize = 6.dp
            val dotMargin = 4.dp

            val digitResIds = context.getDigitDrawables("intervar")

            val bitmaps = remember {
                digitResIds.map { context.getDrawable(it)!!.toBitmap().asImageBitmap() }
            }

            val density = LocalDensity.current
            val dotRadiusPx = with(density) { (dotSize / 2).toPx() }
            val dotMarginPx = with(density) { dotMargin.toPx() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = Dimens.ClockSidePadding),
                horizontalAlignment = Alignment.Start
            ) {
                if (showDate) {
                    Text(
                        text = dateFormat.format(currentTime),
                        fontSize = Dimens.ClockDateFont,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(Dimens.ClockSpacer))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val calendar = Calendar.getInstance()
                        calendar.time = currentTime
                        val hour = calendar.get(if (is24Hour) Calendar.HOUR_OF_DAY else Calendar.HOUR)
                            .let { if (it == 0) 12 else it }
                        val minute = calendar.get(Calendar.MINUTE)

                        val hourStr = hour.toString()
                        val minuteStr = String.format("%02d", minute)
                        val timeDigits = (hourStr + minuteStr).toCharArray()

                        var xOffset = 0f

                        timeDigits.forEachIndexed { index, char ->
                            val bmp = bitmaps.getOrNull(char.digitToIntOrNull() ?: 0) ?: return@forEachIndexed
                            val scaledW = bmp.width * scale
                            val scaledH = bmp.height * scale
                            val yOffset = (size.height - scaledH) / 2f

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(xOffset.toInt(), yOffset.toInt()),
                                dstSize = IntSize(scaledW.toInt(), scaledH.toInt()),
                                colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn)
                            )

                            xOffset += scaledW + with(density) { digitSpacing.toPx() }

                            if (index == hourStr.lastIndex) {
                                val centerX = xOffset
                                val inwardOffset = 24.dp
                                val inwardOffsetPx = with(density) { inwardOffset.toPx() }

                                val topDotY = inwardOffsetPx + dotRadiusPx
                                val bottomDotY = size.height - inwardOffsetPx - dotRadiusPx

                                drawCircle(
                                    color = Color.White,
                                    radius = dotRadiusPx,
                                    center = Offset(centerX, topDotY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = dotRadiusPx,
                                    center = Offset(centerX, bottomDotY)
                                )

                                xOffset += with(density) { (digitSpacing.toPx() * 2) + dotRadiusPx }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.ClockSpacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = stringResource(R.string.temperature_prev),
                        fontSize = Dimens.ClockDateFont,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(Dimens.WeatherSpacerSmall))

                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = stringResource(R.string.weather_prev),
                        modifier = Modifier.size(Dimens.WeatherIcon),
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(Dimens.WeatherSpacerLarge))

                    Text(
                        text = stringResource(R.string.weather_prev),
                        fontSize = Dimens.ClockDateFont,
                        color = Color.White
                    )
                }
            }
        }
    }

    class QuickLookClock(
        override val name: String,
        val showDate: Boolean = true
    ) : ClockItem() {
        override val dateAlignment: DateAlignment? = null
        
        @Composable
        override fun Render(currentTime: Date) {
            val context = LocalContext.current
            val is24Hour = DateFormat.is24HourFormat(context)
            val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            val timeFormat = SimpleDateFormat(if (is24Hour) "H:mm" else "h:mm", Locale.getDefault())
            val ndot = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = Dimens.ClockSidePadding),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = timeFormat.format(currentTime),
                    fontFamily = ndot,
                    fontSize = Dimens.ClockTimeFont,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(Dimens.ClockSpacer))

                Text(
                    text = dateFormat.format(currentTime),
                    fontSize = Dimens.ClockDateFont,
                    fontWeight = FontWeight.Thin,
                    fontFamily = ndot,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(Dimens.ClockSpacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = stringResource(R.string.weather_prev),
                        modifier = Modifier.size(Dimens.WeatherIcon),
                        tint = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(Dimens.WeatherSpacerLarge))

                    Text(
                        text = stringResource(R.string.temperature_prev),
                        fontSize = Dimens.ClockDateFont,
                        fontWeight = FontWeight.Thin,
                        fontFamily = ndot,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(Dimens.WeatherSpacerSmall))

                    Text(
                        text = stringResource(R.string.weather_prev),
                        fontSize = Dimens.ClockDateFont,
                        fontWeight = FontWeight.Thin,
                        fontFamily = ndot,
                        color = Color.White
                    )
                }
            }
        }
    }

    class BitmapDigitClock(
        override val name: String,
        val digitResIds: Array<Int>,
        val digitSpacing: Dp,
        val scale: Float = 1f,
        val horizontalOffset: Dp = 0.dp,
        override val dateAlignment: DateAlignment? = DateAlignment.CENTER
    ) : ClockItem() {
        @Composable
        override fun Render(currentTime: Date) {
            val context = LocalContext.current
            val is24Hour = DateFormat.is24HourFormat(context)
            val timeStr = SimpleDateFormat(if (is24Hour) "Hmm" else "hmm", Locale.getDefault())
                .format(currentTime)

            val bitmaps = remember(digitResIds) {
                digitResIds.map { resId ->
                    context.getDrawable(resId)!!.toBitmap().asImageBitmap()
                }
            }

            val maxWidth = remember { bitmaps.maxOf { it.width } }
            val maxHeight = remember { bitmaps.maxOf { it.height } }
            val maxAspectRatio = remember { maxWidth.toFloat() / maxHeight.toFloat() }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 32.dp)
            ) {
                val availableWidth = this.maxWidth
                val numDigits = timeStr.length
                val totalSpacing = digitSpacing * (numDigits - 1)

                val digitWidth = (availableWidth - totalSpacing) / numDigits
                val digitHeight = digitWidth / maxAspectRatio

                val finalDigitWidth = digitWidth * scale
                val finalDigitHeight = digitHeight * scale
                val finalSpacing = digitSpacing * scale

                val totalWidth = (finalDigitWidth * numDigits) + (finalSpacing * (numDigits - 1))
                val startOffset = (availableWidth - totalWidth) / 2 + horizontalOffset

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = startOffset)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(finalSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        timeStr.forEach { char ->
                            val digitIndex = char.digitToIntOrNull() ?: 0
                            val resId = digitResIds.getOrElse(digitIndex) { digitResIds.first() }

                            Box(
                                modifier = Modifier
                                    .width(finalDigitWidth)
                                    .height(finalDigitHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(resId),
                                    contentDescription = char.toString(),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    class GraphicClock(
        override val name: String,
        override val dateAlignment: DateAlignment? = DateAlignment.CENTER
    ) : ClockItem() {
        @Composable
        override fun Render(currentTime: Date) {
            val context = LocalContext.current
            val clockW = context.scaleRatio * 200.dp
            val clockH = context.scaleRatio * 100.dp
            
            val tickBitmap = remember {
                context.getDrawable(R.drawable.graphic_tick)!!.toBitmap().asImageBitmap()
            }
            val tickLightBitmap = remember {
                context.getDrawable(R.drawable.graphic_tick_light)!!.toBitmap().asImageBitmap()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = clockW, height = clockH)) {
                    val w = size.width
                    val h = size.height
                    val centerX = w / 2
                    val centerY = h / 2
                    
                    val scaleRatio = context.scaleRatio
                    val tickWidth = tickBitmap.width * scaleRatio
                    val tickHeight = tickBitmap.height * scaleRatio
                    
                    val tickLeft = (w - tickWidth) / 2f
                    val tickTop = (h - tickHeight) / 2f
                    
                    drawImage(
                        image = tickLightBitmap,
                        dstOffset = IntOffset(tickLeft.toInt(), tickTop.toInt()),
                        dstSize = IntSize(tickWidth.toInt(), tickHeight.toInt()),
                        colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn)
                    )
                    
                    drawImage(
                        image = tickBitmap,
                        dstOffset = IntOffset(tickLeft.toInt(), tickTop.toInt()),
                        dstSize = IntSize(tickWidth.toInt(), tickHeight.toInt()),
                        colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn)
                    )

                    val calendar = Calendar.getInstance()
                    calendar.time = currentTime
                    val hours = calendar.get(Calendar.HOUR)
                    val minutes = calendar.get(Calendar.MINUTE)
                    val seconds = calendar.get(Calendar.SECOND)

                    val hourAngle = (hours + minutes / 60f) * 5f
                    val minuteAngle = minutes + seconds / 60f
                    val secondAngle = seconds.toFloat()

                    val handSize = 4.dp.toPx()
                    
                    drawHand(centerX, centerY, h, hourAngle, handSize * 2, 0.22f to 0.38f, Color.White)
                    drawHand(centerX, centerY, h, minuteAngle, handSize, 0.22f to 0.42f, Color.White)
                    drawHand(centerX, centerY, h, secondAngle, handSize / 2, 0.19f to 0.42f, Color(0xFFD71921))

                    val dotSize = 6.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = dotSize * 1.5f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = Color(0xFFD71921),
                        radius = dotSize,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }

        private fun DrawScope.drawHand(
            centerX: Float,
            centerY: Float,
            height: Float,
            position: Float,
            thickness: Float,
            multipliers: Pair<Float, Float>,
            color: Color
        ) {
            val angleRad = Math.toRadians((position * 6 - 90).toDouble())
            val startLength = multipliers.first * height
            val endLength = multipliers.second * height
            val startX = centerX - cos(angleRad).toFloat() * startLength
            val startY = centerY - sin(angleRad).toFloat() * startLength
            val endX = centerX + cos(angleRad).toFloat() * endLength
            val endY = centerY + sin(angleRad).toFloat() * endLength
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun QuickLookClock(): ClockItem =
    ClockItem.QuickLookClock("OLD_QUICKLOOK", showDate = true)

@Composable
fun GeneralClock(): ClockItem =
    ClockItem.GeneralClock("GENERAL", showDate = true)

@Composable
fun NtypeClock(): ClockItem {
    val context = LocalContext.current
    return ClockItem.BitmapDigitClock(
        "DEFAULT",
        digitResIds = context.getDigitDrawables("ntype"),
        digitSpacing = 5.dp,
        scale = context.scaleRatio * 0.75f,
        horizontalOffset = 0.dp,
        dateAlignment = DateAlignment.CENTER
    )
}

@Composable
fun SpaceAgeClock(): ClockItem {
    val context = LocalContext.current
    return ClockItem.BitmapDigitClock(
        "SPACE_AGE",
        digitResIds = context.getDigitDrawables("space_age"),
        digitSpacing = (-0.3334).dp,
        scale = context.scaleRatio * 0.75f,
        horizontalOffset = 0.dp,
        dateAlignment = DateAlignment.CENTER
    )
}

@Composable
fun PolylineClock(): ClockItem {
    val context = LocalContext.current
    return ClockItem.BitmapDigitClock(
        "POLYLINE",
        digitResIds = context.getDigitDrawables("polyline"),
        digitSpacing = 5.dp,
        scale = context.scaleRatio * 0.75f,
        horizontalOffset = 0.dp,
        dateAlignment = DateAlignment.CENTER
    )
}

@Composable
fun LondonUGClock(): ClockItem {
    val context = LocalContext.current
    return ClockItem.BitmapDigitClock(
        "LONDON_UG",
        digitResIds = context.getDigitDrawables("london_ug"),
        digitSpacing = 8.dp,
        scale = context.scaleRatio * 0.45f,
        horizontalOffset = 0.dp,
        dateAlignment = DateAlignment.CENTER
    )
}

@Composable
fun NDotClock(): ClockItem {
    val context = LocalContext.current
    return ClockItem.BitmapDigitClock(
        "NDOT",
        digitResIds = context.getDigitDrawables("ndot"),
        digitSpacing = 5.dp,
        scale = context.scaleRatio * 0.75f,
        horizontalOffset = 0.dp,
        dateAlignment = DateAlignment.CENTER
    )
}

@Composable
fun GraphicClock(): ClockItem =
    ClockItem.GraphicClock("GRAPHIC", dateAlignment = null)
