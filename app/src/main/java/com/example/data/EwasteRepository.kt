package com.example.data

import com.example.model.AuthorizedRecycler
import com.example.model.HazardSafetyInfo
import com.example.model.LotStatus
import com.example.model.MaterialCategory
import com.example.model.MaterialLot
import com.example.model.PaymentMode
import com.example.model.PriceRecord
import com.example.model.UnitEconomicsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EwasteRepository(
    private val lotDao: MaterialLotDao,
    private val priceDao: PriceDao,
    private val recyclerDao: RecyclerDao,
    private val ledgerDao: TransactionLedgerDao,
    private val safetyDao: SafetyGuidelineDao,
    private val photoDao: LotPhotoDao,
    private val locationDao: CollectorLocationDao,
    private val connectionDao: ConnectionRequestDao,
    private val quotationDao: QuotationDao,
    private val auditDao: AuditLogDao
) {
    val allLots: Flow<List<MaterialLot>> = lotDao.getAllLots().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allPrices: Flow<List<PriceRecord>> = priceDao.getAllPrices().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allRecyclers: Flow<List<AuthorizedRecycler>> = recyclerDao.getAllRecyclers().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allSafetyGuidelines: Flow<List<HazardSafetyInfo>> = safetyDao.getAllGuidelines().map { entities ->
        if (entities.isNotEmpty()) {
            entities.map { it.toDomainModel() }
        } else {
            Companion.getSafetyGuidance()
        }
    }

    val totalSettledEarnings: Flow<Double> = ledgerDao.getTotalSettledEarnings().map { it ?: 0.0 }
    val pendingDues: Flow<Double> = ledgerDao.getPendingDues().map { it ?: 0.0 }
    val allTransactions: Flow<List<TransactionLedgerEntity>> = ledgerDao.getAllTransactions()
    val unsyncedLotsCount: Flow<Int> = lotDao.getUnsyncedLots().map { it.size }

    val completedTxnCount: Flow<Int> = ledgerDao.getCompletedTxnCount()
    val pendingTxnCount: Flow<Int> = ledgerDao.getPendingTxnCount()

    // ---- Photos -----------------------------------------------------------
    val pendingPhotos: Flow<List<LotPhotoEntity>> = photoDao.getPendingPhotos()

    fun photosForLot(lotId: String): Flow<List<LotPhotoEntity>> =
        photoDao.getPhotosForLot(lotId)

    suspend fun insertDraftPhoto(photo: LotPhotoEntity) = photoDao.insertPhoto(photo)

    suspend fun attachDraftPhotosToLot(draftLotId: String, lotId: String) =
        photoDao.attachPhotosToLot(draftLotId, lotId)

    suspend fun removePhoto(photoId: String) = photoDao.deletePhoto(photoId)

    suspend fun getPendingPhotosNow(): List<LotPhotoEntity> = photoDao.getPendingPhotosNow()

    suspend fun markPhotoStatus(photoId: String, status: String, remotePath: String?) =
        photoDao.updateStatus(photoId, status, remotePath)

    // ---- Collector location (privacy-preserving) ----------------------------
    fun collectorLocation(userId: String): Flow<CollectorLocationEntity?> =
        locationDao.getLocation(userId)

    suspend fun collectorLocationNow(userId: String): CollectorLocationEntity? =
        locationDao.getLocationNow(userId)

    suspend fun saveCollectorLocation(location: CollectorLocationEntity) =
        locationDao.upsert(location)

    // ---- Connections & quotations -------------------------------------------
    val allConnections: Flow<List<ConnectionRequestEntity>> = connectionDao.getAll()
    val activeConnections: Flow<List<ConnectionRequestEntity>> = connectionDao.getActive()
    val allQuotations: Flow<List<QuotationEntity>> = quotationDao.getAll()
    val pendingQuotations: Flow<List<QuotationEntity>> = quotationDao.getPending()

    suspend fun sendConnectionRequest(
        collectorUserId: String,
        recyclerId: String,
        recyclerName: String
    ): ConnectionRequestEntity {
        val existing = connectionDao.getExistingPending(collectorUserId, recyclerId)
        return existing ?: run {
            val now = System.currentTimeMillis()
            val request = ConnectionRequestEntity(
                requestId = "CONN-${now}-${(1000..9999).random()}",
                collectorUserId = collectorUserId,
                recyclerId = recyclerId,
                recyclerName = recyclerName,
                status = "PENDING",
                createdAt = now,
                updatedAt = now
            )
            connectionDao.insert(request)
            request
        }
    }

    suspend fun updateConnectionStatus(requestId: String, status: String) {
        connectionDao.updateStatus(requestId, status, System.currentTimeMillis())
    }

    suspend fun addQuotation(
        requestId: String,
        recyclerId: String,
        recyclerName: String,
        lotId: String?,
        quotedRatePerKg: Double,
        quotedTotalInr: Double?,
        note: String
    ): QuotationEntity {
        val now = System.currentTimeMillis()
        val quotation = QuotationEntity(
            quotationId = "QUO-${now}-${(1000..9999).random()}",
            requestId = requestId,
            recyclerId = recyclerId,
            recyclerName = recyclerName,
            lotId = lotId,
            quotedRatePerKg = quotedRatePerKg,
            quotedTotalInr = quotedTotalInr ?: 0.0,
            note = note,
            status = "PENDING",
            createdAt = now,
            respondedAt = null
        )
        quotationDao.insert(quotation)
        return quotation
    }

    suspend fun respondToQuotation(quotationId: String, accept: Boolean) {
        quotationDao.updateStatus(
            quotationId,
            if (accept) "ACCEPTED" else "REJECTED",
            System.currentTimeMillis()
        )
    }

    // ---- Audit -----------------------------------------------------------------
    suspend fun addAuditLog(userId: String, action: String, detailJson: String) {
        auditDao.insert(
            AuditLogEntity(
                id = "AUD-${System.currentTimeMillis()}-${(1000..9999).random()}",
                userId = userId,
                action = action,
                detailJson = detailJson,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    // ---- Revenue stats (from the real ledger) ------------------------------------
    fun settledEarningsBetween(startMillis: Long, endMillis: Long): Flow<Double> =
        ledgerDao.getSettledEarningsBetween(startMillis, endMillis)

    // ---- Duplicate-lot prevention ---------------------------------------------------
    suspend fun findDuplicateCandidate(category: MaterialCategory, weightKg: Double): MaterialLot? {
        val since = System.currentTimeMillis() - 60_000L
        return lotDao.findRecentDuplicate(category.name, weightKg, since)?.toDomainModel()
    }

    // ---- Live "nearby" helpers -------------------------------------------------------
    fun nearestRecyclers(recyclers: List<AuthorizedRecycler>, lat: Double, lon: Double, limit: Int = 8): List<AuthorizedRecycler> {
        return recyclers
            .map { it to CloudSyncManager.distanceKm(lat, lon, it.latitude, it.longitude) }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    suspend fun syncOfflineData(): Int {
        val count = lotDao.getUnsyncedLotsCount()
        if (count > 0) {
            lotDao.markAllLotsSynced()
        }
        return count
    }

    suspend fun createLot(
        category: MaterialCategory,
        subCategory: String,
        weightKg: Double,
        condition: String,
        location: String,
        gpsCoordinates: String,
        matchedRecycler: AuthorizedRecycler?,
        paymentMode: PaymentMode,
        isOffline: Boolean = false
    ): MaterialLot {
        val lotId = "LOT-MH-" + SimpleDateFormat("yyyy", Locale.US).format(Date()) + "-" + (1000..9999).random()
        val rate = matchedRecycler?.buyingRates?.get(category) ?: category.defaultRatePerKg
        val estimatedValue = weightKg * rate
        val receiptNumber = "EPR-RC-" + (10000..99999).random()

        val entity = MaterialLotEntity(
            lotId = lotId,
            categoryName = category.name,
            subCategory = subCategory,
            weightKg = weightKg,
            condition = condition,
            imageUri = null,
            estimatedValueInr = estimatedValue,
            quotedRatePerKg = rate,
            collectionTimestamp = System.currentTimeMillis(),
            collectionLocation = location,
            gpsCoordinates = gpsCoordinates,
            matchedRecyclerId = matchedRecycler?.recyclerId,
            matchedRecyclerName = matchedRecycler?.name,
            statusName = LotStatus.HANDOVER_PENDING.name,
            paymentModeName = paymentMode.name,
            handoverReceiptNumber = receiptNumber,
            recyclerConfirmed = false,
            eprCertificateNo = null,
            isSynced = !isOffline
        )
        lotDao.insertLot(entity)

        // Also create a ledger entry marked as pending
        val txn = TransactionLedgerEntity(
            transactionId = "TXN-" + System.currentTimeMillis() % 100000,
            lotId = lotId,
            categoryName = category.titleEn,
            weightKg = weightKg,
            ratePerKg = rate,
            totalAmountInr = estimatedValue,
            paymentMode = paymentMode.label,
            recyclerName = matchedRecycler?.name ?: "Authorized Recycler Hub",
            timestamp = System.currentTimeMillis(),
            receiptNumber = receiptNumber,
            isSettled = false
        )
        ledgerDao.insertTransaction(txn)

        return entity.toDomainModel()
    }

    suspend fun confirmRecyclerHandover(lotId: String, verifiedWeight: Double? = null, markPaid: Boolean = true) {
        val existing = lotDao.getLotById(lotId) ?: return
        val finalWeight = verifiedWeight ?: existing.weightKg
        val finalVal = finalWeight * existing.quotedRatePerKg
        val certNo = "EPR-CERT-MoEFCC-" + (10000..99999).random()

        val updated = existing.copy(
            weightKg = finalWeight,
            estimatedValueInr = finalVal,
            statusName = if (markPaid) LotStatus.PAYMENT_COMPLETED.name else LotStatus.RECYCLER_VERIFIED.name,
            recyclerConfirmed = true,
            eprCertificateNo = certNo
        )
        lotDao.updateLot(updated)

        // Update or upsert the single ledger row for this lot (dedup: REPLACE by primary
        // key keeps one row per lot instead of creating a second settled duplicate).
        val existingTxn = ledgerDao.getTransactionByLot(lotId)
        val txn = (existingTxn?.copy(
            categoryName = existing.categoryName,
            weightKg = finalWeight,
            ratePerKg = existing.quotedRatePerKg,
            totalAmountInr = finalVal,
            paymentMode = existing.paymentModeName,
            recyclerName = existing.matchedRecyclerName ?: "EcoReclaim Green Refineries",
            timestamp = System.currentTimeMillis(),
            receiptNumber = existing.handoverReceiptNumber ?: ("RC-" + lotId),
            isSettled = markPaid
        )) ?: TransactionLedgerEntity(
            transactionId = "TXN-SETTLED-" + lotId,
            lotId = lotId,
            categoryName = existing.categoryName,
            weightKg = finalWeight,
            ratePerKg = existing.quotedRatePerKg,
            totalAmountInr = finalVal,
            paymentMode = existing.paymentModeName,
            recyclerName = existing.matchedRecyclerName ?: "EcoReclaim Green Refineries",
            timestamp = System.currentTimeMillis(),
            receiptNumber = existing.handoverReceiptNumber ?: ("RC-" + lotId),
            isSettled = markPaid
        )
        ledgerDao.insertTransaction(txn)
    }

    fun getSafetyGuidance(): List<HazardSafetyInfo> = Companion.getSafetyGuidance()
    fun getUnitEconomics(): List<UnitEconomicsData> = Companion.getUnitEconomics()

    companion object {
        // Safety and Hazard Guidance Dataset
        fun getSafetyGuidance(): List<HazardSafetyInfo> {
            return listOf(
                HazardSafetyInfo(
                    id = "HAZ-01",
                    practiceTitle = "Open-Air Cable Burning (खुली आग में तार जलाना)",
                    whyUnsafe = "Burning plastic & PVC insulation releases deadly Dioxins, Furans, and Lead fumes into your lungs and neighborhood.",
                    whatIsLost = "Burning oxidizes and degrades copper quality, lowering scrap value by 20–30% and causing chronic respiratory illness.",
                    safeFormalAlternative = "Use low-cost mechanical wire stripper or handover intact wires to authorized recyclers for full pure electrolytic copper rates (₹460/kg).",
                    iconEmoji = "🔥",
                    alertLevel = "CRITICAL"
                ),
                HazardSafetyInfo(
                    id = "HAZ-02",
                    practiceTitle = "Acid Leaching of Circuit Boards (एसिड में मदरबोर्ड गलाना)",
                    whyUnsafe = "Using Aqua Regia and Nitric Acid creates toxic nitrogen dioxide clouds, water table poisoning, and high chemical burn risk.",
                    whatIsLost = "Backyard acid only recovers partial gold (loss of 85% palladium, neodymium, gallium, and tantalum worth thousands of rupees).",
                    safeFormalAlternative = "Sell whole PCBs to formal recyclers who operate closed-loop hydrometallurgical recovery, paying for gold, silver, and rare earth contents.",
                    iconEmoji = "🧪",
                    alertLevel = "CRITICAL"
                ),
                HazardSafetyInfo(
                    id = "HAZ-03",
                    practiceTitle = "Shattering CRT Monitors (सीआरटी स्क्रीन तोड़ना)",
                    whyUnsafe = "CRT tubes contain high vacuum (implosion hazard) and 1.5 to 3 kg of lead and toxic barium phosphor powder that causes neurological damage.",
                    whatIsLost = "Broken glass cannot be safely processed and is rejected by formal recyclers, forfeiting your payment.",
                    safeFormalAlternative = "Keep CRT monitors completely intact. Handover in one piece to authorized aggregators for safe glass lead-separation.",
                    iconEmoji = "📺",
                    alertLevel = "HIGH"
                ),
                HazardSafetyInfo(
                    id = "HAZ-04",
                    practiceTitle = "Crushing or Puncturing Lithium Batteries (बैटरी फोड़ना)",
                    whyUnsafe = "Lithium-ion cells catch fire instantaneously upon puncture (thermal runaway up to 600°C) and release toxic hydrofluoric acid gas.",
                    whatIsLost = "Destroys valuable high-grade cobalt and nickel cathodes and creates severe personal burn risks.",
                    safeFormalAlternative = "Store batteries in a dry, cool wooden/plastic crate without metal contact. Formal refiners recover 95% of lithium and cobalt safely.",
                    iconEmoji = "⚡",
                    alertLevel = "CRITICAL"
                )
            )
        }

        // Unit Economics Comparison Dataset
        fun getUnitEconomics(): List<UnitEconomicsData> {
            return listOf(
                UnitEconomicsData(
                    materialType = "PCBs & Motherboards",
                    informalBackyardEarningsPerKg = 240.0,
                    formalPlatformEarningsPerKg = 380.0,
                    differencePercentage = 58.3,
                    lostPreciousMetalsValue = 110.0,
                    eprIncentiveBonus = 30.0,
                    healthAndLegalSecurity = "Full CPCB legal immunity; Zero acid lung damage"
                ),
                UnitEconomicsData(
                    materialType = "Copper Cables & Wiring",
                    informalBackyardEarningsPerKg = 310.0,
                    formalPlatformEarningsPerKg = 460.0,
                    differencePercentage = 48.4,
                    lostPreciousMetalsValue = 90.0,
                    eprIncentiveBonus = 60.0,
                    healthAndLegalSecurity = "No police penalties for open burning; pure copper grade pricing"
                ),
                UnitEconomicsData(
                    materialType = "Lithium Batteries",
                    informalBackyardEarningsPerKg = 90.0,
                    formalPlatformEarningsPerKg = 160.0,
                    differencePercentage = 77.7,
                    lostPreciousMetalsValue = 50.0,
                    eprIncentiveBonus = 20.0,
                    healthAndLegalSecurity = "Zero explosion risk; direct payment on delivery scale"
                ),
                UnitEconomicsData(
                    materialType = "Rare Earth Motor Magnets",
                    informalBackyardEarningsPerKg = 70.0,
                    formalPlatformEarningsPerKg = 210.0,
                    differencePercentage = 200.0,
                    lostPreciousMetalsValue = 120.0,
                    eprIncentiveBonus = 20.0,
                    healthAndLegalSecurity = "Valued as strategic rare-earths instead of ordinary scrap iron"
                )
            )
        }
    }
}

// Extension mappers
private fun MaterialLotEntity.toDomainModel(): MaterialLot {
    val category = runCatching { MaterialCategory.valueOf(categoryName) }.getOrDefault(MaterialCategory.PCB_BOARDS)
    val status = runCatching { LotStatus.valueOf(statusName) }.getOrDefault(LotStatus.VALUATED)
    val paymentMode = runCatching { PaymentMode.valueOf(paymentModeName) }.getOrDefault(PaymentMode.CASH)

    return MaterialLot(
        lotId = lotId,
        category = category,
        subCategory = subCategory,
        weightKg = weightKg,
        condition = condition,
        imageUri = imageUri,
        estimatedValueInr = estimatedValueInr,
        quotedRatePerKg = quotedRatePerKg,
        collectionTimestamp = collectionTimestamp,
        collectionLocation = collectionLocation,
        gpsCoordinates = gpsCoordinates,
        matchedRecyclerId = matchedRecyclerId,
        matchedRecyclerName = matchedRecyclerName,
        status = status,
        paymentMode = paymentMode,
        handoverReceiptNumber = handoverReceiptNumber,
        recyclerConfirmed = recyclerConfirmed,
        eprCertificateNo = eprCertificateNo,
        isSynced = isSynced
    )
}

private fun PriceRecordEntity.toDomainModel(): PriceRecord {
    val category = runCatching { MaterialCategory.valueOf(categoryName) }.getOrDefault(MaterialCategory.PCB_BOARDS)
    val metals = keyMetalsJoined.split(",").map { it.trim() }

    return PriceRecord(
        category = category,
        subCategory = subCategory,
        location = location,
        prevailingBuyRate = prevailingBuyRate,
        marketMin = marketMin,
        marketMax = marketMax,
        trend = trend,
        trendPercentage = trendPercentage,
        unit = unit,
        dateUpdated = dateUpdated,
        keyMetals = metals
    )
}

private fun RecyclerEntity.toDomainModel(): AuthorizedRecycler {
    val accepted = acceptedCategoriesJoined.split(",")
        .mapNotNull { catStr -> runCatching { MaterialCategory.valueOf(catStr.trim()) }.getOrNull() }

    // Standard baseline rates
    val rates = MaterialCategory.values().associateWith { it.defaultRatePerKg }

    return AuthorizedRecycler(
        recyclerId = recyclerId,
        name = name,
        facilityLocation = facilityLocation,
        city = city,
        distanceKm = distanceKm,
        cpcbRegNo = cpcbRegNo,
        authorizationValidity = authorizationValidity,
        phone = phone,
        acceptedCategories = accepted,
        buyingRates = rates,
        doorstepPickup = doorstepPickup,
        minWeightForPickupKg = minWeightForPickupKg,
        paymentModesOffered = listOf(PaymentMode.CASH, PaymentMode.UPI),
        rating = rating,
        latitude = latitude,
        longitude = longitude
    )
}

private fun SafetyGuidelineEntity.toDomainModel(): HazardSafetyInfo {
    return HazardSafetyInfo(
        id = id,
        practiceTitle = practiceTitle,
        whyUnsafe = whyUnsafe,
        whatIsLost = whatIsLost,
        safeFormalAlternative = safeFormalAlternative,
        iconEmoji = iconEmoji,
        alertLevel = alertLevel
    )
}

