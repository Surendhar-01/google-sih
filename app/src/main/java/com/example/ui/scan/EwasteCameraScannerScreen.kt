package com.example.ui.scan

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ai.EwasteScanResult
import com.example.ai.GeminiScannerClient
import com.example.model.Language
import com.example.notification.EwasteNotificationHelper
import com.example.notification.ReminderType
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EwasteCameraScannerScreen(
    language: Language,
    onBack: () -> Unit,
    onAcceptLotToDashboard: ((EwasteScanResult) -> Unit)? = null,
    onSpeakText: ((String, Language) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is needed to scan e-waste items.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var capturedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var scanResults by remember { mutableStateOf<List<EwasteScanResult>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showReminderScheduledDialog by remember { mutableStateOf(false) }

    // POST_NOTIFICATIONS runtime permission (Android 13+) so scheduled reminders can be shown.
    var pendingSchedule by remember { mutableStateOf<Pair<ReminderType, String?>?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingSchedule
        pendingSchedule = null
        if (granted && pending != null) {
            disburseSchedule(context, pending.first, language, pending.second)
            showReminderScheduledDialog = true
        } else if (!granted) {
            Toast.makeText(context, "Notification permission is needed to show pickup reminders.", Toast.LENGTH_LONG).show()
        }
    }

    fun requestSchedule(type: ReminderType, customDateText: String? = null) {
        if (!EwasteNotificationHelper.notificationsEnabled(context)) {
            pendingSchedule = type to customDateText
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            disburseSchedule(context, type, language, customDateText)
            showReminderScheduledDialog = true
        }
    }

    fun analyzeUris(uris: List<Uri>) {
        coroutineScope.launch {
            isAnalyzing = true
            val bitmaps = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                }
            }
            if (bitmaps.isNotEmpty()) {
                capturedImages = capturedImages + bitmaps
                // Analyze every photo (in parallel on IO) with the Gemini AI scanner.
                val results = withContext(Dispatchers.IO) {
                    bitmaps.map { bitmap ->
                        async { GeminiScannerClient.analyzeEwasteImage(bitmap, language) }
                    }.awaitAll().mapNotNull { it.getOrNull() }
                }
                scanResults = scanResults + results
            }
            isAnalyzing = false
        }
    }

    // Multi-photo picker: select one or MANY images in a single selection.
    val addPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            analyzeUris(uris)
        }
    }

    fun clearScan() {
        capturedImages = emptyList()
        scanResults = emptyList()
        isAnalyzing = false
    }

    // Scanner Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "AI E-Waste Visual Scanner"
                                Language.HINDI -> "AI ई-कचरा स्कैनर"
                                Language.MARATHI -> "AI ई-कचरा स्कॅनर"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Gemini AI Item & Hazard Identifier",
                            fontSize = 11.sp,
                            color = ForestGreenDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("scan_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            flashEnabled = !flashEnabled
                            imageCapture?.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        },
                        modifier = Modifier.testTag("scan_flash_toggle")
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash Toggle",
                            tint = if (flashEnabled) WarningAmber else ForestGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (hasCameraPermission && capturedImages.isEmpty()) {
                // Live CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                            } catch (exc: Exception) {
                                // Camera binding failed
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize().testTag("camerax_preview_view")
                )

                // Viewfinder Target Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(2.dp, EmeraldAccent, RoundedCornerShape(24.dp))
                            .drawBehind {
                                val yOffset = size.height * laserY
                                drawLine(
                                    color = EmeraldAccent.copy(alpha = 0.9f),
                                    start = Offset(10f, yOffset),
                                    end = Offset(size.width - 10f, yOffset),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                    )

                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Point camera at circuit board, battery, phone or wires"
                            Language.HINDI -> "कैमरे को ई-कचरे (सर्किट, बैटरी, फोन) के सामने रखें"
                            Language.MARATHI -> "ई-कचऱ्यावर कॅमेरा रोखा"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 132.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                // Camera Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "+" Add Multiple Photos from Gallery
                    IconButton(
                        onClick = { addPhotosLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(ForestGreenPrimary)
                            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                            .testTag("btn_add_photos")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Multiple Photos",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Capture Shutter Button
                    Surface(
                        shape = CircleShape,
                        color = ForestGreenPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable {
                                val capture = imageCapture ?: return@clickable
                                isAnalyzing = true

                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            val single = listOf(bitmap)
                                            capturedImages = capturedImages + single

                                            coroutineScope.launch {
                                                val result = GeminiScannerClient.analyzeEwasteImage(bitmap, language)
                                                result.onSuccess {
                                                    scanResults = scanResults + it
                                                }
                                                isAnalyzing = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isAnalyzing = false
                                            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            .testTag("btn_shutter_capture")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Quick Reminder Notification Trigger
                    IconButton(
                        onClick = {
                            requestSchedule(ReminderType.NEARBY_COLLECTION_DRIVE)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .testTag("btn_schedule_reminder_quick")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Set Collection Reminder",
                            tint = WarningAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else if (!hasCameraPermission) {
                // Permission Request Fallback UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📷", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ForestGreenPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ECOBRIDGES uses the camera with Gemini AI to recognize electronic components, assess toxicity hazards, and give step-by-step safe disposal advice.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = TextSecondaryMuted
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_request_camera_perm")
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { addPhotosLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pick Images from Gallery Instead")
                    }
                }
            }

            // Results Sheet / Card Overlay
            if (capturedImages.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Header with preview strip and actions
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(
                                        text = "AI Visual Analysis",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ForestGreenPrimary
                                    )
                                    Text(
                                        text = when {
                                            isAnalyzing -> "Processing ${capturedImages.size} photo(s) with Gemini AI..."
                                            else -> "${scanResults.size} of ${capturedImages.size} identified"
                                        },
                                        fontSize = 12.sp,
                                        color = if (isAnalyzing) WarningAmber else SuccessGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row {
                                // "+" Add more photos after scanning
                                IconButton(
                                    onClick = { addPhotosLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .testTag("btn_add_more_photos")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add More Photos",
                                        tint = ForestGreenPrimary
                                    )
                                }
                                // Add selected photos from gallery directly
                                IconButton(
                                    onClick = { addPhotosLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .testTag("btn_rescan_again")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Scan Another", tint = ForestGreenPrimary)
                                }
                            }
                        }

                        // Thumbnail strip of all chosen images
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_thumbnail_strip")
                        ) {
                            items(capturedImages.size) { idx ->
                                Image(
                                    bitmap = capturedImages[idx].asImageBitmap(),
                                    contentDescription = "Captured e-waste image $idx",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MintBorder, RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isAnalyzing) {
                        // Analyzing Loading Animation
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MintLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = ForestGreenPrimary, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Analyzing heavy metals, board grade, and recovery instructions...",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (scanResults.isNotEmpty()) {
                        // Aggregate summary when multiple photos were analyzed
                        val totalWeight = scanResults.sumOf { it.estimatedWeightKg }
                        val totalValue = scanResults.sumOf { it.estimatedWeightKg * it.estimatedMarketPricePerKg }
                        val anyHazard = scanResults.any { it.isHazardous }

                        if (scanResults.size > 1) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MintLight),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("scan_summary_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Scanned Items", fontSize = 10.sp, color = TextSecondaryMuted)
                                        Text("${scanResults.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryDark)
                                    }
                                    Column {
                                        Text("Est. Total Weight", fontSize = 10.sp, color = TextSecondaryMuted)
                                        Text("${"%.2f".format(totalWeight)} kg", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryDark)
                                    }
                                    Column {
                                        Text("Est. Total Value", fontSize = 10.sp, color = TextSecondaryMuted)
                                        Text("₹${totalValue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuccessGreen)
                                    }
                                }
                            }
                            if (anyHazard) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (language) {
                                                Language.ENGLISH -> "${scanResults.count { it.isHazardous }} scanned item(s) contain hazardous materials. Follow the disposal steps below or on each card."
                                                Language.HINDI -> "स्कैन की गई ${scanResults.count { it.isHazardous }} वस्तुओं में खतरनाक पदार्थ हैं। नीचे दिए गए निस्तारण चरणों का पालन करें।"
                                                Language.MARATHI -> "स्कॅन केलेल्या ${scanResults.count { it.isHazardous }} वस्तूंमध्ये धोकादायक पदार्थ आहेत. खालील विल्हेवाट पायऱ्या पाळा."
                                            },
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // One identification card per scanned/analyzed photo
                        scanResults.forEachIndexed { idx, res ->
                            ScanResultCard(
                                result = res,
                                index = idx,
                                language = language,
                                onSpeak = { textToSpeak ->
                                    onSpeakText?.invoke(textToSpeak, language)
                                },
                                onScheduleReminder = {
                                    requestSchedule(
                                        type = ReminderType.MONTHLY_DOORSTEP_PICKUP,
                                        customDateText = res.itemName
                                    )
                                },
                                onAddToLots = {
                                    onAcceptLotToDashboard?.invoke(res)
                                    onBack()
                                }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            // Scheduled Reminder Confirmation Dialog
            if (showReminderScheduledDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showReminderScheduledDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ForestGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pickup Reminder Scheduled", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Text(
                            "Local notification has been registered! You will receive reminders for nearby authorized collection drives and scheduled doorstep weight pickups.",
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showReminderScheduledDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanResultCard(
    result: EwasteScanResult,
    index: Int,
    language: Language,
    onSpeak: (String) -> Unit,
    onScheduleReminder: () -> Unit,
    onAddToLots: () -> Unit
) {
    // AI Identification Details Card
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_result_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Item Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${result.identifiedCategory.iconEmoji} ${result.identifiedCategory.getTitle(language)}",
                        fontSize = 12.sp,
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Voice TTS button for the scanned analysis
                Surface(
                    shape = CircleShape,
                    color = MintLight,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            val textToSpeak = "${result.itemName}. ${result.hazardsSummary}. " + result.stepByStepDisposalInstructions.joinToString(". ")
                            onSpeak(textToSpeak)
                        }
                        .testTag("btn_speak_scan_result")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Valuation & Weight Estimate Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MintLight, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Scanned Item", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text("Item #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreenPrimary)
                }
                Column {
                    Text("Est. Benchmark Rate", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text("₹${result.estimatedMarketPricePerKg.toInt()} / kg", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ForestGreenPrimary)
                }
                Column {
                    Text("Typical Unit Weight", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text("${result.estimatedWeightKg} kg", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                }
                Column {
                    Text("Estimated Lot Value", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text("₹${(result.estimatedWeightKg * result.estimatedMarketPricePerKg).toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hazard Alert Pill
            if (result.isHazardous) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hazardous Material Warning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ErrorRed
                            )
                            Text(
                                text = result.hazardsSummary,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Step-by-Step Safe Disposal Instructions
            Text(
                text = "Step-by-Step Safe Disposal Instructions:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            result.stepByStepDisposalInstructions.forEachIndexed { stepIdx, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ForestGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${stepIdx + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = step,
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Add directly as lot or schedule pickup reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Schedule Pickup Reminder
                OutlinedButton(
                    onClick = onScheduleReminder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_schedule_from_scan")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Reminder", fontSize = 12.sp, color = ForestGreenPrimary)
                }

                // Add to My Lots
                Button(
                    onClick = onAddToLots,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_create_lot_from_scan")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Lots", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Convert CameraX ImageProxy to Android Bitmap with proper rotation
private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun disburseSchedule(
    context: Context,
    type: ReminderType,
    language: Language,
    customDateText: String?
) {
    EwasteNotificationHelper.schedulePresetReminder(
        context = context,
        type = type,
        language = language,
        customDateText = customDateText
    )
}