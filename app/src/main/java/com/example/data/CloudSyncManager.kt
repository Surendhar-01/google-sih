package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.auth.SupabaseAuthConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.pow

/**
 * Pushes offline-created lots and their ledger transactions to the Supabase
 * `collector_lots` and `collector_transactions` tables (created by
 * supabase/schema.sql). The push is idempotent (PostgREST upsert keyed on
 * primary key). If the tables have not been provisioned yet, the sync fails
 * softly so the app keeps working fully offline-first.
 */
object CloudSyncManager {

    private const val TAG = "CloudSyncManager"

    @Serializable
    data class CollectorLotDto(
        val lot_id: String,
        val collector_user_id: String,
        val category_name: String,
        val sub_category: String,
        val weight_kg: Double,
        val condition: String,
        val estimated_value_inr: Double,
        val quoted_rate_per_kg: Double,
        val collection_timestamp: Long,
        val collection_location: String,
        val gps_coordinates: String,
        val matched_recycler_id: String? = null,
        val matched_recycler_name: String? = null,
        val status_name: String,
        val payment_mode: String,
        val handover_receipt_number: String? = null,
        val recycler_confirmed: Boolean,
        val epr_certificate_no: String? = null
    )

    @Serializable
    data class CollectorTxnDto(
        val transaction_id: String,
        val lot_id: String,
        val collector_user_id: String,
        val category_name: String,
        val weight_kg: Double,
        val rate_per_kg: Double,
        val total_amount_inr: Double,
        val payment_mode: String,
        val recycler_name: String,
        val timestamp: Long,
        val receipt_number: String,
        val is_settled: Boolean
    )

    @Serializable
    data class LotPhotoDto(
        val photo_id: String,
        val lot_id: String,
        val collector_user_id: String,
        val remote_path: String?,
        val mime_type: String,
        val upload_status: String,
        val is_primary: Boolean,
        val created_at: Long
    )

    @Serializable
    data class CollectorLocationDto(
        val collector_user_id: String,
        val latitude: Double,
        val longitude: Double,
        val area_label: String? = null,
        val is_sharing_on: Boolean,
        val updated_at: Long
    )

    @Serializable
    data class ConnectionRequestDto(
        val request_id: String,
        val collector_user_id: String,
        val recycler_id: String,
        val recycler_name: String? = null,
        val status: String,
        val created_at: Long,
        val updated_at: Long
    )

    @Serializable
    data class QuotationDto(
        val quotation_id: String,
        val request_id: String,
        val recycler_id: String,
        val recycler_name: String? = null,
        val lot_id: String? = null,
        val collector_user_id: String,
        val quoted_rate_per_kg: Double,
        val quoted_total_inr: Double,
        val note: String? = null,
        val status: String,
        val created_at: Long,
        val responded_at: Long? = null
    )

    @Serializable
    data class AuditLogDto(
        val id: String,
        val user_id: String,
        val action: String,
        val detail_json: String? = null,
        val created_at: Long
    )

    /** Remote view of a shared collector's operating area. Privacy-preserving:
     *  only area label + coarse coordinates — never address or phone. */
    @Serializable
    data class RemoteCollector(
        val collector_user_id: String,
        val latitude: Double,
        val longitude: Double,
        val area_label: String? = null,
        val is_sharing_on: Boolean = true
    )

    /** Public registry row for authorized recyclers (government dataset). */
    @Serializable
    data class RemoteRecycler(
        val recycler_id: String,
        val name: String,
        val facility_location: String? = null,
        val city: String? = null,
        val distance_km: Double = 0.0,
        val cpcb_reg_no: String? = null,
        val authorization_validity: String? = null,
        val phone: String? = null,
        val accepted_categories: String? = null,
        val doorstep_pickup: Boolean = false,
        val min_weight_for_pickup_kg: Double = 0.0,
        val rating: Double = 0.0,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )

    data class SyncOutcome(val lotsPushed: Int, val transactionsPushed: Int, val serverReachable: Boolean)

