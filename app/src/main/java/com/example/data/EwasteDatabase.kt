package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MaterialLotEntity::class,
        PriceRecordEntity::class,
        RecyclerEntity::class,
        TransactionLedgerEntity::class,
        SafetyGuidelineEntity::class,
        LotPhotoEntity::class,
        CollectorLocationEntity::class,
        ConnectionRequestEntity::class,
        QuotationEntity::class,
        AuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EwasteDatabase : RoomDatabase() {
    abstract fun materialLotDao(): MaterialLotDao
    abstract fun priceDao(): PriceDao
    abstract fun recyclerDao(): RecyclerDao
    abstract fun transactionLedgerDao(): TransactionLedgerDao
    abstract fun safetyGuidelineDao(): SafetyGuidelineDao
    abstract fun lotPhotoDao(): LotPhotoDao
    abstract fun collectorLocationDao(): CollectorLocationDao
    abstract fun connectionRequestDao(): ConnectionRequestDao
    abstract fun quotationDao(): QuotationDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: EwasteDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): EwasteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EwasteDatabase::class.java,
                    "ewaste_moefcc_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(EwasteDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class EwasteDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(
                            database.priceDao(),
                            database.recyclerDao(),
                            database.materialLotDao(),
                            database.transactionLedgerDao(),
                            database.safetyGuidelineDao()
                        )
                    }
                }
            }
        }

        suspend fun populateInitialDatabase(
            priceDao: PriceDao,
            recyclerDao: RecyclerDao,
            lotDao: MaterialLotDao,
            ledgerDao: TransactionLedgerDao,
            safetyGuidelineDao: SafetyGuidelineDao? = null
        ) {
            // Seed Safety Guidelines
            val seedSafetyGuidelines = listOf(
                SafetyGuidelineEntity(
                    id = "HAZ-01",
                    practiceTitle = "Open-Air Cable Burning (खुली आग में तार जलाना)",
                    whyUnsafe = "Burning plastic & PVC insulation releases deadly Dioxins, Furans, and Lead fumes into your lungs and neighborhood.",
                    whatIsLost = "Burning oxidizes and degrades copper quality, lowering scrap value by 20–30% and causing chronic respiratory illness.",
                    safeFormalAlternative = "Use low-cost mechanical wire stripper or handover intact wires to authorized recyclers for full pure electrolytic copper rates (₹460/kg).",
                    iconEmoji = "🔥",
                    alertLevel = "CRITICAL"
                ),
                SafetyGuidelineEntity(
                    id = "HAZ-02",
                    practiceTitle = "Acid Leaching of Circuit Boards (एसिड में मदरबोर्ड गलाना)",
                    whyUnsafe = "Using Aqua Regia and Nitric Acid creates toxic nitrogen dioxide clouds, water table poisoning, and high chemical burn risk.",
                    whatIsLost = "Backyard acid only recovers partial gold (loss of 85% palladium, neodymium, gallium, and tantalum worth thousands of rupees).",
                    safeFormalAlternative = "Sell whole PCBs to formal recyclers who operate closed-loop hydrometallurgical recovery, paying for gold, silver, and rare earth contents.",
                    iconEmoji = "🧪",
                    alertLevel = "CRITICAL"
                ),
                SafetyGuidelineEntity(
                    id = "HAZ-03",
                    practiceTitle = "Shattering CRT Monitors (सीआरटी स्क्रीन तोड़ना)",
                    whyUnsafe = "CRT tubes contain high vacuum (implosion hazard) and 1.5 to 3 kg of lead and toxic barium phosphor powder that causes neurological damage.",
                    whatIsLost = "Broken glass cannot be safely processed and is rejected by formal recyclers, forfeiting your payment.",
                    safeFormalAlternative = "Keep CRT monitors completely intact. Handover in one piece to authorized aggregators for safe glass lead-separation.",
                    iconEmoji = "📺",
                    alertLevel = "HIGH"
                ),
                SafetyGuidelineEntity(
                    id = "HAZ-04",
                    practiceTitle = "Crushing or Puncturing Lithium Batteries (बैटरी फोड़ना)",
                    whyUnsafe = "Lithium-ion cells catch fire instantaneously upon puncture (thermal runaway up to 600°C) and release toxic hydrofluoric acid gas.",
                    whatIsLost = "Destroys valuable high-grade cobalt and nickel cathodes and creates severe personal burn risks.",
                    safeFormalAlternative = "Store batteries in a dry, cool wooden/plastic crate without metal contact. Formal refiners recover 95% of lithium and cobalt safely.",
                    iconEmoji = "⚡",
                    alertLevel = "CRITICAL"
                )
            )
            safetyGuidelineDao?.insertGuidelines(seedSafetyGuidelines)

            // Seed Price Records for Maharashtra & India
            val seedPrices = listOf(
                PriceRecordEntity(
                    id = "PR-PCB-01",
                    categoryName = "PCB_BOARDS",
                    subCategory = "High-Grade Telecom & PC Motherboards",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 380.0,
                    marketMin = 340.0,
                    marketMax = 420.0,
                    trend = "UP",
                    trendPercentage = 7.4,
                    unit = "₹/kg",
                    dateUpdated = "Today",
                    keyMetalsJoined = "Gold (Au), Copper (Cu), Palladium (Pd), Gallium (Ga)"
                ),
                PriceRecordEntity(
                    id = "PR-CAB-02",
                    categoryName = "Cables & Copper Wiring",
                    subCategory = "Stripped & Insulated Copper Harness",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 460.0,
                    marketMin = 430.0,
                    marketMax = 490.0,
                    trend = "UP",
                    trendPercentage = 5.2,
                    unit = "₹/kg",
                    dateUpdated = "Today",
                    keyMetalsJoined = "High-Purity Electrolytic Copper (Cu)"
                ),
                PriceRecordEntity(
                    id = "PR-BAT-03",
                    categoryName = "BATTERIES",
                    subCategory = "Lithium-Ion Phone & Laptop Packs",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 160.0,
                    marketMin = 135.0,
                    marketMax = 185.0,
                    trend = "UP",
                    trendPercentage = 11.0,
                    unit = "₹/kg",
                    dateUpdated = "Today",
                    keyMetalsJoined = "Cobalt (Co), Lithium (Li), Nickel (Ni)"
                ),
                PriceRecordEntity(
                    id = "PR-MOT-04",
                    categoryName = "MOTORS_MAGNETS",
                    subCategory = "Hard Disk & Speaker Neodymium Assemblies",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 210.0,
                    marketMin = 190.0,
                    marketMax = 230.0,
                    trend = "STABLE",
                    trendPercentage = 0.5,
                    unit = "₹/kg",
                    dateUpdated = "Yesterday",
                    keyMetalsJoined = "Neodymium (Nd), Copper (Cu)"
                ),
                PriceRecordEntity(
                    id = "PR-CRT-05",
                    categoryName = "CRTS_MONITORS",
                    subCategory = "Whole Sealed CRT Monitors (Intact)",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 45.0,
                    marketMin = 35.0,
                    marketMax = 55.0,
                    trend = "STABLE",
                    trendPercentage = 0.0,
                    unit = "₹/kg",
                    dateUpdated = "2 days ago",
                    keyMetalsJoined = "Copper Deflection Yoke, Heavy Lead Glass"
                ),
                PriceRecordEntity(
                    id = "PR-LCD-06",
                    categoryName = "LCD_PANELS",
                    subCategory = "Flat Screen Displays & Laptops",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 120.0,
                    marketMin = 100.0,
                    marketMax = 140.0,
                    trend = "UP",
                    trendPercentage = 3.8,
                    unit = "₹/kg",
                    dateUpdated = "Today",
                    keyMetalsJoined = "Indium Tin Oxide (ITO), Aluminum"
                ),
                PriceRecordEntity(
                    id = "PR-PLS-07",
                    categoryName = "MIXED_PLASTICS",
                    subCategory = "Computer Housings ABS / Polycarbonate",
                    location = "Mumbai & Pune Region",
                    prevailingBuyRate = 32.0,
                    marketMin = 28.0,
                    marketMax = 36.0,
                    trend = "STABLE",
                    trendPercentage = 0.0,
                    unit = "₹/kg",
                    dateUpdated = "3 days ago",
                    keyMetalsJoined = "High-Grade Engineering Polymers"
                )
            )
            priceDao.insertPrices(seedPrices)

            // Seed Authorized Recyclers
            val seedRecyclers = listOf(
                RecyclerEntity(
                    recyclerId = "REC-CPCB-MH-001",
                    name = "EcoReclaim Green Refineries Pvt Ltd",
                    facilityLocation = "Plot C-14, MIDC Turbhe, Navi Mumbai",
                    city = "Mumbai",
                    distanceKm = 4.2,
                    cpcbRegNo = "CPCB/EPR-REC/2023/MH-0042",
                    authorizationValidity = "Valid until Dec 2028",
                    phone = "+91 98201 44521",
                    acceptedCategoriesJoined = "PCB_BOARDS,CABLES_WIRES,BATTERIES,MOTORS_MAGNETS,LCD_PANELS",
                    ratesJson = "Highest verified payout + 10% EPR collection bonus",
                    doorstepPickup = true,
                    minWeightForPickupKg = 25.0,
                    rating = 4.9f,
                    latitude = 19.0688,
                    longitude = 73.0189
                ),
                RecyclerEntity(
                    recyclerId = "REC-CPCB-MH-002",
                    name = "MahaClean Tech Circular Resources",
                    facilityLocation = "Bhiwandi Logistics Park, Thane District",
                    city = "Mumbai",
                    distanceKm = 11.5,
                    cpcbRegNo = "CPCB/EPR-REC/2022/MH-0118",
                    authorizationValidity = "Valid until Aug 2027",
                    phone = "+91 91370 88234",
                    acceptedCategoriesJoined = "PCB_BOARDS,CABLES_WIRES,CRTS_MONITORS,MIXED_PLASTICS",
                    ratesJson = "Instant Cash Handover on digital weighing scale",
                    doorstepPickup = true,
                    minWeightForPickupKg = 50.0,
                    rating = 4.7f,
                    latitude = 19.2967,
                    longitude = 73.0631
                ),
                RecyclerEntity(
                    recyclerId = "REC-CPCB-MH-003",
                    name = "SwachhBharat E-Waste Recyclers",
                    facilityLocation = "Pimpri-Chinchwad MIDC Phase 2, Pune",
                    city = "Pune",
                    distanceKm = 120.0,
                    cpcbRegNo = "CPCB/EPR-REC/2024/MH-0205",
                    authorizationValidity = "Valid until Jan 2029",
                    phone = "+91 98902 55192",
                    acceptedCategoriesJoined = "PCB_BOARDS,BATTERIES,LCD_PANELS,MOTORS_MAGNETS",
                    ratesJson = "Doorstep logistics aggregator vehicle with digital scale",
                    doorstepPickup = true,
                    minWeightForPickupKg = 40.0,
                    rating = 4.8f,
                    latitude = 18.6298,
                    longitude = 73.7997
                ),
                RecyclerEntity(
                    recyclerId = "REC-CPCB-MH-004",
                    name = "Kurla Aggregator & Dismantling Center",
                    facilityLocation = "LBS Marg, Kurla West, Mumbai",
                    city = "Mumbai",
                    distanceKm = 2.8,
                    cpcbRegNo = "CPCB/EPR-REC/2023/MH-0091",
                    authorizationValidity = "Valid until Nov 2027",
                    phone = "+91 98205 11299",
                    acceptedCategoriesJoined = "PCB_BOARDS,CABLES_WIRES,BATTERIES,CRTS_MONITORS,LCD_PANELS,MOTORS_MAGNETS,MIXED_PLASTICS",
                    ratesJson = "Direct cash payment at weighbridge scale",
                    doorstepPickup = true,
                    minWeightForPickupKg = 15.0,
                    rating = 4.6f,
                    latitude = 19.0728,
                    longitude = 72.8795
                )
            )
            recyclerDao.insertRecyclers(seedRecyclers)

            // Seed Initial Sample Collector Lots
            val now = System.currentTimeMillis()
            val seedLots = listOf(
                MaterialLotEntity(
                    lotId = "LOT-MH-2026-8841",
                    categoryName = "PCB_BOARDS",
                    subCategory = "Desktop Computer Motherboards",
                    weightKg = 8.5,
                    condition = "Sorted, Intact High-Grade",
                    imageUri = null,
                    estimatedValueInr = 3230.0,
                    quotedRatePerKg = 380.0,
                    collectionTimestamp = now - 86400000L * 2,
                    collectionLocation = "Dharavi Sector 3, Mumbai",
                    gpsCoordinates = "19.0433° N, 72.8569° E",
                    matchedRecyclerId = "REC-CPCB-MH-001",
                    matchedRecyclerName = "EcoReclaim Green Refineries",
                    statusName = "PAYMENT_COMPLETED",
                    paymentModeName = "CASH",
                    handoverReceiptNumber = "EPR-RC-2026-9912",
                    recyclerConfirmed = true,
                    eprCertificateNo = "EPR-CERT-MoEFCC-00412",
                    isSynced = true
                ),
                MaterialLotEntity(
                    lotId = "LOT-MH-2026-8855",
                    categoryName = "CABLES_WIRES",
                    subCategory = "Telecom Copper Wiring Bundle",
                    weightKg = 12.0,
                    condition = "Unburned Mechanical Stripping Ready",
                    imageUri = null,
                    estimatedValueInr = 5520.0,
                    quotedRatePerKg = 460.0,
                    collectionTimestamp = now - 86400000L,
                    collectionLocation = "Kurla West Scrap Market, Mumbai",
                    gpsCoordinates = "19.0688° N, 72.8797° E",
                    matchedRecyclerId = "REC-CPCB-MH-001",
                    matchedRecyclerName = "EcoReclaim Green Refineries",
                    statusName = "HANDOVER_PENDING",
                    paymentModeName = "CASH",
                    handoverReceiptNumber = "EPR-RC-2026-9938",
                    recyclerConfirmed = false,
                    eprCertificateNo = null,
                    isSynced = true
                )
            )
            for (lot in seedLots) {
                lotDao.insertLot(lot)
            }

            // Seed Initial Transaction Ledger Entry
            val seedTransaction = TransactionLedgerEntity(
                transactionId = "TXN-2026-001",
                lotId = "LOT-MH-2026-8841",
                categoryName = "PCBs & Motherboards",
                weightKg = 8.5,
                ratePerKg = 380.0,
                totalAmountInr = 3230.0,
                paymentMode = "CASH (Immediate Handover Receipt)",
                recyclerName = "EcoReclaim Green Refineries",
                timestamp = now - 86400000L * 2,
                receiptNumber = "EPR-RC-2026-9912",
                isSettled = true
            )
            ledgerDao.insertTransaction(seedTransaction)
        }
    }
}
