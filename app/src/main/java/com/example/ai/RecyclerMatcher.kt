package com.example.ai

import com.example.model.AuthorizedRecycler
import com.example.model.MaterialCategory
import com.example.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Scores an authorized recycler against a collector's lot so that the best
 * match is surfaced instead of the first recycler in the list. Deterministic
 * scoring weighs proximity, verified rating, doorstep-pickup eligibility, and
 * the validity window of the CPCB authorization.
 */
object RecyclerMatcher {

    data class ScoredRecycler(
        val recycler: AuthorizedRecycler,
        val score: Double,
        val reasons: List<String>
    )

    fun rankForCategory(
        category: MaterialCategory,
        weightKg: Double,
        recyclers: List<AuthorizedRecycler>
    ): List<ScoredRecycler> {
        return recyclers
            .map { recycler -> score(recycler, category, weightKg) }
            .sortedByDescending { it.score }
    }

    fun topMatch(
        category: MaterialCategory,
        weightKg: Double,
        recyclers: List<AuthorizedRecycler>
    ): ScoredRecycler? = rankForCategory(category, weightKg, recyclers).firstOrNull()

    private fun score(recycler: AuthorizedRecycler, category: MaterialCategory, weightKg: Double): ScoredRecycler {
        val reasons = mutableListOf<String>()

        val acceptsCategory = recycler.acceptedCategories.contains(category)

        // Category acceptance is the dominant criterion: unmatched recyclers still
        // remain usable (they may buy via aggregation), but score much lower.
        val categoryScore = when {
            acceptsCategory -> 100.0
            recycler.acceptedCategories.isNotEmpty() -> 20.0
            else -> 0.0
        }

        // Proximity: distance in km, flattened to a 0..30 band.
        val distanceScore = 30.0 / (1.0 + recycler.distanceKm / 10.0)
        if (recycler.distanceKm <= 5.0) reasons.add("${recycler.distanceKm} km away")

        // Verified community rating contributes up to 20 points (4.9/5 rating).
        val ratingScore = (recycler.rating.coerceIn(0f, 5f) / 5f) * 20.0
        if (recycler.rating >= 4.5f) reasons.add("${recycler.rating}★ verified rating")

        // Doorstep pickup eligibility when the lot is big enough.
        var pickupScore = 0.0
        if (acceptsCategory && recycler.doorstepPickup && weightKg >= recycler.minWeightForPickupKg) {
            pickupScore = 15.0
            reasons.add("Doorstep pickup eligible (min ${recycler.minWeightForPickupKg.toInt()} kg)")
        } else if (acceptsCategory && recycler.doorstepPickup && weightKg < recycler.minWeightForPickupKg) {
            pickupScore = 4.0
            reasons.add("Drop-off at facility (weight below ${recycler.minWeightForPickupKg.toInt()} kg pickup threshold)")
        }

        // CPCB authorization validity: parse "Valid until <Month> <Year>".
        val validityScore = when (authorizationValid(recycler.authorizationValidity)) {
            AuthorizationState.VALID -> {
                reasons.add("CPCB authorization valid")
                10.0
            }
            AuthorizationState.EXPIRING -> 4.0
            AuthorizationState.EXPIRED -> {
                reasons.add("CPCB authorization has expired")
                -25.0
            }
            AuthorizationState.UNKNOWN -> 6.0
        }

        // Cash-first preference: collectors deal in cash at the weighbridge.
        val paymentScore = if (PaymentMode.CASH in recycler.paymentModesOffered) 5.0 else 0.0

        val score = (categoryScore + distanceScore + ratingScore + pickupScore + validityScore + paymentScore) * weightMultiplier(acceptsCategory)
        return ScoredRecycler(recycler, score, reasons.distinct())
    }

    private fun weightMultiplier(acceptsCategory: Boolean): Double = if (acceptsCategory) 1.0 else 0.55

    private enum class AuthorizationState { VALID, EXPIRING, EXPIRED, UNKNOWN }

    private fun authorizationValid(text: String): AuthorizationState {
        val match = Regex("Valid until\\s+(\\w+)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(text ?: "") ?: return AuthorizationState.UNKNOWN
        val month = match.groupValues[1]
        val year = match.groupValues[2].toIntOrNull() ?: return AuthorizationState.UNKNOWN
        val monthIndex = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            .indexOfFirst { month.startsWith(it, ignoreCase = true) }
        if (monthIndex < 0) return AuthorizationState.UNKNOWN

        val expiry = Calendar.getInstance().apply {
            set(Calendar.MONTH, monthIndex)
            set(Calendar.YEAR, year)
            set(Calendar.DAY_OF_MONTH, 1)
            clear(Calendar.HOUR_OF_DAY); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }.time

        return when {
            expiry.before(Date()) -> AuthorizationState.EXPIRED
            expiry.after(Date(System.currentTimeMillis() + 60L * 24 * 3600 * 1000)) -> AuthorizationState.VALID
            else -> AuthorizationState.EXPIRING
        }
    }
}