    suspend fun syncLotsAndTransactions(
        lots: List<MaterialLotEntity>,
        transactions: List<TransactionLedgerEntity>,
        userId: String
    ): SyncOutcome {
        if (!SupabaseAuthConfig.isConfigured()) {
            return SyncOutcome(0, 0, serverReachable = false)
        }
        try {
            if (lots.isNotEmpty()) {
                val lotDtos = lots.map { it.toSyncDto(userId) }
                SupabaseAuthConfig.client.from("collector_lots")
                    .upsert(lotDtos) { onConflict = "lot_id" }
            }
            if (transactions.isNotEmpty()) {
                val txnDtos = transactions.map { it.toSyncDto(userId) }
                SupabaseAuthConfig.client.from("collector_transactions")
                    .upsert(txnDtos) { onConflict = "transaction_id" }
            }
            Log.i(TAG, "Synced ${lots.size} lots and ${transactions.size} transactions for $userId")
            return SyncOutcome(lots.size, transactions.size, serverReachable = true)
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync skipped (offline or table not provisioned): ${e.message}")
            return SyncOutcome(0, 0, serverReachable = false)
        }
    }

    /** Offline-first photo upload queue. Reads + compresses the captured photo,
     *  uploads it to the private `lot-photos` bucket under {userId}/{photoId}.jpg
     *  and reports per-photo status. Never fabricates a success. */
    suspend fun uploadLotPhoto(
        context: Context,
        photo: LotPhotoEntity,
        userId: String,
        onStatus: (String, String?) -> Unit = { _, _ -> }
    ): Boolean {
        if (!SupabaseAuthConfig.isConfigured()) {
            onStatus("FAILED", null)
            return false
        }
        return try {
            val bytes = readCompressedBytes(context, photo.localUri)
                ?: run {
                    Log.w(TAG, "Photo bytes unavailable for ${photo.photoId}; skipping upload")
                    onStatus("FAILED", null)
                    return false
                }
            val remotePath = "$userId/${photo.photoId}.jpg"
            SupabaseAuthConfig.client.storage.from("lot-photos")
                .upload(remotePath, bytes)
            onStatus("UPLOADED", remotePath)
            Log.i(TAG, "Uploaded photo ${photo.photoId} -> $remotePath")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Photo upload failed for ${photo.photoId}: ${e.message}")
            onStatus("FAILED", null)
            false
        }
    }

    suspend fun pushCollectorLocation(location: CollectorLocationEntity) {
        if (!SupabaseAuthConfig.isConfigured()) return
        try {
            SupabaseAuthConfig.client.from("collector_locations")
                .upsert(location.toDto()) { onConflict = "collector_user_id" }
        } catch (e: Exception) {
            Log.w(TAG, "Location push skipped: ${e.message}")
        }
    }

    suspend fun pushConnectionRequests(userId: String, requests: List<ConnectionRequestEntity>) {
        if (!SupabaseAuthConfig.isConfigured() || requests.isEmpty()) return
        try {
            val dtos = requests.map { it.toDto(userId) }
            SupabaseAuthConfig.client.from("connection_requests")
                .upsert(dtos) { onConflict = "request_id" }
        } catch (e: Exception) {
            Log.w(TAG, "Connection push skipped: ${e.message}")
        }
    }

    suspend fun pushQuotations(userId: String, quotations: List<QuotationEntity>) {
        if (!SupabaseAuthConfig.isConfigured() || quotations.isEmpty()) return
        try {
            val dtos = quotations.map { it.toDto(userId) }
            SupabaseAuthConfig.client.from("quotations")
                .upsert(dtos) { onConflict = "quotation_id" }
        } catch (e: Exception) {
            Log.w(TAG, "Quotation push skipped: ${e.message}")
        }
    }

    suspend fun pushAuditLogs(userId: String, logs: List<AuditLogEntity>) {
        if (!SupabaseAuthConfig.isConfigured() || logs.isEmpty()) return
        try {
            val dtos = logs.map { it.toDto(userId) }
            SupabaseAuthConfig.client.from("audit_logs")
                .upsert(dtos) { onConflict = "id" }
        } catch (e: Exception) {
            Log.w(TAG, "Audit push skipped: ${e.message}")
        }
    }

    /** Fetch other collectors currently sharing their (privacy-preserving)
     *  operating area. Returns rows already filtered to `is_sharing_on = true`;
     *  distance targeting is computed by the caller. */
    suspend fun fetchNearbyCollectors(): List<RemoteCollector> {
        if (!SupabaseAuthConfig.isConfigured()) return emptyList()
        return try {
            SupabaseAuthConfig.client.from("collector_locations")
                .select { filter { eq("is_sharing_on", true) } }
                .decodeList<RemoteCollector>()
        } catch (e: Exception) {
            Log.w(TAG, "Nearby collectors fetch failed: ${e.message}")
            emptyList()
        }
    }

