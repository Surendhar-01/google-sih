package com.example.ui.collector

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncManager.RemoteCollector
import com.example.data.CollectorLocationEntity
import com.example.data.ConnectionRequestEntity
import com.example.data.QuotationEntity
import com.example.data.TransactionLedgerEntity
import com.example.model.AuthorizedRecycler
import com.example.model.HazardSafetyInfo
import com.example.model.Language
import com.example.model.LotStatus
import com.example.model.MaterialCategory
import com.example.model.MaterialLot
import com.example.model.PaymentMode
import com.example.ui.map.MapCollectionPointsScreen
import com.example.ui.scan.EwasteCameraScannerScreen
import com.example.ui.theme.BackgroundCream
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorDashboardScreen(
    viewModel: CollectorDashboardViewModel,
    collectorPhone: String,
    language: Language,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lots by viewModel.lots.collectAsState()
    val prices by viewModel.prices.collectAsState()
    val recyclers by viewModel.recyclers.collectAsState()
    val totalSettled by viewModel.totalSettledEarnings.collectAsState()
    val pendingDues by viewModel.pendingDues.collectAsState()
    val todaySettled by viewModel.todaySettled.collectAsState()
    val monthSettled by viewModel.monthSettled.collectAsState()
    val collectorLocation by viewModel.location.collectAsState()
    val nearbyCollectors by viewModel.nearbyCollectors.collectAsState()
    val connections by viewModel.connections.collectAsState()
    val quotations by viewModel.quotations.collectAsState()

    val transactions by viewModel.transactions.collectAsState()
    val safetyGuidanceList by viewModel.safetyGuidance.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val selectedLot by viewModel.selectedLotForDetail.collectAsState()
    val anomalousLotIds by viewModel.anomalousLotIds.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Lots, 1: Rates, 2: Recyclers, 3: Safety, 4: Ledger & Economics
    var showMapView by remember { mutableStateOf(false) }
    var showScannerView by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var initialCategoryForLot by remember { mutableStateOf<MaterialCategory?>(null) }

    // Collect voice-driven collector operations (hands-free dashboard control)
    val collectorRouter = viewModel.voiceEngine?.router
    LaunchedEffect(collectorRouter) {
        collectorRouter?.collectorEvents?.collect { action ->
            when (action) {
                is com.example.voice.CollectorAction.OpenRevenueSummaryTab -> activeTab = 5
                is com.example.voice.CollectorAction.AnalyzeCurrentLot -> {
                    if (!showCreateDialog) {
                        initialCategoryForLot = null
                        showCreateDialog = true
                    }
                }
                is com.example.voice.CollectorAction.TurnOnLocationSharing -> {
                    activeTab = 3
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    val provider = lm?.getProviders(true)?.firstOrNull()
                    val loc = provider?.let { lm.getLastKnownLocation(it) }
                    viewModel.toggleLocationSharing(
                        true,
                        loc?.latitude ?: 19.0760,
                        loc?.longitude ?: 72.8777,
                        null
                    )
                }
                is com.example.voice.CollectorAction.OpenNearbyCollectorsTab,
                is com.example.voice.CollectorAction.OpenConnectionsTab -> activeTab = 3
                is com.example.voice.CollectorAction.OpenRecyclersTab,
                is com.example.voice.CollectorAction.RequestRecyclerQuote -> activeTab = 2
            }
        }
    }

    if (showScannerView) {
        EwasteCameraScannerScreen(
            language = language,
            onBack = { showScannerView = false },
            onAcceptLotToDashboard = { scanResult ->
                showScannerView = false
                viewModel.createNewLot(
                    category = scanResult.identifiedCategory,
                    subCategory = scanResult.itemName,
                    weightKg = scanResult.estimatedWeightKg,
                    condition = if (scanResult.isHazardous) "Scanned • Intact (Hazardous)" else "Scanned • Standard E-Waste",
                    location = "Local Collection Point",
                    gpsCoordinates = "19.0760, 72.8777",
                    matchedRecycler = com.example.ai.RecyclerMatcher
                        .topMatch(scanResult.identifiedCategory, scanResult.estimatedWeightKg, recyclers)
                        ?.recycler
                        ?: recyclers.firstOrNull(),
                    paymentMode = PaymentMode.CASH
                )
            },
            onSpeakText = { text, lang ->
                viewModel.voiceEngine?.speak(text, lang)
            }
        )
        return
    }

    if (showMapView) {
        MapCollectionPointsScreen(
            recyclers = recyclers,
            language = language,
            onBack = { showMapView = false },
            onSelectRecyclerForLot = { selectedRecycler ->
                showMapView = false
                showCreateDialog = true
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "E-Waste Collector Hub"
                                Language.HINDI -> "ई-कचरा संग्राहक केंद्र"
                                Language.MARATHI -> "ई-कचरा संकलक केंद्र"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "+91 $collectorPhone • CPCB Verified",
                            fontSize = 11.sp,
                            color = ForestGreenDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("collector_nav_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                actions = {
                    // AI Camera E-Waste Scanner Button
                    IconButton(
                        onClick = { showScannerView = true },
                        modifier = Modifier.testTag("open_camera_scanner_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "AI Camera E-Waste Scanner",
                            tint = ForestGreenPrimary
                        )
                    }

                    // Map View Icon Button
                    IconButton(
                        onClick = { showMapView = true },
                        modifier = Modifier.testTag("open_map_view_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Map of Collection Points",
                            tint = ForestGreenPrimary
                        )
                    }

                    // Offline / Online Sync Indicator & Manual Sync Toggle Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOffline) WarningAmber.copy(alpha = 0.2f) else if (unsyncedCount > 0) WarningAmber.copy(alpha = 0.15f) else MintLight,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                if (isOffline) {
                                    viewModel.toggleOfflineSimulation()
                                } else {
                                    viewModel.manualSync()
                                }
                            }
                            .testTag("toggle_offline_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isOffline) WarningAmber else SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSyncing) "Syncing..." else if (isOffline) "Offline" else if (unsyncedCount > 0) "$unsyncedCount Queued" else "Synced",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) WarningAmber else SuccessGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 6.dp
            ) {
                val navItems = listOf(
                    Triple("Lots", "📦", 0),
                    Triple("Prices", "📈", 1),
                    Triple("Recyclers", "🏭", 2),
                    Triple("Connect", "🤝", 3),
                    Triple("Safety", "🛡️", 4),
                    Triple("Ledger", "💰", 5)
                )
                for ((label, iconEmoji, index) in navItems) {
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = index },
                        icon = {
                            Text(text = iconEmoji, fontSize = if (isSelected) 20.sp else 16.sp)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = MintLight
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick AI Camera Scan FAB
                    FloatingActionButton(
                        onClick = { showScannerView = true },
                        containerColor = MintLight,
                        contentColor = ForestGreenPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("ai_camera_scan_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Scan Item")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Scan E-Waste"
                                    Language.HINDI -> "स्कैन करें"
                                    Language.MARATHI -> "स्कॅन करा"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Create Lot FAB
                    FloatingActionButton(
                        onClick = {
                            initialCategoryForLot = null
                            showCreateDialog = true
                        },
                        containerColor = ForestGreenPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("create_new_lot_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Create Lot")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Create Lot"
                                    Language.HINDI -> "लॉट बनाएं"
                                    Language.MARATHI -> "लॉट तयार करा"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> LotsOverviewTab(
                    lots = lots,
                    totalSettled = totalSettled,
                    pendingDues = pendingDues,
                    todaySettled = todaySettled,
                    monthSettled = monthSettled,
                    isOffline = isOffline,
                    isSyncing = isSyncing,
                    unsyncedCount = unsyncedCount,
                    lastSyncTimestamp = lastSyncTimestamp,
                    safetyItems = safetyGuidanceList,
                    language = language,
                    onToggleOffline = { viewModel.toggleOfflineSimulation() },
                    onManualSync = { viewModel.manualSync() },
                    onSpeakAllSafety = { viewModel.speakAllSafetyGuidelines(language) },
                    onSpeakSafetyItem = { item -> viewModel.speakSafety(item, language) },
                    onNavigateToSafetyTab = { activeTab = 4 },
                    onSelectLot = { lot -> viewModel.selectLot(lot) },
                    onCreateLot = {
                        initialCategoryForLot = null
                        showCreateDialog = true
                    },
                    anomalousLotIds = anomalousLotIds
                )
                1 -> PriceBoardSection(
                    prices = prices,
                    language = language,
                    onSpeakPrice = { price -> viewModel.speakPrice(price, language) },
                    onSelectCategoryForLot = { cat ->
                        initialCategoryForLot = cat
                        showCreateDialog = true
                    }
                )
                2 -> RecyclersTab(
                    recyclers = recyclers,
                    language = language,
                    onCallRecycler = { phone ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    },
                    onOpenMap = { showMapView = true }
                )
                3 -> LocationConnectionsTab(
                    recyclers = recyclers,
                    collectorLocation = collectorLocation,
                    nearbyCollectors = nearbyCollectors,
                    connections = connections,
                    quotations = quotations,
                    language = language,
                    onToggleSharing = { share, lat, lng, area ->
                        viewModel.toggleLocationSharing(share, lat, lng, area)
                    },
                    onRefreshNearby = { viewModel.refreshNearbyCollectors() },
                    onRequestConnection = { recycler -> viewModel.requestConnection(recycler) },
                    onRespondToQuotation = { id, accept -> viewModel.respondToQuotation(id, accept) }
                )
                4 -> SafetyGuidanceSection(
                    safetyItems = safetyGuidanceList,
                    language = language,
                    onSpeakSafety = { item -> viewModel.speakSafety(item, language) },
                    onSpeakAll = { viewModel.speakAllSafetyGuidelines(language) }
                )
                5 -> LedgerAndEconomicsTab(
                    transactions = transactions,
                    totalSettled = totalSettled,
                    pendingDues = pendingDues,
                    economicsList = viewModel.unitEconomics,
                    language = language
                )
            }

            // Universal Voice Assistant FAB & Bar
            if (viewModel.voiceEngine != null) {
                com.example.ui.voice.GlobalVoiceAssistantBar(
                    voiceEngine = viewModel.voiceEngine,
                    currentLanguage = language,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    // Create Lot Dialog
    if (showCreateDialog) {
        CreateLotDialog(
            viewModel = viewModel,
            initialCategory = initialCategoryForLot,
            availableRecyclers = recyclers,
            language = language,
            onDismiss = { showCreateDialog = false },
            onLotCreated = { category, subCategory, weightKg, condition, location, gpsCoordinates, matchedRecycler, paymentMode, draftLotId ->
                viewModel.createNewLot(
                    category = category,
                    subCategory = subCategory,
                    weightKg = weightKg,
                    condition = condition,
                    location = location,
                    gpsCoordinates = gpsCoordinates,
                    matchedRecycler = matchedRecycler,
                    paymentMode = paymentMode,
                    draftLotId = draftLotId,
                    onCreated = { newLot ->
                        showCreateDialog = false
                        viewModel.selectLot(newLot)
                    }
                )
            }
        )
    }

    // Handover Detail & QR Dialog
    if (selectedLot != null) {
        HandoverDetailDialog(
            lot = selectedLot!!,
            language = language,
            onDismiss = { viewModel.selectLot(null) },
            onConfirmRecyclerWeighIn = { lotId ->
                viewModel.confirmRecyclerHandover(lotId, markPaid = true)
                viewModel.selectLot(null)
            },
            onSpeakLot = { lot -> viewModel.speakLotEstimate(lot, language) }
        )
    }
}

@Composable
fun LotsOverviewTab(
    lots: List<MaterialLot>,
    totalSettled: Double,
    pendingDues: Double,
    todaySettled: Double,
    monthSettled: Double,
    isOffline: Boolean,
    isSyncing: Boolean,
    unsyncedCount: Int,
    lastSyncTimestamp: Long,
    safetyItems: List<HazardSafetyInfo>,
    language: Language,
    onToggleOffline: () -> Unit,
    onManualSync: () -> Unit,
    onSpeakAllSafety: () -> Unit,
    onSpeakSafetyItem: (HazardSafetyInfo) -> Unit,
    onNavigateToSafetyTab: () -> Unit,
    onSelectLot: (MaterialLot) -> Unit,
    onCreateLot: () -> Unit,
    anomalousLotIds: Set<String> = emptySet()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today / This-Month Earnings Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Today's Earnings"
                                Language.HINDI -> "आज की कमाई"
                                Language.MARATHI -> "आजची कमाई"
                            },
                            color = ForestGreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${todaySettled.toInt()}",
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "This Month"
                                Language.HINDI -> "इस महीने"
                                Language.MARATHI -> "या महिन्यात"
                            },
                            color = ForestGreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${monthSettled.toInt()}",
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Financial Summary Top Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Total Cash Settled"
                                Language.HINDI -> "कुल नकद प्राप्त"
                                Language.MARATHI -> "एकूण जमा रक्कम"
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalSettled.toInt()}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Verified CPCB Payout",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Pending Handover"
                                Language.HINDI -> "हस्तांतरण शेष"
                                Language.MARATHI -> "हस्तांतरण प्रलंबित"
                            },
                            color = TextSecondaryMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${pendingDues.toInt()}",
                            color = WarningAmber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Payable on weigh-in",
                            color = TextSecondaryMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Offline-First Sync Indicator and Manual Synchronization Toggle
        item {
            OfflineSyncIndicatorBar(
                isOffline = isOffline,
                isSyncing = isSyncing,
                unsyncedCount = unsyncedCount,
                lastSyncTimestamp = lastSyncTimestamp,
                language = language,
                onToggleOffline = onToggleOffline,
                onManualSync = onManualSync
            )
        }

        // Dedicated Section for Pictorial and Audio-Based Safety Guidance on Hazardous Waste
        item {
            PictorialSafetyGuidanceCard(
                safetyItems = safetyItems,
                language = language,
                onSpeakAllSafety = onSpeakAllSafety,
                onSpeakItem = onSpeakSafetyItem,
                onNavigateToSafetyTab = onNavigateToSafetyTab
            )
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Active & Completed Digital Lots"
                        Language.HINDI -> "सक्रिय एवं पूर्ण डिजिटल लॉट"
                        Language.MARATHI -> "सक्रिय व पूर्ण झालेले लॉट्स"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${lots.size} Total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenPrimary
                )
            }
        }

        // List of Digital Lots
        items(lots) { lot ->
            LotItemCard(
                lot = lot,
                language = language,
                isAnomalous = lot.lotId in anomalousLotIds,
                onClick = { onSelectLot(lot) }
            )
        }

        // Empty state
        if (lots.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📦", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Digital Lots Created Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Tap 'Create Lot' to photograph and valuate materials",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }
        }

item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun LotItemCard(
    lot: MaterialLot,
    language: Language,
    isAnomalous: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAnomalous) WarningAmber.copy(alpha = 0.06f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAnomalous) WarningAmber.copy(alpha = 0.5f) else MintBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lot_item_${lot.lotId.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MintLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = lot.category.iconEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = lot.lotId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = lot.category.getTitle(language),
                            fontSize = 12.sp,
                            color = TextPrimaryDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (lot.recyclerConfirmed) SuccessGreen.copy(alpha = 0.15f) else MintPill
                ) {
                    Text(
                        text = lot.status.getLabel(language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lot.recyclerConfirmed) SuccessGreen else ForestGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isAnomalous) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmber.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Rate outside market range — verify before handover"
                                Language.HINDI -> "दर बाजार सीमा से बाहर — हस्तांतरण से पहले जांचें"
                                Language.MARATHI -> "दर बाजार श्रेणीबाहेर — हस्तांतरणापूर्वी तपासा"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MintLight.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Weight", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = "${lot.weightKg} kg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                }
                Column {
                    Text(text = "Offered Rate", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = "₹${lot.quotedRatePerKg.toInt()}/kg",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Value", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = "₹${lot.estimatedValueInr.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ForestGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lot.matchedRecyclerName ?: "EcoReclaim Green Refineries",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }

                // Sync status indicator badge for the individual lot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (lot.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (lot.isSynced) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (lot.isSynced) "Synced" else "Queued",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lot.isSynced) SuccessGreen else WarningAmber
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "View Handover",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun RecyclersTab(
    recyclers: List<AuthorizedRecycler>,
    language: Language,
    onCallRecycler: (String) -> Unit,
    onOpenMap: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏭", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Authorized Recyclers & Aggregators"
                                    Language.HINDI -> "अधिकृत रीसायकलर एवं एग्रीगेटर"
                                    Language.MARATHI -> "अधिकृत रीसायकलर आणि संकलन केंद्रे"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "CPCB / SPCB registered with guaranteed electronic weighing scales",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMap() }
                            .testTag("open_map_from_tab_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "View Collection Points on Live Map"
                                    Language.HINDI -> "लाइव मैप पर संग्रहण केंद्र देखें"
                                    Language.MARATHI -> "थेट नकाशावर संकलन केंद्र पहा"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        items(recyclers) { rec ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recycler_card_${rec.recyclerId.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rec.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${rec.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = rec.cpcbRegNo + " • " + rec.authorizationValidity,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = TextSecondaryMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${rec.facilityLocation} (${rec.distanceKm} km away)",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (rec.doorstepPickup) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MintLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Doorstep Pickup Available (Min ${rec.minWeightForPickupKg.toInt()} kg)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instant Cash on Scale or UPI",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ForestGreenPrimary,
                            modifier = Modifier.clickable { onCallRecycler(rec.phone) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Call Recycler", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun LedgerAndEconomicsTab(
    transactions: List<TransactionLedgerEntity>,
    totalSettled: Double,
    pendingDues: Double,
    economicsList: List<com.example.model.UnitEconomicsData>,
    language: Language
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unit Economics assessment section
        item {
            UnitEconomicsCard(
                economicsList = economicsList,
                language = language
            )
        }

        // Ledger Header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Usable Financial Ledger & Receipts"
                        Language.HINDI -> "वित्तीय खाता एवं रसीदें"
                        Language.MARATHI -> "आर्थिक नोंदवही व पावत्या"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Documented proof of transaction for bank credit and formal identity",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }
        }

        items(transactions) { txn ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("txn_item_${txn.transactionId.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = txn.categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "₹${txn.totalAmountInr.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (txn.isSettled) SuccessGreen else WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${txn.weightKg} kg @ ₹${txn.ratePerKg.toInt()}/kg",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                        Text(
                            text = if (txn.isSettled) "PAID IN CASH" else "HANDOVER PENDING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (txn.isSettled) SuccessGreen else WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Recycler: ${txn.recyclerName} • ${dateFormat.format(Date(txn.timestamp))}",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun LocationConnectionsTab(
    recyclers: List<AuthorizedRecycler>,
    collectorLocation: CollectorLocationEntity?,
    nearbyCollectors: List<RemoteCollector>,
    connections: List<ConnectionRequestEntity>,
    quotations: List<QuotationEntity>,
    language: Language,
    onToggleSharing: (Boolean, Double, Double, String?) -> Unit,
    onRefreshNearby: () -> Unit,
    onRequestConnection: (AuthorizedRecycler) -> Unit,
    onRespondToQuotation: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val dateTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    // Resolve a coarse last-known location from the platform (fallback: live map defaults)
    val coarseLocation = remember {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        val provider = lm?.getProviders(true)?.firstOrNull()
        val loc = provider?.let { lm.getLastKnownLocation(it) }
        Pair(if (loc == null) 19.0760 else loc.latitude, if (loc == null) 72.8777 else loc.longitude)
    }
    val sharingOn = collectorLocation?.isSharingOn == true
    val lastAreaLabel = collectorLocation?.areaLabel

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Share My Location"
                                    Language.HINDI -> "मेरा स्थान साझा करें"
                                    Language.MARATHI -> "माझे ठिकाण शेअर करा"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Visible to nearby e-waste collectors only (no address / phone)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = sharingOn,
                            onCheckedChange = { share ->
                                onToggleSharing(share, coarseLocation.first, coarseLocation.second, lastAreaLabel)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (sharingOn) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "📍 ${"%.4f".format(coarseLocation.first)}, ${"%.4f".format(coarseLocation.second)}",
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (collectorLocation != null)
                                "Updated ${dateTimeFormat.format(Date(collectorLocation.updatedAt))}"
                            else when (language) {
                                Language.ENGLISH -> "Never shared yet"
                                Language.HINDI -> "अभी तक साझा नहीं किया"
                                Language.MARATHI -> "अजून शेअर केले नाही"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Nearby collectors discovered from the sharing registry
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Nearby Collectors (50 km)"
                            Language.HINDI -> "आसपास के संग्राहक (50 किमी)"
                            Language.MARATHI -> "जवळचे संकलक (50 किमी)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Aggregators optimise pickup routes automatically",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MintLight,
                    modifier = Modifier
                        .clickable { onRefreshNearby() }
                        .testTag("refresh_nearby_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Refresh",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }
        }

        if (nearbyCollectors.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Group, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sharingOn)
                                    when (language) {
                                        Language.ENGLISH -> "No other collectors sharing right now"
                                        Language.HINDI -> "अभी कोई अन्य संग्राहक शेयर नहीं कर रहा है"
                                        Language.MARATHI -> "आत्ता इतर संकलक शेअर करत नाहीत"
                                    }
                                else when (language) {
                                    Language.ENGLISH -> "Enable location sharing to see the network"
                                    Language.HINDI -> "नेटवर्क देखने के लिए स्थान साझा करना चालू करें"
                                    Language.MARATHI -> "नेटवर्क पाहण्यासाठी स्थान शेअरिंग सुरू करा"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                }
            }
        } else {
            items(nearbyCollectors) { collector ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MintLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "♻️", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = collector.area_label
                                    ?: when (language) {
                                        Language.ENGLISH -> "Nearby collector"
                                        Language.HINDI -> "आस-पास संग्राहक"
                                        Language.MARATHI -> "जवळचा संकलक"
                                    },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = buildString {
                                    val distance = com.example.data.CloudSyncManager.distanceKm(
                                        coarseLocation.first, coarseLocation.second,
                                        collector.latitude, collector.longitude
                                    )
                                    append(String.format(Locale.US, "%.1f km away", distance))
                                },
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        }
                        Icon(imageVector = Icons.Filled.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Connection requests and quotations header
        item {
            Text(
                text = when (language) {
                    Language.ENGLISH -> "Connections & Quotations"
                    Language.HINDI -> "संपर्क और कोटेशन"
                    Language.MARATHI -> "संपर्क आणि भाव पत्र"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (connections.isEmpty() && quotations.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Handshake, contentDescription = null, tint = TextSecondaryMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "No live requests. Reach out to a recycler below."
                                Language.HINDI -> "कोई सक्रिय अनुरोध नहीं। नीचे किसी रीसायकलर से संपर्क करें।"
                                Language.MARATHI -> "सक्रिय विनंती नाही. खालील रीसायकलरशी संपर्क साधा."
                            },
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }
        }

        items(connections) { conn ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conn.recyclerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        val statusColor = when (conn.status) {
                            "ACCEPTED" -> SuccessGreen
                            "REJECTED", "BLOCKED" -> ErrorRed
                            else -> WarningAmber
                        }
                        Text(
                            text = conn.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Text(
                        text = dateTimeFormat.format(Date(conn.createdAt)),
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        }

        items(quotations) { quote ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (quote.status == "PENDING") WarningAmber.copy(alpha = 0.6f) else MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quote.recyclerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "₹${quote.quotedRatePerKg.toInt()}/kg → ₹${quote.quotedTotalInr.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestGreenPrimary
                            )
                        }
                        if (quote.status == "PENDING") {
                            Row {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen,
                                    modifier = Modifier
                                        .clickable { onRespondToQuotation(quote.quotationId, true) }
                                        .testTag("accept_quote_${quote.quotationId}")
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Accept", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                                    modifier = Modifier
                                        .clickable { onRespondToQuotation(quote.quotationId, false) }
                                        .testTag("reject_quote_${quote.quotationId}")
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Reject", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = quote.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (quote.status == "ACCEPTED") SuccessGreen else TextSecondaryMuted
                            )
                        }
                    }
                }
            }
        }

        // Reach a recycler directly
        item {
            Text(
                text = when (language) {
                    Language.ENGLISH -> "Reach a Recycler"
                    Language.HINDI -> "रीसायकलर से संपर्क करें"
                    Language.MARATHI -> "रीसायकलरशी संपर्क साधा"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(recyclers) { rec ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rec.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Filled.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = rec.facilityLocation,
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    val alreadyRequested = connections.any { it.recyclerId == rec.recyclerId && it.status != "REJECTED" }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (alreadyRequested) MintLight else ForestGreenPrimary,
                        modifier = Modifier
                            .clickable(enabled = !alreadyRequested) { onRequestConnection(rec) }
                            .testTag("connect_to_recycler_${rec.recyclerId.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (alreadyRequested) Icons.Filled.Check else Icons.Filled.Handshake,
                                contentDescription = null,
                                tint = if (alreadyRequested) ForestGreenPrimary else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (alreadyRequested) "Requested" else "Connect",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alreadyRequested) ForestGreenPrimary else Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}
