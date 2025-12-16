package com.lgh9900.phoneagent.model

data class ModelResponse(
    val thinking: String,
    val action: String,
    val rawContent: String
)

data class ModelConfig(
    val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
    val apiKey: String = "xxxxx",
    val modelName: String = "autoglm-phone",
    val maxTokens: Int = 3000,
    val temperature: Float = 0.0f,
    val topP: Float = 0.85f,
    val frequencyPenalty: Float = 0.2f
)