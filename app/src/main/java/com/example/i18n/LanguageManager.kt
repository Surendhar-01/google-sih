package com.example.i18n

import com.example.model.Language
import com.example.model.RoleType

object LanguageManager {

    fun getAppName(): String = "ECOBRIDGES"

    fun getSelectLanguage(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Select Language"
        Language.HINDI -> "भाषा चुनें"
        Language.MARATHI -> "भाषा निवडा"
    }

    fun getWelcomeTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Welcome"
        Language.HINDI -> "स्वागत है"
        Language.MARATHI -> "स्वागत आहे"
    }

    fun getWelcomeSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Choose how you want to continue."
        Language.HINDI -> "आगे बढ़ने का विकल्प चुनें।"
        Language.MARATHI -> "पुढे जाण्याचा पर्याय निवडा."
    }

    fun getVoiceSpeakPrompt(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Speak to continue"
        Language.HINDI -> "बोलकर बताएं"
        Language.MARATHI -> "बोलून सांगा"
    }

    fun getVoiceQuestion(lang: Language): String = when (lang) {
        Language.ENGLISH -> "What do you want to do?"
        Language.HINDI -> "आप क्या करना चाहते हैं?"
        Language.MARATHI -> "तुम्हाला काय करायचे आहे?"
    }

    fun getMicTapPrompt(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Tap to speak"
        Language.HINDI -> "बोलने के लिए टैप करें"
        Language.MARATHI -> "बोलण्यासाठी टॅप करा"
    }

    fun getListeningText(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Listening..."
        Language.HINDI -> "सुन रहा हूँ..."
        Language.MARATHI -> "ऐकत आहे..."
    }

    fun getUnderstandingText(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Understanding..."
        Language.HINDI -> "समझ रहा हूँ..."
        Language.MARATHI -> "समजून घेत आहे..."
    }

    fun getListenButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Listen"
        Language.HINDI -> "सुनें"
        Language.MARATHI -> "ऐका"
    }

    fun getStopButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Stop"
        Language.HINDI -> "रोकें"
        Language.MARATHI -> "थांबवा"
    }

    fun getSpeakingStatus(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Speaking..."
        Language.HINDI -> "बोल रहा हूँ..."
        Language.MARATHI -> "बोलत आहे..."
    }

    fun getRoleTitle(role: RoleType, lang: Language): String = when (role) {
        RoleType.INFORMAL_COLLECTOR -> when (lang) {
            Language.ENGLISH -> "Informal Collector"
            Language.HINDI -> "अनौपचारिक कलेक्टर"
            Language.MARATHI -> "अनौपचारिक कलेक्टर"
        }
        RoleType.FORMAL_RECYCLER -> when (lang) {
            Language.ENGLISH -> "Formal Recycler"
            Language.HINDI -> "अधिकृत रीसायकलर"
            Language.MARATHI -> "अधिकृत रीसायकलर"
        }
        RoleType.GOVERNMENT_ADMIN -> when (lang) {
            Language.ENGLISH -> "Government Admin"
            Language.HINDI -> "सरकारी प्रशासन"
            Language.MARATHI -> "सरकारी प्रशासन"
        }
    }

    fun getRoleDescription(role: RoleType, lang: Language): String = when (role) {
        RoleType.INFORMAL_COLLECTOR -> when (lang) {
            Language.ENGLISH -> "Scrap collection & aggregation"
            Language.HINDI -> "स्क्रैप संग्रह और एकत्रीकरण"
            Language.MARATHI -> "स्क्रॅप संकलन आणि एकत्रीकरण"
        }
        RoleType.FORMAL_RECYCLER -> when (lang) {
            Language.ENGLISH -> "Authorized Recycling Center"
            Language.HINDI -> "अधिकृत रीसायकलिंग केंद्र"
            Language.MARATHI -> "अधिकृत रीसायकलिंग केंद्र"
        }
        RoleType.GOVERNMENT_ADMIN -> when (lang) {
            Language.ENGLISH -> "Government Monitoring"
            Language.HINDI -> "सरकारी निगरानी"
            Language.MARATHI -> "शासकीय देखरेख"
        }
    }

    fun getRoleVoiceExplanation(role: RoleType, lang: Language): String = when (role) {
        RoleType.INFORMAL_COLLECTOR -> when (lang) {
            Language.ENGLISH -> "This option is for scrap collectors and aggregators."
            Language.HINDI -> "यह विकल्प स्क्रैप कलेक्टर और एग्रीगेटर के लिए है।"
            Language.MARATHI -> "हा पर्याय स्क्रॅप कलेक्टर आणि एग्रीगेटरसाठी आहे."
        }
        RoleType.FORMAL_RECYCLER -> when (lang) {
            Language.ENGLISH -> "This option is for authorized recycling centers and processing facilities."
            Language.HINDI -> "यह विकल्प अधिकृत रीसायकलिंग केंद्रों और प्रसंस्करण सुविधाओं के लिए है।"
            Language.MARATHI -> "हा पर्याय अधिकृत पुनर्प्रक्रिया केंद्रे आणि सुविधांसाठी आहे."
        }
        RoleType.GOVERNMENT_ADMIN -> when (lang) {
            Language.ENGLISH -> "This option is for government officials and environmental monitoring."
            Language.HINDI -> "यह विकल्प सरकारी प्रशासन और पर्यावरण निगरानी के लिए है।"
            Language.MARATHI -> "हा पर्याय शासकीय प्रशासन आणि पर्यावरण देखरेखीसाठी आहे."
        }
    }

    fun getVoiceSelectedMessage(role: RoleType, lang: Language): String = when (lang) {
        Language.ENGLISH -> "You selected ${getRoleTitle(role, Language.ENGLISH)}. Would you like to continue?"
        Language.HINDI -> "आपने ${getRoleTitle(role, Language.HINDI)} चुना है। क्या आप आगे बढ़ना चाहते हैं?"
        Language.MARATHI -> "तुम्ही ${getRoleTitle(role, Language.MARATHI)} निवडले आहे. तुम्हाला पुढे जायचे आहे का?"
    }

    fun getOpeningRoleLoginMessage(role: RoleType, lang: Language): String = when (role) {
        RoleType.INFORMAL_COLLECTOR -> when (lang) {
            Language.ENGLISH -> "Opening Informal Collector login..."
            Language.HINDI -> "अनौपचारिक कलेक्टर लॉगिन खोल रहे हैं..."
            Language.MARATHI -> "अनौपचारिक संकलन लॉगिन उघडत आहे..."
        }
        RoleType.FORMAL_RECYCLER -> when (lang) {
            Language.ENGLISH -> "Opening Formal Recycler login..."
            Language.HINDI -> "अधिकृत रीसायकलर लॉगिन खोल रहे हैं..."
            Language.MARATHI -> "अधिकृत पुनर्प्रक्रिया लॉगिन उघडत आहे..."
        }
        RoleType.GOVERNMENT_ADMIN -> when (lang) {
            Language.ENGLISH -> "Opening Government Admin login..."
            Language.HINDI -> "सरकारी प्रशासन लॉगिन खोल रहे हैं..."
            Language.MARATHI -> "शासकीय प्रशासन लॉगिन उघडत आहे..."
        }
    }

    fun getContinueQuestion(role: RoleType, lang: Language): String = when (lang) {
        Language.ENGLISH -> "Continue as ${getRoleTitle(role, Language.ENGLISH)}?"
        Language.HINDI -> "${getRoleTitle(role, Language.HINDI)} के रूप में आगे बढ़ें?"
        Language.MARATHI -> "${getRoleTitle(role, Language.MARATHI)} म्हणून पुढे जायचे का?"
    }

    fun getYesButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Yes, Continue"
        Language.HINDI -> "हाँ, आगे बढ़ें"
        Language.MARATHI -> "होय, पुढे जा"
    }

    fun getNoButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "No, Change"
        Language.HINDI -> "नहीं, बदलें"
        Language.MARATHI -> "नाही, बदला"
    }

    fun getCancelButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Cancel"
        Language.HINDI -> "रद्द करें"
        Language.MARATHI -> "रद्द करा"
    }

    fun getAmbiguousVoiceMessage(lang: Language): String = when (lang) {
        Language.ENGLISH -> "I didn't understand which role you want. Please say collector, recycler, or government."
        Language.HINDI -> "मुझे समझ नहीं आया कि आप कौन सा विकल्प चाहते हैं। कृपया कलेक्टर, रीसायकलर या सरकारी कहें।"
        Language.MARATHI -> "मला समजले नाही की तुम्हाला कोणता पर्याय हवा आहे. कृपया कलेक्टर, रीसायकलर किंवा सरकारी म्हणा."
    }

    fun getNeedHelp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Need help?"
        Language.HINDI -> "मदद चाहिए?"
        Language.MARATHI -> "मदत हवी आहे?"
    }

    fun getVoiceHelpButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Voice Help"
        Language.HINDI -> "आवाज़ से मदद"
        Language.MARATHI -> "आवाज मदत"
    }

    fun getVoiceHelpExplanation(lang: Language): String = when (lang) {
        Language.ENGLISH -> "This application has three sections: Informal Collector, Formal Recycler and Government Admin. Choose the option that matches your role."
        Language.HINDI -> "इस एप्लिकेशन में तीन भाग हैं: अनौपचारिक कलेक्टर, अधिकृत रीसायकलर और सरकारी प्रशासन। अपनी भूमिका से मेल खाने वाला विकल्प चुनें।"
        Language.MARATHI -> "या ॲप्लिकेशनमध्ये तीन विभाग आहेत: अनौपचारिक कलेक्टर, अधिकृत रीसायकलर आणि शासकीय प्रशासन. तुमच्या भूमिकेशी जुळणारा पर्याय निवडा."
    }

    fun getVoiceSettings(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Voice Settings"
        Language.HINDI -> "वॉयस सेटिंग्स"
        Language.MARATHI -> "व्हॉइस सेटिंग्ज"
    }

    fun getVoiceGuidance(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Voice Guidance"
        Language.HINDI -> "वॉयस मार्गदर्शन"
        Language.MARATHI -> "व्हॉइस मार्गदर्शन"
    }

    fun getSpeechSpeed(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Speech Speed"
        Language.HINDI -> "बोलने की गति"
        Language.MARATHI -> "बोलण्याचा वेग"
    }

    fun getSpeedSlow(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Slow"
        Language.HINDI -> "धीमी"
        Language.MARATHI -> "हळू"
    }

    fun getSpeedNormal(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Normal"
        Language.HINDI -> "सामान्य"
        Language.MARATHI -> "सामान्य"
    }

    fun getMuteLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Mute Audio"
        Language.HINDI -> "ऑडियो म्यूट"
        Language.MARATHI -> "ऑडिओ म्यूट"
    }

    fun getOfflineWarning(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Internet connection is unavailable. Some voice features may not work."
        Language.HINDI -> "इंटरनेट कनेक्शन उपलब्ध नहीं है। कुछ वॉयस सुविधाएं काम नहीं कर सकती हैं।"
        Language.MARATHI -> "इंटरनेट कनेक्शन उपलब्ध नाही. काही व्हॉइस वैशिष्ट्ये कार्य करणार नाहीत."
    }

    fun getMicPermissionNeeded(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Microphone permission is needed for voice input."
        Language.HINDI -> "आवाज़ इनपुट के लिए माइक्रोफ़ोन अनुमति आवश्यक है।"
        Language.MARATHI -> "व्हॉइस इनपुटसाठी मायक्रोफोन परवानगी आवश्यक आहे."
    }

    fun getAllowPermission(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Allow Microphone"
        Language.HINDI -> "माइक्रोफ़ोन चालू करें"
        Language.MARATHI -> "मायक्रोफोन सुरू करा"
    }

    fun getTryAgain(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Try Again"
        Language.HINDI -> "पुनः प्रयास करें"
        Language.MARATHI -> "पुन्हा प्रयत्न करा"
    }

    fun getIntroVoiceIntroduction(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Welcome to E-Waste Management. Tap the microphone to speak, or select your role: Informal Collector, Formal Recycler, or Government Admin."
        Language.HINDI -> "ई-कचरा प्रबंधन में आपका स्वागत है। बोलने के लिए माइक्रोफ़ोन टैप करें, या अपनी भूमिका चुनें: अनौपचारिक कलेक्टर, अधिकृत रीसायकलर, या सरकारी प्रशासन।"
        Language.MARATHI -> "ई-कचरा व्यवस्थापनामध्ये आपले स्वागत आहे. बोलण्यासाठी मायक्रोफोनवर टॅप करा किंवा आपली भूमिका निवडा: अनौपचारिक कलेक्टर, अधिकृत रीसायकलर किंवा शासकीय प्रशासन."
    }

    fun getOtpSentNotice(phone: String, lang: Language): String = when (lang) {
        Language.ENGLISH -> "OTP sent to registered mobile +91 $phone"
        Language.HINDI -> "पंजीकृत मोबाइल +91 $phone पर ओटीपी भेजा गया है"
        Language.MARATHI -> "नोंदणीकृत मोबाईल +91 $phone वर ओटीपी पाठवला आहे"
    }

    fun getOtpSpokenMessage(otp: String, lang: Language): String {
        val spaced = otp.map { it }.joinToString(" ")
        return when (lang) {
            Language.ENGLISH -> "Your 6-digit verification code is $spaced. Valid for 5 minutes."
            Language.HINDI -> "आपका 6 अंकों का सत्यापन कोड है $spaced। यह 5 मिनट के लिए मान्य है।"
            Language.MARATHI -> "तुमचा ६ अंकी पडताळणी कोड आहे $spaced. हा ५ मिनिटांसाठी वैध आहे."
        }
    }

    fun getResendOtp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Resend OTP"
        Language.HINDI -> "ओटीपी पुनः भेजें"
        Language.MARATHI -> "ओटीपी पुन्हा पाठवा"
    }

    fun getResendInSeconds(seconds: Int, lang: Language): String = when (lang) {
        Language.ENGLISH -> "Resend OTP in ${seconds}s"
        Language.HINDI -> "${seconds} सेकंड में पुनः भेजें"
        Language.MARATHI -> "${seconds} सेकंदात पुन्हा पाठवा"
    }

    fun getInvalidOtpError(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Invalid OTP code. Please enter the 6-digit code sent to your mobile."
        Language.HINDI -> "अमान्य ओटीपी कोड। कृपया अपने मोबाइल पर भेजा गया 6 अंकों का कोड दर्ज करें।"
        Language.MARATHI -> "अवैध ओटीपी कोड. कृपया तुमच्या मोबाईलवर आलेला ६ अंकी कोड टाका."
    }

    fun getEnterValidPhoneError(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Please enter a valid 10-digit mobile number."
        Language.HINDI -> "कृपया एक मान्य 10 अंकों का मोबाइल नंबर दर्ज करें।"
        Language.MARATHI -> "कृपया वैध १० अंकी मोबाईल नंबर प्रविष्ट करा."
    }

    fun getAutoFillOtp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Auto-Fill OTP"
        Language.HINDI -> "ओटीपी स्वतः भरें"
        Language.MARATHI -> "ओटीपी आपोआप भरा"
    }

    fun getCopyOtp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Copy OTP"
        Language.HINDI -> "ओटीपी कॉपी करें"
        Language.MARATHI -> "ओटीपी कॉपी करा"
    }

    fun getListenToOtp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Listen to OTP"
        Language.HINDI -> "ओटीपी सुनें"
        Language.MARATHI -> "ओटीपी ऐका"
    }

    fun getMobileAuthTab(lang: Language): String = when (lang) {
        Language.ENGLISH -> "OTP Verification"
        Language.HINDI -> "ओटीपी सत्यापन"
        Language.MARATHI -> "ओटीपी पडताळणी"
    }

    fun getOtpTabLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "OTP Verification"
        Language.HINDI -> "ओटीपी सत्यापन"
        Language.MARATHI -> "ओटीपी पडताळणी"
    }

    fun getGenerateOtpAction(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Generate Authentication OTP"
        Language.HINDI -> "प्रमाणीकरण ओटीपी जनरेट करें"
        Language.MARATHI -> "प्रमाणीकरण ओटीपी तयार करा"
    }

    fun getGeneratingOtp(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Generating & Dispatching OTP..."
        Language.HINDI -> "ओटीपी जनरेट और भेजा जा रहा है..."
        Language.MARATHI -> "ओटीपी तयार आणि पाठवला जात आहे..."
    }

    fun getVerifyAndLogin(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Verify OTP & Sign In"
        Language.HINDI -> "ओटीपी सत्यापित करें और साइन इन करें"
        Language.MARATHI -> "ओटीपी तपासा आणि साइन इन करा"
    }

    fun getSelectOtpChannel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Select Delivery Channel"
        Language.HINDI -> "ओटीपी प्राप्ति माध्यम चुनें"
        Language.MARATHI -> "ओटीपी मिळण्याचे माध्यम निवडा"
    }

    fun getIncomingAlertsTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Received Inboxes & Alerts"
        Language.HINDI -> "प्राप्त इनबॉक्स और अलर्ट"
        Language.MARATHI -> "मिळालेले इनबॉक्स आणि अलर्ट"
    }

    fun getEmailAuthTab(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Email & Password"
        Language.HINDI -> "ईमेल और पासवर्ड"
        Language.MARATHI -> "ईमेल आणि पासवर्ड"
    }

    fun getGoogleAuthTab(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Google Sign-In"
        Language.HINDI -> "Google साइन-इन"
        Language.MARATHI -> "Google साइन-इन"
    }

    fun getAuthInstructions(role: RoleType, lang: Language): String = when (role) {
        RoleType.INFORMAL_COLLECTOR -> when (lang) {
            Language.ENGLISH -> "Login with registered mobile OTP, official email, or Google account for certified aggregator access."
            Language.HINDI -> "प्रमाणित एग्रीगेटर पहुंच के लिए पंजीकृत मोबाइल ओटीपी, आधिकारिक ईमेल या Google खाते से लॉगिन करें।"
            Language.MARATHI -> "प्रमाणित संकलन प्रवेशासाठी नोंदणीकृत मोबाईल ओटीपी, अधिकृत ईमेल किंवा Google खात्यासह लॉगिन करा."
        }
        RoleType.FORMAL_RECYCLER -> when (lang) {
            Language.ENGLISH -> "Statutory CPCB-authorized recycler portal. Sign in with registered mobile, enterprise email, or Google account."
            Language.HINDI -> "वैधानिक सीपीसीबी-अधिकृत रीसायकलर पोर्टल। पंजीकृत मोबाइल, ईमेल या Google खाते से साइन इन करें।"
            Language.MARATHI -> "वैधानिक सीपीसीबी-अधिकृत पुनर्प्रक्रिया पोर्टल. नोंदणीकृत मोबाईल, ईमेल किंवा Google खात्यासह साइन इन करा."
        }
        RoleType.GOVERNMENT_ADMIN -> when (lang) {
            Language.ENGLISH -> "CPCB / MoEFCC Official Administration Gateway. Authenticate via authorized mobile OTP, gov email, or Google account."
            Language.HINDI -> "सीपीसीबी / पर्यावरण मंत्रालय आधिकारिक प्रशासन। अधिकृत मोबाइल ओटीपी, सरकारी ईमेल या Google खाते से प्रमाणित करें।"
            Language.MARATHI -> "सीपीसीबी / पर्यावरण मंत्रालय अधिकृत प्रशासन. अधिकृत मोबाईल ओटीपी, सरकारी ईमेल किंवा Google खात्यासह प्रमाणित करा."
        }
    }

    fun getVerificationPipelineTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Statutory Verification Pipeline"
        Language.HINDI -> "वैधानिक सत्यापन प्रक्रिया"
        Language.MARATHI -> "वैधानिक पडताळणी प्रक्रिया"
    }

    fun getOtpCardTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Multi-Channel OTP Authentication"
        Language.HINDI -> "मल्टी-चैनल ओटीपी प्रमाणीकरण"
        Language.MARATHI -> "मल्टी-चॅनेल ओटीपी प्रमाणीकरण"
    }

    fun getOtpCardSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Generate and receive secure OTP on your registered Mobile (+91 SMS), Email, and Google Account."
        Language.HINDI -> "अपने पंजीकृत मोबाइल (+91 एसएमएस), ईमेल और गूगल खाते पर सुरक्षित ओटीपी प्राप्त करें।"
        Language.MARATHI -> "तुमच्या नोंदणीकृत मोबाईल (+91 एसएमएस), ईमेल आणि गुगल खात्यावर सुरक्षित ओटीपी मिळवा."
    }

    fun getSelectDestinationHeader(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Select OTP Delivery Destination:"
        Language.HINDI -> "ओटीपी प्राप्ति माध्यम चुनें:"
        Language.MARATHI -> "ओटीपी मिळण्याचे ठिकाण निवडा:"
    }

    fun getAllChannelsTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "All Channels (Mobile + Email + Google)"
        Language.HINDI -> "सभी माध्यम (मोबाइल + ईमेल + Google)"
        Language.MARATHI -> "सर्व चॅनेल्स (मोबाईल + ईमेल + गुगल)"
    }

    fun getAllChannelsSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Simultaneously dispatch OTP across SMS, Email, and Google"
        Language.HINDI -> "एसएमएस, ईमेल और गूगल पर एक साथ ओटीपी भेजें"
        Language.MARATHI -> "एसएमएस, ईमेल आणि गुगलवर एकाच वेळी ओटीपी पाठवा"
    }

    fun getMobileSmsTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Registered Mobile Number (+91 SMS)"
        Language.HINDI -> "पंजीकृत मोबाइल नंबर (+91 एसएमएस)"
        Language.MARATHI -> "नोंदणीकृत मोबाईल नंबर (+91 एसएमएस)"
    }

    fun getMobileSmsSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Instant SMS dispatch via Govt. Telecom Gateway"
        Language.HINDI -> "सरकारी टेलीकॉम गेटवे के माध्यम से त्वरित एसएमएस"
        Language.MARATHI -> "सरकारी टेलिकॉम गेटवेद्वारे त्वरित एसएमएस"
    }

    fun getRegisteredEmailTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Registered Official Email"
        Language.HINDI -> "पंजीकृत आधिकारिक ईमेल"
        Language.MARATHI -> "नोंदणीकृत अधिकृत ईमेल"
    }

    fun getRegisteredEmailSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Secure HTML email to your statutory inbox"
        Language.HINDI -> "आपके आधिकारिक इनबॉक्स में सुरक्षित ईमेल"
        Language.MARATHI -> "तुमच्या अधिकृत इनबॉक्समध्ये सुरक्षित ईमेल"
    }

    fun getGoogleAccountTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Google Account Identity"
        Language.HINDI -> "Google खाता पहचान"
        Language.MARATHI -> "Google खाते ओळख"
    }

    fun getGoogleAccountSubtitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Google Security Alert & in-app verification"
        Language.HINDI -> "Google सुरक्षा चेतावनी और इन-ऐप सत्यापन"
        Language.MARATHI -> "Google सुरक्षा इशारा आणि ॲपमधील पडताळणी"
    }

    fun getMobileNumberLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Registered Mobile Number"
        Language.HINDI -> "पंजीकृत मोबाइल नंबर"
        Language.MARATHI -> "नोंदणीकृत मोबाईल नंबर"
    }

    fun getRegisteredEmailLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Registered Email Address"
        Language.HINDI -> "पंजीकृत ईमेल पता"
        Language.MARATHI -> "नोंदणीकृत ईमेल पत्ता"
    }

    fun getGoogleAccountLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Google Account Email"
        Language.HINDI -> "Google खाता ईमेल"
        Language.MARATHI -> "Google खाते ईमेल"
    }

    fun getGenerateOtpButton(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Generate Authentication OTP"
        Language.HINDI -> "प्रमाणीकरण ओटीपी जनरेट करें"
        Language.MARATHI -> "प्रमाणीकरण ओटीपी तयार करा"
    }

    fun getReceivedDispatchesTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Received Inboxes & Alerts (Live)"
        Language.HINDI -> "प्राप्त इनबॉक्स और अलर्ट (लाइव)"
        Language.MARATHI -> "मिळालेले इनबॉक्स आणि अलर्ट (थेट)"
    }

    fun getEnterOtpHeader(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Enter 6-Digit OTP Code"
        Language.HINDI -> "6 अंकों का ओटीपी कोड दर्ज करें"
        Language.MARATHI -> "६ अंकी ओटीपी कोड प्रविष्ट करा"
    }

    fun getVerifyingCredentials(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Verifying Credentials..."
        Language.HINDI -> "पहचान सत्यापित की जा रही है..."
        Language.MARATHI -> "ओळख पडताळली जात आहे..."
    }

    fun getVerifyOtpAndLogin(lang: Language): String = when (lang) {
        Language.ENGLISH -> "Verify OTP & Sign In"
        Language.HINDI -> "ओटीपी सत्यापित करें और साइन इन करें"
        Language.MARATHI -> "ओटीपी तपासा आणि साइन इन करा"
    }
}

