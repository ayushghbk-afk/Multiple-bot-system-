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

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    
    // As per rules, basic/complex text tasks default model is gemini-3.5-flash
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API Key is configured, checking overrides or BuildConfig.
     */
    fun isApiKeyConfigured(overrideKey: String? = null): Boolean {
        val key = if (!overrideKey.isNullOrBlank()) overrideKey else BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Sends a text prompt to Gemini API and returns the string response.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        apiKeyOverride: String? = null,
        modelOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        val activeModel = if (!modelOverride.isNullOrBlank()) modelOverride else MODEL_NAME
        
        if (!isApiKeyConfigured(apiKey)) {
            return@withContext "API_KEY_MISSING"
        }

        val url = "$BASE_URL/$activeModel:generateContent?key=$apiKey"

        try {
            // Build request json
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val textContent = JSONObject()
            val partsArray = JSONArray()
            val partObject = JSONObject()
            partObject.put("text", prompt)
            partsArray.put(partObject)
            textContent.put("parts", partsArray)
            contentsArray.put(textContent)
            requestJson.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction != null) {
                val systemObject = JSONObject()
                val systemPartsArray = JSONArray()
                val systemPart = JSONObject()
                systemPart.put("text", systemInstruction)
                systemPartsArray.put(systemPart)
                systemObject.put("parts", systemPartsArray)
                requestJson.put("systemInstruction", systemObject)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    Log.e(TAG, "Request failed code: ${response.code}, body: $bodyStr")
                    
                    val errorMessage = try {
                        val json = JSONObject(bodyStr ?: "")
                        val error = json.getJSONObject("error")
                        error.optString("message", "No response body error details")
                    } catch (e: Exception) {
                        bodyStr ?: "No response body text"
                    }

                    val helpMessage = buildString {
                        append("API ERROR ${response.code}\n")
                        append("Reason: $errorMessage\n\n")
                        
                        if (response.code == 403) {
                            append("💡 HTTP 403 Forbidden Troubleshooting Guide:\n")
                            append("1. **Verify API Key**: Check the key in the 'APIs & Squad' tab. Make sure there are no typos, extra spaces, or placeholder strings.\n")
                            append("2. **Model Authorization**: You requested model '$activeModel'. Is it enabled for your key? Standard keys can try choosing 'gemini-3.1-pro-preview' or 'gemini-3.5-flash'.\n")
                            append("3. **Enable API**: In Google Cloud Console, ensure the 'Generative Language API' is enabled for your project.\n")
                            append("4. **Region Restrictions**: Ensure you aren't calling from an IP range blocked by Google.")
                        } else if (response.code == 400) {
                            append("💡 HTTP 400 Bad Request:\n")
                            append("Please check if the requested model '$activeModel' is typed correctly and supported by your key's tier.")
                        } else if (response.code == 429) {
                            append("💡 HTTP 429 Rate Limit/Quota Exceeded:\n")
                            append("You have run out of free tier queries or hit a concurrent limit. Wait a minute before retrying.")
                        }
                    }
                    return@withContext helpMessage
                }

                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text found")
                    }
                }
                return@withContext "No response. Result body empty."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content", e)
            return@withContext "Error: ${e.localizedMessage ?: "Unknown network error"}"
        }
    }
}
