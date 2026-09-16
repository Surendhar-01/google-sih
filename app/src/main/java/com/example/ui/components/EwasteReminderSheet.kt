package com.example.ui.components

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.notification.EwasteNotificationHelper
import com.example.notification.ReminderType
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ForestGreenLight
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPill
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import java.util.Calendar

private fun reminderTitle(type: ReminderType, lang: Language): String = when (type) {
    ReminderType.NEARBY_COLLECTION_DRIVE -> when (lang) {
        Language.ENGLISH -> "Nearby Collection Drive"
        Language.HINDI -> "निकटतम संग्रहण शिविर"
        Language.MARATHI -> "जवळची संकलन मोहीम"
    }
    ReminderType.MONTHLY_DOORSTEP_PICKUP -> when (lang) {
        Language.ENGLISH -> "Monthly Doorstep Pickup"
        Language.HINDI -> "मासिक डोरस्टेप पिकअप"
        Language.MARATHI -> "मासिक घरपोच पिकअप"
    }
    ReminderType.BATTERY_DISPOSAL_ALERT -> when (lang) {
        Language.ENGLISH -> "Battery Disposal Alert"
        Language.HINDI -> "बैटरी निस्तारण अलर्ट"
        Language.MARATHI -> "बॅटरी विल्हेवाट अलर्ट"
    }
}

private fun sheetLabel(lang: Language): String = when (lang) {
    Language.ENGLISH -> "E-Waste Pickup & Collection Reminders"
    Language.HINDI -> "ई-कचरा पिकअप और संग्रहण अनुस्मारक"
    Language.MARATHI -> "ई-कचरा संकलन आणि पिकअप स्मरणपत्रे"
}

private fun sheetSubtitle(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Choose an event type, set a date & time, and we'll post a local notification as a reminder."
    Language.HINDI -> "घटना का प्रकार चुनें, तारीख और समय सेट करें, हम एक स्थानीय सूचना पोस्ट करेंगे।"
    Language.MARATHI -> "कार्यक्रम प्रकार निवडा, तारीख व वेळ निश्चित करा, आम्ही स्थानिक सूचना पाठवू."
}

private fun scheduleLabel(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Schedule Reminder"
    Language.HINDI -> "अनुस्मारक निर्धारित करें"
    Language.MARATHI -> "स्मरणपत्र निश्चित करा"
}

private fun cancelLabel(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Cancel This Schedule"
    Language.HINDI -> "यह अनुस्मारक रद्द करें"
    Language.MARATHI -> "हे स्मरणपत्र रद्द करा"
}

private fun dateLabel(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Date"
    Language.HINDI -> "तारीख"
    Language.MARATHI -> "तारीख"
}

private fun timeLabel(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Time"
    Language.HINDI -> "समय"
    Language.MARATHI -> "वेळ"
}

private fun permissionToast(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Notification permission is required to show pickup reminders."
    Language.HINDI -> "पिकअप अनुस्मारक दिखाने के लिए सूचना अनुमति आवश्यक है।"
    Language.MARATHI -> "पिकअप स्मरणपत्र दाखवण्यासाठी सूचना परवानगी आवश्यक आहे."
}

private fun successToast(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Reminder scheduled!"
    Language.HINDI -> "अनुस्मारक निर्धारित हो गया!"
    Language.MARATHI -> "स्मरणपत्र निश्चित झाले!"
}

private fun cancelledToast(lang: Language): String = when (lang) {
    Language.ENGLISH -> "Scheduled reminder cancelled."
    Language.HINDI -> "निर्धारित अनुस्मारक रद्द कर दिया गया।"
    Language.MARATHI -> "निर्धारित स्मरणपत्र रद्द केले."
}

