package pl.antithiefpro

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("anti_thief_settings", Context.MODE_PRIVATE) }
    private lateinit var status: TextView
    private lateinit var emailField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshStatus()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
            setBackgroundColor(0xFF0B1220.toInt())
        }
        page.addView(TextView(this).apply {
            text = "🛡 AntiThiefPRO"
            textSize = 28f
            setTextColor(0xFFF4F7FB.toInt())
        })
        page.addView(TextView(this).apply {
            text = "Ochrona przed nieautoryzowanym odblokowaniem"
            textSize = 15f
            setTextColor(0xFF9AAAC0.toInt())
            setPadding(0, 8, 0, 20)
        })
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF4ADE80.toInt())
            setPadding(0, 16, 0, 16)
        }
        page.addView(status)
        page.addView(actionButton("Włącz administratora urządzenia") { enableAdmin() })
        page.addView(actionButton("Nadaj uprawnienia aparatu i lokalizacji") { requestProtectionPermissions() })
        page.addView(TextView(this).apply {
            text = "E-MAIL ODBIORCY"
            textSize = 12f
            setTextColor(0xFF9AAAC0.toInt())
            setPadding(0, 24, 0, 8)
        })
        emailField = EditText(this).apply {
            hint = "twoj@email.pl"
            setSingleLine(true)
            setTextColor(0xFFF4F7FB.toInt())
            setHintTextColor(0xFF9AAAC0.toInt())
            setText(prefs.getString("recipient_email", "") ?: "")
        }
        page.addView(emailField)
        page.addView(actionButton("Zapisz adres e-mail") {
            val value = emailField.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                emailField.error = "Wpisz poprawny adres e-mail"
            } else {
                prefs.edit().putString("recipient_email", value).apply()
                EventLog.add(this, "EMAIL_SAVED", "Zapisano adres odbiorcy")
                Toast.makeText(this, "Zapisano adres odbiorcy.", Toast.LENGTH_SHORT).show()
            }
        })
        page.addView(actionButton("Historia zdarzeń") { showHistory() })
        page.addView(actionButton("Odśwież stan ochrony") { refreshStatus() })
        page.addView(TextView(this).apply {
            text = "Ważne: aplikacja nie uruchamia syreny ani własnych powiadomień. Android może wymagać widocznego powiadomienia systemowego dla usług działających w tle. Ta wersja zapisuje nieudane próby odblokowania, ale automatyczne zdjęcia, film i wysyłka e-mail nie są jeszcze gotowe."
            textSize = 13f
            setTextColor(0xFF9AAAC0.toInt())
            setPadding(0, 24, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun enableAdmin() {
        val component = ComponentName(this, AntiThiefAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "AntiThiefPRO zapisuje nieudane próby odblokowania tego telefonu.")
        }
        startActivity(intent)
    }

    private fun requestProtectionPermissions() {
        val needed = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.distinct().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            Toast.makeText(this, "Uprawnienia są już nadane.", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(missing.toTypedArray(), 410)
        }
    }

    private fun refreshStatus() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val active = dpm.isAdminActive(ComponentName(this, AntiThiefAdminReceiver::class.java))
        val camera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val location = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        status.text = "Administrator: " + (if (active) "WŁĄCZONY" else "WYŁĄCZONY") +
            "\nAparat: " + (if (camera) "zezwolono" else "brak zgody") +
            "\nLokalizacja: " + (if (location) "zezwolono" else "brak zgody") +
            "\nZdarzenia zapisane: " + EventLog.read(this).size
        status.gravity = Gravity.START
    }

    private fun showHistory() {
        val events = EventLog.read(this).asReversed()
        val message = if (events.isEmpty()) "Brak zdarzeń." else events.joinToString("\n\n") {
            "${EventLog.formattedTime(it.time)}\n${it.type}: ${it.details}"
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Historia zdarzeń")
            .setMessage(message)
            .setPositiveButton("Zamknij", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }
}
