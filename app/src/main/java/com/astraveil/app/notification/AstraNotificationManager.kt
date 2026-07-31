package com.astraveil.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.astraveil.app.MainActivity
import com.astraveil.app.R

object AstraNotificationManager {

    const val CHANNEL_SU_REQUESTS = "su_requests"
    const val CHANNEL_UPDATES = "updates"
    const val CHANNEL_MODULES = "modules"
    const val CHANNEL_DAEMON = "daemon"
    const val CHANNEL_SECURITY = "security"

    const val ID_SU_REQUEST = 1001
    const val ID_UPDATE_AVAILABLE = 2001
    const val ID_MODULE_INSTALLED = 3001
    const val ID_MODULE_UNINSTALLED = 3002
    const val ID_DAEMON_STATUS = 4001
    const val ID_SECURITY_ALERT = 5001

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannels(listOf(
            NotificationChannel(
                CHANNEL_SU_REQUESTS, "Superuser Requests",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when an app requests root access."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_UPDATES, "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications when a new AstraVeil version is available."
            },
            NotificationChannel(
                CHANNEL_MODULES, "Module Activity",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Module install, uninstall, and status changes."
            },
            NotificationChannel(
                CHANNEL_DAEMON, "Daemon Status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "AstraDaemon connection and health status."
            },
            NotificationChannel(
                CHANNEL_SECURITY, "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Sandbox violations and policy changes."
                enableVibration(true)
            },
        ))
    }

    private fun canPost(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    fun notifySuRequest(context: Context, appName: String, packageName: String, uid: Int) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "superuser")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, ID_SU_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SU_REQUESTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Superuser Request")
            .setContentText("$appName ($packageName, uid $uid) requests root access.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$appName ($packageName, uid $uid) requests root access. Open AstraVeil to grant or deny."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(ID_SU_REQUEST, notification)
    }

    fun notifyUpdateAvailable(context: Context, versionName: String, releaseNotes: String) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "settings/update_center")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, ID_UPDATE_AVAILABLE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AstraVeil $versionName Available")
            .setContentText(releaseNotes.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(releaseNotes))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(ID_UPDATE_AVAILABLE, notification)
    }

    fun notifyModuleChange(context: Context, moduleName: String, installed: Boolean) {
        if (!canPost(context)) return
        val id = if (installed) ID_MODULE_INSTALLED else ID_MODULE_UNINSTALLED
        val action = if (installed) "installed" else "uninstalled"
        val notification = NotificationCompat.Builder(context, CHANNEL_MODULES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Module $action")
            .setContentText("'$moduleName' has been $action.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun notifyDaemonStatus(context: Context, connected: Boolean) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DAEMON)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AstraDaemon")
            .setContentText(
                if (connected) "Connected to astrad."
                else "Disconnected from astrad. Running in local-only mode."
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(connected)
            .build()
        NotificationManagerCompat.from(context).notify(ID_DAEMON_STATUS, notification)
    }

    fun notifySecurityAlert(context: Context, title: String, detail: String) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_SECURITY_ALERT, notification)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
