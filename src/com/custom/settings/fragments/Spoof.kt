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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.preferences.KeyboxDataPreference
import com.android.settings.preferences.BasePreferenceFragment
import com.custom.settings.fragments.spoof.PifRepository
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class Spoof : BasePreferenceFragment(R.xml.spoof) {

    private var keyboxDataPreference: KeyboxDataPreference? = null

    private lateinit var mGmsSpoof: Preference
    private lateinit var mPifJsonFilePreference: Preference
    private lateinit var mUpdateJsonButton: Preference
    private lateinit var mUpdateJsonGoogle: Preference

    private val handler = Handler(Looper.getMainLooper())
    private val pifRepository = PifRepository()

    private val keyboxFilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                keyboxDataPreference?.handleFileSelected(result.data?.data)
            }
        }

    private val pifJsonFilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    loadPifJson(uri)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keyboxDataPreference = findPreference("keybox_data_setting") as? KeyboxDataPreference
        keyboxDataPreference?.setFilePickerLauncher(keyboxFilePickerLauncher)

        mGmsSpoof = findPreference(SYS_GMS_SPOOF)!!
        mPifJsonFilePreference = findPreference(KEY_PIF_JSON_FILE_PREFERENCE)!!
        mUpdateJsonButton = findPreference(KEY_UPDATE_JSON_BUTTON)!!
        mUpdateJsonGoogle = findPreference(KEY_UPDATE_JSON_GOOGLE_BUTTON)!!

        mGmsSpoof.setOnPreferenceChangeListener { _, _ -> true }

        mPifJsonFilePreference.setOnPreferenceClickListener { 
            openPifJsonFileSelector()
            true 
        }
        
        mUpdateJsonButton.setOnPreferenceClickListener { 
            updatePropertiesFromUrl(PIF_JSON_URL)
            true 
        }
        
        mUpdateJsonGoogle.setOnPreferenceClickListener { 
            fetchBetaPif()
            true 
        }

        findPreference<Preference>("show_pif_properties")?.setOnPreferenceClickListener { 
            showPropertiesDialog()
            true 
        }
    }

    private fun openPifJsonFileSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        pifJsonFilePickerLauncher.launch(intent)
    }

    private fun showPropertiesDialog() {
        val properties = pifRepository.getCurrentProperties()
        
        if (properties.isEmpty()) {
            Toast.makeText(
                requireContext(), 
                R.string.error_loading_properties, 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val displayJson = JSONObject(properties).toString(4).replace("\\/", "/")

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.show_pif_properties_title)
            .setMessage(displayJson)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun updatePropertiesFromUrl(urlString: String) {
        showLoadingToast(R.string.toast_updating_from_repo)
        
        Thread {
            when (val result = pifRepository.fetchFromUrl(urlString)) {
                is PifRepository.PifResult.Success -> {
                    pifRepository.applyPifProperties(result.pifData)
                    showSuccessToast(result.model)
                }
                is PifRepository.PifResult.Error -> {
                    showErrorToast(result.message)
                }
            }
        }.start()
    }

    private fun fetchBetaPif() {
        showLoadingToast(R.string.toast_fetching_from_google)
        
        Thread {
            when (val result = pifRepository.fetchBetaPif()) {
                is PifRepository.PifResult.Success -> {
                    pifRepository.applyPifProperties(result.pifData)
                    handler.post {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_google_fetch_success, result.model),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                is PifRepository.PifResult.Error -> {
                    handler.post {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_google_fetch_failure, result.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    private fun loadPifJson(uri: Uri) {
        try {
            requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.readBytes().toString(StandardCharsets.UTF_8)
                
                when (val result = pifRepository.parseFromString(jsonString)) {
                    is PifRepository.PifResult.Success -> {
                        pifRepository.applyPifProperties(result.pifData)
                        Toast.makeText(
                            requireContext(), 
                            R.string.toast_import_success, 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is PifRepository.PifResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_parse_error, result.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading PIF JSON from file", e)
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_file_error, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showLoadingToast(messageResId: Int) {
        handler.post {
            Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessToast(model: String) {
        handler.post {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_spoofing_success, model),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showErrorToast(message: String) {
        handler.post {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_spoofing_failure_error, message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val TAG = "Spoof"
        private const val SYS_GMS_SPOOF = "persist.sys.pixelprops.gms"
        private const val KEY_PIF_JSON_FILE_PREFERENCE = "pif_json_file_preference"
        private const val KEY_UPDATE_JSON_BUTTON = "update_pif_json"
        private const val KEY_UPDATE_JSON_GOOGLE_BUTTON = "update_pif_json_google"
        private const val PIF_JSON_URL =
            "https://raw.githubusercontent.com/AxionAOSP/PlayIntegrityFix/refs/heads/lineage-22.1/pif.json"
    }
}
