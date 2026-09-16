package com.example.voice

import android.content.Context
import android.util.Log
import com.example.data.EwasteRepository
import com.example.model.AppNavigationContext
import com.example.model.AppScreen
import com.example.model.ConversationTurn
import com.example.model.Language
import com.example.model.MaterialCategory
import com.example.model.PendingAction
import com.example.model.RoleType
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import com.example.network.BackendVoiceClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NavigationAction {
    data object GoToIntro : NavigationAction()
    data class OpenLogin(val role: RoleType) : NavigationAction()
    data class OpenPortal(val role: RoleType) : NavigationAction()
    data object CreateLotDialog : NavigationAction()
}

/**
 * UI actions that only make sense while the authentication screen is visible.
 * The login screen collects [VoiceIntentRouter.authEvents] and executes them.
 * SEND_OTP / VERIFY_OTP never carry an OTP value — codes are never spoken.
 */
sealed class AuthUiAction {
    data object SelectMobileLogin : AuthUiAction()
    data object SelectEmailLogin : AuthUiAction()
    data object SelectGoogleLogin : AuthUiAction()
    data object SendOtp : AuthUiAction()
    data object VerifyOtp : AuthUiAction()
    data object ChangePhone : AuthUiAction()
    data object OpenRegister : AuthUiAction()
    data object GoBack : AuthUiAction()
}

/**
 * UI actions that the Collector dashboard screen collects and executes while an
 * informal collector is actively working (voice-driven hands-free operations).
 */
sealed class CollectorAction {
    data object OpenRevenueSummaryTab : CollectorAction()
    data object AnalyzeCurrentLot : CollectorAction()
    data object TurnOnLocationSharing : CollectorAction()
    data object OpenNearbyCollectorsTab : CollectorAction()
    data object OpenRecyclersTab : CollectorAction()
    data object OpenConnectionsTab : CollectorAction()
    data object RequestRecyclerQuote : CollectorAction()
}

/**
 * Centralized Voice Intent Router for the entire application.
 * Coordinates speech-to-text semantic understanding, context management,
 * permission checks, action dispatch, database retrieval, and voice feedback.
 */
