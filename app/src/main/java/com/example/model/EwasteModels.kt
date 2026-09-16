package com.example.model

enum class MaterialCategory(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val titleMr: String,
    val iconEmoji: String,
    val defaultRatePerKg: Double,
    val unit: String = "₹/kg",
    val primaryHazard: String,
    val keyRecoverableMetals: List<String>
) {
    PCB_BOARDS(
        id = "pcb_boards",
        titleEn = "PCBs & Motherboards",
        titleHi = "सर्किट बोर्ड और मदरबोर्ड",
        titleMr = "सर्किट बोर्ड आणि मदरबोर्ड",
        iconEmoji = "💻",
        defaultRatePerKg = 380.0,
        primaryHazard = "Acid leaching releases toxic cyanide & nitrogen oxides",
        keyRecoverableMetals = listOf("Gold (Au)", "Copper (Cu)", "Palladium (Pd)", "Tantalum (Ta)", "Gallium (Ga)")
    ),
    CABLES_WIRES(
        id = "cables_wires",
        titleEn = "Cables & Copper Wiring",
        titleHi = "तार और तांबे की केबल",
        titleMr = "तांब्याची वायर आणि केबल",
        iconEmoji = "🔌",
        defaultRatePerKg = 460.0,
        primaryHazard = "Open-air PVC burning produces deadly cancer-causing dioxins & furans",
        keyRecoverableMetals = listOf("High-Purity Electrolytic Copper (Cu)", "Aluminum (Al)")
    ),
    BATTERIES(
        id = "batteries",
        titleEn = "Lithium & Lead Batteries",
        titleHi = "लिथियम और लेड बैटरी",
        titleMr = "लिथियम आणि लेड बॅटरी",
        iconEmoji = "🔋",
        defaultRatePerKg = 160.0,
        primaryHazard = "Thermal runaway explosion risk & hydrofluoric acid gas",
        keyRecoverableMetals = listOf("Lithium (Li)", "Cobalt (Co)", "Nickel (Ni)", "Lead (Pb)")
    ),
    CRTS_MONITORS(
        id = "crts_monitors",
        titleEn = "CRT Monitors & Glass",
        titleHi = "सीआरटी टीवी और स्क्रीन",
        titleMr = "सीआरटी टीव्ही आणि स्क्रीन",
        iconEmoji = "📺",
        defaultRatePerKg = 45.0,
        primaryHazard = "Vacuum implosion & 1.5–3 kg toxic leaded glass per tube",
        keyRecoverableMetals = listOf("Copper Deflection Yoke", "High-Leaded Glass")
    ),
    LCD_PANELS(
        id = "lcd_panels",
        titleEn = "LCD & LED Panels",
        titleHi = "एलसीडी और एलईडी स्क्रीन",
        titleMr = "एलसीडी आणि एलईडी स्क्रीन",
        iconEmoji = "📱",
        defaultRatePerKg = 120.0,
        primaryHazard = "Mercury vapor from CCFL backlights & sharp glass hazard",
        keyRecoverableMetals = listOf("Indium (In)", "Tin (Sn)", "Specialty Plastics")
    ),
    MOTORS_MAGNETS(
        id = "motors_magnets",
        titleEn = "Motors & Magnet Assemblies",
        titleHi = "मोटर्स और चुंबक असेंबली",
        titleMr = "मोटर्स आणि मॅग्नेट असेंब्ली",
        iconEmoji = "⚙️",
        defaultRatePerKg = 210.0,
        primaryHazard = "Crushing injuries & lost rare-earths if dumped with iron scrap",
        keyRecoverableMetals = listOf("Neodymium (Nd)", "Dysprosium (Dy)", "Copper (Cu)")
    ),
    MIXED_PLASTICS(
        id = "mixed_plastics",
        titleEn = "Engineering Plastics (ABS/PC)",
        titleHi = "इलेक्ट्रॉनिक प्लास्टिक (ABS/PC)",
        titleMr = "इलेक्ट्रॉनिक प्लास्टिक (ABS/PC)",
        iconEmoji = "🧴",
        defaultRatePerKg = 32.0,
        primaryHazard = "Brominated flame retardants (BFRs) release persistent organic toxins",
        keyRecoverableMetals = listOf("Recycled ABS Pellets", "Polycarbonate (PC)")
    );

    fun getTitle(lang: Language): String = when (lang) {
        Language.ENGLISH -> titleEn
        Language.HINDI -> titleHi
        Language.MARATHI -> titleMr
    }
}