/** Converts a UTC-midnight date picker value + local wall-clock hour/minute into epoch millis. */
private fun combineDateAndTime(utcDateMillis: Long, hour: Int, minute: Int): Long {
    val dateCal = Calendar.getInstance()
    dateCal.timeInMillis = utcDateMillis
    val local = Calendar.getInstance()
    local.set(
        dateCal.get(Calendar.YEAR),
        dateCal.get(Calendar.MONTH),
        dateCal.get(Calendar.DAY_OF_MONTH),
        hour,
        minute,
        0
    )
    local.set(Calendar.MILLISECOND, 0)
    return local.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EwasteReminderSheet(
    language: Language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(ReminderType.NEARBY_COLLECTION_DRIVE) }
    var selectedDateUtc by remember { mutableStateOf(System.currentTimeMillis()) }
    var hour by remember { mutableStateOf(10) }
    var minute by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Whether the notification permission is already granted or unnecessary
    fun permissionOk(): Boolean = EwasteNotificationHelper.notificationsEnabled(context)

    fun scheduleFor(type: ReminderType) {
        val triggerAt = combineDateAndTime(selectedDateUtc, hour, minute)
        val clamped = maxOf(triggerAt, System.currentTimeMillis() + 1000L)
        EwasteNotificationHelper.scheduleReminderAtDate(
            context = context,
            type = type,
            language = language,
            triggerAtMillis = clamped,
            customDateText = EwasteNotificationHelper.formatDateLabel(clamped)
        )
        Toast.makeText(context, successToast(language), Toast.LENGTH_LONG).show()
        onDismiss()
    }

    // POST_NOTIFICATIONS runtime permission (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleFor(selectedType)
        } else {
            Toast.makeText(context, permissionToast(language), Toast.LENGTH_LONG).show()
        }
    }

    fun onScheduleClick(type: ReminderType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionOk()) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scheduleFor(type)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MintLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(ForestGreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = sheetLabel(language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = sheetSubtitle(language),
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Event type selector
            Text(
                text = if (language == Language.ENGLISH) "Reminder Type" else if (language == Language.HINDI) "अनुस्मारक प्रकार" else "स्मरणपत्र प्रकार",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReminderType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                        Text(
                            text = reminderTitle(type, language),
                            fontSize = 13.sp,
                            fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (type) {
                                ReminderType.NEARBY_COLLECTION_DRIVE -> Icons.Default.NotificationsActive
                                ReminderType.MONTHLY_DOORSTEP_PICKUP -> Icons.Default.CalendarMonth
                                ReminderType.BATTERY_DISPOSAL_ALERT -> Icons.Default.AccessTime
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = TextPrimaryDark,
                        containerColor = MintPill
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date & time selection card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(dateLabel(language), fontSize = 10.sp, color = TextSecondaryMuted)
                            Text(
                                text = java.text.SimpleDateFormat("EEE, dd MMM yyyy", java.util.Locale.ENGLISH)
                                    .format(java.util.Date(selectedDateUtc)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("✏️", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(timeLabel(language), fontSize = 10.sp, color = TextSecondaryMuted)
                            Text(
                                text = String.format(
                                    java.util.Locale.ENGLISH,
                                    "%02d:%02d %s",
                                    if (hour % 12 == 0) 12 else hour % 12,
                                    minute,
                                    if (hour < 12) "AM" else "PM"
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("✏️", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preview of the scheduled moment
            Text(
                text = EwasteNotificationHelper.formatDateLabel(
                    combineDateAndTime(selectedDateUtc, hour, minute)
                ),
                fontSize = 12.sp,
                color = ForestGreenLight,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onScheduleClick(selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(scheduleLabel(language), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    EwasteNotificationHelper.cancelReminder(context, EwasteNotificationHelper.reminderIdFor(selectedType))
                    Toast.makeText(context, cancelledToast(language), Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ErrorRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(cancelLabel(language), color = ErrorRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateUtc = it }
                    showDatePicker = false
                }) {
                    Text(if (language == Language.ENGLISH) "OK" else if (language == Language.HINDI) "ठीक है" else "ठीक")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Select Time" else if (language == Language.HINDI) "समय चुनें" else "वेळ निवडा",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreenPrimary
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            shape = RoundedCornerShape(22.dp)
        )
    }
}