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
package com.android.settings.customization

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class TopLevelThemePreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {

    private val themePackage = "com.android.axion.themepicker"
    private val themeClass = "com.android.axion.themepicker.ui.MainActivity"

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val preference = screen.findPreference<Preference>(preferenceKey) ?: return
        preference.title = getTitle()
    }

    private fun getTitle(): CharSequence {
        return mContext.getString(R.string.customization_title) 
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preferenceKey == preference.key) {
            val intent = Intent().setComponent(ComponentName(themePackage, themeClass))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            mContext.startActivity(intent)
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }
}
