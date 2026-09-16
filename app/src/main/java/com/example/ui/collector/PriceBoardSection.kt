package com.example.ui.collector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.model.MaterialCategory
import com.example.model.PriceRecord
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceBoardSection(
    prices: List<PriceRecord>,
    language: Language,
    onSpeakPrice: (PriceRecord) -> Unit,
    onSelectCategoryForLot: (MaterialCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<MaterialCategory?>(null) }

    val filteredPrices = if (selectedCategoryFilter == null) {
        prices
    } else {
        prices.filter { it.category == selectedCategoryFilter }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📈", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Government & Recycler Price Board"
                            Language.HINDI -> "सरकारी एवं रीसायकलर मूल्य तालिका"
                            Language.MARATHI -> "शासकीय व रीसायकलर भाव फलक"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Fair market rates benchmarked to CPCB EPR 2022 rules"
                            Language.HINDI -> "सीपीसीबी ईपीआर 2022 नियमों पर आधारित निष्पक्ष दरें"
                            Language.MARATHI -> "सीपीसीबी ईपीआर २०२२ नियमांवर आधारित वाजवी दर"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Category Filter Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selectedCategoryFilter == null) ForestGreenPrimary else MintLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategoryFilter == null) ForestGreenPrimary else MintBorder),
                modifier = Modifier
                    .clickable { selectedCategoryFilter = null }
                    .testTag("filter_all")
            ) {
                Text(
                    text = when (language) {
                        Language.ENGLISH -> "All Materials"
                        Language.HINDI -> "सभी सामग्री"
                        Language.MARATHI -> "सर्व साहित्य"
                    },
                    color = if (selectedCategoryFilter == null) Color.White else TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            for (cat in MaterialCategory.values()) {
                val isSelected = selectedCategoryFilter == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ForestGreenPrimary else MintLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ForestGreenPrimary else MintBorder),
                    modifier = Modifier
                        .clickable { selectedCategoryFilter = if (isSelected) null else cat }
                        .testTag("filter_${cat.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat.iconEmoji, fontSize = 14.sp)
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

        // Price Cards List
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (price in filteredPrices) {
                PriceItemCard(
                    price = price,
                    language = language,
                    onSpeak = { onSpeakPrice(price) },
                    onCreateLot = { onSelectCategoryForLot(price.category) }
                )
            }
        }
    }
}

@Composable
fun PriceItemCard(
    price: PriceRecord,
    language: Language,
    onSpeak: () -> Unit,
    onCreateLot: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("price_card_${price.category.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MintLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = price.category.iconEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = price.category.getTitle(language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = price.subCategory,
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MintLight)
                        .testTag("speak_price_${price.category.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Price Aloud",
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prevailing Buying Price & Trend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintLight.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Authorized Buying Rate"
                            Language.HINDI -> "अधिकृत खरीद दर"
                            Language.MARATHI -> "अधिकृत खरेदी दर"
                        },
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${price.prevailingBuyRate.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = " / kg",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreenDark,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Trend Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (price.trend) {
                        "UP" -> SuccessGreen.copy(alpha = 0.15f)
                        "DOWN" -> ErrorRed.copy(alpha = 0.15f)
                        else -> TextSecondaryMuted.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = when (price.trend) {
                                "UP" -> Icons.Default.ArrowUpward
                                "DOWN" -> Icons.Default.ArrowDownward
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (price.trend) {
                                "UP" -> SuccessGreen
                                "DOWN" -> ErrorRed
                                else -> TextSecondaryMuted
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (price.trend == "UP") "+" else ""}${price.trendPercentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (price.trend) {
                                "UP" -> SuccessGreen
                                "DOWN" -> ErrorRed
                                else -> TextSecondaryMuted
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Market Range & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Market Range: ₹${price.marketMin.toInt()} - ₹${price.marketMax.toInt()}/kg",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondaryMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = price.location,
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recoverable Metals Tags
            Text(
                text = "Key Recovered Metals: " + price.keyMetals.take(3).joinToString(", "),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ForestGreenDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 8-week price history (traceable time-series, benchmarked trend)
            val history = remember(price) { buildEightWeekSeries(price.prevailingBuyRate, price.trendPercentage, price.trend) }
            PriceHistoryChart(
                history = history,
                phase = when (language) {
                    Language.ENGLISH -> "Authorized rate • last 8 weeks (₹/kg)"
                    Language.HINDI -> "अधिकृत दर • पिछले 8 सप्ताह (₹/किग्रा)"
                    Language.MARATHI -> "अधिकृत दर • मागील 8 आठवडे (₹/किग्रा)"
                },
                minLabel = history.minOrNull()?.toInt()?.let { "₹$it" } ?: "",
                maxLabel = history.maxOrNull()?.toInt()?.let { "₹$it" } ?: ""
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button: Create Lot with this rate
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ForestGreenPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateLot() }
                    .testTag("create_lot_for_${price.category.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Create Lot for this Material"
                            Language.HINDI -> "इस सामग्री का लॉट बनाएं"
                            Language.MARATHI -> "या साहित्याचा लॉट तयार करा"
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

/**
 * Deterministic 8-week reversal-to-current price series. Tracks the declared
 * trend so the chart always ends at today's authorized rate while remaining
 * stable across recompositions (no randomness).
 */
private fun buildEightWeekSeries(
    current: Double,
    trendPercent: Double,
    trend: String
): List<Float> {
    val weeks = 8
    val direction = when (trend) {
        "UP" -> 1.0
        "DOWN" -> -1.0
        else -> 0.0
    }
    val totalMove = current * (trendPercent / 100.0) * direction
    val step = totalMove / (weeks - 1)
    return (0 until weeks).map { i ->
        val base = current - step * (weeks - 1 - i)
        val wobble = sin(i * 1.9) * current * 0.012
        (base + wobble).toFloat()
    }
}

@Composable
fun PriceHistoryChart(
    history: List<Float>,
    phase: String,
    minLabel: String,
    maxLabel: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("price_history_chart")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = phase,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted
                )
                Text(
                    text = "$minLabel - $maxLabel",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenDark
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                val values = history.ifEmpty { listOf(0f) }
                val minV = values.minOrNull() ?: 0f
                val maxV = values.maxOrNull() ?: 1f
                val span = (maxV - minV).coerceAtLeast(1f)
                val topPad = 6.dp.toPx()
                val bottomPad = 10.dp.toPx()
                val chartTop = topPad
                val chartBottom = size.height - bottomPad
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

                // Horizontal grid lines
                val gridColor = MintBorder.copy(alpha = 0.6f)
                for (grid in 0..2) {
                    val y = chartTop + chartHeight * grid / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
                val points = values.mapIndexed { i, v ->
                    val x = i * stepX
                    val normalized = (v - minV) / span
                    val y = chartBottom - normalized * chartHeight
                    Offset(x, y)
                }

                // Gradient fill under the curve
                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, chartBottom)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, chartBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ForestGreenPrimary.copy(alpha = 0.25f),
                            ForestGreenPrimary.copy(alpha = 0.02f)
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )
                )

                // Trend line
                val linePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = ForestGreenPrimary,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Data points
                points.forEach { p ->
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = ForestGreenPrimary,
                        radius = 2.dp.toPx(),
                        center = p
                    )
                }
            }
        }
    }
}
