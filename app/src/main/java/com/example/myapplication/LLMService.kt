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
    inputText: String,
    apiKey: String,
    onChunkReceived: suspend (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val messagesArray = JSONArray()
        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", inputText)
        }
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", "gpt-5-nano")
        json.put("messages", messagesArray)
        json.put("stream", true)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("StreamError", "Code: ${response.code}")
                    return@withContext
                }

                val source = response.body?.source() ?: return@withContext

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    if (line.startsWith("data:")) {
                        val data = line.substringAfter("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val jsonObj = JSONObject(data)
                            val choices = jsonObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrEmpty()) {
                                    onChunkReceived(content)
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkError", "Connection error", e)
            onChunkReceived("\n[Ошибка сети: ${e.localizedMessage}]")
        }
    }
}

suspend fun generateChatTitle(userMessage: String, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", "Summarize this message into a short title (max 4 words). Do not use quotes.\n\nMessage: $userMessage")
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
