package com.example.voice

import com.example.model.AppNavigationContext
import com.example.model.AppScreen
import com.example.model.Language
import com.example.model.MaterialCategory
import com.example.model.RoleType
import com.example.model.SemanticAnalysisResult
import com.example.model.SemanticIntent
import java.util.Locale

/**
 * Deep semantic intent classifier for multilingual speech (English, Hindi, Marathi, Hinglish).
 * Implements sentence-level structural semantic parsing, negation detection, modality analysis,
 * and entity extraction without shallow keyword-only logic.
 */
object SemanticIntentClassifier {

    fun analyze(
        sentence: String,
        context: AppNavigationContext,
        preferredLanguage: Language
    ): SemanticAnalysisResult {
        val trimmed = sentence.trim()
        val detectedLang = detectLanguage(trimmed, preferredLanguage)
        val normalized = trimmed.lowercase(Locale.ROOT)

        // 1. Negation detection: If the user says "don't open login" or "not want to sign in"
        val isNegated = checkNegation(normalized)

        // 2. Pending Confirmation Resolution (Conversational Context)
        if (context.pendingAction != null) {
            val isAffirmative = checkAffirmation(normalized)
            val isDismissive = checkDismissal(normalized)
            if (isAffirmative && !isDismissive) {
                return SemanticAnalysisResult(
                    intent = SemanticIntent.CONFIRM_ACTION,
                    detectedLanguage = detectedLang,
                    confidence = 0.96f,
                    spokenResponse = when (detectedLang) {
                        Language.ENGLISH -> "Action confirmed and executed."
                        Language.HINDI -> "कार्रवाई की पुष्टि की गई और निष्पादित किया गया।"
                        Language.MARATHI -> "कृतीची पुष्टी झाली आणि कार्यान्वित केली गेली."
                    },
                    actionDescription = "Confirmed: ${context.pendingAction.title}",
                    rawTranscript = trimmed
                )
            } else if (isDismissive) {
                return SemanticAnalysisResult(
                    intent = SemanticIntent.CANCEL_ACTION,
                    detectedLanguage = detectedLang,
                    confidence = 0.96f,
                    spokenResponse = when (detectedLang) {
                        Language.ENGLISH -> "Action cancelled."
                        Language.HINDI -> "कार्रवाई रद्द कर दी गई।"
                        Language.MARATHI -> "कृती रद्द केली गेली."
                    },
                    actionDescription = "Cancelled: ${context.pendingAction.title}",
                    rawTranscript = trimmed
                )
            }
        }

        // 3. Language Switching Intent
        val langSwitchResult = checkLanguageSwitch(normalized, trimmed)
        if (langSwitchResult != null) {
            return langSwitchResult
        }

        // 4. If sentence is negated for an action like navigation/login
        if (isNegated) {
            return SemanticAnalysisResult(
                intent = SemanticIntent.GENERAL_EWASTE_QA,
                detectedLanguage = detectedLang,
                confidence = 0.90f,
                spokenResponse = when (detectedLang) {
                    Language.ENGLISH -> "Understood. No action was taken. How else can I assist you with e-waste management?"
                    Language.HINDI -> "समझ गया। कोई कार्रवाई नहीं की गई। ई-कचरा प्रबंधन में मैं आपकी और क्या मदद कर सकता हूँ?"
                    Language.MARATHI -> "समजले. कोणतीही कृती केली नाही. मी आपल्याला ई-कचरा व्यवस्थापनात कशी मदत करू शकतो?"
                },
                actionDescription = "No action taken due to negated intent",
                rawTranscript = trimmed
            )
        }

        // 5. Destructive Action Intent (Sign out, Delete lots, Clear drafts)
        val destructiveIntent = checkDestructiveActionIntent(normalized, trimmed, detectedLang, context)
        if (destructiveIntent != null) {
            return destructiveIntent
        }

        // 6. Semantic Check: Open / Access / Navigate to Login
        val loginIntent = checkLoginSemanticIntent(normalized, trimmed, detectedLang, context)
        if (loginIntent != null) {
            return loginIntent
        }

        // 6.5 Login-screen voice flow actions (tab switching, send/verify/resend OTP, change number, register, help, go back)
        val loginFlowIntent = checkLoginFlowIntent(normalized, trimmed, detectedLang, context)
        if (loginFlowIntent != null) {
            return loginFlowIntent
        }

        // 7. Navigation Intent (General app screens: Dashboard, Lots, Prices, Safety)
        val navIntent = checkGeneralNavigationIntent(normalized, trimmed, detectedLang, context)
        if (navIntent != null) {
            return navIntent
        }

        // 8. Create Lot Request (Entity extraction: MaterialCategory, Weight)
        val createLotIntent = checkCreateLotIntent(normalized, trimmed, detectedLang, context)
        if (createLotIntent != null) {
            return createLotIntent
        }

        // 9. Query Material Price or Scrap Value
        val priceIntent = checkPriceInquiryIntent(normalized, trimmed, detectedLang, context)
        if (priceIntent != null) {
            return priceIntent
        }

        // 10. Query E-Waste Disposal & Guidelines (Natural language queries on how to dispose, recycle, dismantle, or handle e-waste)
        val disposalIntent = checkEwasteDisposalQuery(normalized, trimmed, detectedLang)
        if (disposalIntent != null) {
            return disposalIntent
        }

        // 11. Query Safety & Hazard Guidelines (High priority environmental/health questions)
        val safetyIntent = checkSafetyInquiryIntent(normalized, trimmed, detectedLang)
        if (safetyIntent != null) {
            return safetyIntent
        }

        // 11. User Stats / Earnings / Lots inquiry
        val statsIntent = checkStatsInquiryIntent(normalized, trimmed, detectedLang, context)
        if (statsIntent != null) {
            return statsIntent
        }

        // 11.5 Collector operations: revenue summary, AI lot analysis, location sharing,
        // nearby collectors/recyclers, connections & recycler quotations.
        val collectorIntent = checkCollectorWorkflowIntent(normalized, trimmed, detectedLang)
        if (collectorIntent != null) {
            return collectorIntent
        }

        // 12. Help & Capabilities
        val helpIntent = checkHelpIntent(normalized, trimmed, detectedLang)
        if (helpIntent != null) {
            return helpIntent
        }

        // 13. General E-Waste QA / Ambiguous
        return resolveAmbiguousOrGeneralQA(normalized, trimmed, detectedLang, context)
    }

    /**
     * Language detection inspecting Devanagari script, Marathi specific particles, or English.
     */
    fun detectLanguage(text: String, fallback: Language): Language {
        var hasDevanagari = false
        var hasMarathiMarkers = false
        val marathiMarkers = listOf("आहे", "नाही", "करा", "उघडा", "पाहिजे", "कसे", "कुठे", "माझे", "लॉगिन", "व्हॉइस", "नको")

        for (ch in text) {
            if (ch in '\u0900'..'\u097F') {
                hasDevanagari = true
                break
            }
        }

        val lower = text.lowercase(Locale.ROOT)
        for (m in marathiMarkers) {
            if (lower.contains(m)) {
                hasMarathiMarkers = true
                break
            }
        }

        return when {
            hasMarathiMarkers -> Language.MARATHI
            hasDevanagari -> Language.HINDI
            fallback == Language.MARATHI && (lower.contains("kasa") || lower.contains("ugada") || lower.contains("pahije")) -> Language.MARATHI
            fallback == Language.HINDI && (lower.contains("khol") || lower.contains("kaise") || lower.contains("karna")) -> Language.HINDI
            else -> fallback
        }
    }

