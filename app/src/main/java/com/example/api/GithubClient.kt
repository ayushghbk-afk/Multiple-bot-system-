package com.example.api

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object GithubClient {
    private const val TAG = "GithubClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class GithubRepoFile(
        val name: String,
        val path: String,
        val type: String, // "file" or "dir"
        val sha: String,
        val size: Long,
        val downloadUrl: String?
    )

    /**
     * Validates connection to GitHub with a given token, owner and repo.
     */
    suspend fun testConnection(token: String, owner: String, repo: String): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank() || owner.isBlank() || repo.isBlank()) {
            return@withContext Result.failure(Exception("Credentials cannot be blank"))
        }
        val url = "https://api.github.com/repos/$owner/$repo"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "AI-Team-Workspace-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val fullName = json.optString("full_name", "$owner/$repo")
                    val defaultBranch = json.optString("default_branch", "main")
                    Result.success("Connected to $fullName (Default: $defaultBranch)")
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "testConnection: error", e)
            Result.failure(e)
        }
    }

    /**
     * Lists directory/files from the given repo path.
     */
    suspend fun fetchContents(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        path: String = ""
    ): Result<List<GithubRepoFile>> = withContext(Dispatchers.IO) {
        val sanitizedPath = path.trim().trim('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$sanitizedPath?ref=$branch"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "AI-Team-Workspace-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val filesList = mutableListOf<GithubRepoFile>()
                    if (bodyStr.trim().startsWith("[")) {
                        val arr = JSONArray(bodyStr)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            filesList.add(
                                GithubRepoFile(
                                    name = obj.getString("name"),
                                    path = obj.getString("path"),
                                    type = obj.getString("type"),
                                    sha = obj.getString("sha"),
                                    size = obj.optLong("size", 0),
                                    downloadUrl = obj.optString("download_url", null)
                                )
                            )
                        }
                    } else {
                        // It's a single file element, wrap in list
                        val obj = JSONObject(bodyStr)
                        filesList.add(
                            GithubRepoFile(
                                name = obj.getString("name"),
                                path = obj.getString("path"),
                                type = obj.getString("type"),
                                sha = obj.getString("sha"),
                                size = obj.optLong("size", 0),
                                downloadUrl = obj.optString("download_url", null)
                            )
                        )
                    }
                    Result.success(filesList)
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchContents: error", e)
            Result.failure(e)
        }
    }

    /**
     * Pull/download a complete file with dynamic Base64 decryption.
     */
    suspend fun downloadTextFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        filePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val sanitizedPath = filePath.trim().trim('/')
        val url = "https://api.github.com/repos/$owner/$repo/contents/$sanitizedPath?ref=$branch"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "AI-Team-Workspace-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val obj = JSONObject(bodyStr)
                    val contentBase64 = obj.getString("content")
                        .replace("\n", "")
                        .replace("\r", "")
                    val decodedBytes = Base64.decode(contentBase64, Base64.DEFAULT)
                    val decodedText = String(decodedBytes, StandardCharsets.UTF_8)
                    Result.success(decodedText)
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadTextFile: error", e)
            Result.failure(e)
        }
    }

    /**
     * Push or update a text file to GitHub.
     */
    suspend fun pushFile(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        filePath: String,
        content: String,
        commitMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val sanitizedPath = filePath.trim().trim('/')
        
        // 1. Check if file exists to fetch sha
        var existingSha: String? = null
        val fetchUrl = "https://api.github.com/repos/$owner/$repo/contents/$sanitizedPath?ref=$branch"
        val fetchRequest = Request.Builder()
            .url(fetchUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "AI-Team-Workspace-App")
            .build()

        try {
            client.newCall(fetchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        val obj = JSONObject(bodyStr)
                        existingSha = obj.getString("sha")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pushFile: File did not exist on branch, creating new file")
        }

        // 2. Base64 encode target content
        val base64Content = Base64.encodeToString(
            content.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )

        val writeUrl = "https://api.github.com/repos/$owner/$repo/contents/$sanitizedPath"
        
        val requestJson = JSONObject()
        requestJson.put("message", commitMessage)
        requestJson.put("content", base64Content)
        requestJson.put("branch", branch)
        if (existingSha != null) {
            requestJson.put("sha", existingSha)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val pushRequest = Request.Builder()
            .url(writeUrl)
            .put(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "AI-Team-Workspace-App")
            .build()

        try {
            client.newCall(pushRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resJson = JSONObject(bodyStr)
                    val commitJson = resJson.getJSONObject("commit")
                    val commitSha = commitJson.getString("sha")
                    Result.success("Success: Committed file with SHA ${commitSha.take(7)}")
                } else {
                    Result.failure(Exception("Write Failed (${response.code}): $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushFile: exception", e)
            Result.failure(e)
        }
    }
}
