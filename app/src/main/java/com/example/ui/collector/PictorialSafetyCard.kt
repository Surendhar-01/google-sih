package com.example.ui.collector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HazardSafetyInfo
import com.example.model.Language
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

/**
 * Dedicated pictorial and audio-based safety guidance component
 * displayed prominently on the informal collector's main dashboard.
 */
@Composable
fun PictorialSafetyGuidanceCard(
    safetyItems: List<HazardSafetyInfo>,
    language: Language,
    onSpeakAllSafety: () -> Unit,
    onSpeakItem: (HazardSafetyInfo) -> Unit,
    onNavigateToSafetyTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedHazardId by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pictorial_safety_dashboard_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row with Pictorial Badge & Audio Read-Aloud Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(WarningAmber.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Safety Guide",
                            tint = WarningAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Hazardous Waste Safety"
                                    Language.HINDI -> "खतरनाक ई-कचरा सुरक्षा"
                                    Language.MARATHI -> "धोकादायक कचरा सुरक्षा"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ErrorRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "PICTORIAL & AUDIO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ErrorRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Don't Burn or Leach • Tap icons for audio"
                                Language.HINDI -> "कचरा न जलाएं • ऑडियो सुनने के लिए टैप करें"
                                Language.MARATHI -> "जाळू नका, ऍसिड नको • ऑडिओसाठी टॅप करा"
                            },
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                // Primary Audio Button: Listen to complete safety audio
                IconButton(
                    onClick = onSpeakAllSafety,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MintLight)
                        .testTag("btn_speak_all_safety")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to safety rules audio",
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Pictorial Hazard Warning Tiles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val chunkedItems = safetyItems.take(4).chunked(2)
                for (row in chunkedItems) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in row) {
                            val isExpanded = expandedHazardId == item.id
                            PictorialHazardTile(
                                item = item,
                                language = language,
                                isExpanded = isExpanded,
                                onTileClick = {
                                    expandedHazardId = if (isExpanded) null else item.id
                                },
                                onAudioClick = {
                                    onSpeakItem(item)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Expanded Detail View when an informal collector taps a hazard tile
            AnimatedVisibility(visible = expandedHazardId != null) {
                val activeItem = safetyItems.find { it.id == expandedHazardId }
                if (activeItem != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Dangerous,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeItem.practiceTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ErrorRed
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeakItem(activeItem) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak detail",
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ " + activeItem.whyUnsafe,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MintLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✅ SAFE METHOD: " + activeItem.safeFormalAlternative,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ForestGreenDark,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Navigation to complete Safety Manual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintLight.copy(alpha = 0.7f))
                    .clickable { onNavigateToSafetyTab() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("btn_open_safety_tab_from_dashboard"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Open Full Visual Safety & Economics Manual"
                            Language.HINDI -> "पूर्ण सुरक्षा एवं कमाई पुस्तिका देखें"
                            Language.MARATHI -> "संपूर्ण सुरक्षा व नफा पुस्तिका उघडा"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ForestGreenDark
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Safety Tab",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Individual Pictorial Hazard Tile with custom vector visual illustration,
 * prohibited cross badge, title, and audio playback button.
 */
@Composable
fun PictorialHazardTile(
    item: HazardSafetyInfo,
    language: Language,
    isExpanded: Boolean,
    onTileClick: () -> Unit,
    onAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) WarningAmber.copy(alpha = 0.1f) else Color(0xFFFBFBFB)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) WarningAmber else MintBorder
        ),
        modifier = modifier
            .clickable { onTileClick() }
            .testTag("pictorial_tile_${item.id.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pictorial Canvas Diagram with Danger Icon and Prohibited Slash
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, ErrorRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                HazardCanvasIllustration(
                    hazardId = item.id,
                    iconEmoji = item.iconEmoji,
                    modifier = Modifier.size(54.dp)
                )

                // Top right prohibited mini badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(ErrorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Prohibited Label
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ErrorRed.copy(alpha = 0.12f)
            ) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "DO NOT DO"
                        Language.HINDI -> "मना है"
                        Language.MARATHI -> "कधीही करू नका"
                    },
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Short title
            val shortTitle = when (item.id) {
                "HAZ-01" -> if (language == Language.ENGLISH) "Burning Cables" else if (language == Language.HINDI) "तार जलाना" else "वायर जाळणे"
                "HAZ-02" -> if (language == Language.ENGLISH) "Acid Leaching" else if (language == Language.HINDI) "एसिड में गलाना" else "ऍसिड वापरणे"
                "HAZ-03" -> if (language == Language.ENGLISH) "CRT Smashing" else if (language == Language.HINDI) "स्क्रीन तोड़ना" else "स्क्रीन फोडणे"
                "HAZ-04" -> if (language == Language.ENGLISH) "Crushing Lithium" else if (language == Language.HINDI) "बैटरी फोड़ना" else "बॅटरी फोडणे"
                else -> item.practiceTitle.take(15)
            }

            Text(
                text = shortTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = TextPrimaryDark,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Audio Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MintLight)
                    .clickable { onAudioClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Listen",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "Audio"
                        Language.HINDI -> "सुनें"
                        Language.MARATHI -> "ऐका"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
            }
        }
    }
}

/**
 * Custom vector pictorial graphic drawing that illustrates the specific hazard
 * with color-coded warning graphics and a diagonal prohibited strike.
 */
@Composable
fun HazardCanvasIllustration(
    hazardId: String,
    iconEmoji: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Render custom background canvas for danger graphics
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.88f

            // Danger halo background circle
            drawCircle(
                color = Color(0xFFFFEBEE),
                radius = radius,
                center = center
            )

            // Red warning outline ring
            drawCircle(
                color = Color(0xFFE53935),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Prohibited diagonal strike-through bar
            val angleRad = Math.toRadians(45.0)
            val startX = center.x - (radius * Math.cos(angleRad)).toFloat()
            val startY = center.y - (radius * Math.sin(angleRad)).toFloat()
            val endX = center.x + (radius * Math.cos(angleRad)).toFloat()
            val endY = center.y + (radius * Math.sin(angleRad)).toFloat()

            drawLine(
                color = Color(0xFFE53935),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center visual icon emoji
        Text(
            text = iconEmoji,
            fontSize = 26.sp
        )
    }
}
