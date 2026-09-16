package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.model.RoleType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Production authentication service backed by the real Supabase (GoTrue) REST API.
 *
 * When [DEV_ONLY_OTP_ENABLED] is true (development/testing builds only), the
 * mobile OTP flow is replaced with a self-contained, on-device emulation:
 *   - a random 6-digit code is generated locally (full 000000..999999 range),
 *   - it is displayed directly on the OTP verification screen (never sent via SMS),
 *   - verification is checked locally and produces a synthetic local session.
 * When Supabase (or Google OAuth) is not configured, the REAL flow returns an explicit
 * [AuthErrorCode.PROVIDER_NOT_CONFIGURED] error instead of simulating success.
 *
 * To go back to real Supabase SMS OTP, set [DEV_ONLY_OTP_ENABLED] to false; no UI or
 * business-logic changes are needed.
 */
class SupabaseAuthService private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)

    private val _currentPipelineStep = MutableStateFlow(AuthPipelineStep.IDLE)
    val currentPipelineStep: StateFlow<AuthPipelineStep> = _currentPipelineStep.asStateFlow()

    private val _authenticatedUser = MutableStateFlow<UserProfile?>(loadStoredSession())
    val authenticatedUser: StateFlow<UserProfile?> = _authenticatedUser.asStateFlow()

    /**
     * Development-only: the latest locally generated OTP that is displayed on the OTP
     * verification screen so testers can log in without an SMS gateway. Always null when
     * [DEV_ONLY_OTP_ENABLED] is false or when no code is active.
     */
    private val _devDisplayOtp = MutableStateFlow<String?>(null)
    val devDisplayOtp: StateFlow<String?> = _devDisplayOtp.asStateFlow()

    private var activeOtpSession: OtpSession? = null
    private var lastResendTimestamp: Long = 0L

    companion object {
        private const val TAG = "SupabaseAuthService"
        private const val RESEND_COOLDOWN_SECONDS = 45L
        private const val OTP_VALIDITY_MS = 5 * 60 * 1000L
        private const val MAX_OTP_ATTEMPTS = 3

        /**
         * DEV-ONLY TESTING SWITCH — DO NOT ENABLE IN PRODUCTION.
         *
         * When true, the mobile OTP login is emulated entirely on-device:
         * a random 6-digit code is generated, shown on the verification screen, and
         * verified locally. No SMS is sent and no Supabase auth session is created.
         *
         * Flip to false to restore the real Supabase SMS OTP flow (session creation,
         * server-side verification, SMS delivery).
         */
        private const val DEV_ONLY_OTP_ENABLED = true

        @Volatile
        private var instance: SupabaseAuthService? = null

        fun getInstance(context: Context): SupabaseAuthService {
            return instance ?: synchronized(this) {
                instance ?: SupabaseAuthService(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun requirePhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length !in 10..15) {
            throw IllegalArgumentException(AuthErrorCode.INVALID_OTP.userMessage.let {
                "Please enter a valid 10-digit mobile number"
            })
        }
        return digits
    }

    /** Converts a local 10-digit number into E.164 (+91) used by GoTrue. */
    @VisibleForTesting
    internal fun toE164(rawPhone: String): String {
        val digits = requirePhone(rawPhone)
        return if (digits.startsWith("91") && digits.length == 12) "+$digits" else "+91$digits"
    }

    private fun isConfigured(): Boolean = SupabaseAuthConfig.isConfigured()

    /**
     * True when the on-device (no-SMS) OTP emulation is active. The UI uses this to
     * label the flow as development-only and to surface the generated code.
     */
    fun isDevOtpFlowEnabled(): Boolean = DEV_ONLY_OTP_ENABLED

    /**
     * Step 1: Request a real verification code via the Supabase SMS OTP flow.
     * Validation, OTP generation, expiry and delivery are all handled by Supabase.
     * The trailing 10 digits of the phone are retained locally only to drive the UI.
     *
     * DEV-ONLY: when [DEV_ONLY_OTP_ENABLED] is true this generates a random 6-digit
     * code on-device (000000..999999), stores it in the active session and exposes it
     * via [devDisplayOtp]. No SMS is sent and no network call is made.
     */
    suspend fun generateAndSendOtp(
        destination: OtpDeliveryDestination = OtpDeliveryDestination.MOBILE_SMS,
        phoneNumber: String,
        email: String? = null,
        googleAccount: String? = null
    ): Result<String> {
        if (DEV_ONLY_OTP_ENABLED) {
            return generateDevOtp(phoneNumber)
        }
        if (!isConfigured()) {
            return Result.failure(IllegalStateException(AuthErrorCode.PROVIDER_NOT_CONFIGURED.userMessage))
        }

        val cleanPhone = try { requirePhone(phoneNumber) } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException(e.message ?: "Please enter a valid 10-digit mobile number"))
        }

        // Cooldown between resend requests
        val now = System.currentTimeMillis()
        if (now - lastResendTimestamp < RESEND_COOLDOWN_SECONDS * 1000L) {
            val waitSec = RESEND_COOLDOWN_SECONDS - ((now - lastResendTimestamp) / 1000L)
            return Result.failure(IllegalStateException("Please wait $waitSec seconds before requesting a new code"))
        }

        return try {
            SupabaseAuthConfig.client.auth.signInWith(Phone) {
                phone = toE164(cleanPhone)
            }
            lastResendTimestamp = System.currentTimeMillis()
            activeOtpSession = OtpSession(
                destination = OtpDeliveryDestination.MOBILE_SMS,
                phoneNumber = cleanPhone,
                otpCode = "",
                createdAt = System.currentTimeMillis(),
                expiryTimestamp = System.currentTimeMillis() + OTP_VALIDITY_MS
            )
            Log.i(TAG, "SMS verification code requested for +91 $cleanPhone (code never stored locally)")
            Result.success("OTP sent")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            activeOtpSession = null
            Result.failure(sendError(e))
        }
    }

    /**
     * Backward-compatible mobile OTP sender.
     */
    suspend fun sendMobileOtp(phoneNumber: String): Result<String> =
        generateAndSendOtp(destination = OtpDeliveryDestination.MOBILE_SMS, phoneNumber = phoneNumber)

    fun getActiveOtpSession(): OtpSession? = activeOtpSession

    fun getResendCooldownRemaining(): Long {
        val elapsed = (System.currentTimeMillis() - lastResendTimestamp) / 1000L
        return maxOf(0L, RESEND_COOLDOWN_SECONDS - elapsed)
    }

    /**
     * Step 2: Verify the code against Supabase and run the statutory post-auth
     * pipeline (account status -> role -> permissions).
     *
     * DEV-ONLY: when [DEV_ONLY_OTP_ENABLED] is true the entered code is checked
     * against the locally stored OTP. On success a synthetic [UserProfile] is
     * created and the Supabase auth session is not involved.
     */
    suspend fun verifyOtpAndLogin(
        destination: OtpDeliveryDestination = OtpDeliveryDestination.MOBILE_SMS,
        phoneNumber: String?,
        email: String? = null,
        googleAccount: String? = null,
        enteredOtp: String,
        targetRole: RoleType
    ): AuthResult {
        if (DEV_ONLY_OTP_ENABLED) {
            return verifyDevOtp(phoneNumber, enteredOtp, targetRole)
        }
        if (!isConfigured()) {
            return failure(AuthErrorCode.PROVIDER_NOT_CONFIGURED, AuthPipelineStep.AUTHENTICATING_USER)
        }
        val session = activeOtpSession
            ?: return failure(AuthErrorCode.OTP_NOT_REQUESTED, AuthPipelineStep.AUTHENTICATING_USER)
        if (session.isExpired) {
            return failure(
                AuthErrorCode.INVALID_OTP,
                AuthPipelineStep.AUTHENTICATING_USER,
                "The verification code has expired. Please tap 'Resend OTP'."
            )
        }
        if (enteredOtp.trim().length != 6) {
            return failure(AuthErrorCode.INVALID_OTP, AuthPipelineStep.AUTHENTICATING_USER, "Please enter the complete 6-digit code.")
        }

        val phone = phoneNumber?.takeIf { it.isNotBlank() }?.let { toE164(it) }
            ?: session.phoneNumber?.let { toE164(it) }
            ?: return failure(AuthErrorCode.INVALID_OTP, AuthPipelineStep.AUTHENTICATING_USER, "Mobile number missing. Please re-enter it.")

        _currentPipelineStep.value = AuthPipelineStep.AUTHENTICATING_USER
        return try {
            SupabaseAuthConfig.client.auth.verifyPhoneOtp(OtpType.Phone.SMS, phone, enteredOtp.trim())
            val supabaseUser = SupabaseAuthConfig.client.auth.currentUserOrNull()
            if (supabaseUser == null) {
                activeOtpSession = null
                return failure(AuthErrorCode.INVALID_OTP, AuthPipelineStep.AUTHENTICATING_USER)
            }
            activeOtpSession = null
            Log.i(TAG, "SMS OTP verified for $phone")
            executePostAuthPipeline(
                authMethod = AuthMethod.MOBILE_OTP,
                targetRole = targetRole,
                email = supabaseUser.email,
                phoneNumber = "+91 ${session.phoneNumber ?: phone.removePrefix("+91")}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(mapException(e), AuthPipelineStep.AUTHENTICATING_USER)
        }
    }

    /**
     * Backward-compatible mobile OTP verification.
     */
    suspend fun verifyMobileOtpAndLogin(
        phoneNumber: String,
        enteredOtp: String,
        targetRole: RoleType
    ): AuthResult = verifyOtpAndLogin(
        phoneNumber = phoneNumber,
        enteredOtp = enteredOtp,
        targetRole = targetRole
    )

    /**
     * Email & password authentication performed by Supabase GoTrue.
     */
    suspend fun loginWithEmail(
        email: String,
        password: String,
        targetRole: RoleType
    ): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return failure(AuthErrorCode.INVALID_CREDENTIALS, AuthPipelineStep.AUTHENTICATING_USER, "Please enter a valid registered email address.")
        }
        if (trimmedPassword.length < 6) {
            return failure(AuthErrorCode.INVALID_CREDENTIALS, AuthPipelineStep.AUTHENTICATING_USER, "Password must be at least 6 characters long.")
        }
        if (!isConfigured()) {
            return failure(AuthErrorCode.PROVIDER_NOT_CONFIGURED, AuthPipelineStep.AUTHENTICATING_USER)
        }

        _currentPipelineStep.value = AuthPipelineStep.AUTHENTICATING_USER
        return try {
            SupabaseAuthConfig.client.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
            }
            val user = SupabaseAuthConfig.client.auth.currentUserOrNull()
                ?: return failure(AuthErrorCode.INVALID_CREDENTIALS, AuthPipelineStep.AUTHENTICATING_USER)
            Log.i(TAG, "Email login succeeded for ${user.email}")
            executePostAuthPipeline(
                authMethod = AuthMethod.EMAIL_PASSWORD,
                targetRole = targetRole,
                email = user.email,
                phoneNumber = user.phone
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(mapException(e), AuthPipelineStep.AUTHENTICATING_USER)
        }
    }

    @Deprecated("Remove path to simulated login. Use loginWithGoogleAuthResult() instead.")
    suspend fun loginWithGoogle(
        googleAccountEmail: String,
        googleAccountName: String,
        targetRole: RoleType
    ): AuthResult = failure(
        AuthErrorCode.PROVIDER_NOT_CONFIGURED,
        AuthPipelineStep.AUTHENTICATING_USER,
        "Direct account logins are not supported. Use 'Continue with Google'."
    )

    /**
     * Exchanges a Google id_token (from Credential Manager) with Supabase using
     * the ID Token sign-in flow, then runs the statutory pipeline.
     */
    suspend fun loginWithGoogleAuthResult(
        authResult: GoogleAuthResult,
        targetRole: RoleType
    ): AuthResult {
        if (!authResult.isSuccess) {
            return AuthResult(
                isSuccess = false,
                errorMessage = authResult.errorMessage ?: "Google Sign-In failed.",
                failureStep = AuthPipelineStep.AUTHENTICATING_USER,
                errorCode = AuthErrorCode.GENERIC
            )
        }
        val idToken = authResult.idToken
        if (idToken.isNullOrBlank()) {
            return failure(AuthErrorCode.GENERIC, AuthPipelineStep.AUTHENTICATING_USER, "Google could not provide an identity token.")
        }
        if (!isConfigured()) {
            return failure(AuthErrorCode.PROVIDER_NOT_CONFIGURED, AuthPipelineStep.AUTHENTICATING_USER)
        }

        _currentPipelineStep.value = AuthPipelineStep.AUTHENTICATING_USER
        return try {
            SupabaseAuthConfig.client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                if (!authResult.nonce.isNullOrBlank()) nonce = authResult.nonce
            }
            val user = SupabaseAuthConfig.client.auth.currentUserOrNull()
                ?: return failure(AuthErrorCode.GENERIC, AuthPipelineStep.AUTHENTICATING_USER)
            Log.i(TAG, "Google ID token exchanged for ${user.email}")
            executePostAuthPipeline(
                authMethod = AuthMethod.GOOGLE_OAUTH,
                targetRole = targetRole,
                email = user.email,
                phoneNumber = user.phone
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(mapException(e), AuthPipelineStep.AUTHENTICATING_USER)
        }
    }

    /**
     * Statutory pipeline executed after a positive GoTrue authentication:
     * 1. account status/KYC from `profiles`,
     * 2. role match versus the logged-in portal,
     * 3. permissions granted for that role.
     */
    private suspend fun executePostAuthPipeline(
        authMethod: AuthMethod,
        targetRole: RoleType,
        email: String?,
        phoneNumber: String?
    ): AuthResult {
        val client = SupabaseAuthConfig.client
        val supabaseUser = client.auth.currentUserOrNull()
            ?: return failure(AuthErrorCode.GENERIC, AuthPipelineStep.AUTHENTICATING_USER)

        // Step 2: Verify account & KYC profile from the database
        _currentPipelineStep.value = AuthPipelineStep.VERIFYING_ACCOUNT
        val profile = try {
            client.from("profiles")
                .select { filter { eq("auth_user_id", supabaseUser.id) } }
                .decodeSingleOrNull<ProfileRow>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return failure(mapException(e), AuthPipelineStep.VERIFYING_ACCOUNT)
        }

        if (profile == null) {
            // Auto-provision a profile for first-time users (Google, OTP, or email sign-up).
            val roleString = when (targetRole) {
                RoleType.INFORMAL_COLLECTOR -> "informal_collector"
                RoleType.FORMAL_RECYCLER -> "formal_recycler"
                RoleType.GOVERNMENT_ADMIN -> "government_admin"
            }
            val newProfile = ProfileRow(
                auth_user_id = supabaseUser.id,
                role = roleString,
                account_status = "active",
                display_name = (email ?: phoneNumber) ?: "Registered User",
                email = email,
                phone_number = phoneNumber
            )
            // Try upsert (requires unique index on auth_user_id); fall back to plain insert.
            // Both are guarded by the RLS own_profile policy that CHECKs auth.uid() = auth_user_id.
            val upsertError = try {
                client.from("profiles").upsert(newProfile) { onConflict = "auth_user_id" }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }
            if (upsertError != null) {
                try {
                    val rowJson = Json { ignoreUnknownKeys = true }
                        .encodeToJsonElement(ProfileRow.serializer(), newProfile)
                    client.from("profiles").insert(JsonArray(listOf(rowJson)))
                } catch (e: CancellationException) {
                    throw e
                } catch (dup: Exception) {
                    Log.w(TAG, "Profile upsert/insert failed for ${supabaseUser.id}: ${dup.message}")
                    return failure(
                        AuthErrorCode.ROLE_NOT_ASSIGNED,
                        AuthPipelineStep.VERIFYING_ACCOUNT,
                        "Auto-provisioning failed. Run this SQL once in Supabase SQL Editor: " +
                            "CREATE UNIQUE INDEX IF NOT EXISTS profiles_auth_user_id_uidx ON profiles(auth_user_id);"
                    )
                }
            }
            // Re-read the profile after provisioning
            val refreshed = try {
                client.from("profiles")
                    .select { filter { eq("auth_user_id", supabaseUser.id) } }
                    .decodeSingleOrNull<ProfileRow>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile re-read failed: ${e.message}")
                null
            }
            if (refreshed == null) {
                return failure(
                    AuthErrorCode.ROLE_NOT_ASSIGNED,
                    AuthPipelineStep.VERIFYING_ACCOUNT,
                    "Profile created but could not be read back. Ensure RLS policy own_profile exists."
                )
            }
            // Continue pipeline with the freshly provisioned profile
            return executePostAuthPipelineWithProfile(client, supabaseUser, refreshed, authMethod, targetRole, email, phoneNumber)
        }

        return executePostAuthPipelineWithProfile(client, supabaseUser, profile, authMethod, targetRole, email, phoneNumber)
    }

    private suspend fun executePostAuthPipelineWithProfile(
        client: io.github.jan.supabase.SupabaseClient,
        supabaseUser: io.github.jan.supabase.auth.user.UserInfo,
        profile: ProfileRow,
        authMethod: AuthMethod,
        targetRole: RoleType,
        email: String?,
        phoneNumber: String?
    ): AuthResult {

        val assignedRole = mapRole(profile.role)
            ?: return failure(
                AuthErrorCode.ROLE_NOT_ASSIGNED,
                AuthPipelineStep.VERIFYING_ACCOUNT,
                "No recognized role has been assigned to this account."
            )

        // Step 3: Verify statutory role versus requested portal
        _currentPipelineStep.value = AuthPipelineStep.VERIFYING_ROLE
        if (assignedRole != targetRole) {
            val message = when (assignedRole) {
                RoleType.INFORMAL_COLLECTOR -> "This account is registered as an Informal Collector. Continue to the Collector Dashboard?"
                RoleType.FORMAL_RECYCLER -> "This account is registered as a Formal Recycler. Continue to the Recycler Dashboard?"
                RoleType.GOVERNMENT_ADMIN -> "This account is registered as a Government Admin. Continue to the Admin Portal?"
            }
            return failure(AuthErrorCode.ROLE_MISMATCH, AuthPipelineStep.VERIFYING_ROLE, message)
        }

        // Step 3b: Verify account status
        val accountStatus = mapStatus(profile.account_status)
        if (!accountStatus.isAllowed) {
            val statusMessage = when (accountStatus) {
                AccountStatus.PENDING_VERIFICATION -> "Your registration is pending KYC / approval. You will be able to log in once approved."
                AccountStatus.REJECTED -> "Your registration was rejected. Contact the CPCB helpdesk for clarification."
                AccountStatus.SUSPENDED -> "This account is suspended pending a compliance audit. Contact the CPCB helpdesk."
                else -> "This account has been disabled by the administrator."
            }
            return failure(AuthErrorCode.ACCOUNT_GATED, AuthPipelineStep.VERIFYING_ROLE, statusMessage)
        }

        // Step 4: Permissions for the verified role
        _currentPipelineStep.value = AuthPipelineStep.VERIFYING_PERMISSIONS
        val session = client.auth.currentSessionOrNull()
        _currentPipelineStep.value = AuthPipelineStep.SUCCESS

        val profileUser = UserProfile(
            userId = supabaseUser.id,
            displayName = profile.display_name?.takeIf { it.isNotBlank() }
                ?: (email ?: phoneNumber ?: "Registered User"),
            email = email,
            phoneNumber = phoneNumber,
            role = targetRole,
            accountStatus = AccountStatus.ACTIVE,
            permissions = defaultPermissionsOf(targetRole),
            statutoryIdentifier = profile.statutory_identifier ?: "",
            entityName = profile.entity_name ?: "",
            sessionToken = session?.accessToken ?: "",
            authMethod = authMethod,
            verifiedTimestamp = System.currentTimeMillis()
        )
        _authenticatedUser.value = profileUser
        saveSession(profileUser)

        return AuthResult(
            isSuccess = true,
            userProfile = profileUser
        )
    }

    private fun defaultPermissionsOf(role: RoleType): List<String> = when (role) {
        RoleType.INFORMAL_COLLECTOR -> listOf(
            "COLL_ISSUE_RECEIPT", "COLL_VIEW_RATES", "COLL_DIGITAL_WEIGH_IN", "COLL_EPR_CREDIT_ACCRUAL"
        )
        RoleType.FORMAL_RECYCLER -> listOf(
            "REC_CPCB_INBOUND_ACCEPT", "REC_EPR_CERTIFICATE_MINT", "REC_HAZARDOUS_NEUTRALIZE", "REC_DIRECT_ESCROW_SETTLE"
        )
        RoleType.GOVERNMENT_ADMIN -> listOf(
            "ADM_CPCB_PAN_INDIA_OVERSIGHT", "ADM_EPR_COMPLIANCE_AUDIT", "ADM_FACILITY_GEO_INSPECT", "ADM_RULE_13_PENALTY_NOTICE"
        )
    }

    // -----------------------------------------------------------------------
    // DEV-ONLY: on-device OTP emulation (DO NOT USE IN PRODUCTION)
    // -----------------------------------------------------------------------

    /**
     * Generate a random 6-digit OTP in the full range 000000..999999.
     * The code is stored in [activeOtpSession] and displayed via [devDisplayOtp].
     * No SMS is sent and no network call is made.
     */
    private suspend fun generateDevOtp(phoneNumber: String): Result<String> {
        val cleanPhone = try {
            requirePhone(phoneNumber)
        } catch (e: IllegalArgumentException) {
            return Result.failure(IllegalArgumentException(e.message ?: "Please enter a valid 10-digit mobile number"))
        }

        val now = System.currentTimeMillis()
        if (now - lastResendTimestamp < RESEND_COOLDOWN_SECONDS * 1000L) {
            val waitSec = RESEND_COOLDOWN_SECONDS - ((now - lastResendTimestamp) / 1000L)
            return Result.failure(IllegalStateException("Please wait $waitSec seconds before requesting a new code"))
        }

        val code = String.format("%06d", Random.nextInt(1_000_000))

        activeOtpSession = OtpSession(
            destination = OtpDeliveryDestination.MOBILE_SMS,
            phoneNumber = cleanPhone,
            otpCode = code,
            createdAt = now,
            expiryTimestamp = now + OTP_VALIDITY_MS,
            attemptsRemaining = MAX_OTP_ATTEMPTS,
            isLocked = false,
            lockExpiryTimestamp = null
        )
        lastResendTimestamp = now
        _devDisplayOtp.value = code
        Log.i(TAG, "[DEV-ONLY] Generated OTP $code for +91 $cleanPhone (no SMS sent)")
        return Result.success(code)
    }

    /**
     * Verify the entered OTP against the locally stored session and create a
     * synthetic [UserProfile] on success. Handles expiry, attempts and locking.
     */
    private suspend fun verifyDevOtp(
        phoneNumber: String?,
        enteredOtp: String,
        targetRole: RoleType
    ): AuthResult {
        _currentPipelineStep.value = AuthPipelineStep.AUTHENTICATING_USER

        val session = activeOtpSession
            ?: return failure(AuthErrorCode.OTP_NOT_REQUESTED, AuthPipelineStep.AUTHENTICATING_USER)

        if (session.isExpired) {
            _devDisplayOtp.value = null
            activeOtpSession = null
            return failure(
                AuthErrorCode.INVALID_OTP,
                AuthPipelineStep.AUTHENTICATING_USER,
                "The verification code has expired. Please tap 'Resend OTP'."
            )
        }

        if (session.isLocked) {
            _devDisplayOtp.value = null
            return failure(
                AuthErrorCode.INVALID_OTP,
                AuthPipelineStep.AUTHENTICATING_USER,
                "Too many incorrect attempts. Please request a new code."
            )
        }

        val clean = enteredOtp.trim()
        if (clean.length != 6) {
            return failure(AuthErrorCode.INVALID_OTP, AuthPipelineStep.AUTHENTICATING_USER,
                "Please enter the complete 6-digit code.")
        }

        if (clean != session.otpCode) {
            session.attemptsRemaining -= 1
            if (session.attemptsRemaining <= 0) {
                session.isLocked = true
                activeOtpSession = null
                _devDisplayOtp.value = null
                return failure(
                    AuthErrorCode.INVALID_OTP,
                    AuthPipelineStep.AUTHENTICATING_USER,
                    "Too many incorrect attempts. Please request a new code."
                )
            }
            return failure(
                AuthErrorCode.INVALID_OTP,
                AuthPipelineStep.AUTHENTICATING_USER,
                "Incorrect code. ${session.attemptsRemaining} attempt(s) remaining."
            )
        }

        // OTP matched — create a synthetic local session (no Supabase user involved)
        val phone = session.phoneNumber ?: phoneNumber?.filter { it.isDigit() }?.takeLast(10)
        val profileUser = UserProfile(
            userId = "dev-${phone ?: "user"}",
            displayName = "+91 $phone",
            email = null,
            phoneNumber = "+91 $phone",
            role = targetRole,
            accountStatus = AccountStatus.ACTIVE,
            permissions = defaultPermissionsOf(targetRole),
            statutoryIdentifier = "",
            entityName = "",
            sessionToken = "",
            authMethod = AuthMethod.MOBILE_OTP,
            verifiedTimestamp = System.currentTimeMillis()
        )

        activeOtpSession = null
        _devDisplayOtp.value = null
        _authenticatedUser.value = profileUser
        saveSession(profileUser)
        _currentPipelineStep.value = AuthPipelineStep.SUCCESS
        Log.i(TAG, "[DEV-ONLY] OTP verified for +91 $phone — synthetic local session created")
        return AuthResult(isSuccess = true, userProfile = profileUser)
    }

    // -----------------------------------------------------------------------

    private fun failure(
        errorCode: AuthErrorCode,
        step: AuthPipelineStep,
        message: String? = null
    ): AuthResult {
        _currentPipelineStep.value = AuthPipelineStep.FAILED
        return AuthResult(
            isSuccess = false,
            errorMessage = message ?: errorCode.userMessage,
            failureStep = step,
            errorCode = errorCode
        )
    }

    private fun sendError(e: Exception): Exception {
        val code = mapException(e)
        return IllegalStateException(code.userMessage, e)
    }

    private fun mapException(e: Exception): AuthErrorCode = when (e) {
        is CancellationException -> throw e
        is UnknownHostException, is ConnectException, is SocketTimeoutException, is java.net.SocketException -> AuthErrorCode.NETWORK_ERROR
        is java.net.HttpRetryException -> AuthErrorCode.NETWORK_ERROR
        is java.io.IOException -> AuthErrorCode.NETWORK_ERROR
        is AuthRestException -> mapRestException(e)
        else -> AuthErrorCode.GENERIC
    }

    private fun mapRestException(e: AuthRestException): AuthErrorCode {
        val name = e.errorCode?.name?.uppercase() ?: ""
        return when {
            name.contains("OTP") || name.contains("TOKEN_EXPIRED") || name.contains("EXPIRED") ||
                name.contains("INVALID_CODE") || name == "WRONG_OTP_VERIFY_SETTING" -> AuthErrorCode.INVALID_OTP
            name.contains("INVALID_CREDENTIALS") || name.contains("WRONG_PASSWORD") -> AuthErrorCode.INVALID_CREDENTIALS
            name.contains("EMAIL_NOT_CONFIRMED") || name.contains("PHONE_NOT_CONFIRMED") || name.contains("UNVERIFIED") -> AuthErrorCode.EMAIL_NOT_CONFIRMED
            name.contains("USER_NOT_FOUND") || name.contains("ACCOUNT_NOT_FOUND") -> AuthErrorCode.ACCOUNT_NOT_FOUND
            name.contains("USER_ALREADY_EXISTS") || name.contains("ALREADY_REGISTERED") -> AuthErrorCode.EMAIL_ALREADY_REGISTERED
            name.contains("RATE_LIMITED") || name.contains("OVER_REQUEST") ->
                AuthErrorCode.NETWORK_ERROR
            name.contains("SMS_SEND") || name.contains("PROVIDER") || name.contains("SIGNUP_DISABLED") ->
                AuthErrorCode.PROVIDER_NOT_CONFIGURED
            else -> AuthErrorCode.GENERIC
        }
    }

    fun signOut() {
        _authenticatedUser.value = null
        _currentPipelineStep.value = AuthPipelineStep.IDLE
        activeOtpSession = null
        prefs.edit().clear().apply()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (isConfigured()) SupabaseAuthConfig.client.auth.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Local sign-out completed; remote sign-out skipped: ${e.message}")
            }
        }
    }

    private fun saveSession(profile: UserProfile) {
        prefs.edit().apply {
            putString("user_id", profile.userId)
            putString("display_name", profile.displayName)
            putString("email", profile.email)
            putString("phone", profile.phoneNumber)
            putString("role", profile.role.name)
            putString("status", profile.accountStatus.name)
            putString("statutory_id", profile.statutoryIdentifier)
            putString("entity_name", profile.entityName)
            putString("token", profile.sessionToken)
            putString("auth_method", profile.authMethod.name)
            putLong("verified_time", profile.verifiedTimestamp)
            apply()
        }
    }

    private fun loadStoredSession(): UserProfile? {
        val userId = prefs.getString("user_id", null) ?: return null
        val roleStr = prefs.getString("role", null) ?: return null
        val role = try { RoleType.valueOf(roleStr) } catch (e: Exception) { return null }

        return UserProfile(
            userId = userId,
            displayName = prefs.getString("display_name", "Authorized User") ?: "Authorized User",
            email = prefs.getString("email", null),
            phoneNumber = prefs.getString("phone", null),
            role = role,
            accountStatus = AccountStatus.ACTIVE,
            permissions = defaultPermissionsOf(role),
            statutoryIdentifier = prefs.getString("statutory_id", "") ?: "",
            entityName = prefs.getString("entity_name", "") ?: "",
            sessionToken = prefs.getString("token", "") ?: "",
            authMethod = AuthMethod.valueOf(prefs.getString("auth_method", AuthMethod.MOBILE_OTP.name) ?: AuthMethod.MOBILE_OTP.name),
            verifiedTimestamp = prefs.getLong("verified_time", System.currentTimeMillis())
        )
    }

    private fun mapRole(value: String?): RoleType? = when (value?.trim()?.lowercase()) {
        "informal_collector", "collector" -> RoleType.INFORMAL_COLLECTOR
        "formal_recycler", "recycler" -> RoleType.FORMAL_RECYCLER
        "government_admin", "admin" -> RoleType.GOVERNMENT_ADMIN
        else -> null
    }

    private fun mapStatus(value: String?): AccountStatus = when (value?.trim()?.lowercase()) {
        "pending", "pending_verification" -> AccountStatus.PENDING_VERIFICATION
        "rejected" -> AccountStatus.REJECTED
        "suspended" -> AccountStatus.SUSPENDED
        "disabled" -> AccountStatus.DISABLED
        else -> AccountStatus.ACTIVE
    }
}