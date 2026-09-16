package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.i18n.LanguageManager
import com.example.model.Language
import com.example.model.RoleType
import com.example.model.VoiceIntentType
import com.example.model.VoiceSettings
import com.example.model.VoiceSpeed
import com.example.model.VoiceState
import com.example.network.NetworkMonitor
import com.example.voice.NavigationAction
import com.example.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IntroViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ewaste_prefs", Context.MODE_PRIVATE)
    val voiceEngine = VoiceEngine(application, viewModelScope)
    private val networkMonitor = NetworkMonitor(application)

    private val _selectedLanguage = MutableStateFlow(loadSavedLanguage())
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    private val _navigationDestination = MutableStateFlow<RoleType?>(null)
    val navigationDestination: StateFlow<RoleType?> = _navigationDestination.asStateFlow()

    private val _activeSpeakerSection = MutableStateFlow<String?>(null)
    val activeSpeakerSection: StateFlow<String?> = _activeSpeakerSection.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    val voiceState: StateFlow<VoiceState> = voiceEngine.voiceState
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val voiceSettings: StateFlow<VoiceSettings> = voiceEngine.settings
    val lastTranscript = voiceEngine.lastTranscript
    val lastIntentResult = voiceEngine.lastIntentResult

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
            }
        }
        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                if (!speaking) {
                    _activeSpeakerSection.value = null
                }
            }
        }
        viewModelScope.launch {
            voiceEngine.router.navigationEvents.collect { navAction ->
                when (navAction) {
                    is NavigationAction.GoToIntro -> {
                        _navigationDestination.value = null
                    }
                    is NavigationAction.OpenLogin -> {
                        _navigationDestination.value = navAction.role
                    }
                    is NavigationAction.OpenPortal -> {
                        _navigationDestination.value = navAction.role
                    }
                    is NavigationAction.CreateLotDialog -> {
                        _navigationDestination.value = RoleType.INFORMAL_COLLECTOR
                    }
                }
            }
        }
        viewModelScope.launch {
            voiceEngine.lastIntentResult.collect { result ->
                if (result != null && result.intent == VoiceIntentType.ROLE_SELECTION && result.detectedRole != null) {
                    // Open the role login page immediately
                    _navigationDestination.value = result.detectedRole
                }
            }
        }

        voiceEngine.router.onLanguageChange = { lang ->
            setLanguage(lang)
        }
        voiceEngine.router.onSignOut = {
            _navigationDestination.value = null
        }

        loadVoiceSettings()
    }

    private fun loadSavedLanguage(): Language {
        val code = prefs.getString("selected_lang", Language.ENGLISH.code) ?: Language.ENGLISH.code
        return Language.values().firstOrNull { it.code == code } ?: Language.ENGLISH
    }

    fun setLanguage(language: Language) {
        _selectedLanguage.value = language
        prefs.edit().putString("selected_lang", language.code).apply()
        voiceEngine.stopSpeaking()
        _activeSpeakerSection.value = null
    }

    private fun loadVoiceSettings() {
        val guidance = prefs.getBoolean("voice_guidance", true)
        val speedName = prefs.getString("voice_speed", VoiceSpeed.NORMAL.name) ?: VoiceSpeed.NORMAL.name
        val speed = try { VoiceSpeed.valueOf(speedName) } catch (e: Exception) { VoiceSpeed.NORMAL }
        val muted = prefs.getBoolean("voice_muted", false)
        voiceEngine.updateSettings(VoiceSettings(guidance, speed, muted))
    }

    fun updateVoiceSettings(newSettings: VoiceSettings) {
        voiceEngine.updateSettings(newSettings)
        prefs.edit()
            .putBoolean("voice_guidance", newSettings.guidanceEnabled)
            .putString("voice_speed", newSettings.speechSpeed.name)
            .putBoolean("voice_muted", newSettings.isMuted)
            .apply()
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun closeSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun setPermissionDialogVisible(visible: Boolean) {
        _showPermissionDialog.value = visible
    }

    fun toggleMicrophone(hasPermission: Boolean) {
        if (!hasPermission) {
            _showPermissionDialog.value = true
            return
        }

        when (voiceEngine.voiceState.value) {
            VoiceState.IDLE, VoiceState.ERROR -> {
                voiceEngine.startListening(_selectedLanguage.value)
            }
            VoiceState.LISTENING -> {
                voiceEngine.stopListening()
            }
            VoiceState.CONFIRMATION, VoiceState.RESULT -> {
                voiceEngine.cancelVoice()
            }
            VoiceState.PROCESSING -> {
                voiceEngine.cancelVoice()
            }
        }
    }

    fun confirmRoleSelection(role: RoleType) {
        voiceEngine.cancelVoice()
        _navigationDestination.value = role
    }

    fun processVoiceCommand(command: String) {
        voiceEngine.processSpokenInput(command, _selectedLanguage.value)
    }

    fun cancelConfirmation() {
        voiceEngine.cancelVoice()
    }

    fun toggleSpeakerSection(sectionId: String, textToSpeak: String) {
        if (_activeSpeakerSection.value == sectionId && voiceEngine.isSpeaking.value) {
            voiceEngine.stopSpeaking()
            _activeSpeakerSection.value = null
        } else {
            _activeSpeakerSection.value = sectionId
            voiceEngine.speak(textToSpeak, _selectedLanguage.value)
        }
    }

    fun playVoiceHelp() {
        val text = LanguageManager.getVoiceHelpExplanation(_selectedLanguage.value)
        toggleSpeakerSection("help_section", text)
    }

    fun playHeaderIntro() {
        val text = LanguageManager.getIntroVoiceIntroduction(_selectedLanguage.value)
        toggleSpeakerSection("hero_section", text)
    }

    fun selectRoleCard(role: RoleType) {
        voiceEngine.stopSpeaking()
        _navigationDestination.value = role
    }

    fun navigateBackToIntro() {
        _navigationDestination.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
    }
}
