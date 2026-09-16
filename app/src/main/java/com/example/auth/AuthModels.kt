package com.example.auth

import com.example.model.RoleType

enum class AuthMethod(val title: String) {
    OTP_AUTHENTICATION("OTP Verification"),
    MOBILE_OTP("Mobile Number"),
    EMAIL_PASSWORD("Email & Password"),
    GOOGLE_OAUTH("Continue with Google")
}

enum class OtpDeliveryDestination(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val badgeText: String
) {
    ALL_CHANNELS(
        id = "all",
        title = "All Channels (Mobile + Email + Google)",
        subtitle = "Receives identical verification code on Mobile SMS, Email, and Google Account simultaneously",
        iconEmoji = "🚀",
        badgeText = "Recommended"
    ),
    MOBILE_SMS(
        id = "mobile",
        title = "Registered Mobile No",
        subtitle = "Direct cellular SMS & Android push alert (+91)",
        iconEmoji = "📱",
        badgeText = "SMS OTP"
    ),
    REGISTERED_EMAIL(
        id = "email",
        title = "Registered Email",
        subtitle = "Official verification dispatch to your registered inbox",
        iconEmoji = "📧",
        badgeText = "Email OTP"
    ),
    GOOGLE_ACCOUNT(
        id = "google",
        title = "Google Account",
        subtitle = "Delivered to your connected Google Account / Gmail",
        iconEmoji = "🌐",
        badgeText = "Google Code"
    )
}

enum class AccountStatus(val label: String, val isAllowed: Boolean) {
    ACTIVE("Active & Verified", true),
    PENDING_VERIFICATION("Pending KYC / Registration Approval", false),
    REJECTED("Rejected / KYC Non-Compliant", false),
    SUSPENDED("Suspended / Compliance Audit Flagged", false),
    DISABLED("Disabled by Administrator", false)
}

enum class AuthPipelineStep(val stepNumber: Int, val descriptionEn: String, val descriptionHi: String, val descriptionMr: String) {
    IDLE(0, "Awaiting credentials", "विवरण की प्रतीक्षा है", "तपशीलांची वाट पाहत आहे"),
    AUTHENTICATING_USER(1, "Authenticating user credentials...", "उपयोगकर्ता विवरण प्रमाणित किया जा रहा है...", "वापरकर्ता तपशील प्रमाणित केले जात आहेत..."),
    VERIFYING_ACCOUNT(2, "Verifying account status & KYC...", "खाता स्थिति और केवाईसी सत्यापित किया जा रहा है...", "खाते स्थिती आणि केवायसी तपासले जात आहे..."),
    VERIFYING_ROLE(3, "Verifying statutory role authorization...", "वैधानिक भूमिका प्राधिकरण सत्यापित किया जा रहा है...", "वैधानिक भूमिका प्राधिकरण तपासले जात आहे..."),
    VERIFYING_PERMISSIONS(4, "Validating role permissions & compliance...", "भूमिका अनुमतियां और अनुपालन मान्य किया जा रहा है...", "भूमिका परवानग्या आणि अनुपालन तपासले जात आहे..."),
    SUCCESS(5, "Login success: Access granted", "लॉगिन सफल: पहुंच स्वीकृत", "लॉगिन यशस्वी: प्रवेश मंजूर"),
    FAILED(5, "Authentication failed", "प्रमाणीकरण विफल", "प्रमाणीकरण अयशस्वी")
}

data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val role: RoleType,
    val accountStatus: AccountStatus,
    val permissions: List<String>,
    val statutoryIdentifier: String,
    val entityName: String,
    val sessionToken: String,
    val authMethod: AuthMethod,
    val verifiedTimestamp: Long = System.currentTimeMillis()
)

data class OtpSession(
    val destination: OtpDeliveryDestination = OtpDeliveryDestination.ALL_CHANNELS,
    val phoneNumber: String? = null,
    val email: String? = null,
    val googleAccount: String? = null,
    val otpCode: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = System.currentTimeMillis() + 5 * 60 * 1000L, // 5 minutes
    var attemptsRemaining: Int = 3,
    var isLocked: Boolean = false,
    var lockExpiryTimestamp: Long? = null
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiryTimestamp

    val remainingSeconds: Long
        get() = maxOf(0L, (expiryTimestamp - System.currentTimeMillis()) / 1000L)
}

/**
 * Machine-readable error codes for the auth layer so UI/tests can branch on
 * failures without parsing localized messages.
 */
enum class AuthErrorCode(val userMessage: String) {
    PROVIDER_NOT_CONFIGURED("Authentication provider is not configured. Contact the administrator."),
    NETWORK_ERROR("Unable to reach the authentication server. Check your internet connection and try again."),
    INVALID_OTP("The verification code is invalid or has expired. Please request a new code."),
    OTP_NOT_REQUESTED("No active verification code. Tap 'Send Verification OTP' first."),
    INVALID_CREDENTIALS("Invalid email or password. Please check your credentials."),
    EMAIL_NOT_CONFIRMED("This email is registered but not yet confirmed."),
    ACCOUNT_NOT_FOUND("No account is registered with these details."),
    ACCOUNT_GATED("Your account is not active yet."),
    ROLE_MISMATCH("This account is registered under a different role."),
    ROLE_NOT_ASSIGNED("No role has been assigned to this account."),
    EMAIL_ALREADY_REGISTERED("An account with this email already exists."),
    GENERIC("Authentication failed. Please try again.")
}

data class AuthResult(
    val isSuccess: Boolean,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null,
    val failureStep: AuthPipelineStep? = null,
    val errorCode: AuthErrorCode? = null
)