enum class LotStatus(
    val titleEn: String,
    val titleHi: String,
    val titleMr: String
) {
    DRAFT("Draft Estimate", "प्रारूप अनुमान", "मसुदा अंदाज"),
    VALUATED("Valuated by AI", "एआई मूल्यांकित", "एआय मूल्यमापन"),
    MATCHED("Matched to Recycler", "रीसायकलर से जुड़ा", "रीसायकलरशी जुळले"),
    HANDOVER_PENDING("Handover Scheduled", "हस्तांतरण निर्धारित", "हस्तांतरण निश्चित"),
    RECYCLER_VERIFIED("Recycler Confirmed", "रीसायकलर द्वारा सत्यापित", "रीसायकलरने प्रमाणित"),
    PAYMENT_COMPLETED("Payment Received", "भुगतान प्राप्त", "पेमेंट प्राप्त");

    fun getLabel(lang: Language): String = when (lang) {
        Language.ENGLISH -> titleEn
        Language.HINDI -> titleHi
        Language.MARATHI -> titleMr
    }
}

enum class PaymentMode(val label: String) {
    CASH("Instant Cash at Handover"),
    UPI("Direct UPI Transfer"),
    BANK_TRANSFER("Direct Bank Deposit")
}

data class MaterialLot(
    val lotId: String,
    val category: MaterialCategory,
    val subCategory: String,
    val weightKg: Double,
    val condition: String,
    val imageUri: String? = null,
    val estimatedValueInr: Double,
    val quotedRatePerKg: Double,
    val collectionTimestamp: Long,
    val collectionLocation: String,
    val gpsCoordinates: String,
    val matchedRecyclerId: String? = null,
    val matchedRecyclerName: String? = null,
    val status: LotStatus = LotStatus.VALUATED,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val handoverReceiptNumber: String? = null,
    val recyclerConfirmed: Boolean = false,
    val eprCertificateNo: String? = null,
    val isSynced: Boolean = true
)

data class PriceRecord(
    val category: MaterialCategory,
    val subCategory: String,
    val location: String,
    val prevailingBuyRate: Double,
    val marketMin: Double,
    val marketMax: Double,
    val trend: String, // "UP", "STABLE", "DOWN"
    val trendPercentage: Double,
    val unit: String = "₹/kg",
    val dateUpdated: String,
    val keyMetals: List<String>
)

data class AuthorizedRecycler(
    val recyclerId: String,
    val name: String,
    val facilityLocation: String,
    val city: String,
    val distanceKm: Double,
    val cpcbRegNo: String,
    val authorizationValidity: String,
    val phone: String,
    val acceptedCategories: List<MaterialCategory>,
    val buyingRates: Map<MaterialCategory, Double>,
    val doorstepPickup: Boolean,
    val minWeightForPickupKg: Double,
    val paymentModesOffered: List<PaymentMode>,
    val rating: Float,
    val latitude: Double = 19.0760,
    val longitude: Double = 72.8777
)

data class HazardSafetyInfo(
    val id: String,
    val practiceTitle: String,
    val whyUnsafe: String,
    val whatIsLost: String,
    val safeFormalAlternative: String,
    val iconEmoji: String,
    val alertLevel: String // "CRITICAL", "HIGH"
)

data class UnitEconomicsData(
    val materialType: String,
    val informalBackyardEarningsPerKg: Double,
    val formalPlatformEarningsPerKg: Double,
    val differencePercentage: Double,
    val lostPreciousMetalsValue: Double,
    val eprIncentiveBonus: Double,
    val healthAndLegalSecurity: String
)
