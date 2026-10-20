package pl.antithiefpro

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var statusText: TextView
    private lateinit var tone: ToneGenerator
    private var armed = false
    private var alarmOn = false
    private var ignoreMotionUntil = 0L
    private var pin = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        pin = getSharedPreferences("anti_thief", Context.MODE_PRIVATE).getString("pin", "1234") ?: "1234"
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
            setBackgroundColor(0xFF101820.toInt())
        }
        root.addView(TextView(this).apply {
            text = "AntiThiefPRO"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Alarm ruchowy na telefon"
            textSize = 16f
            setTextColor(0xFFB8C7D9.toInt())
            gravity = Gravity.CENTER
        })
        statusText = TextView(this).apply {
            text = "Status: rozbrojony"
            textSize = 20f
            setTextColor(0xFF8FE3A0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 28)
        }
        root.addView(statusText)
        root.addView(Button(this).apply {
            text = "UZBRÓJ ALARM"
            setOnClickListener { armAlarm() }
        })
        root.addView(Button(this).apply {
            text = "ROZBRÓJ / STOP"
            setOnClickListener { askForPin { disarmAlarm() } }
        })
        root.addView(Button(this).apply {
            text = "ZMIEŃ KOD PIN"
            setOnClickListener { changePin() }
        })
        root.addView(TextView(this).apply {
            text = "Domyślny PIN: 1234. Zmień go przed użyciem. Alarm działa, gdy aplikacja jest otwarta."
            textSize = 13f
            setTextColor(0xFFD5DCE5.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 0)
        })
        setContentView(root)
    }

    private fun armAlarm() {
        if (accelerometer == null) {
            Toast.makeText(this, "Ten telefon nie ma czujnika ruchu.", Toast.LENGTH_LONG).show()
            return
        }
        if (armed) return
        alarmOn = false
        armed = true
        ignoreMotionUntil = System.currentTimeMillis() + 1800L
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        statusText.text = "Status: UZBROJONY"
        statusText.setTextColor(0xFFFFC857.toInt())
        Toast.makeText(this, "Alarm uzbrojony. Odłóż telefon i nie dotykaj go.", Toast.LENGTH_LONG).show()
    }

    private fun disarmAlarm() {
        armed = false
        alarmOn = false
        sensorManager.unregisterListener(this)
        tone.stopTone()
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        statusText.text = "Status: rozbrojony"
        statusText.setTextColor(0xFF8FE3A0.toInt())
    }

    private fun askForPin(onSuccess: () -> Unit) {
        val field = EditText(this).apply {
            hint = "Wpisz PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Potwierdź PIN")
            .setView(field)
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("OK") { _, _ ->
                if (field.text.toString() == pin) onSuccess()
                else Toast.makeText(this, "Nieprawidłowy PIN.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun changePin() {
        askForPin {
            val field = EditText(this).apply {
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
        if (abs(magnitude - SensorManager.GRAVITY_EARTH) > 2.8f) triggerAlarm()
    }

    private fun triggerAlarm() {
        alarmOn = true
        statusText.text = "ALARM! WYKRYTO RUCH"
        statusText.setTextColor(0xFFFF5555.toInt())
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 60000)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 350, 180, 350, 180, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onPause() {
        super.onPause()
        // This starter version monitors movement only while the app remains open.
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        tone.release()
        super.onDestroy()
    }
}
