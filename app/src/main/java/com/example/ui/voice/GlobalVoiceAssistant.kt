package com.example.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Language
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import com.example.model.VoiceState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber
import com.example.voice.VoiceEngine
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Universal E-Waste Voice Assistant Floating Action Button and Interactive Voice Sheet.
 * Allows informal collectors, recyclers, and citizens to ask natural language questions
 * regarding e-waste disposal, hazardous material segregation, and CPCB 2022 guidelines.
 */
@Composable
fun GlobalVoiceAssistantBar(
    voiceEngine: VoiceEngine,
    currentLanguage: Language,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by voiceEngine.voiceState.collectAsState()
    val isSpeaking by voiceEngine.isSpeaking.collectAsState()
    val lastTranscript by voiceEngine.lastTranscript.collectAsState()
    val lastSemanticResult by voiceEngine.lastSemanticResult.collectAsState()
    val pendingAction by voiceEngine.router.pendingAction.collectAsState()
    val audioRms by voiceEngine.audioRms.collectAsState()

    var showRecordingSheet by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Auto-show recording sheet when listening or when result arrives
    LaunchedEffect(voiceState) {
        if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.PROCESSING) {
            showRecordingSheet = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        showPermissionDialog = false
        if (granted) {
            voiceEngine.startListening(currentLanguage)
            showRecordingSheet = true
        }
    }

    fun handleVoiceFabClick() {
        if (voiceState == VoiceState.LISTENING) {
            voiceEngine.stopListening()
            return
        }

        if (!voiceEngine.isRecognitionAvailable()) {
            showTextInputDialog = true
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            voiceEngine.startListening(currentLanguage)
            showRecordingSheet = true
        } else {
            showPermissionDialog = true
        }
    }

    // Pulse animation for recording wave
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Pending Confirmation Card (Destructive Actions)
        AnimatedVisibility(
            visible = pendingAction != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            pendingAction?.let { pending ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.5.dp, WarningAmber, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmation Required",
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDark,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pending.promptQuestion,
                            fontSize = 13.sp,
                            color = TextPrimaryDark,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { voiceEngine.router.cancelPendingAction() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("voice_cancel_action_button")
                            ) {
                                Text("Cancel", color = Color.Gray, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { voiceEngine.router.confirmPendingAction() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                modifier = Modifier.testTag("voice_confirm_action_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Main E-Waste Voice Floating Action Button (FAB)
        EwasteVoiceAssistantFab(
            voiceState = voiceState,
            pulseScale = pulseScale,
            onClick = { handleVoiceFabClick() },
            onOpenTypeDialog = { showTextInputDialog = true },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Modal Voice Recording State Sheet (Active audio recording, waveform & disposal answers)
    if (showRecordingSheet) {
        EwasteVoiceRecordingSheet(
            voiceEngine = voiceEngine,
            currentLanguage = currentLanguage,
            onDismiss = {
                showRecordingSheet = false
                if (voiceState == VoiceState.LISTENING) {
                    voiceEngine.stopListening()
                }
            }
        )
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text("Microphone Permission Required", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
            },
            text = {
                Text(
                    "To enable hands-free voice queries on e-waste disposal, safe handling rules, and scrap rates in English, Hindi, and Marathi, please grant microphone access.",
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Natural Voice Command Text Input Dialog (for manual/fallback voice input)
    if (showTextInputDialog) {
        NaturalVoiceInputDialog(
            language = currentLanguage,
            onDismiss = { showTextInputDialog = false },
            onSubmit = { input ->
                showTextInputDialog = false
                showRecordingSheet = true
                voiceEngine.processSpokenInput(input, currentLanguage)
            }
        )
    }
}

/**
 * Prominent Material 3 Floating Action Button specifically for Voice Recording & AI E-Waste Assistant.
 */
@Composable
fun EwasteVoiceAssistantFab(
    voiceState: VoiceState,
    pulseScale: Float,
    onClick: () -> Unit,
    onOpenTypeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState == VoiceState.LISTENING
    val isProcessing = voiceState == VoiceState.PROCESSING

    Surface(
        color = if (isListening) Color(0xFF991B1B) else ForestGreenPrimary,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .border(
                width = if (isListening) 2.dp else 1.2.dp,
                color = if (isListening) Color(0xFFEF4444) else EmeraldAccent.copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .testTag("voice_recording_fab")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Mic Touch Action Target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .testTag("voice_assistant_mic_fab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Color(0xFFEF4444) else EmeraldAccent
                        )
                        .scale(if (isListening) pulseScale else 1f)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Recording Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                isListening -> "Listening... Tap to Stop"
                                isProcessing -> "AI Analyzing Query..."
                                else -> "Ask AI E-Waste Assistant"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (isListening) "Speak your e-waste disposal question" else "Voice queries on disposal, prices, & CPCB rules",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Keyboard text input trigger button
            IconButton(
                onClick = onOpenTypeDialog,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .testTag("voice_assistant_type_query_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Type E-Waste Query",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Modal Bottom Sheet presenting the full voice recording state, live audio waveform,
 * real-time transcription stream, disposal suggestions, and structured AI answers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EwasteVoiceRecordingSheet(
    voiceEngine: VoiceEngine,
    currentLanguage: Language,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voiceState by voiceEngine.voiceState.collectAsState()
    val audioRms by voiceEngine.audioRms.collectAsState()
    val lastTranscript by voiceEngine.lastTranscript.collectAsState()
    val lastSemanticResult by voiceEngine.lastSemanticResult.collectAsState()
    val isSpeaking by voiceEngine.isSpeaking.collectAsState()

    var recordingSeconds by remember { mutableIntStateOf(0) }
    var activeLanguage by remember { mutableStateOf(currentLanguage) }

    // Recording timer
    LaunchedEffect(voiceState) {
        if (voiceState == VoiceState.LISTENING) {
            recordingSeconds = 0
            while (true) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Language Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MintLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI E-Waste Assistant",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenDark
                        )
                        Text(
                            text = "Natural Language Voice Inquiries",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Assistant",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Voice Recording State Section
            Surface(
                color = if (voiceState == VoiceState.LISTENING) Color(0xFFFEF2F2) else MintLight,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (voiceState == VoiceState.LISTENING) Color(0xFFFECACA) else MintBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_recording_state_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recording status badge + timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (voiceState == VoiceState.LISTENING) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECORDING (00:${if (recordingSeconds < 10) "0$recordingSeconds" else "$recordingSeconds"})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        } else if (voiceState == VoiceState.PROCESSING) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PROCESSING INTENT WITH GEMINI AI...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "READY FOR NATURAL VOICE QUERY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Waveform Visualizer
                    AudioWaveformVisualizer(
                        isListening = voiceState == VoiceState.LISTENING,
                        audioRms = audioRms,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Speech Transcript
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        if (lastTranscript.isNotBlank()) {
                            Text(
                                text = "\"$lastTranscript\"",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = if (voiceState == VoiceState.LISTENING)
                                    "Listening... Speak naturally in English, हिंदी, or मराठी about e-waste disposal..."
                                else
                                    "Tap 'Start Recording' or choose a quick disposal query below.",
                                fontSize = 13.sp,
                                color = TextSecondaryMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control Buttons (Start / Stop / Clear)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (voiceState == VoiceState.LISTENING) {
                            Button(
                                onClick = { voiceEngine.stopListening() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("stop_recording_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Analyze", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { voiceEngine.cancelVoice() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("cancel_recording_button")
                            ) {
                                Text("Cancel", color = Color.Gray)
                            }
                        } else {
                            Button(
                                onClick = { voiceEngine.startListening(activeLanguage) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("start_recording_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Voice Recording", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Answer & E-Waste Disposal Result Card
            lastSemanticResult?.let { result ->
                EwasteDisposalAnswerCard(
                    result = result,
                    isSpeaking = isSpeaking,
                    onReplay = { voiceEngine.speak(result.spokenResponse, result.detectedLanguage) },
                    onStopSpeech = { voiceEngine.stopSpeaking() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick E-Waste Disposal Natural Language Query Prompts
            Text(
                text = "Quick Natural Language Disposal Queries",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ForestGreenDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            val disposalPrompts = when (activeLanguage) {
                Language.ENGLISH -> listOf(
                    "How to safely dispose of old lithium-ion phone batteries?",
                    "What are the toxic hazards of broken CRT monitor glass?",
                    "Why is open-air burning of PVC copper cables banned?",
                    "How to properly dispose of computer motherboards?",
                    "How to wipe data and dispose of an old laptop?",
                    "What are the CPCB E-Waste Rules 2022 guidelines?"
                )
                Language.HINDI -> listOf(
                    "पुरानी लिथियम बैटरी का सुरक्षित निपटान कैसे करें?",
                    "सीआरटी मॉनिटर का कांच टूटने पर क्या खतरा होता है?",
                    "तांबे के तार को खुली आग में जलाना क्यों मना है?",
                    "पुराने कंप्यूटर मदरबोर्ड का निपटान कैसे करें?",
                    "लैपटॉप का डेटा मिटाकर कैसे रीसायकल करें?",
                    "सीपीसीबी ई-कचरा नियम 2022 के मुख्य नियम क्या हैं?"
                )
                Language.MARATHI -> listOf(
                    "जुन्या लिथियम बॅटरीची सुरक्षित विल्हेवाट कशी लावावी?",
                    "सीआरटी मॉनिटरची काच फुटल्यास काय धोका असतो?",
                    "तांब्याच्या केबल्स उघड्यावर जाळणे का बेकायदेशीर आहे?",
                    "संगणक मदरबोर्डची पुनर्प्रक्रिया कशी करावी?",
                    "लॅपटॉपचा डेटा नष्ट करून विल्हेवाट कशी लावावी?",
                    "ई-कचरा नियम 2022 नुसार अधिकृत मार्गदर्शक तत्त्वे काय आहेत?"
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                disposalPrompts.forEachIndexed { index, prompt ->
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                voiceEngine.processSpokenInput(prompt, activeLanguage)
                            }
                            .testTag("quick_query_chip_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = prompt,
                                fontSize = 13.sp,
                                color = ForestGreenDark,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Interactive Audio Waveform Canvas that visualizes frequencies during recording.
 */
@Composable
fun AudioWaveformVisualizer(
    isListening: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = spacing * 0.55f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            val sineFactor = if (isListening) {
                val waveAngle = (phase + i * 18f) * (Math.PI / 180.0)
                val rmsMod = (audioRms.coerceIn(0f, 15f) / 15f) * 0.7f + 0.3f
                (sin(waveAngle).toFloat().coerceAtLeast(0.15f)) * rmsMod
            } else {
                0.12f
            }

            val barHeight = (size.height * 0.85f * sineFactor).coerceAtLeast(4f)
            val barX = i * spacing + (spacing - barWidth) / 2f
            val barY = centerY - (barHeight / 2f)

            val barColor = if (isListening) {
                if (i % 2 == 0) Color(0xFFEF4444) else Color(0xFFF87171)
            } else {
                if (i % 2 == 0) EmeraldAccent else ForestGreenPrimary
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(barX, barY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Structured Presentation Card displaying AI reasoning, safe handling instructions,
 * hazardous material warnings, and CPCB regulation badges for e-waste disposal.
 */
@Composable
fun EwasteDisposalAnswerCard(
    result: SemanticAnalysisResult,
    isSpeaking: Boolean,
    onReplay: () -> Unit,
    onStopSpeech: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .border(1.2.dp, MintBorder, RoundedCornerShape(18.dp))
            .testTag("ewaste_disposal_answer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with AI Source & TTS Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MintLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = result.aiEngineSource,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (isSpeaking) onStopSpeech() else onReplay() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MintLight)
                            .testTag("voice_replay_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = "Audio Playback",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Spoken Explanation
            Text(
                text = result.spokenResponse,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )

            // Step-by-Step Safe Handling Guidelines
            if (result.disposalSafetyGuidelines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Mandatory Safe Disposal Protocols:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                result.disposalSafetyGuidelines.forEachIndexed { index, guide ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = ForestGreenPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = guide,
                            fontSize = 12.sp,
                            color = TextPrimaryDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Hazardous Elements Warning Tags
            if (result.hazardousMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hazardous Elements Involved:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.hazardousMaterials.forEach { hazard ->
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = hazard,
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Statutory CPCB Regulation Citation
            result.cpcbRegulation?.let { reg ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ForestGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Statutory Rule: $reg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NaturalVoiceInputDialog(
    language: Language,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    val sampleNaturalPhrases = when (language) {
        Language.ENGLISH -> listOf(
            "How to safely dispose of lithium phone batteries?",
            "What is the toxic risk of breaking CRT monitor glass?",
            "Can I burn copper wire in open air?",
            "Open informal collector login",
            "What is the price of motherboards?",
            "Take me to the recycler portal"
        )
        Language.HINDI -> listOf(
            "लिथियम बैटरी का सुरक्षित निपटान कैसे करें?",
            "सीआरटी मॉनिटर का कांच टूटने पर क्या खतरा होता है?",
            "क्या तार जलाना सुरक्षित है?",
            "कबाड़ी लॉगिन खोलें",
            "सर्किट बोर्ड का क्या भाव है?",
            "रीसायकलिंग पोर्टल में लॉगिन करना है"
        )
        Language.MARATHI -> listOf(
            "लिथियम बॅटरीची सुरक्षित विल्हेवाट कशी लावावी?",
            "सीआरटी मॉनिटरची काच फुटल्यास काय धोका असतो?",
            "केबल्स जाळणे सुरक्षित आहे का?",
            "कलेक्टर लॉगिन पेज उघडा",
            "मदरबोर्डचा काय भाव आहे?",
            "रीसायकलर पोर्टल दाखवा"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ForestGreenPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask AI E-Waste Query", fontWeight = FontWeight.Bold, color = ForestGreenDark, fontSize = 17.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Type or ask any question about e-waste disposal, hazardous material segregation, scrap prices, or platform login in English, Hindi, or Marathi.",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g. How to dispose of old phone batteries safely?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_text_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreenPrimary,
                        focusedLabelColor = ForestGreenPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Try natural queries:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ForestGreenPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleNaturalPhrases) { phrase ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MintLight,
                            modifier = Modifier.clickable {
                                textInput = phrase
                            }
                        ) {
                            Text(
                                text = phrase,
                                fontSize = 11.sp,
                                color = ForestGreenDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSubmit(textInput)
                    }
                },
                enabled = textInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                modifier = Modifier.testTag("voice_command_submit_button")
            ) {
                Text("Ask AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
