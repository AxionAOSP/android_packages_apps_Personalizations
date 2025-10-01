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
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.foundation.*
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
import com.android.settings.R

private const val KEY_WIDGETS = "lockscreen_widgets_extras"
private const val KEY_ENABLED = "lockscreen_widgets_enabled"

data class WidgetItem(
    val name: String,
    val span: Int = 1,
    val iconSize: Dp = 20.dp,
    val shape: Shape = CircleShape,
    val small: Boolean = span == 1,
    val padding: Dp = if (small) 0.dp else 32.dp,
    val alignment: Alignment = if (small) Alignment.Center else Alignment.CenterStart,
    val arrangement: Arrangement.Horizontal = if (small) Arrangement.Center else Arrangement.Start
) {
    @Composable
    fun Render(
        onRemove: () -> Unit,
        showRemove: Boolean = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant(), shape),
            contentAlignment = alignment
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arrangement,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = padding, end = padding)
            ) {
                Icon(
                    imageVector = WidgetIcon(name),
                    contentDescription = WidgetLabel(name),
                    tint = onSurface(),
                    modifier = Modifier.size(iconSize)
                )
                if (!small) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = WidgetLabel(name),
                        fontSize = 14.sp,
                        color = onSurface(),
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee()
                    )
                }
            }
            if (showRemove) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(iconSize)
                        .background(Color.Red, CircleShape)
                        .clip(CircleShape)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "−",
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }
    }
}

fun load(context: Context): List<WidgetItem> {
    return Settings.System.getString(context.contentResolver, KEY_WIDGETS)
        ?.split(",")
        ?.mapNotNull { 
            val parts = it.split(":")
            val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val span = parts.getOrNull(1)?.toIntOrNull() ?: 1
            WidgetItem(name, span)
        } ?: emptyList()
}

fun save(context: Context, widgets: List<WidgetItem>) {
    val serialized = widgets.joinToString(",") { "${it.name}:${it.span}" }
    Settings.System.putStringForUser(
        context.contentResolver,
        KEY_WIDGETS,
        serialized,
        UserHandle.USER_CURRENT
    )
    Settings.System.putIntForUser(
        context.contentResolver,
        KEY_ENABLED,
        if (widgets.isEmpty()) 0 else 1,
        UserHandle.USER_CURRENT
    )
}
