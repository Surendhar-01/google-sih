package com.example.model

enum class AppScreen(val title: String) {
    INTRO("Intro & Welcome"),
    INFORMAL_COLLECTOR_AUTH("Collector Login"),
    FORMAL_RECYCLER_AUTH("Recycler Login"),
    GOVERNMENT_ADMIN_AUTH("Admin Login"),
    COLLECTOR_DASHBOARD("Collector Dashboard"),
    FORMAL_RECYCLER_PORTAL("Formal Recycler Portal"),
    GOVERNMENT_ADMIN_PORTAL("Government Admin Portal")
}

enum class SemanticIntent(val description: String) {
    OPEN_LOGIN("Navigate to authentication / sign-in screen for a specific or general role"),
    SELECT_MOBILE_LOGIN("Switch to the mobile number OTP login tab"),
    SELECT_EMAIL_LOGIN("Switch to the email and password login tab"),
    SELECT_GOOGLE_LOGIN("Switch to the Continue with Google login tab"),
    SEND_OTP("Request sending a verification code to the entered mobile number"),
    RESEND_OTP("Resend the mobile verification code"),
    VERIFY_OTP("Verify the entered six-digit code"),
    CHANGE_PHONE("Clear the phone number and switch back to entering a new number"),
    OPEN_REGISTER("Open the role profile registration flow"),
    VOICE_HELP("Ask for voice assistant help while on the login screen"),
    GO_BACK("Go back to the previous screen / close the login screen"),
    NAVIGATE_SCREEN("Navigate to a specific screen or section within the app"),
    QUERY_MATERIAL_PRICE("Inquire about scrap prices, scrap value, or per-kg rates"),
    QUERY_SAFETY_GUIDELINES("Inquire about hazardous e-waste handling safety or risks"),
    QUERY_EWASTE_DISPOSAL("Inquire about safe e-waste disposal methods, hazardous material handling, and recycler channels"),
    QUERY_USER_STATS("Inquire about user's active lots, settled earnings, or dues"),
    QUERY_REVENUE_SUMMARY("Ask for today's or this month's settled earnings summary"),
    ANALYZE_CURRENT_LOT_AI("Run the on-device AI analysis on the current lot photos"),
    SHARE_MY_LOCATION("Turn on collector location sharing for the network"),
    FIND_NEARBY_RECYCLERS("List authorized recyclers near the collector"),
    FIND_NEARBY_COLLECTORS("Show nearby collectors sharing their location"),
    OPEN_CONNECTIONS("Open the connections, requests, and quotations hub"),
    REQUEST_RECYCLER_QUOTE("Request a live quotation from a recycler"),
    CREATE_LOT_REQUEST("Initiate creation of a new scrap collection lot"),
    DESTRUCTIVE_ACTION_REQUEST("Perform a high-impact or destructive action (delete, wipe, sign-out)"),
    CONFIRM_ACTION("Confirm an ongoing pending action"),
    CANCEL_ACTION("Cancel or abort a pending action"),
    CHANGE_LANGUAGE("Switch application language to English, Hindi, or Marathi"),
    HELP_AND_CAPABILITIES("Ask for assistance or guidance on voice capabilities"),
    GENERAL_EWASTE_QA("General questions about e-waste disposal, EPR rules, or compliance"),
    CLARIFY_REQUEST("Ambiguous input requiring user clarification"),
    UNKNOWN("Unrecognized command or noisy input")
}

data class SemanticAnalysisResult(
    val intent: SemanticIntent,
    val targetRole: RoleType? = null,
    val targetScreen: AppScreen? = null,
    val materialCategory: MaterialCategory? = null,
    val quantityKg: Double? = null,
    val detectedLanguage: Language = Language.ENGLISH,
    val confidence: Float = 0.95f,
    val spokenResponse: String,
    val actionDescription: String,
    val requiresConfirmation: Boolean = false,
    val isDestructive: Boolean = false,
    val rawTranscript: String,
    val clarificationQuestion: String? = null,
    val aiEngineSource: String = "On-Device Semantic NLP",
    val disposalSafetyGuidelines: List<String> = emptyList(),
    val hazardousMaterials: List<String> = emptyList(),
    val cpcbRegulation: String? = null
)

data class PendingAction(
    val id: String,
    val title: String,
    val promptQuestion: String,
    val onConfirm: suspend () -> Unit,
    val onCancel: () -> Unit
)

data class ConversationTurn(
    val role: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppNavigationContext(
    val currentScreen: AppScreen,
    val currentRole: RoleType? = null,
    val isAuthenticated: Boolean = false,
    val pendingAction: PendingAction? = null,
    val lastMentionedCategory: MaterialCategory? = null
)
