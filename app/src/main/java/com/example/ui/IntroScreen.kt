package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.i18n.LanguageManager
import com.example.model.Language
import com.example.model.RoleType
import com.example.model.VoiceIntentType
import com.example.model.VoiceSpeed
import com.example.model.VoiceState
import com.example.ui.components.EwasteReminderSheet
import com.example.ui.theme.BackgroundCream
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    viewModel: IntroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.selectedLanguage.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val activeSpeakerSection by viewModel.activeSpeakerSection.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val lastTranscript by viewModel.lastTranscript.collectAsState()
    val lastIntentResult by viewModel.lastIntentResult.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()

    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var showVoiceInputDialog by remember { mutableStateOf(false) }
    var showScannerScreen by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }

    if (showScannerScreen) {
        com.example.ui.scan.EwasteCameraScannerScreen(
            language = currentLanguage,
            onBack = { showScannerScreen = false },
            onSpeakText = { text, lang ->
                viewModel.voiceEngine.speak(text, lang)
            }
        )
        return
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setPermissionDialogVisible(false)
        if (isGranted) {
            viewModel.toggleMicrophone(hasPermission = true)
        }
    }

    fun handleMicTap() {
        if (!viewModel.voiceEngine.isRecognitionAvailable()) {
            showVoiceInputDialog = true
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleMicrophone(hasPermission = true)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Offline Warning Banner
            if (!isOnline) {
                Surface(
                    color = WarningAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.getOfflineWarning(currentLanguage),
                            fontSize = 12.sp,
                            color = ForestGreenDark,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 1. TOP HEADER
            TopHeader(
                currentLanguage = currentLanguage,
                isDropdownExpanded = languageDropdownExpanded,
                onDropdownToggle = { languageDropdownExpanded = !languageDropdownExpanded },
                onLanguageSelect = { lang ->
                    viewModel.setLanguage(lang)
                    languageDropdownExpanded = false
                },
                onScannerClick = { showScannerScreen = true },
                onReminderClick = { showReminderSheet = true },
                onSettingsClick = { viewModel.openSettingsDialog() },
                onDismissDropdown = { languageDropdownExpanded = false }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1.5 QUICK ACTIONS: Scan E-Waste & Pickup Reminder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showScannerScreen = true },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_scan_ewaste_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = when (currentLanguage) {
                                Language.ENGLISH -> "Scan E-Waste"
                                Language.HINDI -> "ई-कचरा स्कैन करें"
                                Language.MARATHI -> "ई-कचरा स्कॅन करा"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Gemini AI item ID",
                            fontSize = 9.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showReminderSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_pickup_reminder_btn")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = when (currentLanguage) {
                                Language.ENGLISH -> "Pickup Reminder"
                                Language.HINDI -> "पिकअप अनुस्मारक"
                                Language.MARATHI -> "पिकअप स्मरणपत्र"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = when (currentLanguage) {
                                Language.ENGLISH -> "Collections & pickups"
                                Language.HINDI -> "संग्रहण एवं पिकअप"
                                Language.MARATHI -> "संकलन व पिकअप"
                            },
                            fontSize = 9.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. MAIN WELCOME / BRANDING
            WelcomeSection(
                language = currentLanguage,
                isSpeaking = isSpeaking && activeSpeakerSection == "hero_section",
                onSpeakToggle = {
                    viewModel.playHeaderIntro()
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 3. LARGE VOICE ASSISTANT MICROPHONE
            VoiceAssistantSection(
                language = currentLanguage,
                voiceState = voiceState,
                isSpeaking = isSpeaking,
                lastTranscript = lastTranscript,
                onMicTap = { handleMicTap() },
                onListenToggle = {
                    viewModel.playHeaderIntro()
                },
                onVoiceCommand = { cmd ->
                    viewModel.processVoiceCommand(cmd)
                },
                onVoiceSimulateClick = {
                    showVoiceInputDialog = true
                }
            )

            // Confirmation Box if a role was detected by voice
            AnimatedVisibility(
                visible = voiceState == VoiceState.CONFIRMATION && lastIntentResult?.detectedRole != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                lastIntentResult?.detectedRole?.let { role ->
                    VoiceConfirmationCard(
                        role = role,
                        language = currentLanguage,
                        transcript = lastTranscript,
                        onConfirm = { viewModel.confirmRoleSelection(role) },
                        onCancel = { viewModel.cancelConfirmation() }
                    )
                }
            }

            // Ambiguous Input Guidance
            AnimatedVisibility(
                visible = voiceState == VoiceState.ERROR && lastIntentResult?.intent == VoiceIntentType.UNKNOWN,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LanguageManager.getAmbiguousVoiceMessage(currentLanguage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ForestGreenPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 4. THREE ROLE CARDS (EXACTLY 3, NO CITIZEN OPTION)
            RoleCard(
                role = RoleType.INFORMAL_COLLECTOR,
                illustrationRes = R.drawable.ic_collector_card,
                language = currentLanguage,
                isSpeaking = isSpeaking && activeSpeakerSection == RoleType.INFORMAL_COLLECTOR.id,
                onCardClick = { viewModel.selectRoleCard(RoleType.INFORMAL_COLLECTOR) },
                onSpeakerClick = {
                    val text = LanguageManager.getRoleVoiceExplanation(RoleType.INFORMAL_COLLECTOR, currentLanguage)
                    viewModel.toggleSpeakerSection(RoleType.INFORMAL_COLLECTOR.id, text)
                },
                testTag = "collector_role_card"
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                role = RoleType.FORMAL_RECYCLER,
                illustrationRes = R.drawable.ic_recycler_card,
                language = currentLanguage,
                isSpeaking = isSpeaking && activeSpeakerSection == RoleType.FORMAL_RECYCLER.id,
                onCardClick = { viewModel.selectRoleCard(RoleType.FORMAL_RECYCLER) },
                onSpeakerClick = {
                    val text = LanguageManager.getRoleVoiceExplanation(RoleType.FORMAL_RECYCLER, currentLanguage)
                    viewModel.toggleSpeakerSection(RoleType.FORMAL_RECYCLER.id, text)
                },
                testTag = "recycler_role_card"
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                role = RoleType.GOVERNMENT_ADMIN,
                illustrationRes = R.drawable.ic_gov_admin_card,
                language = currentLanguage,
                isSpeaking = isSpeaking && activeSpeakerSection == RoleType.GOVERNMENT_ADMIN.id,
                onCardClick = { viewModel.selectRoleCard(RoleType.GOVERNMENT_ADMIN) },
                onSpeakerClick = {
                    val text = LanguageManager.getRoleVoiceExplanation(RoleType.GOVERNMENT_ADMIN, currentLanguage)
                    viewModel.toggleSpeakerSection(RoleType.GOVERNMENT_ADMIN.id, text)
                },
                testTag = "admin_role_card"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 5. VOICE HELP SECTION
            VoiceHelpSection(
                language = currentLanguage,
                isSpeaking = isSpeaking && activeSpeakerSection == "help_section",
                onHelpClick = { viewModel.playVoiceHelp() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Information
            Text(
                text = "E-Waste (Management) Rules, Government of India • Voice AI v1.0",
                fontSize = 11.sp,
                color = TextSecondaryMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Voice Settings Bottom Sheet
        if (showSettingsDialog) {
            VoiceSettingsSheet(
                settings = voiceSettings,
                language = currentLanguage,
                onDismiss = { viewModel.closeSettingsDialog() },
                onUpdate = { newSettings -> viewModel.updateVoiceSettings(newSettings) }
            )
        }

        // E-Waste Pickup / Collection Reminder Scheduling Sheet
        if (showReminderSheet) {
            EwasteReminderSheet(
                language = currentLanguage,
                onDismiss = { showReminderSheet = false }
            )
        }

        // Voice Command Input / Simulation Dialog
        if (showVoiceInputDialog) {
            VoiceCommandInputDialog(
                language = currentLanguage,
                onDismiss = { showVoiceInputDialog = false },
                onExecuteCommand = { cmd ->
                    showVoiceInputDialog = false
                    viewModel.processVoiceCommand(cmd)
                }
            )
        }

        // Permission Request Alert Dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setPermissionDialogVisible(false) },
                title = {
                    Text(
                        text = LanguageManager.getMicPermissionNeeded(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = when (currentLanguage) {
                            Language.ENGLISH -> "Allow microphone access to enable voice role selection and spoken navigation in your language."
                            Language.HINDI -> "अपनी भाषा में बोलकर भूमिका चुनने और वॉयस सहायता के लिए माइक्रोफ़ोन की अनुमति दें।"
                            Language.MARATHI -> "तुमच्या भाषेत बोलून भूमिका निवडण्यासाठी आणि व्हॉइस मदतीसाठी मायक्रोफोनला परवानगी द्या."
                        },
                        fontSize = 14.sp,
                        color = TextSecondaryMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                    ) {
                        Text(LanguageManager.getAllowPermission(currentLanguage))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setPermissionDialogVisible(false) }) {
                        Text("Cancel", color = TextSecondaryMuted)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun TopHeader(
    currentLanguage: Language,
    isDropdownExpanded: Boolean,
    onDropdownToggle: () -> Unit,
    onLanguageSelect: (Language) -> Unit,
    onScannerClick: () -> Unit,
    onReminderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDismissDropdown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_ewaste_logo),
                contentDescription = "E-Waste Logo",
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = LanguageManager.getAppName(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "India Clean Eco Portal",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action controls (Camera Scanner, Reminder, Settings & Language Dropdown)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onScannerClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("intro_camera_scanner_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "AI Camera Scanner",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onReminderClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("intro_reminder_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Collection Reminder",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("voice_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Voice Settings",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .clickable(onClick = onDropdownToggle)
                        .testTag("language_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐",
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentLanguage.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "▼",
                            fontSize = 10.sp,
                            color = ForestGreenPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = onDismissDropdown,
                    modifier = Modifier
                        .background(Color.White)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MintBorder, RoundedCornerShape(16.dp))
                        .testTag("language_dropdown_menu")
                ) {
                    Language.values().forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = lang.displayName,
                                            fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                            color = if (lang == currentLanguage) ForestGreenPrimary else TextPrimaryDark,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = lang.code.uppercase(),
                                            fontSize = 11.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }
                                    if (lang == currentLanguage) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ForestGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { onLanguageSelect(lang) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSection(
    language: Language,
    isSpeaking: Boolean,
    onSpeakToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = LanguageManager.getWelcomeTitle(language),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ForestGreenPrimary,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = LanguageManager.getWelcomeSubtitle(language),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceAssistantSection(
    language: Language,
    voiceState: VoiceState,
    isSpeaking: Boolean,
    lastTranscript: String,
    onMicTap: () -> Unit,
    onListenToggle: () -> Unit,
    onVoiceCommand: (String) -> Unit,
    onVoiceSimulateClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_animation")

    // Pulsing outer halo scale
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (voiceState == VoiceState.LISTENING) 1.25f else 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    // Pulsing halo alpha
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = if (voiceState == VoiceState.LISTENING) 0.35f else 0.15f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val micBgColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> EmeraldAccent
            VoiceState.PROCESSING -> ForestGreenDark
            VoiceState.ERROR -> WarningAmber
            else -> ForestGreenPrimary
        },
        label = "mic_color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Microphone Circle with animated ripples
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Outer glow ring 2
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary.copy(alpha = haloAlpha))
            )

            // Outer glow ring 1
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale((haloScale + 1f) / 2f)
                    .clip(CircleShape)
                    .background(MintBorder.copy(alpha = haloAlpha * 2))
            )

            // Main Green Circular Microphone Button
            Surface(
                onClick = onMicTap,
                shape = CircleShape,
                color = micBgColor,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(94.dp)
                    .testTag("microphone_main_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Close else Icons.Default.Mic,
                        contentDescription = "Voice Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Text & Subtitle
        Text(
            text = when (voiceState) {
                VoiceState.LISTENING -> LanguageManager.getListeningText(language)
                VoiceState.PROCESSING -> LanguageManager.getUnderstandingText(language)
                else -> LanguageManager.getVoiceSpeakPrompt(language)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (voiceState == VoiceState.LISTENING) EmeraldAccent else ForestGreenPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = if (voiceState == VoiceState.IDLE) {
                LanguageManager.getVoiceQuestion(language)
            } else if (lastTranscript.isNotBlank()) {
                "\"$lastTranscript\""
            } else {
                LanguageManager.getMicTapPrompt(language)
            },
            fontSize = 14.sp,
            color = TextSecondaryMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sound Waves / Speaker Indicator Bar
        if (voiceState == VoiceState.LISTENING || isSpeaking) {
            SoundWaveAnimation(isLive = true)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Listen Button (🔊 Listen / Stop)
        Surface(
            onClick = onListenToggle,
            shape = RoundedCornerShape(20.dp),
            color = if (isSpeaking) ForestGreenPrimary else MintLight,
            border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
            modifier = Modifier.testTag("listen_audio_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (isSpeaking) Color.White else ForestGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSpeaking) {
                        LanguageManager.getStopButton(language)
                    } else {
                        "🔊 " + LanguageManager.getListenButton(language)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSpeaking) Color.White else ForestGreenPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Spoken Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (language) {
                    Language.ENGLISH -> "Say or tap to open login:"
                    Language.HINDI -> "बोलें या सीधे लॉगिन खोलने के लिए टैप करें:"
                    Language.MARATHI -> "बोला किंवा लॉगिन उघडण्यासाठी टॅप करा:"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    onClick = { onVoiceCommand("open informal collector") },
                    shape = RoundedCornerShape(16.dp),
                    color = MintPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("quick_voice_collector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎙️ ", fontSize = 12.sp)
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Open Informal Collector"
                                Language.HINDI -> "कलेक्टर लॉगिन खोलो"
                                Language.MARATHI -> "कलेक्टर लॉगिन उघडा"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                    }
                }

                Surface(
                    onClick = { onVoiceCommand("open formal recycler") },
                    shape = RoundedCornerShape(16.dp),
                    color = MintPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("quick_voice_recycler")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎙️ ", fontSize = 12.sp)
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Open Formal Recycler"
                                Language.HINDI -> "रीसायकलर लॉगिन"
                                Language.MARATHI -> "रीसायकलर लॉगिन"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    onClick = { onVoiceCommand("open government admin") },
                    shape = RoundedCornerShape(16.dp),
                    color = MintPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("quick_voice_admin")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎙️ ", fontSize = 12.sp)
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Open Admin Login"
                                Language.HINDI -> "प्रशासन लॉगिन"
                                Language.MARATHI -> "प्रशासन लॉगिन"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                    }
                }

                Surface(
                    onClick = { onVoiceCommand("other login") },
                    shape = RoundedCornerShape(16.dp),
                    color = MintPill,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("quick_voice_other")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎙️ ", fontSize = 12.sp)
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Other Login"
                                Language.HINDI -> "अन्य लॉगिन"
                                Language.MARATHI -> "इतर लॉगिन"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark
                        )
                    }
                }
            }

            TextButton(
                onClick = onVoiceSimulateClick,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "⌨️ Type or test voice commands"
                        Language.HINDI -> "⌨️ वॉयस कमांड लिखकर जांचें"
                        Language.MARATHI -> "⌨️ व्हॉइस कमांड लिहून तपासा"
                    },
                    fontSize = 11.sp,
                    color = ForestGreenPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SoundWaveAnimation(isLive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_waves")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 22f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label = "h5"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(30.dp)
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EmeraldAccent)
            )
        }
    }
}

@Composable
private fun VoiceConfirmationCard(
    role: RoleType,
    language: Language,
    transcript: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldAccent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("voice_confirmation_dialog")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MintLight,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transcript.isNotBlank()) {
                Text(
                    text = "\"$transcript\"",
                    fontSize = 14.sp,
                    color = TextSecondaryMuted,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = LanguageManager.getContinueQuestion(role, language),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("voice_cancel_button")
                ) {
                    Text(
                        text = "✕ " + LanguageManager.getNoButton(language),
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("voice_confirm_button")
                ) {
                    Text(
                        text = "✓ " + LanguageManager.getYesButton(language),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: RoleType,
    illustrationRes: Int,
    language: Language,
    isSpeaking: Boolean,
    onCardClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    testTag: String
) {
    val title = LanguageManager.getRoleTitle(role, language)
    val description = LanguageManager.getRoleDescription(role, language)

    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Role Illustration Asset
            Surface(
                color = MintLight,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(76.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = illustrationRes),
                        contentDescription = title,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondaryMuted,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Cluster: Speaker icon & Forward Arrow
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Independent Speaker Button (does not navigate card)
                Surface(
                    onClick = onSpeakerClick,
                    shape = CircleShape,
                    color = if (isSpeaking) ForestGreenPrimary else MintLight,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("${testTag}_speaker")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = if (isSpeaking) Color.White else ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Forward Arrow
                Surface(
                    color = MintPill,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceHelpSection(
    language: Language,
    isSpeaking: Boolean,
    onHelpClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MintLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_help_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ForestGreenPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = LanguageManager.getNeedHelp(language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Tap for audio explanation"
                            Language.HINDI -> "ऑडियो विवरण के लिए टैप करें"
                            Language.MARATHI -> "ऑडिओ स्पष्टीकरणासाठी टॅप करा"
                        },
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Button(
                onClick = onHelpClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSpeaking) EmeraldAccent else ForestGreenPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("voice_help_button")
            ) {
                Text(
                    text = if (isSpeaking) {
                        LanguageManager.getStopButton(language)
                    } else {
                        LanguageManager.getVoiceHelpButton(language)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSettingsSheet(
    settings: com.example.model.VoiceSettings,
    language: Language,
    onDismiss: () -> Unit,
    onUpdate: (com.example.model.VoiceSettings) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.getVoiceSettings(language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryMuted)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Guidance Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LanguageManager.getVoiceGuidance(language),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Read prompts automatically",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
                Switch(
                    checked = settings.guidanceEnabled,
                    onCheckedChange = { onUpdate(settings.copy(guidanceEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ForestGreenPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speech Speed
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = LanguageManager.getSpeechSpeed(language),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.speechSpeed == VoiceSpeed.SLOW,
                        onClick = { onUpdate(settings.copy(speechSpeed = VoiceSpeed.SLOW)) },
                        label = { Text(LanguageManager.getSpeedSlow(language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = settings.speechSpeed == VoiceSpeed.NORMAL,
                        onClick = { onUpdate(settings.copy(speechSpeed = VoiceSpeed.NORMAL)) },
                        label = { Text(LanguageManager.getSpeedNormal(language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mute Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LanguageManager.getMuteLabel(language),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Silence all spoken voice responses",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
                Switch(
                    checked = settings.isMuted,
                    onCheckedChange = { onUpdate(settings.copy(isMuted = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WarningAmber
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VoiceCommandInputDialog(
    language: Language,
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    var commandText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Voice Command"
                        Language.HINDI -> "वॉयस कमांड"
                        Language.MARATHI -> "व्हॉइस कमांड"
                    },
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Say or type what to open (e.g. \"open informal collector\", \"other login\", or \"open recycler\"): "
                        Language.HINDI -> "क्या खोलना है बोलें या लिखें (जैसे \"कलेक्टर लॉगिन खोलो\", \"अन्य लॉगिन\"): "
                        Language.MARATHI -> "काय उघडायचे आहे ते बोला किंवा टाईप करा: "
                    },
                    fontSize = 13.sp,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    placeholder = {
                        Text(
                            text = "e.g. open informal collector",
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_input_field")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Quick options:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExecuteCommand("open informal collector") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Collector", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onExecuteCommand("open formal recycler") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Recycler", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExecuteCommand("open government admin") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Admin", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onExecuteCommand("other login") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Other", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onExecuteCommand(commandText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                modifier = Modifier.testTag("run_voice_command_button")
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.getCancelButton(language))
            }
        }
    )
}
