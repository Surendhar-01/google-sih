package com.example.network

import android.util.Log
import com.example.model.Language
import com.example.model.RoleType
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android client network bridge to the ECOBRIDGES NestJS Backend & FastAPI Voice Layer.
 * Interfaces with the centralized VoiceIntentRouter in NestJS and Whisper/NLP in FastAPI.
 */
object BackendVoiceClient {
    private const val TAG = "BackendVoiceClient"

    // Default development host addresses (10.0.2.2 is Android Emulator's alias to host loopback)
    private var nestJsBaseUrl: String = "http://10.0.2.2:3000"
    private var fastApiBaseUrl: String = "http://10.0.2.2:8000"

    fun configureEndpoints(nestUrl: String, fastApiUrl: String) {
        nestJsBaseUrl = nestUrl.removeSuffix("/")
        fastApiBaseUrl = fastApiUrl.removeSuffix("/")
    }

    /**
     * Calls NestJS centralized VoiceIntentRouter endpoint (/api/voice/intent)
     */
    suspend fun queryNestJsVoiceIntent(
        transcript: String,
        currentScreen: String,
        currentRole: RoleType?,
        language: Language
    ): SemanticAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$nestJsBaseUrl/api/voice/intent")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("transcript", transcript)
                put("userRole", currentRole?.name)
                put("context", JSONObject().apply {
                    put("currentScreen", currentScreen)
                    put("currentRole", currentRole?.name)
                    put("activeLanguage", when (language) {
                        Language.HINDI -> "hi"
                        Language.MARATHI -> "mr"
                        Language.ENGLISH -> "en"
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val intentStr = json.optString("intent", "UNKNOWN")
                val targetRoleStr = if (json.has("targetRole") && !json.isNull("targetRole")) json.getString("targetRole") else null
                val spokenFeedbackObj = json.optJSONObject("spokenFeedback")
                val spoken = when (language) {
                    Language.HINDI -> spokenFeedbackObj?.optString("hi")
                    Language.MARATHI -> spokenFeedbackObj?.optString("mr")
                    Language.ENGLISH -> spokenFeedbackObj?.optString("en")
                } ?: "Processed"

                val intent = try {
                    SemanticIntent.valueOf(intentStr)
                } catch (e: Exception) {
                    SemanticIntent.UNKNOWN
                }

                val role = targetRoleStr?.let {
                    try { RoleType.valueOf(it) } catch (e: Exception) { null }
                }

                return@withContext SemanticAnalysisResult(
                    intent = intent,
                    targetRole = role,
                    confidence = json.optDouble("confidence", 0.95).toFloat(),
                    spokenResponse = spoken,
                    actionDescription = "Resolved by NestJS VoiceIntentRouter",
                    rawTranscript = transcript,
                    detectedLanguage = language,
                    requiresConfirmation = json.optBoolean("requiresConfirmation", false),
                    aiEngineSource = "NestJS Backend VoiceIntentRouter"
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "NestJS voice intent backend query bypassed or unavailable: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Calls FastAPI NLP intent analysis endpoint directly (/api/v1/voice/semantic-intent)
     */
    suspend fun queryFastApiNlpIntent(
        transcript: String,
        currentScreen: String,
        currentRole: RoleType?,
        language: Language
    ): SemanticAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$fastApiBaseUrl/api/v1/voice/semantic-intent")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
            }

            val langCode = when (language) {
                Language.HINDI -> "hi"
                Language.MARATHI -> "mr"
                Language.ENGLISH -> "en"
            }

            val body = JSONObject().apply {
                put("transcript", transcript)
                put("language", langCode)
                put("currentScreen", currentScreen)
                put("currentRole", currentRole?.name)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val intentStr = json.optString("intent", "UNKNOWN")
                val targetRoleStr = if (json.has("targetRole") && !json.isNull("targetRole")) json.getString("targetRole") else null
                val spokenFeedbackObj = json.optJSONObject("spokenFeedback")
                val spoken = spokenFeedbackObj?.optString(langCode) ?: "Processed"

                val intent = try {
                    SemanticIntent.valueOf(intentStr)
                } catch (e: Exception) {
                    SemanticIntent.UNKNOWN
                }

                val role = targetRoleStr?.let {
                    try { RoleType.valueOf(it) } catch (e: Exception) { null }
                }

                return@withContext SemanticAnalysisResult(
                    intent = intent,
                    targetRole = role,
                    confidence = json.optDouble("confidence", 0.95).toFloat(),
                    spokenResponse = spoken,
                    actionDescription = "Resolved by FastAPI NLP Intent Layer",
                    rawTranscript = transcript,
                    detectedLanguage = language,
                    requiresConfirmation = json.optBoolean("requiresConfirmation", false),
                    aiEngineSource = "FastAPI Whisper & NLP Layer"
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "FastAPI NLP query bypassed or unavailable: ${e.message}")
        }
        return@withContext null
    }
}
