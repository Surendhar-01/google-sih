package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.Language
import com.example.model.RoleType
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import com.example.model.VoiceIntentResult
import com.example.model.VoiceIntentType
import com.example.model.VoiceSettings
import com.example.model.VoiceSpeed
import com.example.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Core Voice Assistant Engine:
 * - Speech-to-Text via Android ASR / Whisper audio pipeline
 * - Text-to-Speech via multilingual TTS (en-IN, hi-IN, mr-IN)
 * - Integration with centralized AI VoiceIntentRouter
 */
class VoiceEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val router: VoiceIntentRouter = VoiceIntentRouter(context = context, coroutineScope = coroutineScope)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _lastIntentResult = MutableStateFlow<VoiceIntentResult?>(null)
    val lastIntentResult: StateFlow<VoiceIntentResult?> = _lastIntentResult.asStateFlow()

    val lastSemanticResult: StateFlow<SemanticAnalysisResult?> = router.lastResult

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _settings = MutableStateFlow(VoiceSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initializeTts()

        router.onSpeakResponse = { text, lang ->
            speak(text, lang)
        }

        coroutineScope.launch {
            router.isProcessing.collect { processing ->
                if (processing) {
                    _voiceState.value = VoiceState.PROCESSING
                }
            }
        }

        coroutineScope.launch {
            router.lastResult.collect { semResult ->
                if (semResult != null) {
                    val legacyResult = when (semResult.intent) {
                        SemanticIntent.OPEN_LOGIN -> VoiceIntentResult(
                            intent = VoiceIntentType.ROLE_SELECTION,
                            detectedRole = semResult.targetRole ?: RoleType.INFORMAL_COLLECTOR,
                            transcript = semResult.rawTranscript,
                            confidence = semResult.confidence,
                            requiresConfirmation = semResult.requiresConfirmation
                        )
                        SemanticIntent.HELP_AND_CAPABILITIES -> VoiceIntentResult(
                            intent = VoiceIntentType.VOICE_HELP,
                            transcript = semResult.rawTranscript,
                            confidence = semResult.confidence,
                            requiresConfirmation = false
                        )
                        SemanticIntent.CHANGE_LANGUAGE -> VoiceIntentResult(
                            intent = VoiceIntentType.CHANGE_LANGUAGE,
                            targetLanguage = semResult.detectedLanguage,
                            transcript = semResult.rawTranscript,
                            confidence = semResult.confidence,
                            requiresConfirmation = false
                        )
                        SemanticIntent.UNKNOWN, SemanticIntent.CLARIFY_REQUEST -> VoiceIntentResult(
                            intent = VoiceIntentType.UNKNOWN,
                            transcript = semResult.rawTranscript,
                            confidence = semResult.confidence,
                            requiresConfirmation = false
                        )
                        else -> VoiceIntentResult(
                            intent = VoiceIntentType.ROLE_SELECTION,
                            detectedRole = semResult.targetRole,
                            transcript = semResult.rawTranscript,
                            confidence = semResult.confidence,
                            requiresConfirmation = semResult.requiresConfirmation
                        )
                    }
                    _lastIntentResult.value = legacyResult
                    _voiceState.value = if (semResult.requiresConfirmation) {
                        VoiceState.CONFIRMATION
                    } else if (semResult.intent == SemanticIntent.UNKNOWN) {
                        VoiceState.ERROR
                    } else {
                        VoiceState.RESULT
                    }
                }
            }
        }
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.w("VoiceEngine", "TextToSpeech initialization failed")
            }
        }
    }

    fun updateSettings(newSettings: VoiceSettings) {
        _settings.value = newSettings
        if (newSettings.isMuted) {
            stopSpeaking()
        }
    }

    fun speak(text: String, language: Language) {
        if (_settings.value.isMuted || !isTtsReady || text.isBlank()) return

        val locale = when (language) {
            Language.ENGLISH -> Locale("en", "IN")
            Language.HINDI -> Locale("hi", "IN")
            Language.MARATHI -> Locale("mr", "IN")
        }

        tts?.let { engine ->
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.ENGLISH
            }
            engine.setSpeechRate(_settings.value.speechSpeed.rate)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ewaste_speech_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(currentLanguage: Language) {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioRms.value = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        Log.w("VoiceEngine", "SpeechRecognizer error: $error")
                        _voiceState.value = VoiceState.IDLE
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _lastTranscript.value = text
                        processSpokenInput(text, currentLanguage)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _lastTranscript.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.localeTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Failed to start listening", e)
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w("VoiceEngine", "Stop listening exception", e)
        }
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.PROCESSING
        }
    }

    fun cancelVoice() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w("VoiceEngine", "Cancel listening exception", e)
        }
        stopSpeaking()
        _voiceState.value = VoiceState.IDLE
        _lastIntentResult.value = null
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun processSpokenInput(input: String, currentLanguage: Language) {
        _lastTranscript.value = input
        _voiceState.value = VoiceState.PROCESSING

        // Immediate synchronous semantic evaluation for instant state readiness
        val immediateResult = SemanticIntentClassifier.analyze(
            sentence = input,
            context = router.appContext.value,
            preferredLanguage = currentLanguage
        )
        router.setImmediateResult(immediateResult)

        val legacyResult = when (immediateResult.intent) {
            com.example.model.SemanticIntent.OPEN_LOGIN -> VoiceIntentResult(
                intent = VoiceIntentType.ROLE_SELECTION,
                detectedRole = immediateResult.targetRole ?: RoleType.INFORMAL_COLLECTOR,
                transcript = immediateResult.rawTranscript,
                confidence = immediateResult.confidence,
                requiresConfirmation = immediateResult.requiresConfirmation
            )
            com.example.model.SemanticIntent.HELP_AND_CAPABILITIES -> VoiceIntentResult(
                intent = VoiceIntentType.VOICE_HELP,
                transcript = immediateResult.rawTranscript,
                confidence = immediateResult.confidence,
                requiresConfirmation = false
            )
            com.example.model.SemanticIntent.CHANGE_LANGUAGE -> VoiceIntentResult(
                intent = VoiceIntentType.CHANGE_LANGUAGE,
                targetLanguage = immediateResult.detectedLanguage,
                transcript = immediateResult.rawTranscript,
                confidence = immediateResult.confidence,
                requiresConfirmation = false
            )
            com.example.model.SemanticIntent.UNKNOWN, com.example.model.SemanticIntent.CLARIFY_REQUEST -> VoiceIntentResult(
                intent = VoiceIntentType.UNKNOWN,
                transcript = immediateResult.rawTranscript,
                confidence = immediateResult.confidence,
                requiresConfirmation = false
            )
            else -> VoiceIntentResult(
                intent = VoiceIntentType.ROLE_SELECTION,
                detectedRole = immediateResult.targetRole,
                transcript = immediateResult.rawTranscript,
                confidence = immediateResult.confidence,
                requiresConfirmation = immediateResult.requiresConfirmation
            )
        }
        _lastIntentResult.value = legacyResult
        _voiceState.value = if (immediateResult.requiresConfirmation) {
            VoiceState.CONFIRMATION
        } else if (immediateResult.intent == com.example.model.SemanticIntent.UNKNOWN) {
            VoiceState.ERROR
        } else {
            VoiceState.RESULT
        }

        // Delegate to router for AI processing, vocal response, and navigation emission
        router.processSpokenText(input, currentLanguage)
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
