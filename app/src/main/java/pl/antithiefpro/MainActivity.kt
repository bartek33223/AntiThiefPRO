package pl.antithiefpro

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.StatFs
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var statusText: TextView
    private lateinit var statusDot: TextView
    private lateinit var armButton: Button
    private lateinit var sensitivityText: TextView
    private lateinit var tone: ToneGenerator
    private val handler = Handler(Looper.getMainLooper())
    private var armed = false
    private var alarmOn = false
    private var countdown = false
    private var countdownValue = 5
    private var ignoreMotionUntil = 0L
    private var pin = "1234"
    private var sensitivity = 2.8f
    private var soundEnabled = true
    private var vibrationEnabled = true
    private var lastTrigger = 0L
    private var pendingCsv = ""
    private val exportRequestCode = 9012

    private val countdownTask = object : Runnable {
        override fun run() {
            if (!countdown) return
            if (countdownValue > 0) {
                statusText.text = "Odłóż telefon… $countdownValue"
                countdownValue--
                handler.postDelayed(this, 1000L)
            } else {
                countdown = false
                armed = true
                ignoreMotionUntil = System.currentTimeMillis() + 700L
                sensorManager.registerListener(this@MainActivity, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                updateStatus("UZBROJONY", AMBER)
                EventLog.add(this@MainActivity, "ARMED", "Alarm ruchowy uzbrojony")
                armButton.text = "ALARM UZBROJONY"
                Toast.makeText(this@MainActivity, "Alarm uzbrojony. Nie ruszaj telefonu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val alarmTask = object : Runnable {
        override fun run() {
            if (!alarmOn) return
            if (soundEnabled) tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 650)
            handler.postDelayed(this, 850L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = getSharedPreferences("anti_thief", Context.MODE_PRIVATE)
        pin = prefs.getString("pin", "1234") ?: "1234"
        sensitivity = prefs.getFloat("sensitivity", 2.8f)
        soundEnabled = prefs.getBoolean("sound", true)
        vibrationEnabled = prefs.getBoolean("vibration", true)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        buildUi()
        EventLog.add(this, "APP_START", "Otwarto AntiThiefPRO")
        updateStatus("ROZBROJONY", GREEN)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7301)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 20, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(26))
            setBackgroundColor(BG)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            text = "🛡"
            textSize = 29f
            gravity = Gravity.CENTER
            background = rounded(ACCENT, 16)
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        brandRow.addView(logo)
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(text("AntiThiefPRO", 23f, WHITE, true))
        brand.addView(text("TWOJA TARCZA • ALARM RUCHOWY", 10f, MUTED, true))
        brandRow.addView(brand)
        page.addView(brandRow)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(24), dp(20), dp(22))
            background = rounded(CARD, 24, BORDER)
        }
        val shield = text("◉", 36f, ACCENT, true).apply { gravity = Gravity.CENTER }
        hero.addView(shield)
        statusText = text("Gotowy do ochrony", 22f, WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(5))
        }
        hero.addView(statusText)
        statusDot = text("●  SYSTEM GOTOWY", 12f, GREEN, true).apply { gravity = Gravity.CENTER }
        hero.addView(statusDot)
        val sub = text("Włącz czujnik, odłóż telefon i wykryj niepożądany ruch.", 13f, MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        hero.addView(sub)
        val heroParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
        page.addView(hero, heroParams)

        armButton = Button(this).apply {
            text = "UZBRÓJ ALARM  ·  5 SEK."
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(ACCENT, 16)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                if (armed || countdown || alarmOn) askForPin { disarmAlarm() } else armAlarm()
            }
        }
        page.addView(armButton, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(14) })

        val stopButton = Button(this).apply {
            text = "STOP / ROZBRÓJ • WYMAGANY PIN"
            isAllCaps = false
            textSize = 13f
            setTextColor(WHITE)
            background = rounded(CARD, 14, BORDER)
            setOnClickListener { askForPin { disarmAlarm() } }
        }
        page.addView(stopButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        page.addView(sectionTitle("USTAWIENIA ALARMU"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        val settings = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD, 20, BORDER)
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        val sensitivityHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        sensitivityHeader.addView(text("Czułość czujnika", 15f, WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        sensitivityText = text(String.format(java.util.Locale.US, "%.1f", sensitivity), 14f, ACCENT, true)
        sensitivityHeader.addView(sensitivityText)
        settings.addView(sensitivityHeader)
        settings.addView(text("Niższa = mniej czuły • Wyższa = łatwiej wykrywa ruch", 12f, MUTED).apply {
            setPadding(0, dp(4), 0, 0)
        })
        val seek = SeekBar(this).apply {
            max = 35
            progress = (((sensitivity - 1.5f) / 0.1f).toInt()).coerceIn(0, 35)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    sensitivity = 1.5f + progress * 0.1f
                    if (fromUser) EventLog.add(this@MainActivity, "SETTING", "Zmieniono czułość czujnika")
                    sensitivityText.text = String.format(java.util.Locale.US, "%.1f", sensitivity)
                    getSharedPreferences("anti_thief", MODE_PRIVATE).edit().putFloat("sensitivity", sensitivity).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        settings.addView(seek)
        settings.addView(makeSwitch("Dźwięk alarmu", "Głośny sygnał po wykryciu ruchu", soundEnabled) {
            soundEnabled = it
            EventLog.add(this@MainActivity, "SETTING", "Dźwięk alarmu: " + if (it) "włączony" else "wyłączony")
            getSharedPreferences("anti_thief", MODE_PRIVATE).edit().putBoolean("sound", it).apply()
        })
        settings.addView(makeSwitch("Wibracje", "Wibracje razem z alarmem", vibrationEnabled) {
            vibrationEnabled = it
            EventLog.add(this@MainActivity, "SETTING", "Wibracje: " + if (it) "włączone" else "wyłączone")
            getSharedPreferences("anti_thief", MODE_PRIVATE).edit().putBoolean("vibration", it).apply()
        })
        page.addView(settings, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val pinButton = Button(this).apply {
            text = "🔐  ZMIEŃ KOD PIN"
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(CARD, 14, BORDER)
            setOnClickListener { changePin() }
        }
        page.addView(pinButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })

        val historyButton = Button(this).apply {
            text = "📋  HISTORIA ZDARZEŃ"
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(CARD, 14, BORDER)
            setOnClickListener { showEventHistory() }
        }
        page.addView(historyButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })

        val diagnosticsButton = Button(this).apply {
            text = "🩺  DIAGNOSTYKA SYSTEMU"
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(CARD, 14, BORDER)
            setOnClickListener { showDiagnostics() }
        }
        page.addView(diagnosticsButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        val exportButton = Button(this).apply {
            text = "📤  EKSPORTUJ RAPORT CSV"
            isAllCaps = false
            setTextColor(WHITE)
            background = rounded(CARD, 14, BORDER)
            setOnClickListener { beginExport() }
        }
        page.addView(exportButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })

        page.addView(text("Wskazówka: przetestuj alarm przed użyciem. Ta wersja wykrywa ruch tylko, gdy aplikacja pozostaje otwarta.", 12f, MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(18), dp(4), 0)
        })
        setContentView(scroll)
    }

    private fun sectionTitle(label: String) = text(label, 12f, MUTED, true).apply {
        setPadding(dp(2), 0, 0, dp(2))
    }

    private fun makeSwitch(title: String, detail: String, initial: Boolean, changed: (Boolean) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(8))
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        labels.addView(text(title, 15f, WHITE, true))
        labels.addView(text(detail, 12f, MUTED).apply { setPadding(0, dp(3), 0, 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> changed(checked) }
        })
        return row
    }

    private fun updateStatus(label: String, color: Int) {
        statusText.text = when (label) {
            "ROZBROJONY" -> "Gotowy do ochrony"
            "UZBROJONY" -> "Telefon chroniony"
            "ALARM" -> "WYKRYTO RUCH!"
            else -> label
        }
        statusDot.text = when (label) {
            "ROZBROJONY" -> "●  SYSTEM GOTOWY"
            "UZBROJONY" -> "●  ALARM AKTYWNY"
            "ALARM" -> "●  ALARM! SPRAWDŹ TELEFON"
            else -> "●  $label"
        }
        statusDot.setTextColor(color)
        armButton.text = when (label) {
            "ROZBROJONY" -> "UZBRÓJ ALARM  ·  5 SEK."
            "UZBROJONY" -> "ALARM UZBROJONY • DOTKNIJ, ABY STOP"
            "ALARM" -> "ALARM DZIAŁA • DOTKNIJ, ABY STOP"
            else -> label
        }
        armButton.background = rounded(if (label == "ALARM") RED else if (label == "UZBROJONY") AMBER else ACCENT, 16)
    }

    private fun armAlarm() {
        if (accelerometer == null) {
            Toast.makeText(this, "Ten telefon nie ma czujnika ruchu.", Toast.LENGTH_LONG).show()
            return
        }
        if (armed || countdown) return
        if (!soundEnabled && !vibrationEnabled) {
            Toast.makeText(this, "Włącz dźwięk lub wibracje w ustawieniach.", Toast.LENGTH_LONG).show()
            return
        }
        alarmOn = false
        EventLog.add(this, "ARM_REQUEST", "Rozpoczęto odliczanie do uzbrojenia")
        countdown = true
        countdownValue = 5
        updateStatus("PRZYGOTOWANIE", AMBER)
        handler.post(countdownTask)
    }

    private fun disarmAlarm() {
        handler.removeCallbacks(countdownTask)
        handler.removeCallbacks(alarmTask)
        countdown = false
        armed = false
        alarmOn = false
        EventLog.add(this, "DISARMED", "Alarm rozbrojony lub wyłączony")
        sensorManager.unregisterListener(this)
        tone.stopTone()
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        updateStatus("ROZBROJONY", GREEN)
        Toast.makeText(this, "Alarm wyłączony.", Toast.LENGTH_SHORT).show()
    }

    private fun askForPin(onSuccess: () -> Unit) {
        val field = android.widget.EditText(this).apply {
            hint = "Wpisz PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Potwierdź PIN")
            .setView(field)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("OK") { _, _ ->
                if (field.text.toString() == pin) onSuccess()
                else {
                    EventLog.add(this, "PIN_FAILURE", "Nieprawidłowy PIN w AntiThiefPRO")
                    Toast.makeText(this, "Nieprawidłowy PIN.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun changePin() {
        askForPin {
            val field = android.widget.EditText(this).apply {
                hint = "Nowy PIN (4–8 cyfr)"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            AlertDialog.Builder(this)
                .setTitle("Ustaw nowy PIN")
                .setView(field)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Zapisz") { _, _ ->
                    val value = field.text.toString()
                    if (value.length in 4..8 && value.all { it.isDigit() }) {
                        pin = value
                        EventLog.add(this@MainActivity, "PIN_CHANGED", "Zmieniono PIN aplikacji")
                        getSharedPreferences("anti_thief", Context.MODE_PRIVATE).edit().putString("pin", pin).apply()
                        Toast.makeText(this, "PIN został zmieniony.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "PIN musi mieć 4–8 cyfr.", Toast.LENGTH_LONG).show()
                    }
                }
                .show()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!armed || alarmOn || System.currentTimeMillis() < ignoreMotionUntil) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (abs(magnitude - SensorManager.GRAVITY_EARTH) > sensitivity) triggerAlarm()
    }

    private fun triggerAlarm() {
        if (System.currentTimeMillis() - lastTrigger < 1500L) return
        lastTrigger = System.currentTimeMillis()
        alarmOn = true
        EventLog.add(this, "MOTION_ALARM", "Wykryto ruch przekraczający ustawioną czułość")
        updateStatus("ALARM", RED)
        if (soundEnabled) handler.post(alarmTask)
        if (vibrationEnabled) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 350, 180, 350, 180, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }


    private fun showEventHistory() {
        val events = EventLog.read(this).asReversed()
        val content = if (events.isEmpty()) "Brak zapisanych zdarzeń." else
            events.joinToString("\n\n") {
                "${EventLog.formattedTime(it.time)}\n${it.type}: ${it.details}"
            }
        AlertDialog.Builder(this)
            .setTitle("Historia zdarzeń (${events.size})")
            .setMessage(content)
            .setPositiveButton("Zamknij", null)
            .setNeutralButton("Wyczyść") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Wyczyścić historię?")
                    .setMessage("Usunie to lokalny dziennik zdarzeń.")
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Wyczyść") { _, _ ->
                        getSharedPreferences("anti_thief_event_log", MODE_PRIVATE).edit().clear().apply()
                        EventLog.add(this, "HISTORY_CLEARED", "Historia została wyczyszczona")
                        Toast.makeText(this, "Historia wyczyszczona.", Toast.LENGTH_SHORT).show()
                    }.show()
            }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun showDiagnostics() {
        val sensorOk = accelerometer != null
        val connected = try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.activeNetworkInfo?.isConnected == true
        } catch (_: Exception) { false }
        val storage = try {
            val stats = StatFs(filesDir.absolutePath)
            (stats.availableBytes / (1024 * 1024)).toString() + " MB wolnego miejsca"
        } catch (_: Exception) { "Nie udało się odczytać" }
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "nieznana"
        } catch (_: Exception) { "nieznana" }
        val adminActive = try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            dpm.isAdminActive(android.content.ComponentName(this, AntiThiefAdminReceiver::class.java))
        } catch (_: Exception) { false }
        val report = listOf(
            "Wersja aplikacji: " + version,
            "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
            "Czujnik przyspieszenia: " + if (sensorOk) "dostępny" else "BRAK",
            "Internet: " + if (connected) "połączony" else "brak połączenia",
            "Pamięć aplikacji: " + storage,
            "Administrator urządzenia: " + if (adminActive) "włączony" else "wyłączony",
            "Historia zdarzeń: szyfrowana lokalnie",
            "Monitoring ruchu: działa tylko, gdy aplikacja jest otwarta",
            "Automatyczne zdjęcia, nagrywanie i wysyłanie lokalizacji: nieaktywne"
        ).joinToString("\n\n")
        AlertDialog.Builder(this)
            .setTitle("Diagnostyka AntiThiefPRO")
            .setMessage(report)
            .setPositiveButton("OK", null)
            .setNeutralButton(if (adminActive) "Wyłącz administratora" else "Aktywuj administratora") { _, _ ->
                val component = android.content.ComponentName(this, AntiThiefAdminReceiver::class.java)
                if (adminActive) {
                    startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                    Toast.makeText(this, "W ustawieniach otwórz aplikacje administratora urządzenia i wyłącz AntiThiefPRO przed odinstalowaniem.", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "AntiThiefPRO używa standardowego uprawnienia administratora Androida. Nie gwarantuje ono zablokowania odinstalowania aplikacji.")
                    startActivity(intent)
                }
            }
            .show()
    }

    private fun beginExport() {
        val events = EventLog.read(this)
        if (events.isEmpty()) {
            Toast.makeText(this, "Historia jest pusta.", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = mutableListOf("timestamp,type,details")
        events.forEach { event ->
            fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
            rows.add(quote(EventLog.formattedTime(event.time)) + "," + quote(event.type) + "," + quote(event.details))
        }
        pendingCsv = rows.joinToString("\r\n")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "AntiThiefPRO-raport.csv")
        }
        try {
            startActivityForResult(intent, exportRequestCode)
        } catch (_: Exception) {
            Toast.makeText(this, "Nie można otworzyć okna zapisu pliku.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android API; retained for compatibility without extra dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequestCode && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openOutputStream(data.data!!)?.use {
                    it.write(pendingCsv.toByteArray(Charsets.UTF_8))
                }
                EventLog.add(this, "REPORT_EXPORTED", "Wyeksportowano historię do CSV")
                Toast.makeText(this, "Raport zapisany.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Nie udało się zapisać raportu.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onPause() {
        super.onPause()
        // This version intentionally monitors motion only while the app remains open.
    }

    override fun onDestroy() {
        handler.removeCallbacks(countdownTask)
        handler.removeCallbacks(alarmTask)
        sensorManager.unregisterListener(this)
        tone.release()
        super.onDestroy()
    }

    companion object {
        private const val BG = 0xFF0B1220.toInt()
        private const val CARD = 0xFF131F33.toInt()
        private const val BORDER = 0xFF26364F.toInt()
        private const val WHITE = 0xFFF4F7FB.toInt()
        private const val MUTED = 0xFF9AAAC0.toInt()
        private const val ACCENT = 0xFF3478F6.toInt()
        private const val GREEN = 0xFF4ADE80.toInt()
        private const val AMBER = 0xFFFBBF24.toInt()
        private const val RED = 0xFFFB5252.toInt()
    }
}
