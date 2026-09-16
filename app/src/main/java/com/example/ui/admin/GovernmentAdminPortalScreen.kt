package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.ui.theme.BackgroundCream
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GovernmentAdminPortalScreen(
    adminId: String,
    language: Language,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CPCB E-Waste EPR Oversight",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ForestGreenPrimary
                        )
                        Text(
                            text = "Central Pollution Control Board • MoEFCC",
                            fontSize = 11.sp,
                            color = ForestGreenDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_nav_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundCream)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // National EPR Monitor Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldAccent)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "National E-Waste Formalization Registry",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Admin Officer ID: $adminId (MoEFCC)",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "Backyard Diversion", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                    Text(text = "84.6%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(text = "+22% this quarter", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "Informal Collectors", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                    Text(text = "12,480", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(text = "Directly registered", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "EPR Traceability", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                    Text(text = "100%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(text = "Digital QR Verified", color = EmeraldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Field Research & Collector Usability Studies
            item {
                Text(
                    text = "Field Research Involving Working Scrap Collectors",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
            }

            // Case Study 1: Ramu Yadav
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MintLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Subject 1: Ramu Yadav (Age 42)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                                Text(text = "Dharavi Scrap Aggregator, Mumbai • 14 Years Collecting", fontSize = 11.sp, color = TextSecondaryMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MintLight.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "• Baseline: Sold scrap motherboards to local middlemen at ₹240/kg (often cheated on mechanical scale weight).", fontSize = 11.sp, color = TextSecondaryMuted)
                                Text(text = "• Platform Outcome: Received ₹380/kg from EcoReclaim Refineries with digital weighing scale and instant cash payment.", fontSize = 11.sp, color = ForestGreenDark, fontWeight = FontWeight.SemiBold)
                                Text(text = "• Monthly Net Income: Increased from ₹14,200 to ₹21,800 (+53.5% gain) without police harassment.", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Case Study 2: Sunita Devi
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MintLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "👤", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Subject 2: Sunita Devi (Age 38)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryDark)
                                Text(text = "Waste-Picker & Itinerant Aggregator, Pune • Low-Literacy User", fontSize = 11.sp, color = TextSecondaryMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MintLight.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "• Usability Finding: Could not read English or fine text; relied on Marathi voice readout and pictorial icons for cables and batteries.", fontSize = 11.sp, color = TextSecondaryMuted)
                                Text(text = "• Health Finding: Stopped burning PVC cables after understanding dioxin lung hazards; now hands over unburned wire bundles.", fontSize = 11.sp, color = ForestGreenDark, fontWeight = FontWeight.SemiBold)
                                Text(text = "• Payment: Receives instant cash handover receipt with verifiable QR code accepted at regional Swachh Bharat aggregation centers.", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Platform Sustainability & Unit Economics Model
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "How the Platform Sustains Operations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Recycler EPR Service Fee: Authorized recyclers pay a 1.5% transaction facilitation fee to obtain verified, audit-ready EPR credit certificates for electronics brands (OEMs).\n2. Recovery Value Surplus: High-tech hydrometallurgical recovery extracts gold, palladium, and rare-earths yielding ~₹90/kg net value surplus above informal burning.\n3. Zero Compliance Cost for Collectors: Completely free for informal waste pickers with cash settlement.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
