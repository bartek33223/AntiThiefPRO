package pl.antithiefpro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle

class AntiThiefAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        EventLog.add(context, "DEVICE_ADMIN_ENABLED", "Włączono administratora urządzenia")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        EventLog.add(context, "DEVICE_ADMIN_DISABLED", "Wyłączono administratora urządzenia")
        super.onDisabled(context, intent)
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        val attempts = try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            dpm.currentFailedPasswordAttempts
        } catch (_: Exception) { -1 }
        EventLog.add(
            context,
            "DEVICE_PIN_FAILURE",
            if (attempts >= 0) "Nieudana próba odblokowania urządzenia; licznik systemowy: $attempts" else "Nieudana próba odblokowania urządzenia"
        )
        showAlert(context, attempts)
    }

    private fun showAlert(context: Context, attempts: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "anti_thief_security"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Ochrona antykradzieżowa", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            502,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = if (attempts >= 0) "Nieudana próba odblokowania. Licznik: $attempts." else "Wykryto nieudaną próbę odblokowania."
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("AntiThiefPRO: próba odblokowania")
            .setContentText(message)
            .setStyle(android.app.Notification.BigTextStyle().bigText(message + " Otwórz aplikację, aby sprawdzić dziennik."))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(502, notification)
        } catch (_: SecurityException) {
            EventLog.add(context, "NOTIFICATION_BLOCKED", "Android zablokował powiadomienie; sprawdź uprawnienia powiadomień")
        }
    }
}
