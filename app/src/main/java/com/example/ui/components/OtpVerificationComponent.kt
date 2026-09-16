package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.LanguageManager
import com.example.model.Language
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber

/**
 * Six-digit separate cell OTP input field.
 * Renders 6 distinct modern digit cells with active focus border, animations, and auto-submit capability.
 */
@Composable
fun SixDigitOtpInputField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isEnabled: Boolean = true,
    autoFocus: Boolean = true,
    onCompleted: ((String) -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (autoFocus && isEnabled) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore focus request failure on first render
            }
        }
    }

    BasicTextField(
        value = otpValue,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() }.take(6)
            onOtpChange(filtered)
            if (filtered.length == 6) {
                focusManager.clearFocus()
                onCompleted?.invoke(filtered)
            }
        },
        enabled = isEnabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                if (otpValue.length == 6) {
                    onCompleted?.invoke(otpValue)
                }
            }
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .testTag("six_digit_otp_input"),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until 6) {
                    val char = otpValue.getOrNull(index)?.toString() ?: ""
                    val isCurrentFocus = isEnabled && otpValue.length == index
                    val isFilled = char.isNotEmpty()

                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isError -> MaterialTheme.colorScheme.error
                            isCurrentFocus -> ForestGreenPrimary
                            isFilled -> EmeraldAccent
                            else -> MintBorder
                        },
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "borderColor"
                    )

                    val borderWidth by animateDpAsState(
                        targetValue = if (isCurrentFocus || isError) 2.dp else 1.dp,
                        animationSpec = tween(durationMillis = 180),
                        label = "borderWidth"
                    )

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            isCurrentFocus -> MintLight
                            isFilled -> Color.White
                            else -> Color(0xFFF9FBFA)
                        },
                        label = "backgroundColor"
                    )

                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(backgroundColor)
                            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp))
                            .testTag("otp_digit_box_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = char,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary,
                                textAlign = TextAlign.Center
                            )
                        } else if (isCurrentFocus) {
                            // Blinking cursor dot indicator
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 20.dp)
                                    .background(ForestGreenPrimary, RoundedCornerShape(1.dp))
                            )
                        } else {
                            Text(
                                text = "•",
                                fontSize = 20.sp,
                                color = TextSecondaryMuted.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}

/**
 * Resend Timer View with countdown formatting (mm:ss) and active trigger action.
 */
@Composable
fun ResendTimerView(
    countdownSeconds: Int,
    isRequested: Boolean,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
    language: Language = Language.ENGLISH,
    maxDurationSeconds: Int = 45
) {
    val isTimerActive = countdownSeconds > 0 && isRequested
    val minutes = countdownSeconds / 60
    val seconds = countdownSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progress = if (maxDurationSeconds > 0) (countdownSeconds.toFloat() / maxDurationSeconds.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        color = if (isTimerActive) MintLight.copy(alpha = 0.5f) else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isTimerActive) MintBorder else EmeraldAccent.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTimerActive) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                        CircularProgressIndicator(
                            progress = { progress },
                            strokeWidth = 2.5.dp,
                            color = ForestGreenPrimary,
                            trackColor = MintBorder,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Resend OTP in $formattedTime",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Standard telecom SMS & WhatsApp gateway",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MintLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Didn't receive the OTP code?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "You can request a fresh code now",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }

            TextButton(
                onClick = onResendClick,
                enabled = !isTimerActive,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ForestGreenPrimary,
                    disabledContentColor = TextSecondaryMuted.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("resend_otp_trigger_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = LanguageManager.getResendOtp(language),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Complete, standalone OTP Verification UI Component for Phone Number Verification.
 * Includes:
 * 1. Masked phone info header with Edit option
 * 2. Dedicated 6-Digit input field with copy/paste and clear
 * 3. Audio readout integration for accessibility
 * 4. Resend timer (countdown + one-tap resend)
 * 5. Verify & Sign-in action button with progress state
 */
@Composable
fun OtpVerificationComponent(
    phoneNumber: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isOtpRequested: Boolean,
    resendCountdown: Int,
    isVerifying: Boolean,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    modifier: Modifier = Modifier,
    language: Language = Language.ENGLISH,
    errorMessage: String? = null,
    infoMessage: String? = null,
    onEditPhoneNumber: (() -> Unit)? = null,
    onListenOtp: (() -> Unit)? = null,
    onAutofillCode: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    val maskedPhone = remember(phoneNumber) {
        if (phoneNumber.length >= 10) {
            "+91 ${phoneNumber.take(2)}•••••${phoneNumber.takeLast(3)}"
        } else if (phoneNumber.isNotEmpty()) {
            "+91 $phoneNumber"
        } else {
            "+91 (Registered Number)"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MintBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Icon + Title + Masked Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MintLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "OTP Verification",
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone Number Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ForestGreenPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = TextSecondaryMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Code sent to $maskedPhone",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                if (onEditPhoneNumber != null && !isVerifying) {
                    IconButton(
                        onClick = onEditPhoneNumber,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Phone Number",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-header with Listen and Clipboard helpers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enter 6-Digit Verification Code",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio readout button
                    if (onListenOtp != null) {
                        TextButton(
                            onClick = onListenOtp,
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Listen to OTP",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Listen",
                                fontSize = 11.sp,
                                color = ForestGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Paste button
                    TextButton(
                        onClick = {
                            val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            val code = clipText.filter { it.isDigit() }.take(6)
                            if (code.isNotEmpty()) {
                                onOtpChange(code)
                                onAutofillCode?.invoke(code)
                                Toast.makeText(context, "Pasted: $code", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No numeric code in clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste OTP",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Paste",
                            fontSize = 11.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6-Digit Input Field
            SixDigitOtpInputField(
                otpValue = otpCode,
                onOtpChange = onOtpChange,
                isError = errorMessage != null,
                isEnabled = !isVerifying,
                onCompleted = {
                    if (it.length == 6 && !isVerifying) {
                        onVerifyOtp()
                    }
                }
            )

            // Dynamic Info / Error Banner
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (infoMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MintLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = infoMessage,
                            fontSize = 11.sp,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend Timer Widget
            ResendTimerView(
                countdownSeconds = resendCountdown,
                isRequested = isOtpRequested,
                onResendClick = onResendOtp,
                language = language
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Submit / Verify Button
            Button(
                onClick = onVerifyOtp,
                enabled = !isVerifying && otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForestGreenPrimary,
                    disabledContainerColor = ForestGreenPrimary.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_otp_submit_button")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = LanguageManager.getVerifyingCredentials(language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.getVerifyOtpAndLogin(language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
