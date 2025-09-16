/*
 * Copyright (C) 2025 AxionOS Project
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
package com.custom.settings.fragments

import android.os.Bundle
import android.os.SystemProperties
import android.view.View
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.preferences.BasePreferenceFragment
import com.android.settings.preferences.SecureSettingSeekBarPreference

class Performance : BasePreferenceFragment(R.xml.performance) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val freqLittlePref = findPreference<SecureSettingSeekBarPreference>("axion_min_freq_boost")
        val freqBigPref = findPreference<SecureSettingSeekBarPreference>("axion_min_freq_big_boost")

        val freqProp = SystemProperties.get("persist.sys.ax_max_cpu_freqs", "")
        val maxFreqs = freqProp.split(",")

        if (maxFreqs.size < 2) {
            preferenceScreen.removePreference(freqLittlePref as Preference)
            preferenceScreen.removePreference(freqBigPref as Preference)
            return
        }

        val littleMax = maxFreqs[0].toIntOrNull() ?: 0
        val bigMax = maxFreqs[1].toIntOrNull() ?: 0

        freqLittlePref?.setMax(littleMax)
        freqLittlePref?.setDefaultValue(1000000)
        freqBigPref?.setMax(bigMax)
        freqBigPref?.setDefaultValue(1000000)
    }
}
