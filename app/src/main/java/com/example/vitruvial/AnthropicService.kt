package com.example.vitruvial

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicService {
    @POST("v1/messages")
    suspend fun getMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body requestBody: ClaudeMessageRequest
    ): Response<ClaudeMessageResponse>
}

data class ClaudeMessageRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    val system: String? = null,
    val max_tokens: Int = 1000,
    val temperature: Double = 0.3
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String?,
    val stop_sequence: String?,
    val usage: ClaudeUsage
)

data class ContentBlock(
    val type: String,
    val text: String
)

data class ClaudeUsage(
    val input_tokens: Int,
    val output_tokens: Int
)