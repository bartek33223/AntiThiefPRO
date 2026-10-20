package pl.antithiefpro

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
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
        val detail = if (attempts >= 0) "Nieudana próba odblokowania; licznik systemowy: $attempts"
            else "Nieudana próba odblokowania"
        EventLog.add(context, "DEVICE_UNLOCK_FAILURE", detail)
        // Celowo bez syreny i bez powiadomienia aplikacji.
        // Aparatu nie wolno uruchamiać tu bez uwzględnienia ograniczeń pracy w tle Androida.
    }
}
