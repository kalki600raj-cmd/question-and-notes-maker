package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generates study content or questions based on a custom prompt.
     * Returns the text response or null if there was an error.
     */
    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured! Default placeholder or empty value found.")
            return@withContext null
        }

        try {
            val jsonRequest = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()

            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonRequest.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction != null) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                jsonRequest.put("systemInstruction", sysInstObj)
            }

            // Request config for JSON formatting if needed
            val genConfig = JSONObject()
            if (prompt.contains("json", ignoreCase = true)) {
                genConfig.put("responseMimeType", "application/json")
            }
            genConfig.put("temperature", 0.7)
            jsonRequest.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonRequest.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "Empty body"
                    Log.e(TAG, "API Call failed with code: ${response.code}, message: ${response.message}, body: $errBody")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                Log.e(TAG, "Unable to extract text from response: $responseBody")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call: ${e.message}", e)
            return@withContext null
        }
    }
}
