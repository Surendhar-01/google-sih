package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.RoleType
import com.example.ui.FormalRecyclerAuthScreen
import com.example.ui.GovernmentAdminAuthScreen
import com.example.ui.InformalCollectorAuthScreen
import com.example.ui.IntroScreen
import com.example.ui.IntroViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.voice.GlobalVoiceAssistantBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavHost()
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    viewModel: IntroViewModel = viewModel()
) {
    val currentDestination by viewModel.navigationDestination.collectAsState()
    val currentLanguage by viewModel.selectedLanguage.collectAsState()

    // Sync active screen with centralized VoiceIntentRouter
    LaunchedEffect(currentDestination) {
        val screen = when (currentDestination) {
            null -> AppScreen.INTRO
            RoleType.INFORMAL_COLLECTOR -> AppScreen.INFORMAL_COLLECTOR_AUTH
            RoleType.FORMAL_RECYCLER -> AppScreen.FORMAL_RECYCLER_AUTH
            RoleType.GOVERNMENT_ADMIN -> AppScreen.GOVERNMENT_ADMIN_AUTH
        }
        viewModel.voiceEngine.router.updateScreen(screen, currentDestination)
    }

    // Handle system back navigation when in role authentication
    BackHandler(enabled = currentDestination != null) {
        viewModel.navigateBackToIntro()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val role = currentDestination) {
            null -> {
                IntroScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            RoleType.INFORMAL_COLLECTOR -> {
                InformalCollectorAuthScreen(
                    language = currentLanguage,
                    voiceEngine = viewModel.voiceEngine,
                    onBack = { viewModel.navigateBackToIntro() }
                )
            }
            RoleType.FORMAL_RECYCLER -> {
                FormalRecyclerAuthScreen(
                    language = currentLanguage,
                    voiceEngine = viewModel.voiceEngine,
                    onBack = { viewModel.navigateBackToIntro() }
                )
            }
            RoleType.GOVERNMENT_ADMIN -> {
                GovernmentAdminAuthScreen(
                    language = currentLanguage,
                    voiceEngine = viewModel.voiceEngine,
                    onBack = { viewModel.navigateBackToIntro() }
                )
            }
        }

        // Universal Voice Assistant overlay available across all application sections
        GlobalVoiceAssistantBar(
            voiceEngine = viewModel.voiceEngine,
            currentLanguage = currentLanguage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

