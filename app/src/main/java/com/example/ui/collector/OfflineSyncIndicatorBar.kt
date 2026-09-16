package com.example.ui.collector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated sync indicator and manual synchronization control bar
 * for the offline-first data layer (Room DB + CPCB cloud sync).
 */
@Composable
fun OfflineSyncIndicatorBar(
    isOffline: Boolean,
    isSyncing: Boolean,
    unsyncedCount: Int,
    lastSyncTimestamp: Long,
    language: Language,
    onToggleOffline: () -> Unit,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val lastSyncStr = timeFormat.format(Date(lastSyncTimestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline) WarningAmber.copy(alpha = 0.08f) else MintLight.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOffline) WarningAmber.copy(alpha = 0.35f) else MintBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_sync_indicator_bar")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon and Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSyncing) MintLight
                                else if (isOffline) WarningAmber.copy(alpha = 0.2f)
                                else SuccessGreen.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = ForestGreenPrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(rotationAngle)
                            )
                        } else if (isOffline) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Offline Mode",
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSyncing) {
                                    when (language) {
                                        Language.ENGLISH -> "Syncing with Cloud..."
                                        Language.HINDI -> "क्लाउड से सिंक हो रहा है..."
                                        Language.MARATHI -> "क्लाउडवर सिंक होत आहे..."
                                    }
                                } else if (isOffline) {
                                    when (language) {
                                        Language.ENGLISH -> "Offline Storage Active"
                                        Language.HINDI -> "ऑफलाइन मोड सक्रिय"
                                        Language.MARATHI -> "ऑफलाइन मोड सुरू"
                                    }
                                } else if (unsyncedCount > 0) {
                                    when (language) {
                                        Language.ENGLISH -> "$unsyncedCount Lots Queued Offline"
                                        Language.HINDI -> "$unsyncedCount लॉट सिंक बाकी"
                                        Language.MARATHI -> "$unsyncedCount लॉट्स सिंक बाकी"
                                    }
                                } else {
                                    when (language) {
                                        Language.ENGLISH -> "CPCB Database Synced"
                                        Language.HINDI -> "डेटाबेस पूर्ण सिंक है"
                                        Language.MARATHI -> "डेटाबेस पूर्ण सिंक आहे"
                                    }
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isOffline) WarningAmber else ForestGreenDark
                            )

                            if (unsyncedCount > 0 && !isOffline) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = WarningAmber
                                ) {
                                    Text(
                                        text = "$unsyncedCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isOffline) {
                                when (language) {
                                    Language.ENGLISH -> "Saved locally in SQLite. Will sync on reconnect."
                                    Language.HINDI -> "लोकल रूम डेटाबेस में सुरक्षित। कनेक्शन पर सिंक होगा।"
                                    Language.MARATHI -> "स्थानिक रूम डेटाबेसमध्ये सुरक्षित. जोडणीवर सिंक होईल."
                                }
                            } else {
                                when (language) {
                                    Language.ENGLISH -> "Last synced at $lastSyncStr • Offline-First active"
                                    Language.HINDI -> "अंतिम सिंक समय: $lastSyncStr • ऑफलाइन-फर्स्ट तैयार"
                                    Language.MARATHI -> "शेवटचे सिंक: $lastSyncStr • ऑफलाइन-फर्स्ट सक्रिय"
                                }
                            },
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                // Manual Synchronization Action Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOffline) Color.LightGray.copy(alpha = 0.4f) else ForestGreenPrimary,
                    modifier = Modifier
                        .clickable(enabled = !isOffline && !isSyncing) { onManualSync() }
                        .testTag("btn_manual_sync_now")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Manual Sync",
                            tint = if (isOffline) Color.DarkGray else Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> if (isSyncing) "Syncing" else "Sync Now"
                                Language.HINDI -> if (isSyncing) "सिंक जारी" else "मैन्युअल सिंक"
                                Language.MARATHI -> if (isSyncing) "सिंक सुरू" else "मॅन्युअल सिंक"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOffline) Color.DarkGray else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Controls: Connectivity Simulator Toggle Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOffline) "📴" else "📶",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Network Connectivity Mode"
                                Language.HINDI -> "नेटवर्क कनेक्टिविटी मोड"
                                Language.MARATHI -> "नेटवर्क कनेक्टिव्हिटी मोड"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isOffline) "Simulated Offline (Room DB Cache)" else "Simulated Online (Cloud Sync Enabled)",
                            fontSize = 9.sp,
                            color = if (isOffline) WarningAmber else SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOffline) "OFFLINE" else "ONLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOffline) WarningAmber else SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = !isOffline,
                        onCheckedChange = { onToggleOffline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = WarningAmber
                        ),
                        modifier = Modifier
                            .size(width = 44.dp, height = 24.dp)
                            .testTag("toggle_offline_switch")
                    )
                }
            }
        }
    }
}
