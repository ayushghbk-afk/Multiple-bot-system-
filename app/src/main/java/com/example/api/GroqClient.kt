package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GroqClient {
    private const val TAG = "GroqClient"
    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    
    // Default high-performance model on Groq
    const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    
    val SUPPORTED_MODELS = listOf(
        "llama-3.3-70b-versatile" to "Llama 3.3 70B (High Intelligence & Reasoning)",
        "llama-3.1-8b-instant" to "Llama 3.1 8B (Super Fast Inference)",
        "mixtral-8x7b-32768" to "Mixtral 8x7B (High-context Mixture of Experts)",
        "gemma2-9b-it" to "Gemma 2 9B (Google Open Weights Optimized)"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a key looks like a valid Groq API key.
     */
    fun isGroqKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        return key.trim().startsWith("gsk_")
    }

    /**
     * Sends a chat completion prompt to the Groq APIs.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        apiKey: String,
        model: String? = null
    ): String = withContext(Dispatchers.IO) {
        val activeModel = if (!model.isNullOrBlank() && SUPPORTED_MODELS.any { it.first == model }) model else DEFAULT_MODEL
        val finalApiKey = apiKey.trim()

        if (finalApiKey.isEmpty()) {
            return@withContext "API_KEY_MISSING"
        }

        try {
            val requestJson = JSONObject()
            requestJson.put("model", activeModel)
            requestJson.put("temperature", 0.5)

            val messagesArray = JSONArray()

            // System Instruction (if provided)
            if (!systemInstruction.isNullOrBlank()) {
                val systemMsg = JSONObject()
                systemMsg.put("role", "system")
                systemMsg.put("content", systemInstruction)
                messagesArray.put(systemMsg)
            }

            // User Prompt
            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", prompt)
            messagesArray.put(userMsg)

            requestJson.put("messages", messagesArray)

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $finalApiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    Log.e(TAG, "Groq Request failed: code=${response.code}, body=$bodyStr")
                    
                    val errorMessage = try {
                        val json = JSONObject(bodyStr ?: "")
                        val error = json.getJSONObject("error")
                        error.optString("message", "No detailed message provided.")
                    } catch (e: Exception) {
                        bodyStr ?: "No response body text"
                    }

                    val helpMessage = buildString {
                        append("GROQ API ERROR ${response.code}\n")
                        append("Reason: $errorMessage\n\n")
                        
                        if (response.code == 401 || response.code == 403) {
                            append("💡 HTTP ${response.code} Authentication Troubleshooting Guide:\n")
                            append("1. **Verify Your Key**: Check that your Groq key under 'APIs & Squad' starts with 'gsk_' and matches exactly.\n")
                            append("2. **No Extra Spaces**: Make sure no leading/trailing blank characters exist.\n")
                            append("3. **Organization/Billing**: Double-check your Groq dashboard details at console.groq.com.")
                        } else if (response.code == 429) {
                            append("💡 HTTP 429 Rate Limit/Quota Exceeded:\n")
                            append("Your Groq API key hit standard RPM/TPM limits. Groq free limits can be strict. Please wait a moment before trying again.")
                        } else if (response.code == 400) {
                            append("💡 HTTP 400 Bad Request:\n")
                            append("Make sure the model '$activeModel' is correctly spelled and enabled for your Groq account tier.")
                        }
                    }
                    return@withContext helpMessage
                }

                val responseJson = JSONObject(bodyStr)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrBlank()) {
                        return@withContext content
                    }
                }
                return@withContext "Empty response from Groq API."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Groq content", e)
            return@withContext "Groq Network Error: ${e.localizedMessage ?: "Unknown HTTP issue"}"
        }
    }
}
