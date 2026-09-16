package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material_lots")
data class MaterialLotEntity(
    @PrimaryKey val lotId: String,
    val categoryName: String,
    val subCategory: String,
    val weightKg: Double,
    val condition: String,
    val imageUri: String?,
    val estimatedValueInr: Double,
    val quotedRatePerKg: Double,
    val collectionTimestamp: Long,
    val collectionLocation: String,
    val gpsCoordinates: String,
    val matchedRecyclerId: String?,
    val matchedRecyclerName: String?,
    val statusName: String,
    val paymentModeName: String,
    val handoverReceiptNumber: String?,
    val recyclerConfirmed: Boolean,
    val eprCertificateNo: String?,
    val isSynced: Boolean
)

@Entity(tableName = "price_records")
data class PriceRecordEntity(
    @PrimaryKey val id: String,
    val categoryName: String,
    val subCategory: String,
    val location: String,
    val prevailingBuyRate: Double,
    val marketMin: Double,
    val marketMax: Double,
    val trend: String,
    val trendPercentage: Double,
    val unit: String,
    val dateUpdated: String,
    val keyMetalsJoined: String
)

@Entity(tableName = "authorized_recyclers")
data class RecyclerEntity(
    @PrimaryKey val recyclerId: String,
    val name: String,
    val facilityLocation: String,
    val city: String,
    val distanceKm: Double,
    val cpcbRegNo: String,
    val authorizationValidity: String,
    val phone: String,
    val acceptedCategoriesJoined: String,
    val ratesJson: String,
    val doorstepPickup: Boolean,
    val minWeightForPickupKg: Double,
    val rating: Float,
    val latitude: Double = 19.0760,
    val longitude: Double = 72.8777
)

@Entity(tableName = "transaction_ledger")
data class TransactionLedgerEntity(
    @PrimaryKey val transactionId: String,
    val lotId: String,
    val categoryName: String,
    val weightKg: Double,
    val ratePerKg: Double,
    val totalAmountInr: Double,
    val paymentMode: String,
    val recyclerName: String,
    val timestamp: Long,
    val receiptNumber: String,
    val isSettled: Boolean
)

@Entity(tableName = "safety_guidelines")
data class SafetyGuidelineEntity(
    @PrimaryKey val id: String,
    val practiceTitle: String,
    val whyUnsafe: String,
    val whatIsLost: String,
    val safeFormalAlternative: String,
    val iconEmoji: String,
    val alertLevel: String
)

/** Photos attached to a lot. lotId == "DRAFT-<id>" while the lot is being
 *  authored, and is rewritten to the final lotId once the lot is created.
 *  uploadStatus is PENDING / UPLOADED / FAILED (offline-first queue). */
@Entity(tableName = "lot_photos")
data class LotPhotoEntity(
    @PrimaryKey val photoId: String,
    val lotId: String,
    val collectorUserId: String,
    val localUri: String,
    val remotePath: String?,
    val mimeType: String,
    val uploadStatus: String,
    val isPrimary: Boolean,
    val createdAt: Long
)

/** Collector's shared operating location (privacy-preserving: area + coarse
 *  coordinates only — never an exact house address). */
@Entity(tableName = "collector_locations")
data class CollectorLocationEntity(
    @PrimaryKey val collectorUserId: String,
    val latitude: Double,
    val longitude: Double,
    val areaLabel: String?,
    val isSharingOn: Boolean,
    val updatedAt: Long
)

/** Collector -> recycler connection request. Status: PENDING / ACCEPTED /
 *  REJECTED / BLOCKED / REPORTED. */
@Entity(tableName = "connection_requests")
data class ConnectionRequestEntity(
    @PrimaryKey val requestId: String,
    val collectorUserId: String,
    val recyclerId: String,
    val recyclerName: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Quotation issued against a connection request / lot by a recycler.
 *  Status: PENDING / ACCEPTED / REJECTED. */
@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey val quotationId: String,
    val requestId: String,
    val recyclerId: String,
    val recyclerName: String,
    val lotId: String?,
    val quotedRatePerKg: Double,
    val quotedTotalInr: Double,
    val note: String,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long?
)

/** Immutable audit trail for security-critical actions (block, report,
 *  handover confirmation). */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val action: String,
    val detailJson: String,
    val createdAt: Long
)

