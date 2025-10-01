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

import android.graphics.Typeface
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

@Composable
fun PreviewClock() {
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000L)
        }
    }

    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val ndot = FontFamily(Typeface.create("nothingdot57", Typeface.NORMAL))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ClockTopPadding, start = Dimens.ClockSidePadding),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = dateFormat.format(currentTime),
            fontSize = Dimens.ClockDateFont,
            fontWeight = FontWeight.Thin,
            fontFamily = ndot,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(Dimens.ClockSpacer))

        Text(
            text = timeFormat.format(currentTime),
            fontFamily = ndot,
            fontSize = Dimens.ClockTimeFont,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(Dimens.ClockSpacer))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = stringResource(R.string.temperature_prev),
                fontSize = Dimens.ClockDateFont,
                fontWeight = FontWeight.Thin,
                fontFamily = ndot,
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
                fontWeight = FontWeight.Thin,
                fontFamily = ndot,
                color = Color.White
            )
        }
    }
}