class VoiceIntentRouter(
    private val context: Context,
    private val repository: EwasteRepository? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val geminiClient = GeminiAiClient()

    private val _appContext = MutableStateFlow(
        AppNavigationContext(
            currentScreen = AppScreen.INTRO,
            currentRole = null,
            isAuthenticated = false
        )
    )
    val appContext: StateFlow<AppNavigationContext> = _appContext.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationAction>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<NavigationAction> = _navigationEvents.asSharedFlow()

    private val _authEvents = MutableSharedFlow<AuthUiAction>(extraBufferCapacity = 1)
    val authEvents: SharedFlow<AuthUiAction> = _authEvents.asSharedFlow()

    private val _collectorEvents = MutableSharedFlow<CollectorAction>(extraBufferCapacity = 1)
    val collectorEvents: SharedFlow<CollectorAction> = _collectorEvents.asSharedFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationTurn>> = _conversationHistory.asStateFlow()

    private val _lastResult = MutableStateFlow<SemanticAnalysisResult?>(null)
    val lastResult: StateFlow<SemanticAnalysisResult?> = _lastResult.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Callback for voice output
    var onSpeakResponse: ((String, Language) -> Unit)? = null

    // Callback for language change
    var onLanguageChange: ((Language) -> Unit)? = null

    // Callback for sign out
    var onSignOut: (() -> Unit)? = null

    fun setImmediateResult(result: SemanticAnalysisResult) {
        _lastResult.value = result
    }

    fun updateScreen(screen: AppScreen, role: RoleType? = null, authenticated: Boolean = false) {
        _appContext.value = _appContext.value.copy(
            currentScreen = screen,
            currentRole = role ?: _appContext.value.currentRole,
            isAuthenticated = authenticated
        )
    }

    /**
     * Entry point for natural language speech transcript from anywhere in the app.
     */
    fun processSpokenText(
        transcript: String,
        currentLanguage: Language
    ) {
        if (transcript.isBlank()) return

        coroutineScope.launch {
            _isProcessing.value = true

            // Add user turn to conversation history
            addConversationTurn("user", transcript)

            val currentCtx = _appContext.value.copy(pendingAction = _pendingAction.value)

            // Step 1: Query NestJS VoiceIntentRouter backend (with Gemini AI & local NLP fallback)
            val backendResult = BackendVoiceClient.queryNestJsVoiceIntent(
                transcript = transcript,
                currentScreen = currentCtx.currentScreen.name,
                currentRole = currentCtx.currentRole,
                language = currentLanguage
            )
            val result = backendResult ?: geminiClient.analyzeIntent(transcript, currentCtx, currentLanguage)
            _lastResult.value = result

            // Update conversational state memory
            if (result.materialCategory != null) {
                _appContext.value = _appContext.value.copy(lastMentionedCategory = result.materialCategory)
            }

            // Step 2: Route and execute based on semantic intent
            executeIntent(result, currentLanguage)

            // Step 3: Add assistant turn & trigger voice response
            addConversationTurn("assistant", result.spokenResponse)
            onSpeakResponse?.invoke(result.spokenResponse, result.detectedLanguage)

            _isProcessing.value = false
        }
    }

    private suspend fun executeIntent(
        result: SemanticAnalysisResult,
        language: Language
    ) {
        when (result.intent) {
            SemanticIntent.OPEN_LOGIN -> {
                val targetRole = result.targetRole ?: RoleType.INFORMAL_COLLECTOR
                _navigationEvents.tryEmit(NavigationAction.OpenLogin(targetRole))
            }

            SemanticIntent.SELECT_MOBILE_LOGIN -> _authEvents.tryEmit(AuthUiAction.SelectMobileLogin)

            SemanticIntent.SELECT_EMAIL_LOGIN -> _authEvents.tryEmit(AuthUiAction.SelectEmailLogin)

            SemanticIntent.SELECT_GOOGLE_LOGIN -> _authEvents.tryEmit(AuthUiAction.SelectGoogleLogin)

            SemanticIntent.SEND_OTP, SemanticIntent.RESEND_OTP -> _authEvents.tryEmit(AuthUiAction.SendOtp)

            SemanticIntent.VERIFY_OTP -> _authEvents.tryEmit(AuthUiAction.VerifyOtp)

            SemanticIntent.CHANGE_PHONE -> _authEvents.tryEmit(AuthUiAction.ChangePhone)

            SemanticIntent.OPEN_REGISTER -> _authEvents.tryEmit(AuthUiAction.OpenRegister)

            SemanticIntent.GO_BACK -> _authEvents.tryEmit(AuthUiAction.GoBack)

            SemanticIntent.VOICE_HELP -> {
                // Informational; the spoken guidance comes from the analysis result.
            }

            SemanticIntent.NAVIGATE_SCREEN -> {
                when (result.targetScreen) {
                    AppScreen.INTRO -> _navigationEvents.tryEmit(NavigationAction.GoToIntro)
                    AppScreen.INFORMAL_COLLECTOR_AUTH -> _navigationEvents.tryEmit(NavigationAction.OpenLogin(RoleType.INFORMAL_COLLECTOR))
                    AppScreen.FORMAL_RECYCLER_AUTH -> _navigationEvents.tryEmit(NavigationAction.OpenLogin(RoleType.FORMAL_RECYCLER))
                    AppScreen.GOVERNMENT_ADMIN_AUTH -> _navigationEvents.tryEmit(NavigationAction.OpenLogin(RoleType.GOVERNMENT_ADMIN))
                    AppScreen.COLLECTOR_DASHBOARD -> _navigationEvents.tryEmit(NavigationAction.OpenPortal(RoleType.INFORMAL_COLLECTOR))
                    AppScreen.FORMAL_RECYCLER_PORTAL -> _navigationEvents.tryEmit(NavigationAction.OpenPortal(RoleType.FORMAL_RECYCLER))
                    AppScreen.GOVERNMENT_ADMIN_PORTAL -> _navigationEvents.tryEmit(NavigationAction.OpenPortal(RoleType.GOVERNMENT_ADMIN))
                    else -> _navigationEvents.tryEmit(NavigationAction.GoToIntro)
                }
            }

            SemanticIntent.DESTRUCTIVE_ACTION_REQUEST -> {
                // Register pending confirmation action
                val action = PendingAction(
                    id = "action_${System.currentTimeMillis()}",
                    title = result.actionDescription,
                    promptQuestion = result.spokenResponse,
                    onConfirm = {
                        if (result.actionDescription.contains("Sign out", ignoreCase = true)) {
                            onSignOut?.invoke()
                            _navigationEvents.tryEmit(NavigationAction.GoToIntro)
                        }
                    },
                    onCancel = {
                        Log.d("VoiceIntentRouter", "Action cancelled by user")
                    }
                )
                _pendingAction.value = action
            }

            SemanticIntent.CONFIRM_ACTION -> {
                val pending = _pendingAction.value
                if (pending != null) {
                    pending.onConfirm.invoke()
                    _pendingAction.value = null
                }
            }

            SemanticIntent.CANCEL_ACTION -> {
                val pending = _pendingAction.value
                if (pending != null) {
                    pending.onCancel.invoke()
                    _pendingAction.value = null
                }
            }

            SemanticIntent.CHANGE_LANGUAGE -> {
                onLanguageChange?.invoke(result.detectedLanguage)
            }

            SemanticIntent.CREATE_LOT_REQUEST -> {
                _navigationEvents.tryEmit(NavigationAction.CreateLotDialog)
            }

            SemanticIntent.QUERY_REVENUE_SUMMARY -> {
                _collectorEvents.tryEmit(CollectorAction.OpenRevenueSummaryTab)
            }

            SemanticIntent.ANALYZE_CURRENT_LOT_AI -> {
                _collectorEvents.tryEmit(CollectorAction.AnalyzeCurrentLot)
            }

            SemanticIntent.SHARE_MY_LOCATION -> {
                _collectorEvents.tryEmit(CollectorAction.TurnOnLocationSharing)
            }

            SemanticIntent.FIND_NEARBY_COLLECTORS -> {
                _collectorEvents.tryEmit(CollectorAction.OpenNearbyCollectorsTab)
            }

            SemanticIntent.FIND_NEARBY_RECYCLERS -> {
                _collectorEvents.tryEmit(CollectorAction.OpenRecyclersTab)
            }

            SemanticIntent.OPEN_CONNECTIONS -> {
                _collectorEvents.tryEmit(CollectorAction.OpenConnectionsTab)
            }

            SemanticIntent.REQUEST_RECYCLER_QUOTE -> {
                _collectorEvents.tryEmit(CollectorAction.RequestRecyclerQuote)
            }

            SemanticIntent.QUERY_MATERIAL_PRICE,
            SemanticIntent.QUERY_SAFETY_GUIDELINES,
            SemanticIntent.QUERY_EWASTE_DISPOSAL,
            SemanticIntent.QUERY_USER_STATS,
            SemanticIntent.HELP_AND_CAPABILITIES,
            SemanticIntent.GENERAL_EWASTE_QA,
            SemanticIntent.CLARIFY_REQUEST,
            SemanticIntent.UNKNOWN -> {
                // Pure informational response or clarification handled via spokenResponse
            }
        }
    }

    fun confirmPendingAction() {
        coroutineScope.launch {
            val pending = _pendingAction.value
            if (pending != null) {
                pending.onConfirm.invoke()
                _pendingAction.value = null
                val confText = "Action successfully executed."
                addConversationTurn("assistant", confText)
                onSpeakResponse?.invoke(confText, Language.ENGLISH)
            }
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingAction.value
        if (pending != null) {
            pending.onCancel.invoke()
            _pendingAction.value = null
            val cancelText = "Action cancelled."
            addConversationTurn("assistant", cancelText)
            onSpeakResponse?.invoke(cancelText, Language.ENGLISH)
        }
    }

    private fun addConversationTurn(role: String, text: String) {
        val current = _conversationHistory.value.toMutableList()
        current.add(ConversationTurn(role, text))
        if (current.size > 8) {
            current.removeAt(0)
        }
        _conversationHistory.value = current
    }

    fun clearHistory() {
        _conversationHistory.value = emptyList()
        _lastResult.value = null
        _pendingAction.value = null
    }
}
