package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.auth.AuthErrorCode
import com.example.auth.AuthMethod
import com.example.auth.AuthPipelineStep
import com.example.auth.GoogleAuthManager
import com.example.auth.OtpDeliveryDestination
import com.example.auth.SupabaseAuthConfig
import com.example.auth.SupabaseAuthService
import com.example.i18n.LanguageManager
import com.example.model.Language
import com.example.model.RoleType
import com.example.ui.admin.GovernmentAdminPortalScreen
import com.example.ui.collector.CollectorDashboardScreen
import com.example.ui.collector.CollectorDashboardViewModel
import com.example.ui.components.OtpVerificationComponent
import com.example.ui.components.ResendTimerView
import com.example.ui.components.SixDigitOtpInputField
import com.example.ui.recycler.FormalRecyclerPortalScreen
import com.example.ui.theme.BackgroundCream
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.voice.AuthUiAction
import com.example.voice.VoiceEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified authentication screen for all three roles:
 * - Mobile OTP (SMS) — primary login method
 * - Email & Password
 * - Continue with Google (Credential Manager + Supabase ID token exchange)
 *
 * In production the OTP is created, delivered and verified by Supabase and is never
 * generated, displayed, logged or spoken by the app. When the development-only OTP
 * emulation is enabled (SupabaseAuthService.DEV_ONLY_OTP_ENABLED == true), the code is
 * generated on-device and displayed in a clearly-labelled development banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedRoleAuthScreen(
    role: RoleType,
    language: Language,
    voiceEngine: VoiceEngine? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authService = remember { SupabaseAuthService.getInstance(context) }
    val googleAuthManager = remember { GoogleAuthManager(context) }

    // Authentication States
    var selectedAuthMethod by remember { mutableStateOf(AuthMethod.OTP_AUTHENTICATION) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isOtpRequested by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableIntStateOf(0) }

    val currentStep by authService.currentPipelineStep.collectAsState()
    val authenticatedUser by authService.authenticatedUser.collectAsState()
    val devDisplayOtp by authService.devDisplayOtp.collectAsState()

    // Resend countdown timer loop
    LaunchedEffect(isOtpRequested) {
        if (isOtpRequested) {
            resendCountdown = authService.getResendCooldownRemaining().toInt().coerceAtLeast(45)
            while (resendCountdown > 0) {
                delay(1000L)
                resendCountdown--
            }
        }
    }

    // Role-specific Branding & Metadata
    val roleTitle = LanguageManager.getRoleTitle(role, language)
    val roleIcon: ImageVector = when (role) {
        RoleType.INFORMAL_COLLECTOR -> Icons.Default.Recycling
        RoleType.FORMAL_RECYCLER -> Icons.Default.Factory
        RoleType.GOVERNMENT_ADMIN -> Icons.Default.Shield
    }

    val statutorySubtitle = when (role) {
        RoleType.INFORMAL_COLLECTOR -> "CPCB / MoEFCC Registered Aggregator Gateway"
        RoleType.FORMAL_RECYCLER -> "Rule 13 Authorized Treatment & Recycling Facility"
        RoleType.GOVERNMENT_ADMIN -> "MoEFCC EPR Regulatory & Statutory Oversight Portal"
    }

    // Reusable OTP send action (button and voice both use it). Never stores or speaks the OTP.
    val sendOtpAction: () -> Unit = {
        coroutineScope.launch {
            if (phoneNumber.length != 10) {
                errorMessage = "Please enter a valid 10-digit mobile number."
                voiceEngine?.speak(errorMessage ?: "", language)
                return@launch
            }
            isVerifying = true
            errorMessage = null
            infoMessage = null
            val result = authService.generateAndSendOtp(
                destination = OtpDeliveryDestination.MOBILE_SMS,
                phoneNumber = phoneNumber
            )
            isVerifying = false
            result.onSuccess { otpCodePayload ->
                isOtpRequested = true
                otpCode = ""
                if (authService.isDevOtpFlowEnabled()) {
                    infoMessage = "DEV TESTING ONLY — Verification code generated locally. SMS gateway not contacted."
                    voiceEngine?.speak(
                        "Development mode. Verification code displayed on screen. No SMS was sent.",
                        language
                    )
                } else {
                    infoMessage = "Verification code sent to +91 $phoneNumber via SMS."
                    voiceEngine?.speak(
                        "A verification code has been sent to your mobile by SMS. Please enter it to continue.",
                        language
                    )
                }
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to send verification code."
                voiceEngine?.speak(errorMessage ?: "", language)
                isOtpRequested = false
            }
        }
    }

    // Reusable OTP verify action (button and voice both use it).
    val verifyOtpAction: () -> Unit = {
        if (otpCode.length != 6) {
            errorMessage = "Please enter the complete 6-digit verification code."
            voiceEngine?.speak(errorMessage ?: "", language)
        } else {
            isVerifying = true
            errorMessage = null
            coroutineScope.launch {
                val res = authService.verifyOtpAndLogin(
                    destination = OtpDeliveryDestination.MOBILE_SMS,
                    phoneNumber = phoneNumber,
                    enteredOtp = otpCode,
                    targetRole = role
                )
                isVerifying = false
                if (!res.isSuccess) {
                    errorMessage = res.errorMessage ?: "Verification failed."
                    voiceEngine?.speak(
                        "Verification failed. ${res.errorMessage ?: "Please try again."}",
                        language
                    )
                } else {
                    infoMessage = "Verified successfully! Access granted."
                    voiceEngine?.speak(
                        "Authentication confirmed. Access granted to $roleTitle portal.",
                        language
                    )
                }
            }
        }
    }

    // Execute voice-driven login-screen actions. SEND_OTP / VERIFY_OTP never carry the code value.
    LaunchedEffect(voiceEngine) {
        voiceEngine?.router?.authEvents?.collect { action ->
            when (action) {
                AuthUiAction.SelectMobileLogin -> selectedAuthMethod = AuthMethod.OTP_AUTHENTICATION
                AuthUiAction.SelectEmailLogin -> selectedAuthMethod = AuthMethod.EMAIL_PASSWORD
                AuthUiAction.SelectGoogleLogin -> selectedAuthMethod = AuthMethod.GOOGLE_OAUTH
                AuthUiAction.SendOtp -> sendOtpAction()
                AuthUiAction.VerifyOtp -> {
                    errorMessage = null
                    voiceEngine?.speak(
                        "Please enter the 6 digit code on the screen. I cannot read it aloud for your security.",
                        language
                    )
                }
                AuthUiAction.ChangePhone -> {
                    isOtpRequested = false
                    otpCode = ""
                    phoneNumber = ""
                    errorMessage = null
                    infoMessage = null
                }
                AuthUiAction.OpenRegister -> voiceEngine?.speak(
                    "Role profile registration is not available yet. Please contact the CPCB help desk.",
                    language
                )
                AuthUiAction.GoBack -> onBack()
            }
        }
    }

    // If already authenticated, display the matching role dashboard
    if (authenticatedUser != null) {
        val user = authenticatedUser!!
        when (user.role) {
            RoleType.INFORMAL_COLLECTOR -> {
                val collectorVm = remember {
                    CollectorDashboardViewModel(
                        application = context.applicationContext as Application,
                        voiceEngine = voiceEngine
                    )
                }
                CollectorDashboardScreen(
                    viewModel = collectorVm,
                    collectorPhone = user.phoneNumber ?: user.statutoryIdentifier,
                    language = language,
                    onBack = {
                        authService.signOut()
                        onBack()
                    }
                )
            }
            RoleType.FORMAL_RECYCLER -> {
                val collectorVm = remember {
                    CollectorDashboardViewModel(context.applicationContext as Application)
                }
                FormalRecyclerPortalScreen(
                    viewModel = collectorVm,
                    facilityName = user.entityName,
                    cpcbNumber = user.statutoryIdentifier,
                    language = language,
                    onBack = {
                        authService.signOut()
                        onBack()
                    }
                )
            }
            RoleType.GOVERNMENT_ADMIN -> {
                GovernmentAdminPortalScreen(
                    adminId = user.statutoryIdentifier,
                    language = language,
                    onBack = {
                        authService.signOut()
                        onBack()
                    }
                )
            }
        }
        return
    }

    // Authentication Scaffold
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = LanguageManager.getAppName(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ForestGreenPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "$roleTitle Authentication",
                            fontSize = 11.sp,
                            color = ForestGreenDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("auth_nav_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val instructions = LanguageManager.getAuthInstructions(role, language)
                            voiceEngine?.speak(instructions, language)
                        },
                        modifier = Modifier.testTag("auth_voice_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Hear Authentication Guidance",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Role Badge & Statutory Designation Header
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_auth_header_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = roleIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = roleTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = statutorySubtitle,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = LanguageManager.getAuthInstructions(role, language),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Authentication Methods Tab Selector (OTP / Email / Continue with Google)
            val authTabs = listOf(
                AuthMethod.OTP_AUTHENTICATION,
                AuthMethod.EMAIL_PASSWORD,
                AuthMethod.GOOGLE_OAUTH
            )
            val selectedTabIndex = authTabs.indexOf(selectedAuthMethod).coerceAtLeast(0)

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = ForestGreenPrimary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = ForestGreenPrimary,
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    authTabs.forEach { method ->
                        Tab(
                            selected = selectedAuthMethod == method,
                            onClick = {
                                selectedAuthMethod = method
                                errorMessage = null
                                infoMessage = null
                            },
                            text = {
                                Text(
                                    text = when (method) {
                                        AuthMethod.OTP_AUTHENTICATION -> LanguageManager.getOtpTabLabel(language)
                                        AuthMethod.MOBILE_OTP -> LanguageManager.getMobileAuthTab(language)
                                        AuthMethod.EMAIL_PASSWORD -> LanguageManager.getEmailAuthTab(language)
                                        AuthMethod.GOOGLE_OAUTH -> LanguageManager.getGoogleAuthTab(language)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedAuthMethod == method) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = when (method) {
                                        AuthMethod.OTP_AUTHENTICATION -> Icons.Default.Security
                                        AuthMethod.MOBILE_OTP -> Icons.Default.Smartphone
                                        AuthMethod.EMAIL_PASSWORD -> Icons.Default.Email
                                        AuthMethod.GOOGLE_OAUTH -> Icons.Default.Language
                                    },
                                    contentDescription = method.title,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_${method.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Error and Info Banners
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_error_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (infoMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_info_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Info",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = infoMessage ?: "",
                            color = ForestGreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Form by Selected Authentication Method
            when (selectedAuthMethod) {
                AuthMethod.OTP_AUTHENTICATION, AuthMethod.MOBILE_OTP -> {
                    PhoneOtpAuthCard(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                        otpCode = otpCode,
                        onOtpChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                        isOtpRequested = isOtpRequested,
                        resendCountdown = resendCountdown,
                        isVerifying = isVerifying,
                        devOtp = devDisplayOtp,
                        language = language,
                        onGenerateOtp = sendOtpAction,
                        onVerifyLogin = verifyOtpAction,
                        onChangeNumber = {
                            isOtpRequested = false
                            otpCode = ""
                            phoneNumber = ""
                            errorMessage = null
                            infoMessage = null
                        }
                    )
                }

                AuthMethod.EMAIL_PASSWORD -> {
                    EmailPasswordAuthCard(
                        email = emailInput,
                        onEmailChange = { emailInput = it },
                        password = passwordInput,
                        onPasswordChange = { passwordInput = it },
                        isVerifying = isVerifying,
                        language = language,
                        onLogin = {
                            if (!emailInput.contains("@") || emailInput.length < 5) {
                                errorMessage = "Please enter a valid registered email address."
                                voiceEngine?.speak(errorMessage ?: "", language)
                                return@EmailPasswordAuthCard
                            }
                            if (passwordInput.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                                voiceEngine?.speak(errorMessage ?: "", language)
                                return@EmailPasswordAuthCard
                            }

                            isVerifying = true
                            errorMessage = null
                            coroutineScope.launch {
                                val res = authService.loginWithEmail(
                                    email = emailInput,
                                    password = passwordInput,
                                    targetRole = role
                                )
                                isVerifying = false
                                if (!res.isSuccess) {
                                    errorMessage = res.errorMessage ?: "Authentication failed."
                                    voiceEngine?.speak(errorMessage ?: "", language)
                                } else {
                                    infoMessage = "Email credentials authenticated. Access authorized."
                                    voiceEngine?.speak("Email verification successful. Loading $roleTitle portal.", language)
                                }
                            }
                        }
                    )
                }

                AuthMethod.GOOGLE_OAUTH -> {
                    GoogleOAuthCard(
                        isVerifying = isVerifying,
                        onContinueWithGoogle = {
                            if (!SupabaseAuthConfig.googleIsConfigured()) {
                                errorMessage = "Google Sign-In is not configured yet. Add GOOGLE_WEB_CLIENT_ID to the project .env file."
                                return@GoogleOAuthCard
                            }
                            isVerifying = true
                            errorMessage = null
                            infoMessage = null
                            coroutineScope.launch {
                                val googleResult = googleAuthManager.signInWithGoogle(
                                    activityContext = context,
                                    serverClientId = SupabaseAuthConfig.googleWebClientId
                                )
                                if (googleResult.isSuccess) {
                                    val res = authService.loginWithGoogleAuthResult(
                                        authResult = googleResult,
                                        targetRole = role
                                    )
                                    isVerifying = false
                                    if (!res.isSuccess) {
                                        errorMessage = res.errorMessage ?: "Statutory authorization failed."
                                        voiceEngine?.speak(errorMessage ?: "", language)
                                    } else {
                                        infoMessage = "Google Authentication verified. Access granted."
                                        voiceEngine?.speak("Google authentication successful. Welcome, ${res.userProfile?.displayName}.", language)
                                    }
                                } else if (googleResult.isCancelled) {
                                    isVerifying = false
                                    errorMessage = "Google Sign-In was cancelled."
                                } else {
                                    isVerifying = false
                                    errorMessage = googleResult.errorMessage ?: "Google Sign-In failed."
                                    voiceEngine?.speak(errorMessage ?: "", language)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Statutory Verification Pipeline Card (Visual Multi-Step Progress)
            VerificationPipelineCard(
                currentStep = currentStep,
                language = language
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Security Assurance & Statutory Notice
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Statutory E-Waste (Management) Rules, 2022 • Supabase Auth • Central Pollution Control Board (CPCB) Verified",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. MOBILE OTP AUTHENTICATION CARD (SMS, phone-first)
// -------------------------------------------------------------------------------------
@Composable
private fun PhoneOtpAuthCard(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isOtpRequested: Boolean,
    resendCountdown: Int,
    isVerifying: Boolean,
    devOtp: String?,
    language: Language,
    onGenerateOtp: () -> Unit,
    onVerifyLogin: () -> Unit,
    onChangeNumber: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MintLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = LanguageManager.getOtpCardTitle(language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = LanguageManager.getOtpCardSubtitle(language),
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(visible = !isOtpRequested) {
                Column {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneChange,
                        label = { Text(LanguageManager.getMobileNumberLabel(language)) },
                        placeholder = { Text("Mobile Number") },
                        prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = ForestGreenPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreenPrimary,
                            focusedLabelColor = ForestGreenPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_phone_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onGenerateOtp,
                        enabled = !isVerifying && phoneNumber.length == 10,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_send_otp_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending Code...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageManager.getGenerateOtpButton(language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "A 6-digit verification code will be sent to your mobile via SMS.",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted,
                        lineHeight = 14.sp
                    )
                }
            }

            AnimatedVisibility(visible = isOtpRequested) {
                Column {
                    Text(
                        text = "We sent a 6-digit verification code to",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        modifier = Modifier.testTag("auth_otp_sent_to")
                    )
                    Text(
                        text = "+91 $phoneNumber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ForestGreenPrimary
                    )
                    TextButton(
                        onClick = onChangeNumber,
                        enabled = !isVerifying,
                        modifier = Modifier.testTag("auth_change_number")
                    ) {
                        Text(
                            text = "Change Number",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // DEV-ONLY: display the generated OTP prominently so testers
                    // can copy it without checking logcat or an SMS gateway.
                    if (devOtp != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF8E1) // warm amber background
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFFFB300)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_dev_otp_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = null,
                                        tint = Color(0xFFF57F17),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DEVELOPMENT TESTING ONLY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFF57F17)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your verification code is:",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = devOtp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = 8.sp,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.testTag("auth_dev_otp_display")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Enter this code to sign in. No SMS was sent.",
                                    fontSize = 10.sp,
                                    color = TextSecondaryMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = LanguageManager.getEnterOtpHeader(language),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ForestGreenPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SixDigitOtpInputField(
                        otpValue = otpCode,
                        onOtpChange = onOtpChange,
                        isEnabled = !isVerifying,
                        autoFocus = true,
                        onCompleted = {
                            if (it.length == 6 && !isVerifying) {
                                onVerifyLogin()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResendTimerView(
                        countdownSeconds = resendCountdown,
                        isRequested = isOtpRequested,
                        onResendClick = onGenerateOtp,
                        language = language
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onVerifyLogin,
                        enabled = !isVerifying && otpCode.length == 6,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_verify_otp_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.getVerifyingCredentials(language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.getVerifyOtpAndLogin(language), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (devOtp != null) {
                        Text(
                            text = "Development-only OTP flow: the code is generated and shown on-device for testing. No SMS is sent. This flow must NOT be used in production.",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 14.sp,
                            modifier = Modifier.testTag("auth_dev_otp_security_note")
                        )
                    } else {
                        Text(
                            text = "For your security, we will never ask you to read or share this code.",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 14.sp,
                            modifier = Modifier.testTag("auth_otp_security_note")
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. EMAIL & PASSWORD AUTHENTICATION CARD
// -------------------------------------------------------------------------------------
@Composable
private fun EmailPasswordAuthCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isVerifying: Boolean,
    language: Language,
    onLogin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Official Email Authentication",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ForestGreenPrimary
            )
            Text(
                text = "Sign in via Supabase Auth with your registered official email and password.",
                fontSize = 12.sp,
                color = TextSecondaryMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Official Email Address") },
                placeholder = { Text("you@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenPrimary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isVerifying,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreenPrimary,
                    focusedLabelColor = ForestGreenPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenPrimary)
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isVerifying,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestGreenPrimary,
                    focusedLabelColor = ForestGreenPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogin,
                enabled = !isVerifying && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_email_login_button")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticating...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In with Email", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. CONTINUE WITH GOOGLE AUTHENTICATION CARD
// -------------------------------------------------------------------------------------
@Composable
private fun GoogleOAuthCard(
    isVerifying: Boolean,
    onContinueWithGoogle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F0FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Google Sign In",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Identity Sign-In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Credential Manager + Supabase ID Token",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Surface(
                    color = MintLight,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "OAuth 2.0",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sign in with your verified Google Account. The id_token is securely exchanged with Supabase, then statutory credentials are validated against the CPCB National Registry.",
                fontSize = 12.sp,
                color = TextSecondaryMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onContinueWithGoogle,
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8),
                    disabledContainerColor = Color(0xFF1A73E8).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_continue_google_button")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Authenticating with Google...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Continue with Google Account", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 4. STATUTORY VERIFICATION PIPELINE PROGRESS CARD
// -------------------------------------------------------------------------------------
@Composable
private fun VerificationPipelineCard(
    currentStep: AuthPipelineStep,
    language: Language
) {
    val steps = listOf(
        AuthPipelineStep.AUTHENTICATING_USER,
        AuthPipelineStep.VERIFYING_ACCOUNT,
        AuthPipelineStep.VERIFYING_ROLE,
        AuthPipelineStep.VERIFYING_PERMISSIONS
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_pipeline_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = LanguageManager.getVerificationPipelineTitle(language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ForestGreenPrimary
                )
                Text(
                    text = when (currentStep) {
                        AuthPipelineStep.IDLE -> "Standby"
                        AuthPipelineStep.SUCCESS -> "Verified"
                        AuthPipelineStep.FAILED -> "Verification Failed"
                        else -> "Processing Step ${currentStep.stepNumber}/4"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (currentStep) {
                        AuthPipelineStep.SUCCESS -> SuccessGreen
                        AuthPipelineStep.FAILED -> Color(0xFFC62828)
                        else -> EmeraldAccent
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            steps.forEachIndexed { index, step ->
                val isCompleted = currentStep.stepNumber > step.stepNumber || currentStep == AuthPipelineStep.SUCCESS
                val isCurrent = currentStep == step
                val isPending = currentStep.stepNumber < step.stepNumber && currentStep != AuthPipelineStep.SUCCESS

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> SuccessGreen
                                    isCurrent -> EmeraldAccent
                                    else -> Color(0xFFE0E0E0)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else if (isCurrent) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        } else {
                            Text(
                                text = "${index + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = when (language) {
                            Language.ENGLISH -> step.descriptionEn
                            Language.HINDI -> step.descriptionHi
                            Language.MARATHI -> step.descriptionMr
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCompleted -> SuccessGreen
                            isCurrent -> ForestGreenPrimary
                            else -> TextSecondaryMuted
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// BACKWARD-COMPATIBLE PUBLIC SCREEN EXPORTS
// -------------------------------------------------------------------------------------
@Composable
fun InformalCollectorAuthScreen(
    language: Language,
    voiceEngine: VoiceEngine? = null,
    onBack: () -> Unit
) {
    UnifiedRoleAuthScreen(
        role = RoleType.INFORMAL_COLLECTOR,
        language = language,
        voiceEngine = voiceEngine,
        onBack = onBack
    )
}

@Composable
fun FormalRecyclerAuthScreen(
    language: Language,
    voiceEngine: VoiceEngine? = null,
    onBack: () -> Unit
) {
    UnifiedRoleAuthScreen(
        role = RoleType.FORMAL_RECYCLER,
        language = language,
        voiceEngine = voiceEngine,
        onBack = onBack
    )
}

@Composable
fun GovernmentAdminAuthScreen(
    language: Language,
    voiceEngine: VoiceEngine? = null,
    onBack: () -> Unit
) {
    UnifiedRoleAuthScreen(
        role = RoleType.GOVERNMENT_ADMIN,
        language = language,
        voiceEngine = voiceEngine,
        onBack = onBack
    )
}