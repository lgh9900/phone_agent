package com.lgh9900.phoneagent.utils

import com.google.gson.Gson

object MessageBuilder {

    private val gson = Gson()


    fun createSystemMessage(content: String): Map<String, Any> {
        return mapOf(
            "role" to "system",
            "content" to content
        )
    }


    fun createUserMessage(
        text: String,
        imageBase64: String? = null
    ): Map<String, Any> {
        val content = mutableListOf<Map<String, Any>>()

        if (imageBase64 != null) {
            content.add(
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/png;base64,$imageBase64"
                    )
                )
            )
        }

        content.add(
            mapOf(
                "type" to "text",
                "text" to text
            )
        )

        return mapOf(
            "role" to "user",
            "content" to content
        )
    }


    fun createAssistantMessage(content: String): Map<String, Any> {
        return mapOf(
            "role" to "assistant",
            "content" to content
        )
    }


    fun removeImagesFromMessage(message: Map<String, Any>): Map<String, Any> {
        val content = message["content"]

        if (content is List<*>) {
            val filteredContent = (content as List<Map<String, Any>>)
                .filter { it["type"] != "image_url" }

            return message.toMutableMap().apply {
                put("content", filteredContent)
            }
        }

        return message
    }


    fun buildScreenInfo(currentApp: String): String {
        val info = mapOf("current_app" to currentApp)
        return gson.toJson(info)
    }
}