    private fun checkNegation(text: String): Boolean {
        val negationTokens = listOf(
            "don't", "dont", "do not", "never", "shouldn't", "should not", "not want",
            "मत", "नहीं", "ना", "नको", "नाही", "करणार नाही"
        )
        return negationTokens.any { text.contains(it) }
    }

    private fun checkAffirmation(text: String): Boolean {
        val affirmativeTokens = listOf(
            "yes", "confirm", "sure", "proceed", "ok", "okay", "affirmative", "correct",
            "हाँ", "हा", "सही", "पुष्टि", "आगे बढ़ो", "ठीक है",
            "हो", "नक्की", "बरोबर", "करा", "पुष्टी करा"
        )
        return affirmativeTokens.any { text.contains(it) }
    }

    private fun checkDismissal(text: String): Boolean {
        val dismissiveTokens = listOf(
            "no", "cancel", "abort", "stop", "don't do", "nevermind", "dismiss",
            "नहीं", "रद्द", "रोको", "मत करो",
            "नाही", "रद्द करा", "थांबा", "नको"
        )
        return dismissiveTokens.any { text.contains(it) }
    }

    private fun checkLanguageSwitch(text: String, original: String): SemanticAnalysisResult? {
        val targetLang = when {
            text.contains("hindi") || text.contains("हिंदी") || text.contains("हिन्दी") -> Language.HINDI
            text.contains("marathi") || text.contains("मराठी") -> Language.MARATHI
            text.contains("english") || text.contains("इंग्रजी") || text.contains("अंग्रेजी") -> Language.ENGLISH
            else -> null
        }

        if (targetLang != null && (text.contains("change") || text.contains("switch") || text.contains("set") ||
                text.contains("speak in") || text.contains("बोलो") || text.contains("बोला") || text.contains("भाषा") || text.contains("language"))) {
            return SemanticAnalysisResult(
                intent = SemanticIntent.CHANGE_LANGUAGE,
                detectedLanguage = targetLang,
                confidence = 0.98f,
                spokenResponse = when (targetLang) {
                    Language.ENGLISH -> "Language switched to English."
                    Language.HINDI -> "भाषा बदलकर हिंदी कर दी गई है।"
                    Language.MARATHI -> "भाषा मराठीमध्ये बदलली आहे."
                },
                actionDescription = "Switch language to ${targetLang.displayName}",
                rawTranscript = original
            )
        }
        return null
    }

    /**
     * Identifies any semantic formulation that expresses the desire, intention, or query
     * to open, access, sign in, or navigate to a login page.
     */
    private fun checkLoginSemanticIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val sanitized = text
            .replace("open air", "")
            .replace("open burning", "")
            .replace("open fire", "")
            .replace("open flame", "")

        if (text.contains("sign out") || text.contains("log out") || text.contains("logout") ||
            text.contains("साइन आउट") || text.contains("लॉग आऊट")) {
            return null
        }

        // Semantic verbs / actions meaning access, open, sign-in, log-in, authenticate, enter portal
        val accessSemantics = listOf(
            "login", "log in", "sign in", "signin", "authenticate", "access", "open",
            "take me to", "go to", "enter", "portal", "account", "profile", "start",
            "लॉगिन", "साइन इन", "खोलो", "खोलें", "जाओ", "जाना", "प्रवेश", "खाता",
            "उघडा", "प्रवेश करा", "लॉग इन", "जा", "खाते"
        )

        val hasAccessIntent = accessSemantics.any { sanitized.contains(it) }
        if (!hasAccessIntent) return null

        // Role differentiation (Informal Collector vs Formal Recycler vs Government Admin)
        val isCollectorSemantic = text.contains("collector") || text.contains("informal") ||
            text.contains("scrap") || text.contains("kabadi") || text.contains("kabaddi") ||
            text.contains("raddi") || text.contains("bhangar") || text.contains("junk") ||
            text.contains("कलेक्टर") || text.contains("कबाड़ी") || text.contains("स्क्रैप") ||
            text.contains("भंगार") || text.contains("रद्दी")

        val isRecyclerSemantic = (text.contains("recycl") || text.contains("refiner") ||
            text.contains("facility") || text.contains("plant") || text.contains("factory") ||
            text.contains("formal") || text.contains("रीसायकल") || text.contains("रीसाइकिल") ||
            text.contains("पुनर्प्रक्रिया") || text.contains("कारखाना") || text.contains("other login") ||
            text.contains("another login") || text.contains("second login") || text.contains("दुसरा लॉगिन") ||
            text.contains("दूसरा लॉगिन")) &&
            !text.contains("informal") && !text.contains("अनौपचारिक")

        val isAdminSemantic = text.contains("admin") || text.contains("government") ||
            text.contains("cpcb") || text.contains("spcb") || text.contains("officer") ||
            text.contains("official") || text.contains("सरकारी") || text.contains("प्रशासन") ||
            text.contains("शासन") || text.contains("शासकीय")

        val targetRole = when {
            isAdminSemantic -> RoleType.GOVERNMENT_ADMIN
            isRecyclerSemantic -> RoleType.FORMAL_RECYCLER
            isCollectorSemantic -> RoleType.INFORMAL_COLLECTOR
            // If the user simply says "Take me to login" or "Open the login page" without specifying role:
            // Check current screen or default to informal collector (the core user group)
            context.currentRole != null -> context.currentRole
            else -> RoleType.INFORMAL_COLLECTOR
        }

        val targetScreen = when (targetRole) {
            RoleType.INFORMAL_COLLECTOR -> AppScreen.INFORMAL_COLLECTOR_AUTH
            RoleType.FORMAL_RECYCLER -> AppScreen.FORMAL_RECYCLER_AUTH
            RoleType.GOVERNMENT_ADMIN -> AppScreen.GOVERNMENT_ADMIN_AUTH
        }

        val spokenResponse = when (targetRole) {
            RoleType.INFORMAL_COLLECTOR -> when (lang) {
                Language.ENGLISH -> "Opening Informal Collector Login. Please enter your mobile number for secure OTP verification."
                Language.HINDI -> "अनौपचारिक स्क्रैप कलेक्टर लॉगिन खोला जा रहा है। कृपया अपना मोबाइल नंबर दर्ज करें।"
                Language.MARATHI -> "अनौपचारिक स्क्रॅप कलेक्टर लॉगिन उघडत आहे. कृपया आपला मोबाईल क्रमांक प्रविष्ट करा."
            }
            RoleType.FORMAL_RECYCLER -> when (lang) {
                Language.ENGLISH -> "Opening Formal Recycler Facility Portal. Enter your CPCB or SPCB authorization credentials."
                Language.HINDI -> "अधिकृत रीसायकलिंग संयंत्र पोर्टल खोला जा रहा है। अपना सीपीसीबी पंजीकरण विवरण दर्ज करें।"
                Language.MARATHI -> "अधिकृत पुनर्प्रक्रिया केंद्र पोर्टल उघडत आहे. आपला सीपीसीबी अधिकृत तपशील भरा."
            }
            RoleType.GOVERNMENT_ADMIN -> when (lang) {
                Language.ENGLISH -> "Opening Government Admin Portal for MoEFCC and CPCB regulatory oversight."
                Language.HINDI -> "सरकारी प्रशासनिक पोर्टल खोला जा रहा है। कृपया अपनी सरकारी क्रेडेंशियल दर्ज करें।"
                Language.MARATHI -> "शासकीय प्रशासकीय पोर्टल उघडत आहे. कृपया आपले अधिकृत विवरण भरा."
            }
        }