    /** Fetch the public CPCB/SPCB authorized-recycler registry, falling back to
     *  empty (UI then uses the on-device seeded registry). */
    suspend fun fetchAuthorizedRecyclers(): List<RemoteRecycler> {
        if (!SupabaseAuthConfig.isConfigured()) return emptyList()
        return try {
            SupabaseAuthConfig.client.from("authorized_recyclers")
                .select()
                .decodeList<RemoteRecycler>()
        } catch (e: Exception) {
            Log.w(TAG, "Registry fetch failed: ${e.message}")
            emptyList()
        }
    }

    /** Haversine distance in kilometres between two coordinates. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = (Math.sin(dLat / 2)).pow(2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * (Math.sin(dLon / 2)).pow(2)
        return 2 * r * Math.asin(Math.sqrt(a))
    }

    private fun readCompressedBytes(context: Context, uri: String): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(Uri.parse(uri)) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            if (bitmap == null) return null
            val scaled = if (bitmap.width > 1600 || bitmap.height > 1600) {
                val ratio = min(1600f / bitmap.width, 1600f / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            if (scaled != bitmap) scaled.recycle()
            bitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Photo decode failed for $uri: ${e.message}")
            null
        }
    }
}

private fun MaterialLotEntity.toSyncDto(userId: String): CloudSyncManager.CollectorLotDto =
    CloudSyncManager.CollectorLotDto(
        lot_id = lotId,
        collector_user_id = userId,
        category_name = categoryName,
        sub_category = subCategory,
        weight_kg = weightKg,
        condition = condition,
        estimated_value_inr = estimatedValueInr,
        quoted_rate_per_kg = quotedRatePerKg,
        collection_timestamp = collectionTimestamp,
        collection_location = collectionLocation,
        gps_coordinates = gpsCoordinates,
        matched_recycler_id = matchedRecyclerId,
        matched_recycler_name = matchedRecyclerName,
        status_name = statusName,
        payment_mode = paymentModeName,
        handover_receipt_number = handoverReceiptNumber,
        recycler_confirmed = recyclerConfirmed,
        epr_certificate_no = eprCertificateNo
    )

private fun TransactionLedgerEntity.toSyncDto(userId: String): CloudSyncManager.CollectorTxnDto =
    CloudSyncManager.CollectorTxnDto(
        transaction_id = transactionId,
        lot_id = lotId,
        collector_user_id = userId,
        category_name = categoryName,
        weight_kg = weightKg,
        rate_per_kg = ratePerKg,
        total_amount_inr = totalAmountInr,
        payment_mode = paymentMode,
        recycler_name = recyclerName,
        timestamp = timestamp,
        receipt_number = receiptNumber,
        is_settled = isSettled
    )

private fun LotPhotoEntity.toDto(userId: String): CloudSyncManager.LotPhotoDto =
    CloudSyncManager.LotPhotoDto(
        photo_id = photoId,
        lot_id = lotId,
        collector_user_id = userId,
        remote_path = remotePath,
        mime_type = mimeType,
        upload_status = uploadStatus,
        is_primary = isPrimary,
        created_at = createdAt
    )

private fun CollectorLocationEntity.toDto(): CloudSyncManager.CollectorLocationDto =
    CloudSyncManager.CollectorLocationDto(
        collector_user_id = collectorUserId,
        latitude = latitude,
        longitude = longitude,
        area_label = areaLabel,
        is_sharing_on = isSharingOn,
        updated_at = updatedAt
    )

private fun ConnectionRequestEntity.toDto(userId: String): CloudSyncManager.ConnectionRequestDto =
    CloudSyncManager.ConnectionRequestDto(
        request_id = requestId,
        collector_user_id = userId,
        recycler_id = recyclerId,
        recycler_name = recyclerName,
        status = status,
        created_at = createdAt,
        updated_at = updatedAt
    )

private fun QuotationEntity.toDto(userId: String): CloudSyncManager.QuotationDto =
    CloudSyncManager.QuotationDto(
        quotation_id = quotationId,
        request_id = requestId,
        recycler_id = recyclerId,
        recycler_name = recyclerName,
        lot_id = lotId,
        collector_user_id = userId,
        quoted_rate_per_kg = quotedRatePerKg,
        quoted_total_inr = quotedTotalInr,
        note = note,
        status = status,
        created_at = createdAt,
        responded_at = respondedAt
    )

private fun AuditLogEntity.toDto(userId: String): CloudSyncManager.AuditLogDto =
    CloudSyncManager.AuditLogDto(
        id = id,
        user_id = userId,
        action = action,
        detail_json = detailJson,
        created_at = createdAt
    )