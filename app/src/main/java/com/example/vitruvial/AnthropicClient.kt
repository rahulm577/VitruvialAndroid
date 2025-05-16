package com.example.vitruvial

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AnthropicClient(private val context: Context) {
    private val TAG = "AnthropicClient"
    private val apiVersion = "2023-06-01"
    private val service: AnthropicService
    private val gson = Gson()

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(AnthropicService::class.java)
    }
    
    private fun getApiKey(): String {
        // Get the API key from BuildConfig
        return BuildConfig.CLAUDE_API_KEY
    }

    suspend fun extractPatientInfo(text: String): PatientInfo {
        Log.d(TAG, "Starting patient info extraction with Claude API")
        Log.d(TAG, "Input text length: ${text.length}")
        Log.d(TAG, "First 100 chars of text: ${text.take(100)}")
        
        try {
            val prompt = """
                Extract patient information from the following text that was obtained through OCR from a medical document or patient sticker.
                
                Here's the OCR text:
                $text
                
                Please extract and return ONLY the following information in JSON format:
                {
                  "firstName": "First name of the patient", 
                  "lastName": "Last name of the patient",
                  "dateOfBirth": "Date of birth (any format found)",
                  "address": "Full address if available",
                  "phoneNumber": "Patient's phone number if available",
                  "medicareNumber": "Medicare number if available",
                  "healthcareFund": "Name of healthcare fund if available",
                  "healthcareFundNumber": "Healthcare fund number if available"
                }
                
                If a field is not found, leave it as an empty string. Analyze the text carefully for these details, looking for patterns or labels like "Name:", "DOB:", "Address:", etc. 
                Return ONLY the JSON, no other text.
            """.trimIndent()

            Log.d(TAG, "Created prompt for Claude")
            
            // Claude API expects system prompt as a top-level parameter
            val systemPrompt = "You are a helpful assistant that extracts structured patient information from OCR text. Only respond with the JSON data, nothing else."
            
            // User messages only (no system message in the messages array)
            val messages = listOf(
                ClaudeMessage("user", prompt)
            )

            val request = ClaudeMessageRequest(
                model = "claude-3-5-sonnet-20241022",
                messages = messages,
                system = systemPrompt
            )

            Log.d(TAG, "Sending request to Claude API")
            val response = service.getMessage(getApiKey(), apiVersion, request)
            
            Log.d(TAG, "Received response from Claude API, success=${response.isSuccessful}, code=${response.code()}")
            
            if (response.isSuccessful) {
                val completionResponse = response.body()
                Log.d(TAG, "Response body: $completionResponse")
                
                // Claude response has content as a list of blocks
                val content = completionResponse?.content?.firstOrNull { it.type == "text" }?.text ?: ""
                Log.d(TAG, "Response content: $content")

                // Try to extract JSON from the response
                try {
                    // The response might contain markdown code blocks, so we need to extract just the JSON
                    val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
                    val jsonMatch = jsonRegex.find(content)?.value ?: content
                    
                    Log.d(TAG, "Extracted JSON string: $jsonMatch")
                    
                    // Parse the JSON
                    val patientInfo = gson.fromJson(jsonMatch, PatientInfo::class.java)
                    Log.d(TAG, "Parsed patient info: $patientInfo")
                    return patientInfo
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON from Claude response: ${e.message}")
                    Log.d(TAG, "Raw content: $content")
                    
                    // Try alternative parsing approach
                    try {
                        val emptyPatientInfo = PatientInfo()
                        
                        // Extract using a regex-based approach as fallback
                        val firstNameRegex = "\"firstName\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val lastNameRegex = "\"lastName\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val dobRegex = "\"dateOfBirth\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val addressRegex = "\"address\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val phoneRegex = "\"phoneNumber\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val medicareRegex = "\"medicareNumber\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val fundRegex = "\"healthcareFund\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        val fundNumberRegex = "\"healthcareFundNumber\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                        
                        firstNameRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.firstName = it
                        }
                        lastNameRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.lastName = it
                        }
                        dobRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.dateOfBirth = it
                        }
                        addressRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.address = it
                        }
                        phoneRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.phoneNumber = it
                        }
                        medicareRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.medicareNumber = it
                        }
                        fundRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.healthcareFund = it
                        }
                        fundNumberRegex.find(content)?.groupValues?.getOrNull(1)?.let {
                            emptyPatientInfo.healthcareFundNumber = it
                        }
                        
                        Log.d(TAG, "Alternative parsing produced: $emptyPatientInfo")
                        return emptyPatientInfo
                    } catch (e2: Exception) {
                        Log.e(TAG, "Alternative parsing also failed: ${e2.message}")
                        return PatientInfo()
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error from Claude API: $errorBody")
                return PatientInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in extractPatientInfo: ${e.message}", e)
            return PatientInfo()
        }
    }
} 