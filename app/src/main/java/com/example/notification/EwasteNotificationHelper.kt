package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Language

class EwasteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "E-Waste Collection Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Don't forget your scheduled e-waste pickup or nearby collection event!"
        val notificationId = intent.getIntExtra(EXTRA_ID, 1001)

        EwasteNotificationHelper.showNotification(context, title, message, notificationId)
    }

    companion object {
        const val EXTRA_TITLE = "extra_notification_title"
        const val EXTRA_MESSAGE = "extra_notification_message"
        const val EXTRA_ID = "extra_notification_id"
    }
}

object EwasteNotificationHelper {
    private const val CHANNEL_ID = "ewaste_reminders_channel"
    private const val CHANNEL_NAME = "E-Waste Collection & Pickups"
    private const val CHANNEL_DESC = "Notifications for scheduled e-waste collection events and regular doorstep pickups"

    const val ID_NEARBY_COLLECTION_DRIVE = 3001
    const val ID_MONTHLY_DOORSTEP_PICKUP = 3002
    const val ID_BATTERY_DISPOSAL_ALERT = 3003

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Whether notifications can be displayed (always true before Android 13). */
    fun notificationsEnabled(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        // Silently skip on Android 13+ when the runtime permission was not granted
        if (!notificationsEnabled(context)) return
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Schedules a broadcast-based alarm that fires [EwasteNotificationReceiver] at
     * [triggerAtMillis] and shows the local notification. Reuses the same [reminderId]
     * so calling again simply updates the existing schedule.
     */
    fun scheduleReminder(
        context: Context,
        title: String,
        message: String,
        triggerAtMillis: Long,
        reminderId: Int = ID_MONTHLY_DOORSTEP_PICKUP
    ) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, EwasteNotificationReceiver::class.java).apply {
            putExtra(EwasteNotificationReceiver.EXTRA_TITLE, title)
            putExtra(EwasteNotificationReceiver.EXTRA_MESSAGE, message)
            putExtra(EwasteNotificationReceiver.EXTRA_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** Drops the pending (or already fired) schedule for [reminderId]. */
    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, EwasteNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderContent(
        type: ReminderType,
        language: Language,
        customDateText: String?
    ): Triple<String, String, Long> = when (type) {
        ReminderType.NEARBY_COLLECTION_DRIVE -> {
            val t = when (language) {
                Language.HINDI -> "निकटतम ई-कचरा संग्रहण शिविर"
                Language.MARATHI -> "जवळची ई-कचरा संकलन मोहीम"
                Language.ENGLISH -> "Nearby E-Waste Collection Drive"
            }
            val m = when (language) {
                Language.HINDI -> "कल सुबह 10 बजे आपके क्षेत्र में CPCB अधिकृत संग्रहण वैन आ रही है। अपना पुराना ई-कचरा तैयार रखें।"
                Language.MARATHI -> "उद्या सकाळी १० वाजता तुमच्या भागात अधिकृत संकलन व्हॅन येत आहे. जुना ई-कचरा तयार ठेवा."
                Language.ENGLISH -> "CPCB authorized collection van visiting your ward tomorrow at 10 AM. Keep electronic scrap segregated!"
            }
            Triple(t, m, 5000L) // Fallback demo trigger in 5s when no date supplied
        }
        ReminderType.MONTHLY_DOORSTEP_PICKUP -> {
            val t = when (language) {
                Language.HINDI -> "नियमित मासिक ई-कचरा पिकअप"
                Language.MARATHI -> "नियमित मासिक ई-कचरा पिकअप"
                Language.ENGLISH -> "Regular Monthly E-Waste Pickup"
            }
            val m = when (language) {
                Language.HINDI -> "आपका मासिक ई-कचरा पिकअप निर्धारित है ($customDateText)। डिजिटल वजन कांटा और तत्काल UPI भुगतान उपलब्ध।"
                Language.MARATHI -> "तुमचा मासिक ई-कचरा पिकअप नियोजित आहे ($customDateText). डिजिटल वजन आणि तात्काळ UPI पेमेंट उपलब्ध."
                Language.ENGLISH -> "Scheduled doorstep pickup ($customDateText). Guaranteed digital weighing and instant scale settlement."
            }
            Triple(t, m, 6000L)
        }
        ReminderType.BATTERY_DISPOSAL_ALERT -> {
            val t = when (language) {
                Language.HINDI -> "सुरक्षित बैटरी निस्तारण अनुस्मारक"
                Language.MARATHI -> "सुरक्षित बॅटरी विल्हेवाट स्मरणपत्र"
                Language.ENGLISH -> "Safe Battery Disposal Reminder"
            }
            val m = when (language) {
                Language.HINDI -> "पुरानी लिथियम बैटरी घर पर न रखें। निकटतम CPCB केंद्र पर जमा कर आग के जोखिम से बचें।"
                Language.MARATHI -> "जुन्या लिथियम बॅटरी घरी साठवू नका. आगीचा धोका टाळण्यासाठी अधिकृत केंद्रात जमा करा."
                Language.ENGLISH -> "Never store swollen or old Li-ion batteries at home. Hand over to authorized recyclers."
            }
            Triple(t, m, 4000L)
        }
    }

    fun reminderIdFor(type: ReminderType): Int = when (type) {
        ReminderType.NEARBY_COLLECTION_DRIVE -> ID_NEARBY_COLLECTION_DRIVE
        ReminderType.MONTHLY_DOORSTEP_PICKUP -> ID_MONTHLY_DOORSTEP_PICKUP
        ReminderType.BATTERY_DISPOSAL_ALERT -> ID_BATTERY_DISPOSAL_ALERT
    }

    /** Quick demo path: shows immediately and also queues an alarm a few seconds out. */
    fun schedulePresetReminder(
        context: Context,
        type: ReminderType,
        language: Language,
        customDateText: String? = null
    ): Boolean {
        val (title, message, delayMillis) = reminderContent(type, language, customDateText)
        showNotification(context, title, message)

        val triggerAtMillis = System.currentTimeMillis() + delayMillis.coerceAtLeast(1000L)
        scheduleReminder(
            context = context,
            title = title,
            message = message,
            triggerAtMillis = triggerAtMillis,
            reminderId = reminderIdFor(type)
        )
        return true
    }

    /** Schedules a real reminder at an absolute date/time using a stable id, so it can be cancelled later. */
    fun scheduleReminderAtDate(
        context: Context,
        type: ReminderType,
        language: Language,
        triggerAtMillis: Long,
        customDateText: String?
    ): Int {
        val (title, message, _) = reminderContent(type, language, customDateText)
        val reminderId = reminderIdFor(type)
        scheduleReminder(context, title, message, triggerAtMillis, reminderId)
        return reminderId
    }

    /** Formats a Long epoch millis into a short, locale-friendly date label. */
    fun formatDateLabel(epochMillis: Long): String {
        return java.text.SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", java.util.Locale.ENGLISH)
            .format(java.util.Date(epochMillis))
    }
}

enum class ReminderType {
    NEARBY_COLLECTION_DRIVE,
    MONTHLY_DOORSTEP_PICKUP,
    BATTERY_DISPOSAL_ALERT
}