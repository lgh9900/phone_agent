package com.lgh9900.phoneagent

import com.lgh9900.phoneagent.model.ModelConfig
import com.lgh9900.phoneagent.model.ModelResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LLMApi(private val config: ModelConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()


    suspend fun request(
        messages: List<Map<String, Any>>,
        onThinking: ((String) -> Unit)? = null
    ): ModelResponse {
        return suspendCoroutine { continuation ->

            val requestBody = JsonObject().apply {
                addProperty("model", config.modelName)
                add("messages", gson.toJsonTree(messages))
                addProperty("temperature", config.temperature)
                addProperty("top_p", config.topP)
                addProperty("frequency_penalty", config.frequencyPenalty)
                addProperty("max_tokens", config.maxTokens)
                addProperty("stream", false)
            }

            val request = Request.Builder()
                .url(config.baseUrl)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .post(requestBody.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            continuation.resumeWithException(
                                IOException("HTTP ${response.code}: ${response.message}")
                            )
                            return
                        }

                        val responseBody = response.body?.string()
                        
                        if (responseBody != null) {
                            val json = JsonParser.parseString(responseBody).asJsonObject
                            val choices = json.getAsJsonArray("choices")
                            
                            if (choices != null && choices.size() > 0) {
                                val choice = choices[0].asJsonObject
                                val message = choice.getAsJsonObject("message")
                                val content = message?.get("content")?.asString ?: ""
                                
                                onThinking?.invoke(content)
                                
                                val (thinking, action) = parseResponse(content)
                                
                                continuation.resume(
                                    ModelResponse(
                                        thinking = thinking,
                                        action = action,
                                        rawContent = content
                                    )
                                )
                            } else {
                                continuation.resumeWithException(IOException("Invalid response format"))
                            }
                        } else {
                            continuation.resumeWithException(IOException("Empty response body"))
                        }

                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    private fun parseResponse(content: String): Pair<String, String> {
        if ("finish(message=" in content) {
            val parts = content.split("finish(message=", limit = 2)
            val thinking = parts[0].trim()
            val action = "finish(message=" + parts[1]
            return Pair(thinking, action)
        }

        if ("do(action=" in content) {
            val parts = content.split("do(action=", limit = 2)
            val thinking = parts[0].trim()
            val action = "do(action=" + parts[1]
            return Pair(thinking, action)
        }

        if ("<answer>" in content) {
            val parts = content.split("<answer>", limit = 2)
            val thinking = parts[0]
                .replace("<think>", "")
                .replace("</think>", "")
                .trim()
            val action = parts[1]
                .replace("</answer>", "")
                .trim()
            return Pair(thinking, action)
        }

        return Pair("", content)
    }

}