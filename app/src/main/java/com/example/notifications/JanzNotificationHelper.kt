package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.UserAccount

object JanzNotificationHelper {

    const val CHANNEL_ID = "janzflare_alerts"
    const val EXTRA_VIDEO_ID = "extra_video_id"
    const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"
    private const val PREFS_NAME = "janz_notif_prefs"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "JanzFlare Activity & Uploads",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for new videos from subscribed creators, subscribers, likes, and comments."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun wasPermissionPromptShown(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("prompted_notifications", false)
    }

    fun markPermissionPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("prompted_notifications", true)
            .apply()
    }

    fun processIncomingNotifications(
        context: Context,
        userId: String,
        notifications: List<AppNotification>,
        userPrefs: UserAccount?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyLastSec = "last_notif_sec_$userId"
        val keySeenIds = "seen_notif_ids_$userId"

        val storedLastSec = prefs.getLong(keyLastSec, -1L)
        if (storedLastSec < 0L) {
            // First time initializing on this device for this user: baseline to newest existing notification (or now - 5s)
            val maxExistingSec = notifications.maxOfOrNull { it.createdAt?.seconds ?: 0L }
                ?: ((System.currentTimeMillis() / 1000L) - 5L)
            val existingIds = notifications.map { it.id }.take(100).toSet()
            prefs.edit()
                .putLong(keyLastSec, maxExistingSec)
                .putStringSet(keySeenIds, existingIds)
                .apply()
            return
        }

        val seenIds = prefs.getStringSet(keySeenIds, emptySet())?.toMutableSet() ?: mutableSetOf()
        var newestSec = storedLastSec
        val toDisplay = mutableListOf<AppNotification>()

        for (notif in notifications) {
            if (notif.id.isBlank()) continue
            val tsSec = notif.createdAt?.seconds ?: (System.currentTimeMillis() / 1000L)
            if (tsSec > newestSec) newestSec = tsSec

            val isAlreadySeen = notif.id in seenIds
            if (!isAlreadySeen && !notif.read && tsSec >= storedLastSec) {
                seenIds.add(notif.id)
                if (shouldNotifyForType(notif.type, userPrefs)) {
                    toDisplay.add(notif)
                }
            } else if (!isAlreadySeen) {
                seenIds.add(notif.id)
            }
        }

        prefs.edit()
            .putLong(keyLastSec, newestSec)
            .putStringSet(keySeenIds, seenIds.take(150).toSet())
            .apply()

        if (toDisplay.isNotEmpty() && hasNotificationPermission(context)) {
            ensureChannel(context)
            toDisplay.take(3).forEach { notif ->
                showSystemNotification(context, notif)
            }
        }
    }

    private fun shouldNotifyForType(type: String, user: UserAccount?): Boolean {
        if (user == null) return true
        return when (type) {
            "upload" -> user.notifyUploads
            "follow" -> user.notifyFollowers
            "like" -> user.notifyLikes
            "comment", "reply" -> user.notifyComments
            else -> true
        }
    }

    private fun showSystemNotification(context: Context, notif: AppNotification) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (notif.targetVideoId.isNotBlank()) {
                putExtra(EXTRA_VIDEO_ID, notif.targetVideoId)
            } else {
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notif.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (notif.type) {
            "upload" -> "New Video • ${notif.actorName}"
            "follow" -> "New Subscriber on JanzFlare"
            "comment" -> "New Comment • ${notif.actorName}"
            "reply" -> "New Reply • ${notif.actorName}"
            "like" -> "New Like • ${notif.actorName}"
            else -> "JanzFlare"
        }

        val largeLogo = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_janzflare)
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_flame)
            .setColor(0xFFFF6B2C.toInt())
            .setContentTitle(title)
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (largeLogo != null) {
            builder.setLargeIcon(largeLogo)
        }

        try {
            NotificationManagerCompat.from(context).notify(notif.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked at runtime
        }
    }
}
