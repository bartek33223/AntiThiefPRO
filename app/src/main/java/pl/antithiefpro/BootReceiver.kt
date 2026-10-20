package pl.antithiefpro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            EventLog.add(context, "BOOT", "System Android uruchomił się ponownie")
        }
    }
}
