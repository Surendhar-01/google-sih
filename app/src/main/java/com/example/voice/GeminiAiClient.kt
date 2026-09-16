package com.example.voice

import android.util.Log
import com.example.BuildConfig
import com.example.model.AppNavigationContext
import com.example.model.AppScreen
import com.example.model.Language
import com.example.model.MaterialCategory
import com.example.model.RoleType
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Intelligent AI Intent Engine powered by Google Gemini API (gemini-3.5-flash)
 * with direct REST communication, structured JSON output, and automatic
 * semantic fallback if offline or unconfigured.
 */
class GeminiAiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeIntent(
        spokenText: String,
        context: AppNavigationContext,
        language: Language
    ): SemanticAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If no valid key or placeholder, use the deep on-device semantic classifier
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiAiClient", "Using on-device semantic engine (Gemini API key not configured or placeholder)")
            return@withContext SemanticIntentClassifier.analyze(spokenText, context, language)
        }

        try {
            val systemPrompt = """
                You are the intelligent Voice Intent Router for an Indian E-Waste Management application.
                Users speak in English, Hindi, Marathi, or Hinglish.
                You must analyze the user's spoken sentence and output strict JSON with this exact schema:
                {
                   "intent": "OPEN_LOGIN" | "NAVIGATE_SCREEN" | "QUERY_MATERIAL_PRICE" | "QUERY_SAFETY_GUIDELINES" | "QUERY_USER_STATS" | "CREATE_LOT_REQUEST" | "DESTRUCTIVE_ACTION_REQUEST" | "CONFIRM_ACTION" | "CANCEL_ACTION" | "CHANGE_LANGUAGE" | "HELP_AND_CAPABILITIES" | "GENERAL_EWASTE_QA" | "CLARIFY_REQUEST" | "SELECT_MOBILE_LOGIN" | "SELECT_EMAIL_LOGIN" | "SELECT_GOOGLE_LOGIN" | "SEND_OTP" | "RESEND_OTP" | "VERIFY_OTP" | "CHANGE_PHONE" | "OPEN_REGISTER" | "VOICE_HELP" | "GO_BACK",
                   "targetRole": "INFORMAL_COLLECTOR" | "FORMAL_RECYCLER" | "GOVERNMENT_ADMIN" | null,
                   "targetScreen": "INTRO" | "INFORMAL_COLLECTOR_AUTH" | "FORMAL_RECYCLER_AUTH" | "GOVERNMENT_ADMIN_AUTH" | "COLLECTOR_DASHBOARD" | "FORMAL_RECYCLER_PORTAL" | "GOVERNMENT_ADMIN_PORTAL" | null,
                   "materialCategory": "PCB_BOARDS" | "CABLES_WIRES" | "BATTERIES" | "CRTS_MONITORS" | "LCD_PANELS" | "MOTORS_MAGNETS" | "MIXED_PLASTICS" | null,
                   "quantityKg": number or null,
                   "detectedLanguage": "en" | "hi" | "mr",
                   "spokenResponse": "Short, clear, friendly spoken response in the user's detected language",
                   "actionDescription": "Brief description of the action",
                   "requiresConfirmation": boolean,
                   "isDestructive": boolean
                }

                Current Application Context:
                - Current Screen: ${context.currentScreen.name}
                - Current Role: ${context.currentRole?.name ?: "None"}
                - Is Authenticated: ${context.isAuthenticated}
                - Has Pending Action: ${context.pendingAction != null}

                CRITICAL DIRECTIVE:
                If the user says anything that means opening, accessing, or going to the login page, or signing in (for collector, recycler, admin, or in general), you must recognize intent = "OPEN_LOGIN", set targetRole appropriately (or default to INFORMAL_COLLECTOR), set targetScreen accordingly, and provide a prompt response.
                DO NOT output markdown code blocks or commentary. Output raw JSON only.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "User spoken utterance: \"$spokenText\"\nPreferred Language: ${language.displayName}")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful || responseBody.isBlank()) {
                Log.w("GeminiAiClient", "Gemini API call failed code ${response.code}: $responseBody")
                return@withContext SemanticIntentClassifier.analyze(spokenText, context, language)
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArr = contentObj?.optJSONArray("parts")
            val rawText = partsArr?.optJSONObject(0)?.optString("text") ?: ""

            if (rawText.isBlank()) {
                return@withContext SemanticIntentClassifier.analyze(spokenText, context, language)
            }

            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val intentName = parsed.optString("intent", "UNKNOWN")
            val intent = try {
                SemanticIntent.valueOf(intentName)
            } catch (e: Exception) {
                SemanticIntent.UNKNOWN
            }

            val targetRoleName = parsed.optString("targetRole", "")
            val targetRole = if (targetRoleName.isNotBlank() && targetRoleName != "null") {
                try { RoleType.valueOf(targetRoleName) } catch (e: Exception) { null }
            } else null

            val targetScreenName = parsed.optString("targetScreen", "")
            val targetScreen = if (targetScreenName.isNotBlank() && targetScreenName != "null") {
                try { AppScreen.valueOf(targetScreenName) } catch (e: Exception) { null }
            } else null

            val categoryName = parsed.optString("materialCategory", "")
            val materialCategory = if (categoryName.isNotBlank() && categoryName != "null") {
                try { MaterialCategory.valueOf(categoryName) } catch (e: Exception) { null }
            } else null

            val qty = if (parsed.has("quantityKg") && !parsed.isNull("quantityKg")) {
                parsed.optDouble("quantityKg")
            } else null

            val detectedLangCode = parsed.optString("detectedLanguage", language.code)
            val detectedLanguage = when (detectedLangCode) {
                "hi" -> Language.HINDI
                "mr" -> Language.MARATHI
                else -> Language.ENGLISH
            }

            val spokenResponse = parsed.optString("spokenResponse", "")
            val actionDesc = parsed.optString("actionDescription", "Execute voice action")
            val requiresConfirm = parsed.optBoolean("requiresConfirmation", false)
            val isDestructive = parsed.optBoolean("isDestructive", false)

            SemanticAnalysisResult(
                intent = intent,
                targetRole = targetRole,
                targetScreen = targetScreen,
                materialCategory = materialCategory,
                quantityKg = qty,
                detectedLanguage = detectedLanguage,
                confidence = 0.99f,
                spokenResponse = spokenResponse.ifBlank { "Action processed." },
                actionDescription = actionDesc,
                requiresConfirmation = requiresConfirm,
                isDestructive = isDestructive,
                rawTranscript = spokenText,
                aiEngineSource = "Gemini 3.5 Flash"
            )
        } catch (e: Exception) {
            Log.w("GeminiAiClient", "Exception during Gemini AI parsing, falling back to local semantic engine", e)
            SemanticIntentClassifier.analyze(spokenText, context, language)
        }
    }
}
