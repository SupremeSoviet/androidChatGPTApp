package com.example.myapplication

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

suspend fun getAssistantReplyFromProxyApi(
    chatHistory: List<ChatMessage>,
    apiKey: String,
    modelId: String,
    apiEndpoint: String
): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val relevantHistory = chatHistory
            .filter { it.text.isNotBlank() && !it.text.startsWith("Ожидание") && !it.text.startsWith("Generating") }
            .takeLast(20)

        val messagesArray = JSONArray()

        val systemMessage = JSONObject().apply {
            put("role", "system")
            put("content", "IMPORTANT: Write your response in plain text only. Do not use Markdown formatting (no bold, italics, headers, or code blocks).")
        }
        messagesArray.put(systemMessage)

        for (msg in relevantHistory) {
            val role = if (msg.isUser) "user" else "assistant"
            val messageJson = JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            }
            messagesArray.put(messageJson)
        }

        val json = JSONObject()
        json.put("model", modelId)
        json.put("messages", messagesArray)
        json.put("stream", false)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(apiEndpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://github.com/YourApp")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: ${response.code} ${response.message}"
                }
                val responseBody = response.body?.string() ?: return@withContext ""

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    return@withContext message?.optString("content") ?: ""
                }
                return@withContext ""
            }
        } catch (e: Exception) {
            Log.e("LLM", "Error", e)
            return@withContext "[Connection Error: ${e.localizedMessage}]"
        }
    }
}

suspend fun generateChatTitle(userMessage: String, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", "Summarize the following message into a short title (max 4 words). IMPORTANT: The title must be in the same language as the message. Do not use quotes.\n\nMessage: $userMessage")
        })

        val json = JSONObject()
        json.put("model", "gpt-5-nano")
        json.put("messages", messagesArray)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "New Chat"
                val text = response.body?.string().orEmpty()
                val obj = JSONObject(text)
                val choices = obj.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrEmpty()) {
                        return@withContext content.trim().removeSurrounding("\"")
                    }
                }
                "New Chat"
            }
        } catch (e: Exception) {
            "New Chat"
        }
    }
}