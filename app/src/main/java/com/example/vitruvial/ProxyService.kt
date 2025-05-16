package com.example.vitruvial

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * This service communicates with our proxy server, which will add the 
 * actual Claude API key on the server-side before forwarding requests to Anthropic.
 * The API key never exists in the client app.
 */
interface ProxyService {
    @POST("claude/extract-patient-info")
    suspend fun extractPatientInfo(
        @Header("Authorization") appToken: String,
        @Body request: PatientExtractionRequest
    ): Response<PatientInfo>
}

/**
 * Request with just the text to analyze, without API keys or model parameters
 * The proxy server will handle adding those details
 */
data class PatientExtractionRequest(
    val text: String
) 