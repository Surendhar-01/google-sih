package com.example.ui.collector

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.model.AuthorizedRecycler
import com.example.model.Language
import com.example.model.MaterialCategory
import com.example.model.MaterialLot
import com.example.model.PaymentMode
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber
import java.io.File

/** Editable component line produced by AI or added manually by the collector. */
private data class ComponentDraft(
    val name: String,
    val isUncertain: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateLotDialog(
    viewModel: CollectorDashboardViewModel,
    initialCategory: MaterialCategory? = null,
    availableRecyclers: List<AuthorizedRecycler>,
    language: Language,
    onDismiss: () -> Unit,
    onLotCreated: (
        category: MaterialCategory,
        subCategory: String,
        weightKg: Double,
        condition: String,
        location: String,
        gpsCoordinates: String,
        matchedRecycler: AuthorizedRecycler?,
        paymentMode: PaymentMode,
        draftLotId: String?
    ) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory ?: MaterialCategory.PCB_BOARDS) }
    var weightInput by remember { mutableStateOf("5.0") }
    var weightEdited by remember { mutableStateOf(false) }
    var conditionText by remember { mutableStateOf("Sorted & Clean Scrap") }
    var locationInput by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }
    var components by remember { mutableStateOf<List<ComponentDraft>>(emptyList()) }

    val currentWeight = weightInput.toDoubleOrNull() ?: 1.0
    val rankedMatch = com.example.ai.RecyclerMatcher.topMatch(selectedCategory, currentWeight, availableRecyclers)
    val matchedRecycler = rankedMatch?.recycler

    val duplicateHint by viewModel.duplicateHint.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val aiUnavailable by viewModel.aiUnavailableReason.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val aiSnapshot = aiAnalysis
    val unavailableSnapshot = aiUnavailable

    val prevailingRate = matchedRecycler?.buyingRates?.get(selectedCategory) ?: selectedCategory.defaultRatePerKg
    val estimatedTotalInr = currentWeight * prevailingRate
    val rateLabel = aiAnalysis?.let { a ->
        val min = a.priceMinPerKg
        val max = a.priceMaxPerKg
        if (min != null && max != null) "AI range ₹${min.toInt()}–₹${max.toInt()}/kg" else "Rate ₹${prevailingRate.toInt()}/kg"
    } ?: "Rate ₹${prevailingRate.toInt()}/kg"

    val context = LocalContext.current

    // Start a fresh creation session when this dialog opens.
    LaunchedEffect(Unit) {
        viewModel.beginCreationSession()
        viewModel.clearAiAnalysis()
        viewModel.refreshWorkbench()
        locationInput = viewModel.location.value?.areaLabel?.let { "$it, approx. area" } ?: ""
    }

    val currentSessionId = viewModel.draftLotId.value
    val sessionPhotos = viewModel.pendingPhotos.collectAsState().value
        .filter { currentSessionId == null || it.lotId == currentSessionId }

    // Keep the duplicate warning live whenever category/weight change.
    LaunchedEffect(selectedCategory, currentWeight) {
        viewModel.checkDuplicateCandidate(selectedCategory, currentWeight)
    }

    // When AI analysis arrives, prefill the editable summary + components + weight once.
    LaunchedEffect(aiAnalysis) {
        val analysis = aiAnalysis ?: return@LaunchedEffect
        if (!weightEdited && analysis.estimatedWeightKg != null && analysis.estimatedWeightKg > 0) {
            weightInput = "%.2f".format(analysis.estimatedWeightKg)
            weightEdited = true
        }
        components = analysis.components.map { ComponentDraft(it.name, it.isUncertain) }
    }

    fun cleanupAndDismiss() {
        sessionPhotos.forEach { viewModel.removeDraftPhoto(it.photoId) }
        viewModel.endCreationSession()
        onDismiss()
    }

    // Real device capture: gallery picker (max 10) and system camera via FileProvider.
    var lastCameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { viewModel.addDraftPhoto(it.toString()) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            lastCameraUri?.let { viewModel.addDraftPhoto(it.toString()) }
        }
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "photo_cache").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        lastCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val gpsCoordinates = viewModel.location.value?.let { "${it.latitude}, ${it.longitude}" } ?: ""

    Dialog(
        onDismissRequest = { cleanupAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .testTag("create_lot_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Create Digital E-Waste Lot"
                                Language.HINDI -> "डिजिटल ई-कचरा लॉट बनाएं"
                                Language.MARATHI -> "नवीन ई-कचरा लॉट तयार करा"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Real photos • AI prefill • Direct recycler match",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    IconButton(
                        onClick = { cleanupAndDismiss() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MintLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 1. Real Photo Capture (up to 10) — camera or gallery
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Photograph the Material"
                                    Language.HINDI -> "सामग्री की फोटो खींचें"
                                    Language.MARATHI -> "साहित्याचा फोटो काढा"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = "${sessionPhotos.size}/10",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sessionPhotos.size >= 10) WarningAmber else TextSecondaryMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (sessionPhotos.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sessionPhotos.forEach { photo ->
                                    Box {
                                        AsyncImage(
                                            model = photo.localUri,
                                            contentDescription = "Attached photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeDraftPhoto(photo.photoId) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (sessionPhotos.size < 10) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { launchCamera() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("capture_photo_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("gallery_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Photos are stored securely and uploaded to your private lot folder."
                                Language.HINDI -> "फोटो सुरक्षित रूप से आपके निजी लॉट फ़ोल्डर में अपलोड होते हैं।"
                                Language.MARATHI -> "फोटो तुमच्या खाजगी लॉट फोल्डरमध्ये सुरक्षित अपलोड होतात."
                            },
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                // 2. AI Analysis — real call, honest unavailable state
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, if (aiAnalysis != null || isAnalyzing) ForestGreenPrimary else MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (language) {
                                        Language.ENGLISH -> "AI Material Analysis"
                                        Language.HINDI -> "एआई सामग्री विश्लेषण"
                                        Language.MARATHI -> "एआय साहित्य विश्लेषण"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                            }
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ForestGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAnalyzing) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Inspecting photos… this may take a few seconds."
                                    Language.HINDI -> "फोटो की जांच हो रही है… कुछ सेकंड लगेंगे।"
                                    Language.MARATHI -> "फोटो तपासले जात आहेत… काही सेकंद लागतील."
                                },
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        } else if (aiSnapshot != null) {
                            Text(
                                text = aiSnapshot.summary,
                                fontSize = 12.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            aiSnapshot.priceMinPerKg?.let { min ->
                                aiSnapshot.priceMaxPerKg?.let { max ->
                                    Text(
                                        text = "Estimate range ₹${min.toInt()}–₹${max.toInt()}/kg",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary
                                    )
                                }
                            }
                            Text(
                                text = aiSnapshot.disclaimer,
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        } else if (unavailableSnapshot != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = unavailableSnapshot,
                                    fontSize = 11.sp,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Capture photos, then run AI to prefill the summary, weight and components."
                                    Language.HINDI -> "फोटो लें, फिर एआई से सारांश, वजन और घटक भरें।"
                                    Language.MARATHI -> "फोटो घ्या, नंतर एआय सारांश, वजन व घटक भरेल."
                                },
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.analyzeDraftPhotos(selectedCategory, language) },
                            enabled = sessionPhotos.isNotEmpty() && !isAnalyzing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintLight, disabledContainerColor = Color(0xFFEEEEEE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("analyze_photos_btn")
                        ) {
                            Text(
                                text = if (aiAnalysis != null)
                                    (when (language) {
                                        Language.ENGLISH -> "Re-analyze Photos"
                                        Language.HINDI -> "फोटो दोबारा जांचें"
                                        Language.MARATHI -> "पुन्हा फोटो तपासा"
                                    })
                                else
                                    (when (language) {
                                        Language.ENGLISH -> "Analyze Photos with AI"
                                        Language.HINDI -> "एआई से फोटो विश्लेषण करें"
                                        Language.MARATHI -> "एआयने फोटो विश्लेषण करा"
                                    }),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (sessionPhotos.isNotEmpty()) ForestGreenPrimary else TextSecondaryMuted
                            )
                        }
                    }
                }

                // 3. Duplicate prevention warning (real, from recent identical lots)
                duplicateHint?.let { dup ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, WarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${dup.weightKg} kg ${dup.category.getTitle(language)} lot already created recently. Please verify you are not entering a duplicate.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF8A5A00)
                                )
                                Text(
                                    text = "Lot ${dup.lotId} • ${dup.collectionTimestamp}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }
                }

                // 4. Material Category Selector (large visual buttons)
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Select Material Category"
                            Language.HINDI -> "सामग्री का प्रकार चुनें"
                            Language.MARATHI -> "साहित्याचा प्रकार निवडा"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (cat in MaterialCategory.values()) {
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) ForestGreenPrimary else MintLight,
                                border = BorderStroke(1.dp, if (isSelected) ForestGreenPrimary else MintBorder),
                                modifier = Modifier
                                    .clickable { selectedCategory = cat }
                                    .testTag("lot_cat_${cat.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat.iconEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.getTitle(language),
                                        color = if (isSelected) Color.White else TextPrimaryDark,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Approximate Weight (editable; AI prefill highlighted as estimate)
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Weight (Kilograms)"
                            Language.HINDI -> "वजन (किलोग्राम)"
                            Language.MARATHI -> "वजन (किलो)"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                weightEdited = true
                                val w = (currentWeight - 1.0).coerceAtLeast(0.5)
                                weightInput = String.format("%.1f", w)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintLight)
                                .testTag("weight_minus")
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = ForestGreenPrimary)
                        }

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                weightEdited = true
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weight_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = ForestGreenPrimary
                            ),
                            suffix = { Text("kg", fontWeight = FontWeight.Bold) }
                        )

                        IconButton(
                            onClick = {
                                weightEdited = true
                                val w = currentWeight + 1.0
                                weightInput = String.format("%.1f", w)
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MintLight)
                                .testTag("weight_plus")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = ForestGreenPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2.0, 5.0, 10.0, 25.0, 50.0).forEach { presetKg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MintLight,
                                border = BorderStroke(1.dp, MintBorder),
                                modifier = Modifier
                                    .clickable {
                                        weightEdited = true
                                        weightInput = presetKg.toString()
                                    }
                            ) {
                                Text(
                                    text = "${presetKg.toInt()} kg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 6. Editable components (from AI or manual)
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Components & Items in Lot"
                            Language.HINDI -> "लॉट में घटक और वस्तुएं"
                            Language.MARATHI -> "लॉटमधील घटक व वस्तू"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    components.forEachIndexed { index, component ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = component.name,
                                onValueChange = { newName ->
                                    components = components.toMutableList().also { it[index] = it[index].copy(name = newName) }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                leadingIcon = if (component.isUncertain) {
                                    {
                                        Text(
                                            text = "?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmber,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                } else null
                            )
                            IconButton(
                                onClick = { components = components.toMutableList().also { it.removeAt(index) } },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFB00020), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Button(
                        onClick = { components = components + ComponentDraft("", false) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Add another component"
                                    Language.HINDI -> "एक और घटक जोड़ें"
                                    Language.MARATHI -> "आणखी एक घटक जोडा"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                }

                // 7. Instant estimate (AI range if available, otherwise board rate)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MintLight),
                    border = BorderStroke(1.5.dp, ForestGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Estimated Payout"
                                    Language.HINDI -> "अनुमानित भुगतान"
                                    Language.MARATHI -> "अंदाजे देय रक्कम"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = rateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${estimatedTotalInr.toInt()}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Estimate — verify at scale"
                                    Language.HINDI -> "अनुमान — तौल पर सत्यापित करें"
                                    Language.MARATHI -> "अंदाज — वजनावर पडताळा करा"
                                },
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Text(
                            text = "Recovered: " + selectedCategory.keyRecoverableMetals.take(3).joinToString(", "),
                            fontSize = 11.sp,
                            color = ForestGreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 8. Matched Authorized Recycler
                if (matchedRecycler != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MintBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Top Matched Recycler",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                                Text(
                                    text = "${matchedRecycler.distanceKm} km away",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestGreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = matchedRecycler.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = matchedRecycler.facilityLocation + " • CPCB Valid",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                            rankedMatch?.reasons?.take(2)?.let { reasons ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reasons.joinToString(" • "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestGreenDark
                                )
                            }
                        }
                    }
                }

                // 9. Payment Preference (Instant Cash vs UPI)
                Column {
                    Text(
                        text = "Preferred Payment Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPaymentMode == PaymentMode.CASH) ForestGreenPrimary else MintLight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaymentMode = PaymentMode.CASH }
                        ) {
                            Text(
                                text = "💵 Cash on Scale",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPaymentMode == PaymentMode.CASH) Color.White else TextPrimaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPaymentMode == PaymentMode.UPI) ForestGreenPrimary else MintLight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaymentMode = PaymentMode.UPI }
                        ) {
                            Text(
                                text = "📱 Direct UPI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPaymentMode == PaymentMode.UPI) Color.White else TextPrimaryDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                // 10. Submit Button
                Button(
                    onClick = {
                        val subCategory = if (aiSnapshot != null) {
                            (aiSnapshot.summary.take(200) + " ${selectedCategory.getTitle(language)}").trim()
                        } else {
                            components.joinToString(", ") { it.name.replace("\n", " ") }.take(200)
                                .ifBlank { selectedCategory.getTitle(language) }
                        }
                        onLotCreated(
                            selectedCategory,
                            subCategory.ifBlank { selectedCategory.getTitle(language) },
                            currentWeight,
                            conditionText,
                            locationInput,
                            gpsCoordinates,
                            matchedRecycler,
                            selectedPaymentMode,
                            viewModel.draftLotId.value
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_create_lot_btn")
                ) {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Confirm & Generate Handover Lot"
                            Language.HINDI -> "पुष्टि करें एवं डिजिटल लॉट बनाएं"
                            Language.MARATHI -> "पुष्टी करा व डिजिटल लॉट तयार करा"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}