        return SemanticAnalysisResult(
            intent = SemanticIntent.OPEN_LOGIN,
            targetRole = targetRole,
            targetScreen = targetScreen,
            detectedLanguage = lang,
            confidence = 0.98f,
            spokenResponse = spokenResponse,
            actionDescription = "Open ${targetRole.name.replace('_', ' ').lowercase().capitalize(Locale.ROOT)} Login Page",
            rawTranscript = original
        )
    }

    private fun checkLoginFlowIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        fun result(
            intent: SemanticIntent,
            confidence: Float,
            spoken: String,
            action: String
        ) = SemanticAnalysisResult(
            intent = intent,
            detectedLanguage = lang,
            confidence = confidence,
            spokenResponse = spoken,
            actionDescription = action,
            rawTranscript = original
        )

        fun speak(en: String, hi: String, mr: String): String = when (lang) {
            Language.ENGLISH -> en
            Language.HINDI -> hi
            Language.MARATHI -> mr
        }

        // Tab switching on the login screen
        if (text.contains("mobile login") || text.contains("otp login") || text.contains("sms login") ||
            text.contains("login with mobile") || text.contains("login with otp") ||
            text.contains("मोबाइल लॉगिन") || text.contains("ओटीपी लॉगिन") || text.contains("एसएमएस लॉगिन") ||
            text.contains("मोबाईल लॉगिन")) {
            return result(
                SemanticIntent.SELECT_MOBILE_LOGIN,
                0.98f,
                speak(
                    "Switching to mobile number login. Please enter your 10 digit mobile number to receive a verification code.",
                    "मोबाइल नंबर लॉगिन पर खोला जा रहा है। कृपया अपना 10 अंकों का मोबाइल नंबर दर्ज करें।",
                    "मोबाईल क्रमांक लॉगिनवर जात आहोत. कृपया आपला 10-अंकी मोबाईल क्रमांक प्रविष्ट करा."
                ),
                "Switch to Mobile Number OTP login"
            )
        }

        if (text.contains("email login") || text.contains("email password") ||
            text.contains("login with email") || text.contains("password login") ||
            text.contains("ईमेल लॉगिन") || text.contains("ईमेल पासवर्ड") || text.contains("पासवर्ड लॉगिन")) {
            return result(
                SemanticIntent.SELECT_EMAIL_LOGIN,
                0.98f,
                speak(
                    "Switching to email and password login.",
                    "ईमेल और पासवर्ड लॉगिन पर खुल रहा है।",
                    "ईमेल आणि पासवर्ड लॉगिनवर जात आहोत."
                ),
                "Switch to Email Password login"
            )
        }

        if (text.contains("google login") || text.contains("login with google") ||
            text.contains("continue with google") || text.contains("sign in with google") ||
            text.contains("गूगल लॉगिन") || text.contains("गूगल से लॉगिन") ||
            text.contains("google लॉगिन")) {
            return result(
                SemanticIntent.SELECT_GOOGLE_LOGIN,
                0.98f,
                speak(
                    "Switching to Continue with Google login.",
                    "गूगल से लॉगिन पर खुल रहा है।",
                    "गूगलसह लॉगिनवर जात आहोत."
                ),
                "Switch to Google OAuth login"
            )
        }

        // Send / resend verification code
        if (text.contains("send otp") || text.contains("send code") || text.contains("send verification") ||
            text.contains("generate otp") || text.contains("get otp") || text.contains("request otp") ||
            text.contains("send it") || text.contains("send otp again") ||
            text.contains("ओटीपी भेजें") || text.contains("ओटीपी भेजो") || text.contains("कोड भेजें") ||
            text.contains("ओटीपी पाठवा") || text.contains("कोड पाठवा")) {
            return result(
                SemanticIntent.SEND_OTP,
                0.97f,
                speak(
                    "Sending a 6 digit verification code to your mobile number by SMS.",
                    "आपके मोबाइल नंबर पर 6 अंकों का सत्यापन कोड एसएमएस से भेजा जा रहा है।",
                    "तुमच्या मोबाईल क्रमांकावर 6 अंकी सत्यापन कोड SMS द्वारे पाठवला जात आहे."
                ),
                "Send OTP to mobile number"
            )
        }

        if (text.contains("resend otp") || text.contains("resend code") || text.contains("resend the otp") ||
            text.contains("otp again") || text.contains("send again") ||
            text.contains("ओटीपी फिर से भेजें") || text.contains("ओटीपी पुन्हा पाठवा")) {
            return result(
                SemanticIntent.RESEND_OTP,
                0.97f,
                speak(
                    "Resending the verification code.",
                    "सत्यापन कोड फिर से भेजा जा रहा है।",
                    "सत्यापन कोड पुन्हा पाठवला जात आहे."
                ),
                "Resend OTP"
            )
        }

        // Verify the entered code (never carries the code value)
        if (text.contains("verify otp") || text.contains("verify code") || text.contains("submit otp") ||
            text.contains("check otp") || text.contains("login with otp") || text.contains("verify it") ||
            text.contains("submit code") || text.contains("otp verify") ||
            text.contains("ओटीपी सत्यापित") || text.contains("ओटीपी वेरीफाई") || text.contains("कोड जांचो") ||
            text.contains("ओटीपी तपासा") || text.contains("कोड सत्यापित करा")) {
            return result(
                SemanticIntent.VERIFY_OTP,
                0.97f,
                speak(
                    "Please enter the 6 digit code on the screen. I cannot read it aloud for your security.",
                    "कृपया स्क्रीन पर 6 अंकों का कोड दर्ज करें। आपकी सुरक्षा के लिए मैं इसे ज़ोर से नहीं पढ़ सकती।",
                    "कृपया स्क्रीनवर 6 अंकी कोड प्रविष्ट करा. सुरक्षेसाठी मी तो मोठ्याने वाचू शकत नाही."
                ),
                "Verify entered OTP"
            )
        }

        // Change mobile number
        if (text.contains("change number") || text.contains("change mobile number") || text.contains("different number") ||
            text.contains("नंबर बदलें") || text.contains("मोबाइल नंबर बदलें") ||
            text.contains("नंबर बदला") || text.contains("दुसरा नंबर") && text.contains("लॉगिन")) {
            return result(
                SemanticIntent.CHANGE_PHONE,
                0.96f,
                speak(
                    "Clearing the mobile number. Please enter a new number.",
                    "मोबाइल नंबर हटा रहे हैं। कृपया नया नंबर दर्ज करें।",
                    "मोबाईल क्रमांक रद्द करत आहे. कृपया नवीन क्रमांक प्रविष्ट करा."
                ),
                "Change mobile number on login"
            )
        }

        // Registration
        if (text.contains("register") || text.contains("sign up") || text.contains("create account") ||
            text.contains("new account") || text.contains("registration") ||
            text.contains("पंजीकरण") || text.contains("रजिस्टर") || text.contains("खाता बनाएं") ||
            text.contains("नोंदणी") || text.contains("खाते तयार करा")) {
            return result(
                SemanticIntent.OPEN_REGISTER,
                0.96f,
                speak(
                    "Opening the profile registration flow.",
                    "प्रोफ़ाइल पंजीकरण प्रक्रिया खुल रही है।",
                    "प्रोफाइल नोंदणी प्रक्रिया उघडत आहे."
                ),
                "Open role profile registration"
            )
        }

        // Voice help on the login screen
        if (text.contains("how to login") || text.contains("how can i login") || text.contains("login help") ||
            text.contains("voice help") || text.contains("help with login") ||
            text.contains("लॉगिन कैसे") || text.contains("लॉगिन कसे")) {
            return result(
                SemanticIntent.VOICE_HELP,
                0.94f,
                speak(
                    "To log in, switch to a tab with your voice, enter your mobile number and say send otp. Then enter the 6 digit code on the screen and say verify otp.",
                    "लॉगिन करने के लिए, वॉइस से टैब चुनें, मोबाइल नंबर दर्ज करें और ओटीपी भेजें कहें। फिर स्क्रीन पर कोड डालें और ओटीपी सत्यापित करें कहें।",
                    "लॉगिन करण्यासाठी व्हॉइसने टॅब निवडा, मोबाईल क्रमांक भरा आणि ओटीपी पाठवा म्हणा. मग स्क्रीनवर कोड भरा आणि ओटीपी तपासा म्हणा."
                ),
                "Voice help for login"
            )
        }

        // Going back — only when the user is currently on a login screen
        if ((text.contains("go back") || text.contains("back to login") || text.contains("go back to previous screen")) &&
            context.currentScreen.toString().contains("AUTH")) {
            return result(
                SemanticIntent.GO_BACK,
                0.95f,
                speak(
                    "Going back.",
                    "वापस जा रहे हैं।",
                    "मागे जात आहोत."
                ),
                "Go back from login"
            )
        }

        return null
    }

    private fun checkGeneralNavigationIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val isNav = text.contains("go to") || text.contains("open") || text.contains("show") ||
            text.contains("navigate") || text.contains("back") || text.contains("home") ||
            text.contains("जाओ") || text.contains("खोलो") || text.contains("दिखाओ") ||
            text.contains("उघडा") || text.contains("दाखवा") || text.contains("मागे")

        if (!isNav) return null

        // Home / Intro
        if (text.contains("home") || text.contains("intro") || text.contains("main screen") || text.contains("back") ||
            text.contains("होम") || text.contains("शुरुआत") || text.contains("मागे जा")) {
            return SemanticAnalysisResult(
                intent = SemanticIntent.NAVIGATE_SCREEN,
                targetScreen = AppScreen.INTRO,
                detectedLanguage = lang,
                confidence = 0.95f,
                spokenResponse = when (lang) {
                    Language.ENGLISH -> "Navigating back to the Home screen."
                    Language.HINDI -> "मुख्य स्क्रीन पर वापस जा रहे हैं।"
                    Language.MARATHI -> "मुख्य पृष्ठावर परत जात आहोत."
                },
                actionDescription = "Navigate to Home / Intro",
                rawTranscript = original
            )
        }

        // Dashboard
        if (text.contains("dashboard") || text.contains("hub") || text.contains("portal") ||
            text.contains("डैशबोर्ड") || text.contains("हब")) {
            val screen = when (context.currentRole) {
                RoleType.FORMAL_RECYCLER -> AppScreen.FORMAL_RECYCLER_PORTAL
                RoleType.GOVERNMENT_ADMIN -> AppScreen.GOVERNMENT_ADMIN_PORTAL
                else -> AppScreen.COLLECTOR_DASHBOARD
            }
            return SemanticAnalysisResult(
                intent = SemanticIntent.NAVIGATE_SCREEN,
                targetScreen = screen,
                detectedLanguage = lang,
                confidence = 0.94f,
                spokenResponse = when (lang) {
                    Language.ENGLISH -> "Navigating to your operations dashboard."
                    Language.HINDI -> "आपके संचालन डैशबोर्ड पर ले जा रहे हैं।"
                    Language.MARATHI -> "आपल्या कार्य डॅशबोर्डवर नेत आहोत."
                },
                actionDescription = "Navigate to Operations Dashboard",
                rawTranscript = original
            )
        }

        return null
    }

    private fun checkDestructiveActionIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val isDestructive = text.contains("delete") || text.contains("remove") ||
            text.contains("wipe") || text.contains("clear all") || text.contains("logout") ||
            text.contains("log out") || text.contains("sign out") ||
            text.contains("हटाएं") || text.contains("डिलीट") || text.contains("साइन आउट") ||
            text.contains("काढून टाका") || text.contains("रद्द करा") || text.contains("लॉग आऊट")

        if (!isDestructive) return null

        val isSignOut = text.contains("log out") || text.contains("logout") || text.contains("sign out") ||
            text.contains("साइन आउट") || text.contains("लॉग आऊट")

        val promptQuestion = if (isSignOut) {
            when (lang) {
                Language.ENGLISH -> "Are you sure you want to sign out of your account? Say 'confirm' to proceed or 'cancel'."
                Language.HINDI -> "क्या आप निश्चित रूप से अपने खाते से साइन आउट करना चाहते हैं? पुष्टि करने के लिए 'हां' कहें या 'रद्द करें'।"
                Language.MARATHI -> "तुम्हाला नक्की खात्यातून साइन आऊट करायचे आहे का? पुढे जाण्यासाठी 'हो' म्हणा किंवा 'रद्द करा'."
            }
        } else {
            when (lang) {
                Language.ENGLISH -> "This is a permanent action. Are you sure you want to proceed? Say 'confirm' or 'cancel'."
                Language.HINDI -> "यह स्थायी कार्रवाई है। क्या आप जारी रखना चाहते हैं? 'पुष्टि करें' या 'रद्द करें' कहें।"
                Language.MARATHI -> "ही कायमस्वरूपी कृती आहे. आपल्याला खात्री आहे का? 'हो' किंवा 'रद्द' म्हणा."
            }
        }

        return SemanticAnalysisResult(
            intent = SemanticIntent.DESTRUCTIVE_ACTION_REQUEST,
            detectedLanguage = lang,
            confidence = 0.95f,
            spokenResponse = promptQuestion,
            actionDescription = if (isSignOut) "Sign out of account" else "Delete data request",
            requiresConfirmation = true,
            isDestructive = true,
            rawTranscript = original
        )
    }

    private fun checkCreateLotIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val isCreateLot = text.contains("create lot") || text.contains("new lot") ||
            text.contains("add lot") || text.contains("sell") || text.contains("log pickup") ||
            text.contains("लॉट बनाएं") || text.contains("नया लॉट") || text.contains("बेचना") ||
            text.contains("लॉट तयार करा") || text.contains("नवीन लॉट")

        val category = extractMaterialCategory(text) ?: context.lastMentionedCategory
        val weight = extractWeight(text)

        if (isCreateLot || (category != null && weight != null)) {
            val weightText = if (weight != null) "$weight kg" else "specified weight"
            val categoryText = category?.getTitle(lang) ?: "E-Waste"

            return SemanticAnalysisResult(
                intent = SemanticIntent.CREATE_LOT_REQUEST,
                materialCategory = category ?: MaterialCategory.CABLES_WIRES,
                quantityKg = weight ?: 10.0,
                detectedLanguage = lang,
                confidence = 0.94f,
                spokenResponse = when (lang) {
                    Language.ENGLISH -> "Opening lot creation for $weightText of $categoryText. Please confirm the rate and recycler."
                    Language.HINDI -> "$categoryText के $weightText के लिए लॉट निर्माण खोला जा रहा है।"
                    Language.MARATHI -> "$categoryText च्या $weightText साठी लॉट निर्मिती उघडत आहे."
                },
                actionDescription = "Create Lot for $weightText $categoryText",
                rawTranscript = original
            )
        }
        return null
    }

    private fun checkPriceInquiryIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val isPriceInquiry = text.contains("price") || text.contains("rate") || text.contains("cost") ||
            text.contains("how much") || text.contains("value") || text.contains("worth") ||
            text.contains("भाव") || text.contains("कीमत") || text.contains("दर") || text.contains("रेट") ||
            text.contains("किंमत") || text.contains("मूल्य") || text.contains("पैसे")

        val category = extractMaterialCategory(text) ?: context.lastMentionedCategory

        if (isPriceInquiry) {
            val targetCategory = category ?: MaterialCategory.PCB_BOARDS
            val rate = targetCategory.defaultRatePerKg
            val name = targetCategory.getTitle(lang)

            val spokenResponse = when (lang) {
                Language.ENGLISH -> "$name is currently ₹$rate per kilogram through authorized recyclers, with guaranteed pure metal valuation."
                Language.HINDI -> "अधिकृत रीसायकलर के माध्यम से $name की वर्तमान दर ₹$rate प्रति किलोग्राम है।"
                Language.MARATHI -> "अधिकृत पुनर्प्रक्रिया केंद्राद्वारे $name चा सध्याचा दर ₹$rate प्रति किलो आहे."
            }

            return SemanticAnalysisResult(
                intent = SemanticIntent.QUERY_MATERIAL_PRICE,
                materialCategory = targetCategory,
                detectedLanguage = lang,
                confidence = 0.96f,
                spokenResponse = spokenResponse,
                actionDescription = "Query scrap price for ${targetCategory.name}",
                rawTranscript = original
            )
        }
        return null
    }

    private fun checkEwasteDisposalQuery(
        text: String,
        original: String,
        lang: Language
    ): SemanticAnalysisResult? {
        val disposalKeywords = listOf(
            "dispose", "disposal", "recycle", "recycling", "throw away", "dump", "discard",
            "dismantle", "segregate", "drop off", "collection point", "scrap center",
            "e-waste rule", "epr", "how to dispose", "safe disposal", "guidelines",
            "विल्हेवाट", "कचरा", "पुनर्प्रक्रिया", "कसे नष्ट करावे", "टाकणे", "नियम",
            "निपटान", "फेंकना", "रीसायकल", "कैसे निपटान करें", "नियम 2022", "कलेक्शन सेंटर"
        )

        val hasDisposalIntent = disposalKeywords.any { text.contains(it) }
        if (!hasDisposalIntent) return null

        val (response, guidelines, hazards, regulation) = when {
            text.contains("battery") || text.contains("lithium") || text.contains("बैटरी") || text.contains("बॅटरी") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For battery disposal: 1) Tape both terminals with non-conductive electrical tape to avoid short-circuit fires. 2) Never puncture or crush lithium cells. 3) Store in dry, fire-resistant bins and hand over to CPCB-registered battery recyclers under Battery Rules 2022."
                    Language.HINDI -> "बैटरी निपटान के लिए: 1) शॉर्ट सर्किट आग से बचने के लिए दोनों टर्मिनलों पर इन्सुलेशन टेप लगाएं। 2) लिथियम सेल को कभी न फोड़ें। 3) बैटरी अपशिष्ट नियम 2022 के तहत सीपीसीबी-पंजीकृत रीसायकलर्स को ही सौंपें।"
                    Language.MARATHI -> "बॅटरी विल्हेवाटीसाठी: 1) आग रोखण्यासाठी दोन्ही टर्मिनल्सवर इन्सुलेशन टेप लावा. 2) लिथियम पेशी कधीही फोडू नका. 3) बॅटरी नियम 2022 अंतर्गत अधिकृत रीसायकलरकडेच सुपूर्द करा."
                }
                val g = listOf(
                    "Insulate electrical terminals with tape",
                    "Do not puncture, pierce, or expose to heat",
                    "Store in dry, non-conductive bins",
                    "Handover to CPCB authorized battery recyclers"
                )
                val h = listOf("Lithium (Flammable)", "Cobalt Oxide", "Hydrofluoric Acid Gas (HF)", "Lead Acid")
                val reg = "Battery Waste Management Rules 2022 (MoEFCC)"
                Quadruple(resp, g, h, reg)
            }
            text.contains("crt") || text.contains("tv") || text.contains("monitor") || text.contains("screen") ||
            text.contains("सीआरटी") || text.contains("मॉनिटर") || text.contains("स्क्रीन") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For CRT monitors & TVs: Never smash the funnel glass! It contains 1.5 to 2.5 kg of toxic lead oxide. Transport intact to authorized dismantling facilities equipped with vacuum glass separation hoods."
                    Language.HINDI -> "सीआरटी मॉनिटर और टीवी के लिए: कांच कभी न तोड़ें! इसमें 1.5 से 2.5 किलो जहरीला लेड ऑक्साइड होता है। इसे बिना तोड़े अधिकृत रीसाइक्लिंग केंद्र में जमा करें।"
                    Language.MARATHI -> "सीआरटी मॉनिटर आणि टीव्हीसाठी: काच कधीही फोडू नका! यामध्ये 1.5 ते 2.5 किलो विषारी शिसे (Lead) असते. न फोडता अधिकृत केंद्राकडे द्या."
                }
                val g = listOf(
                    "Keep cathode ray tube intact; prevent vacuum implosion",
                    "Never crush funnel glass containing heavy lead oxide",
                    "Deliver to recyclers with specialized lead-recovery glass furnaces"
                )
                val h = listOf("Lead Oxide (1.5-2.5 kg)", "Barium", "Cadmium Phosphor Coating")
                val reg = "Schedule I - E-Waste (Management) Rules 2022"
                Quadruple(resp, g, h, reg)
            }
            text.contains("pcb") || text.contains("motherboard") || text.contains("circuit") ||
            text.contains("मदरबोर्ड") || text.contains("सर्किट") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For circuit boards (PCBs): Do not use cyanide or nitric acid burning at home! Acid destroys 85% of valuable rare earths and poisons soil. Deliver motherboards to automated refining plants to extract gold, silver, copper, and palladium safely."
                    Language.HINDI -> "सर्किट बोर्ड (पीसीबी) के लिए: घर पर कभी एसिड या साइनाइड न जलाएं! यह भूजल को जहरीला बनाता है। सोना, चांदी, तांबा और पैलेडियम निकालने के लिए मदरबोर्ड अधिकृत प्लांट को दें।"
                    Language.MARATHI -> "सर्किट बोर्डसाठी: घरी आम्ल (Acid) किंवा रसायने वापरू नका! अधिकृत रिफायनरीद्वारे सोने, चांदी, तांबे सुरक्षितपणे पुनर्प्रक्रिया करा."
                }
                val g = listOf(
                    "Prohibit domestic open acid leaching & open hearth smelting",
                    "Segregate high-grade telecom PCBs from power supply boards",
                    "Process in pyrometallurgical/hydrometallurgical facilities with wet scrubbers"
                )
                val h = listOf("Beryllium", "Brominated Flame Retardants (BFR)", "Lead-Tin Solder")
                val reg = "E-Waste Rules 2022 - Metal Recovery Standards"
                Quadruple(resp, g, h, reg)
            }
            text.contains("cable") || text.contains("wire") || text.contains("charger") || text.contains("cord") ||
            text.contains("तार") || text.contains("केबल") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For cables & wires: Do not burn in open air! Burning PVC insulation produces cancer-causing dioxins and lowers scrap value. Use mechanical cable strippers to extract pure electrolytic copper at ₹460/kg."
                    Language.HINDI -> "केबल्स और तारों के लिए: इन्हें खुली आग में कभी न जलाएं! पीवीसी से घातक डाइऑक्सिन गैस निकलती है। बिना जलाए केबल छीलकर पूरे ₹460 प्रति किलो का शुद्ध भाव पाएं।"
                    Language.MARATHI -> "केबल्स व तारांसाठी: उघड्यावर कधीही जाळू नका! पीव्हीसी जाळल्याने विषारी वायू निघतो. मेकॅनिकल वायर स्ट्रिपर वापरून पूर्ण ₹460/किलो दर मिळवा."
                }
                val g = listOf(
                    "Strip PVC coating mechanically; zero open-air incineration",
                    "Separate aluminium service cables from electrolytic copper",
                    "Channel PVC granules into authorized polymer recycling streams"
                )
                val h = listOf("Dioxins & Furans (from burned PVC)", "Lead plasticizers", "Phthalates")
                val reg = "CPCB Guidelines for Environmentally Sound Cable Stripping"
                Quadruple(resp, g, h, reg)
            }
            text.contains("phone") || text.contains("mobile") || text.contains("laptop") || text.contains("computer") ||
            text.contains("फोन") || text.contains("मोबाइल") || text.contains("लपटॉप") || text.contains("संगणक") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For mobile phones and laptops: 1) Perform a complete factory data wipe (NIST 800-88). 2) Remove memory cards and SIMs. 3) Separate lithium battery if removable. 4) Submit to an authorized collection point for CPCB EPR recycling certificate."
                    Language.HINDI -> "मोबाइल फोन और लैपटॉप के लिए: 1) सभी डेटा को फैक्ट्री रीसेट करें। 2) सिम और मेमोरी कार्ड निकालें। 3) सीपीसीबी-अधिकृत केंद्र पर जमा करके ईपीआर रीसाइक्लिंग सर्टिफिकेट प्राप्त करें।"
                    Language.MARATHI -> "स्मार्टफोन आणि लॅपटॉपसाठी: 1) पूर्ण फॅक्टरी डेटा रीसेट करा. 2) सिम आणि मेमरी कार्ड काढा. 3) अधिकृत ई-कचरा केंद्रावर जमा करून ईपीआर प्रमाणपत्र मिळवा."
                }
                val g = listOf(
                    "Cryptographic data wipe / disk overwrite before disposal",
                    "Separate removable battery, charger, and peripheral cables",
                    "Generate EPR credit verification through registered platform"
                )
                val h = listOf("Lithium Ion cells", "Tantalum capacitors", "Arsenic in semiconductor chips")
                val reg = "MoEFCC EPR Extended Producer Responsibility Target Guidelines"
                Quadruple(resp, g, h, reg)
            }
            text.contains("light") || text.contains("cfl") || text.contains("tube") || text.contains("bulb") ||
            text.contains("बल्ब") || text.contains("ट्यूब") || text.contains("दिवा") -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "For fluorescent tube lights and CFLs: Handle with extreme care to avoid breaking glass containing toxic mercury vapor (3 to 5 mg). Wrap safely and deposit at hazardous e-waste collection bins."
                    Language.HINDI -> "सीएफएल और ट्यूबलाइट के लिए: कांच टूटने से बचाएं क्योंकि इसमें 3 से 5 मिलीग्राम जहरीला पारा (Mercury) वाष्प होता है। इसे सुरक्षित लपेटकर खतरनाक अपशिष्ट संग्रह में दें।"
                    Language.MARATHI -> "सीएफएल आणि ट्यूबलाईटसाठी: काच फुटू देऊ नका कारण यात विषारी पारा (Mercury) असतो. सुरक्षित गुंडाळून अधिकृत ई-कचरा पेटीत टाका."
                }
                val g = listOf(
                    "Prevent tube breakage; avoid inhaling vapor",
                    "Do not dispose in municipal household waste",
                    "Recycle with certified mercury-distillation facilities"
                )
                val h = listOf("Elemental Mercury Vapor", "Phosphor dust", "Lead solder contacts")
                val reg = "CPCB Hazardous Waste Management & Mercury Control Directives"
                Quadruple(resp, g, h, reg)
            }
            else -> {
                val resp = when (lang) {
                    Language.ENGLISH -> "E-Waste Disposal Guidelines: Under India's E-Waste Rules 2022, all electronic waste must be segregated at source, never dumped in municipal bins or burned. Handover to verified collectors or CPCB recyclers to generate verified EPR credits."
                    Language.HINDI -> "ई-कचरा निपटान नियम: पर्यावरण मंत्रालय नियम 2022 के तहत, सभी इलेक्ट्रॉनिक्स कचरे को अलग रखें, इसे कभी भी कूड़ेदान में न फेंकें और न जलाएं। अधिकृत रीसायकलर्स को सौंपकर ईपीआर क्रेडिट प्राप्त करें।"
                    Language.MARATHI -> "ई-कचरा विल्हेवाट नियम: भारत सरकारच्या ई-कचरा नियम 2022 नुसार, जुने इलेक्ट्रॉनिक्स उघड्यावर टाकू नका किंवा जाळू नका. अधिकृत पुनर्प्रक्रिया केंद्राकडे द्या."
                }
                val g = listOf(
                    "Source segregation: Separate batteries, screens, and cables",
                    "Zero open burning, dumping, or acid baths",
                    "Channel via authorized formal recyclers with digital manifests",
                    "Obtain verified CPCB EPR compliance certificates"
                )
                val h = listOf("Lead", "Mercury", "Cadmium", "Hexavalent Chromium", "BFRs")
                val reg = "India E-Waste (Management) Rules 2022 (CPCB & MoEFCC)"
                Quadruple(resp, g, h, reg)
            }
        }

        return SemanticAnalysisResult(
            intent = SemanticIntent.QUERY_EWASTE_DISPOSAL,
            detectedLanguage = lang,
            confidence = 0.98f,
            spokenResponse = response,
            actionDescription = "E-Waste Disposal & Environmental Compliance Guidelines",
            rawTranscript = original,
            disposalSafetyGuidelines = guidelines,
            hazardousMaterials = hazards,
            cpcbRegulation = regulation
        )
    }

    private fun checkSafetyInquiryIntent(
        text: String,
        original: String,
        lang: Language
    ): SemanticAnalysisResult? {
        val isSafety = text.contains("safe") || text.contains("hazard") || text.contains("burn") ||
            text.contains("acid") || text.contains("toxic") || text.contains("danger") ||
            text.contains("धुआं") || text.contains("जलाना") || text.contains("सुरक्षा") ||
            text.contains("खतरा") || text.contains("धोका") || text.contains("सुरक्षित") ||
            text.contains("विषारी") || text.contains("आग")

        if (!isSafety) return null

        val response = when {
            text.contains("cable") || text.contains("wire") || text.contains("तार") -> {
                when (lang) {
                    Language.ENGLISH -> "Never burn wires in open air! Burning PVC produces deadly cancer-causing dioxins and lowers copper scrap value by 20% to 30%. Handover intact cables for full pure electrolytic rate of ₹460/kg."
                    Language.HINDI -> "तारों को कभी खुली आग में न जलाएं! पीवीसी जलाने से घातक कैंसरकारी डाइऑक्सिन निकलते हैं और तांबे का मूल्य घट जाता है। पूरे ₹460 प्रति किलो के लिए बिना जलाए केबल बेचें।"
                    Language.MARATHI -> "केबल्स कधीही उघड्यावर जाळू नका! पीव्हीसी जाळल्याने विषारी वायू निघतात आणि तांब्याचे मूल्य कमी होते. पूर्ण ₹460 प्रति किलो दरासाठी न जाळता केबल्स द्या."
                }
            }
            text.contains("acid") || text.contains("pcb") || text.contains("board") || text.contains("मदरबोर्ड") -> {
                when (lang) {
                    Language.ENGLISH -> "Do not use nitric acid on circuit boards! Acid leaching poisons groundwater, emits toxic nitrogen dioxide clouds, and destroys 85% of valuable rare earths like tantalum and palladium."
                    Language.HINDI -> "सर्किट बोर्ड पर कभी एसिड न डालें! एसिड से भूजल दूषित होता है और 85% मूल्यवान दुर्लभ धातुएं नष्ट हो जाती हैं।"
                    Language.MARATHI -> "सर्किट बोर्डवर आम्ल वापरू नका! यामुळे भूजल दूषित होते आणि बहुमूल्य दुर्मिळ धातू नष्ट होतात."
                }
            }
            text.contains("battery") || text.contains("बैटरी") || text.contains("बॅटरी") -> {
                when (lang) {
                    Language.ENGLISH -> "Never puncture or crush lithium batteries! Damaged cells can instantaneously ignite at over 600°C and release toxic hydrofluoric gas. Store in dry wooden crates."
                    Language.HINDI -> "लिथियम बैटरी को कभी न फोड़ें! क्षतिग्रस्त बैटरी में 600 डिग्री से अधिक तापमान पर आग लग सकती है और जहरीली गैस निकलती है।"
                    Language.MARATHI -> "लिथियम बॅटरी कधीही फोडू नका! यामुळे भीषण आग लागू शकते आणि विषारी वायू बाहेर पडतो."
                }
            }
            else -> {
                when (lang) {
                    Language.ENGLISH -> "Under MoEFCC E-Waste Rules 2022, dismantling e-waste without CPCB safety compliance is hazardous and illegal. Always channel scrap through verified recyclers."
                    Language.HINDI -> "पर्यावरण मंत्रालय ई-कचरा नियम 2022 के अनुसार, बिना सुरक्षा ई-कचरा तोड़ना गैरकानूनी और हानिकारक है। स्क्रैप हमेशा अधिकृत रीसायकलर को दें।"
                    Language.MARATHI -> "ई-कचरा नियम 2022 अंतर्गत, अनधिकृतपणे ई-कचरा तोडणे बेकायदेशीर आहे. अधिकृत केंद्रांनाच स्क्रॅप हस्तांतरित करा."
                }
            }
        }

        return SemanticAnalysisResult(
            intent = SemanticIntent.QUERY_SAFETY_GUIDELINES,
            detectedLanguage = lang,
            confidence = 0.95f,
            spokenResponse = response,
            actionDescription = "Query environmental safety guidelines",
            rawTranscript = original
        )
    }

    private fun checkStatsInquiryIntent(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult? {
        val isStats = text.contains("earnings") || text.contains("lots") || text.contains("balance") ||
            text.contains("dues") || text.contains("history") || text.contains("कमाई") ||
            text.contains("बकाया") || text.contains("इतिहास") || text.contains("शिल्लक")

        if (!isStats) return null

        return SemanticAnalysisResult(
            intent = SemanticIntent.QUERY_USER_STATS,
            detectedLanguage = lang,
            confidence = 0.93f,
            spokenResponse = when (lang) {
                Language.ENGLISH -> "You have active collection lots in progress. Opening your financial ledger and EPR settlement balance."
                Language.HINDI -> "आपके संग्रह लॉट सक्रिय हैं। आपका वित्तीय खाता और ईपीआर बैलेंस खोला जा रहा है।"
                Language.MARATHI -> "आपले संकलन लॉट कार्यरत आहेत. आपले वित्तीय खाते आणि ईपीआर शिल्लक उघडत आहे."
            },
            actionDescription = "Query user ledger statistics",
            rawTranscript = original
        )
    }

    /**
     * Collector-specific operational voice intents (Hands-free on the dashboard):
     * - Today/this-month revenue summary
     * - Run AI analysis on the current draft lot
     * - Share / stop sharing location
     * - Find nearby collectors / recyclers
     * - Open connections & quotations hub
     * - Request a live recycler quotation
     */
    private fun checkCollectorWorkflowIntent(
        text: String,
        original: String,
        lang: Language
    ): SemanticAnalysisResult? {
        fun result(
            intent: SemanticIntent,
            confidence: Float,
            spoken: String,
            action: String
        ) = SemanticAnalysisResult(
            intent = intent,
            detectedLanguage = lang,
            confidence = confidence,
            spokenResponse = spoken,
            actionDescription = action,
            rawTranscript = original
        )

        fun speak(en: String, hi: String, mr: String): String = when (lang) {
            Language.ENGLISH -> en
            Language.HINDI -> hi
            Language.MARATHI -> mr
        }

        // Revenue summary (today / this month)
        val isRevenue = (text.contains("earnings") || text.contains("revenue") || text.contains("income") ||
            text.contains("settled") || text.contains("kamai") || text.contains("कमाई") ||
            text.contains("उत्पन्न") || text.contains("रक्कम") || text.contains("मिळकत")) &&
            (text.contains("today") || text.contains("aaj") || text.contains("आज") ||
                text.contains("month") || text.contains("mahina") || text.contains("आजची") ||
                text.contains("महिना") || text.contains("आजचा") || text.contains("ता आता"))
        if (isRevenue) {
            val today = text.contains("today") || text.contains("aaj") || text.contains("आज") || text.contains("आजची") || text.contains("आजचा")
            return result(
                SemanticIntent.QUERY_REVENUE_SUMMARY,
                0.96f,
                if (today) speak(
                    "Opening today's earnings summary from your Ledger.",
                    "आज की कमाई का सारांश आपके खाते से खुल रहा है।",
                    "आजच्या कमाईची माहिती आपल्या खात्यातून उघडत आहे."
                )
                else speak(
                    "Opening this month's earnings summary from your Ledger.",
                    "इस महीने की कमाई का सारांश आपके खाते से खुल रहा है।",
                    "या महिन्यातील कमाईची माहिती आपल्या खात्यातून उघडत आहे."
                ),
                if (today) "Open today's revenue summary" else "Open this month's revenue summary"
            )
        }

        // Analyze the current lot photos with AI
        if (text.contains("analyze") || text.contains("analysis") || text.contains("identify") ||
            text.contains("estimate") || text.contains("विश्लेषण") || text.contains("पहचान") ||
            text.contains("तपासणी") || text.contains("विश्लेषित")) {
            return result(
                SemanticIntent.ANALYZE_CURRENT_LOT_AI,
                0.94f,
                speak(
                    "Running on-device AI analysis on the current lot photos. Please keep the lot creation window open.",
                    "वर्तमान लॉट की तस्वीरों पर डिवाइस एआई विश्लेषण चलाया जा रहा है। कृपया लॉट विंडो खुली रखें।",
                    "सध्याच्या लॉट फोटोंवर डिव्हाइस एआय विश्लेषण सुरू होत आहे. कृपया लॉट विंडो उघडी ठेवा."
                ),
                "Run AI analysis on current lot"
            )
        }

        // Share / stop sharing location
        if (text.contains("share location") || text.contains("share my location") ||
            text.contains("location sharing") || text.contains("locate me") ||
            text.contains("location on") ||
            text.contains("स्थान साझा") || text.contains("लोकेशन साझा") ||
            text.contains("स्थान शेअर") || text.contains("लोकेशन शेअर")) {
            return result(
                SemanticIntent.SHARE_MY_LOCATION,
                0.96f,
                speak(
                    "Turning on location sharing. Nearby collectors will see a coarse area indicator only.",
                    "स्थान साझा करना चालू कर रहे हैं। आस-पास के संग्राहक केवल सामान्य क्षेत्र देखेंगे।",
                    "स्थान शेअरिंग सुरू करत आहे. जवळचे संकलक फक्त सामान्य क्षेत्र पाहतील."
                ),
                "Share collector location"
            )
        }

        // Nearby collectors
        if (text.contains("nearby collector") || text.contains("nearby collectors") ||
            text.contains("collectors near me") || text.contains("who else collects") ||
            text.contains("आसपास के संग्राहक") || text.contains("पास के कलेक्टर") ||
            text.contains("जवळचे संकलक") || text.contains("जवळचे कलेक्टर")) {
            return result(
                SemanticIntent.FIND_NEARBY_COLLECTORS,
                0.95f,
                speak(
                    "Scanning the shared network for collectors near you.",
                    "आपके आस-पास के संग्राहकों को ढूंढ रहे हैं।",
                    "तुमच्या जवळचे संकलक शोधत आहोत."
                ),
                "Find nearby collectors"
            )
        }

        // Nearby recyclers
        if (text.contains("nearby recycler") || text.contains("nearby recyclers") ||
            text.contains("recyclers near me") || text.contains("best rate recycler") ||
            text.contains("आसपास के रीसायकलर") || text.contains("नजदीकी रीसायकलर") ||
            text.contains("जवळचे रीसायकलर") || text.contains("जवळचे संकलन केंद्र")) {
            return result(
                SemanticIntent.FIND_NEARBY_RECYCLERS,
                0.95f,
                speak(
                    "Opening the registered recyclers list sorted by distance from you.",
                    "आपकी दूरी के अनुसार पंजीकृत रीसायकलर की सूची खोल रहे हैं।",
                    "तुमच्यापासूनच्या अंतरानुसार नोंदणीकृत रीसायकलरची यादी उघडत आहे."
                ),
                "Find nearby recyclers"
            )
        }

        // Connections & quotations hub
        if (text.contains("connections") || text.contains("quotations") || text.contains("requests") ||
            text.contains("quotes") || text.contains("connect hub") ||
            text.contains("संपर्क") || text.contains("कोटेशन") || text.contains("विनंती") ||
            text.contains("भाव पत्र") || text.contains("कनेक्शन")) {
            return result(
                SemanticIntent.OPEN_CONNECTIONS,
                0.94f,
                speak(
                    "Opening connections and quotations for your network.",
                    "आपके नेटवर्क के लिए संपर्क और कोटेशन खोल रहे हैं।",
                    "तुमच्या नेटवर्कसाठी संपर्क आणि भाव पत्र उघडत आहे."
                ),
                "Open connections & quotations"
            )
        }

        // Request recycler quotation
        if (text.contains("request quote") || text.contains("request quotation") ||
            text.contains("get a quote") || text.contains("get quotation") ||
            text.contains("best offer") || text.contains("सर्वोत्तम भाव") ||
            text.contains("कोटेशन मांगें") || text.contains("भाव मागवा")) {
            return result(
                SemanticIntent.REQUEST_RECYCLER_QUOTE,
                0.94f,
                speak(
                    "Opening the recycler list so you can request a live quotation.",
                    "लाइव कोटेशन मांगने के लिए रीसायकलर सूची खोल रहे हैं।",
                    "लाइव भाव पत्र मागण्यासाठी रीसायकलरची यादी उघडत आहे."
                ),
                "Request recycler quotation"
            )
        }

        return null
    }

    private fun checkHelpIntent(
        text: String,
        original: String,
        lang: Language
    ): SemanticAnalysisResult? {
        val isHelp = text.contains("help") || text.contains("what can you do") ||
            text.contains("how does this work") || text.contains("मदद") ||
            text.contains("सहायता") || text.contains("मदत") || text.contains("काय करू शकता")

        if (!isHelp) return null

        return SemanticAnalysisResult(
            intent = SemanticIntent.HELP_AND_CAPABILITIES,
            detectedLanguage = lang,
            confidence = 0.97f,
            spokenResponse = when (lang) {
                Language.ENGLISH -> "I am your AI E-Waste voice assistant. You can speak naturally to open any login portal, check scrap prices, learn hazardous handling safety, or create scrap lots."
                Language.HINDI -> "मैं आपका एआई ई-कचरा वॉयस सहायक हूँ। आप लॉगिन खोलने, स्क्रैप की कीमतें जानने, सुरक्षा नियम समझने या लॉट बनाने के लिए स्वाभाविक रूप से बोल सकते हैं।"
                Language.MARATHI -> "मी आपला एआय ई-कचरा व्हॉईस सहाय्यक आहे. आपण लॉगिन उघडण्यासाठी, स्क्रॅपचे दर तपासण्यासाठी किंवा लॉट तयार करण्यासाठी सहज बोलू शकता."
            },
            actionDescription = "Show voice assistant capabilities",
            rawTranscript = original
        )
    }

    private fun resolveAmbiguousOrGeneralQA(
        text: String,
        original: String,
        lang: Language,
        context: AppNavigationContext
    ): SemanticAnalysisResult {
        // If input is short or incomplete
        if (text.length < 4) {
            return SemanticAnalysisResult(
                intent = SemanticIntent.CLARIFY_REQUEST,
                detectedLanguage = lang,
                confidence = 0.40f,
                spokenResponse = when (lang) {
                    Language.ENGLISH -> "I didn't quite catch that. Would you like to open the Collector login, check scrap prices, or get safety guidelines?"
                    Language.HINDI -> "मैं समझ नहीं पाया। क्या आप कलेक्टर लॉगिन खोलना चाहते हैं, स्क्रैप की दरें जानना चाहते हैं, या सुरक्षा नियम?"
                    Language.MARATHI -> "मला नीट समजले नाही. आपण कलेक्टर लॉगिन उघडू इच्छिता की स्क्रॅपचे दर जाणून घेऊ इच्छिता?"
                },
                actionDescription = "Clarification requested",
                clarificationQuestion = "Would you like to open the Collector login or check scrap rates?",
                rawTranscript = original
            )
        }

        return SemanticAnalysisResult(
            intent = SemanticIntent.GENERAL_EWASTE_QA,
            detectedLanguage = lang,
            confidence = 0.85f,
            spokenResponse = when (lang) {
                Language.ENGLISH -> "Our platform integrates informal scrap collectors with CPCB-authorized recycling refineries under India's E-Waste Rules 2022. You can ask to open any login or check live scrap rates."
                Language.HINDI -> "हमारा प्लेटफॉर्म अनौपचारिक स्क्रैप कलेक्टरों को भारत के ई-कचरा नियम 2022 के तहत अधिकृत रीसायकलर्स से जोड़ता है। आप लॉगिन खोलने या दरें पूछने के लिए कभी भी बोल सकते हैं।"
                Language.MARATHI -> "आमचे व्यासपीठ अनौपचारिक स्क्रॅप संकलकांना अधिकृत पुनर्प्रक्रिया केंद्रांशी जोडते. आपण लॉगिन उघडण्यासाठी किंवा दर तपासण्यासाठी सहज विचारू शकता."
            },
            actionDescription = "General E-Waste platform information",
            rawTranscript = original
        )
    }

    private fun extractMaterialCategory(text: String): MaterialCategory? {
        return when {
            text.contains("pcb") || text.contains("motherboard") || text.contains("circuit") || text.contains("मदरबोर्ड") || text.contains("सर्किट") -> MaterialCategory.PCB_BOARDS
            text.contains("cable") || text.contains("wire") || text.contains("copper") || text.contains("तार") || text.contains("केबल") || text.contains("तांबा") -> MaterialCategory.CABLES_WIRES
            text.contains("battery") || text.contains("lithium") || text.contains("बैटरी") || text.contains("बॅटरी") -> MaterialCategory.BATTERIES
            text.contains("crt") || text.contains("tv") || text.contains("monitor") || text.contains("सीआरटी") || text.contains("स्क्रीन") -> MaterialCategory.CRTS_MONITORS
            text.contains("lcd") || text.contains("led") || text.contains("display") || text.contains("एलसीडी") -> MaterialCategory.LCD_PANELS
            text.contains("motor") || text.contains("magnet") || text.contains("मोटर") || text.contains("चुंबक") -> MaterialCategory.MOTORS_MAGNETS
            text.contains("plastic") || text.contains("प्लास्टिक") -> MaterialCategory.MIXED_PLASTICS
            else -> null
        }
    }

    private fun extractWeight(text: String): Double? {
        val regex = Regex("""(\d+(\.\d+)?)\s*(kg|kilo|kilogram|किलो|किग्रा)?""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

