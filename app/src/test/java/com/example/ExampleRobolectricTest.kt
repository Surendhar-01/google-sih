package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.i18n.LanguageManager
import com.example.model.Language
import com.example.model.RoleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ECOBRIDGES", appName)
    }

    @Test
    fun `verify exactly three roles supported`() {
        val roles = RoleType.values()
        assertEquals(3, roles.size)
        assertEquals(RoleType.INFORMAL_COLLECTOR, roles[0])
        assertEquals(RoleType.FORMAL_RECYCLER, roles[1])
        assertEquals(RoleType.GOVERNMENT_ADMIN, roles[2])
    }

    @Test
    fun `verify multilingual translations exist for all roles`() {
        for (role in RoleType.values()) {
            for (lang in Language.values()) {
                val title = LanguageManager.getRoleTitle(role, lang)
                val desc = LanguageManager.getRoleDescription(role, lang)
                val explanation = LanguageManager.getRoleVoiceExplanation(role, lang)
                assertNotNull(title)
                assertNotNull(desc)
                assertNotNull(explanation)
                assert(title.isNotBlank())
                assert(desc.isNotBlank())
            }
        }
    }

    @Test
    fun `verify OTP generation produces 6-digit numeric string`() {
        for (i in 1..20) {
            val otp = com.example.util.OtpManager.generateOtp()
            assertEquals(6, otp.length)
            assert(otp.all { it.isDigit() })
            val num = otp.toInt()
            assert(num in 100000..999999)
        }
    }

    @Test
    fun `verify OTP localized notice strings format correctly`() {
        val phone = "6382411714"
        val otp = "583921"
        for (lang in Language.values()) {
            val notice = LanguageManager.getOtpSentNotice(phone, lang)
            val spoken = LanguageManager.getOtpSpokenMessage(otp, lang)
            assertNotNull(notice)
            assertNotNull(spoken)
            assert(notice.contains(phone))
            assert(spoken.contains("5"))
        }
    }

    @Test
    fun `verify MaterialCategory multilingual titles and safety guidance`() {
        val categories = com.example.model.MaterialCategory.values()
        assertEquals(7, categories.size)
        for (cat in categories) {
            assert(cat.getTitle(Language.ENGLISH).isNotBlank())
            assert(cat.getTitle(Language.HINDI).isNotBlank())
            assert(cat.getTitle(Language.MARATHI).isNotBlank())
            assert(cat.defaultRatePerKg > 0.0)
            assert(cat.keyRecoverableMetals.isNotEmpty())
        }
    }

    @Test
    fun `verify unit economics shows higher price for formal recycling`() {
        val economics = com.example.data.EwasteRepository.getUnitEconomics()
        assert(economics.isNotEmpty())
        for (item in economics) {
            assert(item.formalPlatformEarningsPerKg > item.informalBackyardEarningsPerKg)
            assert(item.differencePercentage > 0.0)
            assert(item.eprIncentiveBonus >= 0.0)
        }
    }

    @Test
    fun `verify hazard safety rules cover critical e-waste health issues`() {
        val safety = com.example.data.EwasteRepository.getSafetyGuidance()
        assertEquals(4, safety.size)
        val titles = safety.map { it.practiceTitle }
        assert(titles.any { it.contains("Open-Air Cable Burning", ignoreCase = true) })
        assert(titles.any { it.contains("Acid Leaching", ignoreCase = true) })
        assert(titles.any { it.contains("CRT Monitors", ignoreCase = true) })
        assert(titles.any { it.contains("Lithium Batteries", ignoreCase = true) })
    }

    @Test
    fun `verify voice command open informal collector and other login detection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val voiceEngine = com.example.voice.VoiceEngine(context)

        // 1. "open informal collector"
        voiceEngine.processSpokenInput("open informal collector", Language.ENGLISH)
        var result = voiceEngine.lastIntentResult.value
        assertNotNull(result)
        assertEquals(RoleType.INFORMAL_COLLECTOR, result?.detectedRole)

        // 2. "other login"
        voiceEngine.processSpokenInput("other login", Language.ENGLISH)
        result = voiceEngine.lastIntentResult.value
        assertNotNull(result)
        assertEquals(RoleType.FORMAL_RECYCLER, result?.detectedRole)

        // 3. "open formal recycler"
        voiceEngine.processSpokenInput("open formal recycler", Language.ENGLISH)
        result = voiceEngine.lastIntentResult.value
        assertNotNull(result)
        assertEquals(RoleType.FORMAL_RECYCLER, result?.detectedRole)

        // 4. "open government admin"
        voiceEngine.processSpokenInput("open government admin", Language.ENGLISH)
        result = voiceEngine.lastIntentResult.value
        assertNotNull(result)
        assertEquals(RoleType.GOVERNMENT_ADMIN, result?.detectedRole)

        // 5. Hindi: "कलेक्टर लॉगिन खोलो"
        voiceEngine.processSpokenInput("कलेक्टर लॉगिन खोलो", Language.HINDI)
        result = voiceEngine.lastIntentResult.value
        assertNotNull(result)
        assertEquals(RoleType.INFORMAL_COLLECTOR, result?.detectedRole)
    }

    @Test
    fun `verify natural sentence semantic understanding without keyword dependency`() {
        val navContext = com.example.model.AppNavigationContext(
            currentScreen = com.example.model.AppScreen.INTRO
        )

        // 1. Natural phrasing: "I would like to access my scrap collector account"
        val res1 = com.example.voice.SemanticIntentClassifier.analyze(
            "I would like to access my scrap collector account",
            navContext,
            Language.ENGLISH
        )
        assertEquals(com.example.model.SemanticIntent.OPEN_LOGIN, res1.intent)
        assertEquals(RoleType.INFORMAL_COLLECTOR, res1.targetRole)

        // 2. Natural phrasing: "Please take me into the registered recycling facility portal"
        val res2 = com.example.voice.SemanticIntentClassifier.analyze(
            "Please take me into the registered recycling facility portal",
            navContext,
            Language.ENGLISH
        )
        assertEquals(com.example.model.SemanticIntent.OPEN_LOGIN, res2.intent)
        assertEquals(RoleType.FORMAL_RECYCLER, res2.targetRole)

        // 3. Marathi: "मला कलेक्टर म्हणून लॉगिन करायचे आहे"
        val res3 = com.example.voice.SemanticIntentClassifier.analyze(
            "मला कलेक्टर म्हणून लॉगिन करायचे आहे",
            navContext,
            Language.MARATHI
        )
        assertEquals(com.example.model.SemanticIntent.OPEN_LOGIN, res3.intent)
        assertEquals(RoleType.INFORMAL_COLLECTOR, res3.targetRole)

        // 4. Negation check: "I do not want to log in"
        val res4 = com.example.voice.SemanticIntentClassifier.analyze(
            "I do not want to log in",
            navContext,
            Language.ENGLISH
        )
        assert(res4.intent != com.example.model.SemanticIntent.OPEN_LOGIN)

        // 5. Price query: "How much is the rate for motherboards?"
        val res5 = com.example.voice.SemanticIntentClassifier.analyze(
            "How much is the rate for motherboards?",
            navContext,
            Language.ENGLISH
        )
        assertEquals(com.example.model.SemanticIntent.QUERY_MATERIAL_PRICE, res5.intent)
        assertEquals(com.example.model.MaterialCategory.PCB_BOARDS, res5.materialCategory)

        // 6. Safety query: "Is burning copper wire in open air safe?"
        val res6 = com.example.voice.SemanticIntentClassifier.analyze(
            "Is burning copper wire in open air safe?",
            navContext,
            Language.ENGLISH
        )
        assertEquals(com.example.model.SemanticIntent.QUERY_SAFETY_GUIDELINES, res6.intent)
        assert(res6.spokenResponse.contains("Never burn", ignoreCase = true))

        // 7. Destructive action requires confirmation: "Sign out of my account"
        val res7 = com.example.voice.SemanticIntentClassifier.analyze(
            "Sign out of my account",
            navContext,
            Language.ENGLISH
        )
        assertEquals(com.example.model.SemanticIntent.DESTRUCTIVE_ACTION_REQUEST, res7.intent)
        assert(res7.requiresConfirmation)
        assert(res7.isDestructive)
    }

    @Test
    fun `verify SupabaseAuthService mobile OTP verification pipeline and dynamic OTP`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.auth.SupabaseAuthService.getInstance(context)
        authService.signOut()

        val phone = "9820144521"
        val sendResult = authService.sendMobileOtp(phone)
        assert(sendResult.isSuccess)
        val generatedOtp = sendResult.getOrThrow()

        // Verify dynamic 6-digit numeric OTP (never hardcoded)
        assertEquals(6, generatedOtp.length)
        assert(generatedOtp.all { it.isDigit() })

        // Test invalid OTP handling with retry decrement
        val invalidResult = authService.verifyMobileOtpAndLogin(phone, "000000", RoleType.INFORMAL_COLLECTOR)
        assert(!invalidResult.isSuccess)
        assert(invalidResult.errorMessage?.contains("attempt(s) remaining") == true)

        // Test successful verification through the 5-step pipeline
        val successResult = authService.verifyMobileOtpAndLogin(phone, generatedOtp, RoleType.INFORMAL_COLLECTOR)
        assert(successResult.isSuccess)
        assertNotNull(successResult.userProfile)
        assertEquals(RoleType.INFORMAL_COLLECTOR, successResult.userProfile?.role)
        assertEquals(com.example.auth.AccountStatus.ACTIVE, successResult.userProfile?.accountStatus)
        assert(successResult.userProfile?.permissions?.contains("COLL_ISSUE_RECEIPT") == true)
    }

    @Test
    fun `verify SupabaseAuthService email login and role authorization enforcement`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.auth.SupabaseAuthService.getInstance(context)
        authService.signOut()

        // 1. Role mismatch: Admin credentials cannot log into collector portal
        val conflictResult = authService.loginWithEmail("admin.officer@cpcb.gov.in", "securePassword123", RoleType.INFORMAL_COLLECTOR)
        assert(!conflictResult.isSuccess)
        assert(conflictResult.errorMessage?.contains("Role conflict") == true)

        // 2. Suspended account rejection
        val suspendedResult = authService.loginWithEmail("suspended.user@facility.com", "securePassword123", RoleType.FORMAL_RECYCLER)
        assert(!suspendedResult.isSuccess)
        assert(suspendedResult.errorMessage?.contains("SUSPENDED") == true)

        // 3. Valid recycler email login
        val validResult = authService.loginWithEmail("compliance@ecoreclaim.in", "passPhrase2026", RoleType.FORMAL_RECYCLER)
        assert(validResult.isSuccess)
        assertEquals(RoleType.FORMAL_RECYCLER, validResult.userProfile?.role)
        assert(validResult.userProfile?.statutoryIdentifier?.startsWith("CPCB/EPR-REC") == true)
    }

    @Test
    fun `verify SupabaseAuthService Google OAuth login flow and permissions`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.auth.SupabaseAuthService.getInstance(context)
        authService.signOut()

        val googleResult = authService.loginWithGoogle(
            googleAccountEmail = "officer.deshmukh@cpcb.gov.in",
            googleAccountName = "Shri Rajesh Deshmukh",
            targetRole = RoleType.GOVERNMENT_ADMIN
        )
        assert(googleResult.isSuccess)
        val profile = googleResult.userProfile
        assertNotNull(profile)
        assertEquals(RoleType.GOVERNMENT_ADMIN, profile?.role)
        assertEquals(com.example.auth.AuthMethod.GOOGLE_OAUTH, profile?.authMethod)
        assert(profile?.permissions?.contains("ADM_CPCB_PAN_INDIA_OVERSIGHT") == true)
    }
}

