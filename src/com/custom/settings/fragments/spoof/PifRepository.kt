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
package com.custom.settings.fragments.spoof

import android.os.SystemProperties
import android.util.Log
import org.json.JSONObject
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.random.Random

class PifRepository {

    sealed class PifResult {
        data class Success(val model: String, val pifData: JSONObject) : PifResult()
        data class Error(val message: String, val exception: Exception? = null) : PifResult()
    }

    fun fetchBetaPif(): PifResult {
        return try {
            Log.d(TAG, "Fetching Pixel Beta metadata from Google Developer...")

            val versionsHtml = fetchUrl("$GOOGLE_URL/about/versions")

            val versionPattern = Regex("""https://developer\.android\.com/about/versions/(\d+)""")
            val versions = versionPattern.findAll(versionsHtml)
                .map { it.groupValues[1].toInt() }
                .toSet()
                .sortedDescending()

            if (versions.isEmpty()) {
                return PifResult.Error("Could not find any Android version pages")
            }

            Log.d(TAG, "Found versions: $versions")

            for (version in versions) {
                val versionPage = "$GOOGLE_URL/about/versions/$version"
                Log.d(TAG, "Checking version page: $versionPage")
                
                try {
                    val versionHtml = fetchUrl(versionPage)

                    val qprPattern = Regex("""href="(/about/versions/$version/qpr(\d+)/download-ota)"""")
                    val qprMatches = qprPattern.findAll(versionHtml)
                        .map { match ->
                            val path = match.groupValues[1]
                            val qprNumber = match.groupValues[2].toInt()
                            qprNumber to path
                        }
                        .toList()
                        .sortedByDescending { it.first }

                    if (qprMatches.isEmpty()) {
                        Log.d(TAG, "No QPR beta pages found for version $version")
                        continue
                    }

                    for ((qprNum, qprPath) in qprMatches) {
                        val otaPage = GOOGLE_URL + qprPath
                        Log.d(TAG, "Trying OTA page: $otaPage (QPR$qprNum)")

                        try {
                            val otaHtml = fetchUrl(otaPage)

                            val otaUrlList = Regex("""href="(https://dl\.google\.com/[^"]*ota/([^/"]+_beta)[^"]*?)"""")
                                .findAll(otaHtml)
                                .map { it.groupValues[1] to it.groupValues[2] }
                                .toList()

                            if (otaUrlList.isEmpty()) {
                                Log.d(TAG, "No beta OTA URLs found on this page, trying next...")
                                continue
                            }

                            Log.d(TAG, "Found ${otaUrlList.size} beta devices")

                            val devices = mutableListOf<Triple<String, String, String>>()
                            
                            for ((otaUrl, product) in otaUrlList) {
                                val urlIndex = otaHtml.indexOf(otaUrl)
                                if (urlIndex == -1) continue
                                
                                val htmlBefore = otaHtml.substring(0, urlIndex)
                                
                                val tdPattern = Regex("""<td[^>]*>([^<]+)</td>""")
                                val tdMatches = tdPattern.findAll(htmlBefore).toList()
                                
                                if (tdMatches.isNotEmpty()) {
                                    val model = tdMatches.last().groupValues[1].trim()
                                    val device = product.replace("_beta", "")
                                    
                                    devices.add(Triple(model, product, otaUrl))
                                    Log.d(TAG, "Matched: $model -> $product")
                                }
                            }

                            if (devices.isEmpty()) {
                                Log.d(TAG, "Could not match devices to OTA URLs, trying next...")
                                continue
                            }

                            val idx = Random.nextInt(devices.size)
                            val (model, product, otaUrl) = devices[idx]
                            val device = product.replace("_beta", "")

                            Log.d(TAG, "Selected: $model ($product) from Android $version QPR$qprNum")
                            Log.d(TAG, "OTA URL: $otaUrl")

                            Log.d(TAG, "Fetching first 4KB from OTA...")
                            val partialData = fetchPartialUrl(otaUrl, 4096)

                            val fingerprintMatch = Regex("""post-build=(.*)""")
                                .find(partialData)
                                ?: return PifResult.Error("Could not extract fingerprint from OTA metadata")

                            val securityPatchMatch = Regex("""security-patch-level=(.*)""")
                                .find(partialData)
                                ?: return PifResult.Error("Could not extract security patch from OTA metadata")

                            val fingerprint = fingerprintMatch.groupValues[1].trim()
                            val securityPatch = securityPatchMatch.groupValues[1].trim()

                            Log.d(TAG, "Fingerprint: $fingerprint")
                            Log.d(TAG, "Security Patch: $securityPatch")

                            val pifJson = JSONObject().apply {
                                put("MANUFACTURER", "Google")
                                put("MODEL", model)
                                put("PRODUCT", product)
                                put("DEVICE", device)
                                put("FINGERPRINT", fingerprint)
                                put("SECURITY_PATCH", securityPatch)
                                put("DEVICE_INITIAL_SDK_INT", "32")
                            }

                            return PifResult.Success(model, pifJson)

                        } catch (e: Exception) {
                            Log.d(TAG, "Failed to fetch from QPR$qprNum: ${e.message}")
                            continue
                        }
                    }

                } catch (e: Exception) {
                    Log.d(TAG, "Failed to fetch version $version: ${e.message}")
                    continue
                }
            }

            return PifResult.Error("Could not find any valid beta OTA pages")

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Google developer site", e)
            PifResult.Error("Failed to fetch from Google: ${e.message}", e)
        }
    }

    fun fetchFromUrl(urlString: String): PifResult {
        return try {
            Log.d(TAG, "Fetching PIF from URL: $urlString")
            val json = URL(urlString).readBytes().toString(StandardCharsets.UTF_8)
            val jsonObject = JSONObject(json)
            val model = jsonObject.optString("MODEL", "Unknown")
            PifResult.Success(model, jsonObject)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PIF JSON from URL", e)
            PifResult.Error("Failed to download from repository: ${e.message}", e)
        }
    }

    fun parseFromString(jsonString: String): PifResult {
        return try {
            val jsonObject = JSONObject(jsonString)
            val model = jsonObject.optString("MODEL", "Unknown")
            PifResult.Success(model, jsonObject)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PIF JSON", e)
            PifResult.Error("Failed to parse JSON: ${e.message}", e)
        }
    }

    fun applyPifProperties(jsonObject: JSONObject) {
        val keys = mutableListOf<String>()
        
        for (entry in jsonObject.keys()) {
            val key = entry
            val value = jsonObject.getString(key)
            val pifKey = when (key) {
                "MANUFACTURER" -> "MF"
                "MODEL" -> "MD"
                "FINGERPRINT" -> "FP"
                "PRODUCT" -> "PR"
                "DEVICE" -> "DV"
                "SECURITY_PATCH" -> "SP"
                "DEVICE_INITIAL_SDK_INT" -> "ISDK"
                else -> key
            }
            SystemProperties.set("persist.sys.propshooks_$pifKey", value)
            keys.add(pifKey)
        }

        val keysCsv = keys.joinToString(",")
        SystemProperties.set("persist.sys.propshooks_keys", keysCsv)

        val hash = MessageDigest.getInstance("SHA-256").digest(
            keys.sorted().joinToString("") { k -> 
                k + SystemProperties.get("persist.sys.propshooks_$k", "") 
            }.toByteArray()
        ).joinToString("") { "%02x".format(it) }

        SystemProperties.set("persist.sys.propshooks_data_hash", hash)
        
        Log.d(TAG, "PIF properties applied successfully")
    }

    fun getCurrentProperties(): Map<String, String> {
        val keys = SystemProperties.get("persist.sys.propshooks_keys", "")
        if (keys.isEmpty()) {
            return emptyMap()
        }

        val commonKeys = mapOf(
            "MF" to "MANUFACTURER",
            "MD" to "MODEL",
            "FP" to "FINGERPRINT",
            "PR" to "PRODUCT",
            "DV" to "DEVICE",
            "SP" to "SECURITY_PATCH",
            "ISDK" to "DEVICE_INITIAL_SDK_INT"
        )

        return keys.split(",").associate { key ->
            val fullKey = commonKeys.getOrDefault(key, key)
            fullKey to SystemProperties.get("persist.sys.propshooks_$key", "")
        }
    }

    private fun fetchUrl(url: String): String {
        return URL(url).readText(StandardCharsets.UTF_8)
    }

    private fun fetchPartialUrl(url: String, maxBytes: Int): String {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.getInputStream().use { inputStream ->
            val buffer = ByteArray(512)
            val result = StringBuilder()
            var totalRead = 0

            while (totalRead < maxBytes) {
                val read = inputStream.read(buffer)
                if (read == -1) break

                val chunk = buffer.copyOf(read)
                result.append(String(chunk, StandardCharsets.ISO_8859_1))
                totalRead += read
            }

            return result.toString()
        }
    }

    companion object {
        private const val TAG = "PifRepository"
        private const val GOOGLE_URL = "https://developer.android.com"
    }
}
