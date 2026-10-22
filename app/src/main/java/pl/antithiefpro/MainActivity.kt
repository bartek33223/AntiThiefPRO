package pl.antithiefpro

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ContentValues
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider

class MainActivity : ComponentActivity() {
    private lateinit var content: LinearLayout
    private lateinit var navigation: LinearLayout
    private var page = "Pulpit"
    private var cameraBusy = false
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else toast("Bez dostępu do aparatu zdjęcie nie powstanie.")
    }

    private val background = Color.rgb(8, 13, 25)
    private val surface = Color.rgb(17, 26, 44)
    private val raised = Color.rgb(24, 37, 59)
    private val border = Color.rgb(42, 60, 87)
    private val ink = Color.rgb(241, 246, 255)
    private val muted = Color.rgb(151, 169, 196)
    private val mint = Color.rgb(83, 239, 190)
    private val blue = Color.rgb(107, 177, 255)
    private val red = Color.rgb(255, 117, 135)
    private val amber = Color.rgb(255, 194, 103)
    private val purple = Color.rgb(190, 157, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            windowInsets
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(17), dp(20), dp(15))
        }
        header.addView(label("A", 23f, mint, true).apply {
            gravity = Gravity.CENTER
            setBackground(rounded(Color.rgb(18, 66, 61), 18))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(label("AntiThiefPRO", 20f, ink, true))
            addView(label("OCHRONA URZĄDZENIA", 9f, muted, true))
        })
        header.addView(label("●", 15f, mint, true).apply {
            gravity = Gravity.CENTER
            setBackground(rounded(raised, 15))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
        root.addView(header)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(15), 0, dp(15), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll)
        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackground(rounded(Color.rgb(13, 21, 36), 20))
        }
        root.addView(LinearLayout(this).apply {
            setPadding(dp(9), dp(2), dp(9), dp(9))
            addView(navigation)
        })
        setContentView(root)
        render("Pulpit", false)
    }

    private fun render(destination: String, animate: Boolean = true) {
        page = destination
        content.removeAllViews()
        navigation.removeAllViews()
        listOf("Pulpit" to "⌂", "Aparat" to "◎", "Historia" to "◷", "Ochrona" to "◇", "Ustawienia" to "⚙").forEach { item ->
            val selected = item.first == page
            navigation.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(8), dp(3), dp(8))
                setBackground(rounded(if (selected) Color.rgb(22, 58, 54) else Color.TRANSPARENT, 14))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnClickListener { render(item.first) }
                addView(label(item.second, 20f, if (selected) mint else muted, true).apply { gravity = Gravity.CENTER })
                addView(label(item.first, 9f, if (selected) mint else muted, true).apply { gravity = Gravity.CENTER })
            })
        }
        when (page) {
            "Pulpit" -> dashboard()
            "Aparat" -> cameraPage()
            "Historia" -> historyPage()
            "Ochrona" -> protectionPage()
            "Ustawienia" -> settingsPage()
        }
        if (animate) {
            content.alpha = 0f
            content.translationY = dp(5).toFloat()
            content.animate().alpha(1f).translationY(0f).setDuration(170).start()
        }
    }

    private fun dashboard() {
        val events = EventLog.read(this).asReversed()
        val failures = events.count { it.type == "DEVICE_UNLOCK_FAILURE" }
        card {
            addView(label("CENTRUM BEZPIECZEŃSTWA", 10f, mint, true))
            addView(label("Twój telefon.\nPod Twoją kontrolą.", 27f, ink, true).apply {
                setPadding(0, dp(13), 0, dp(8))
            })
            addView(label("Monitoruj próby odblokowania i zarządzaj materiałami z aparatu.", 13f, muted))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(18), 0, 0)
                addView(metric(failures.toString(), "Błędne próby", red), LinearLayout.LayoutParams(0, -2, 1f))
                addView(gap(7))
                addView(metric(events.size.toString(), "Zdarzenia", blue), LinearLayout.LayoutParams(0, -2, 1f))
                addView(gap(7))
                addView(metric(if (adminEnabled()) "ON" else "OFF", "Administrator", if (adminEnabled()) mint else amber), LinearLayout.LayoutParams(0, -2, 1f))
            })
        }
        sectionTitle("SZYBKI DOSTĘP", "Najważniejsze narzędzia")
        val row1 = horizontal()
        row1.addView(actionTile("◎", "Aparat", "Zrób zdjęcie", purple) { render("Aparat") }, weight())
        row1.addView(gap(9))
        row1.addView(actionTile("◇", "Ochrona", "Administrator", mint) { render("Ochrona") }, weight())
        content.addView(row1)
        val row2 = horizontal().apply { setPadding(0, dp(9), 0, 0) }
        row2.addView(actionTile("◷", "Historia", "Dziennik zdarzeń", blue) { render("Historia") }, weight())
        row2.addView(gap(9))
        row2.addView(actionTile("⚙", "Ustawienia", "Konfiguracja", amber) { render("Ustawienia") }, weight())
        content.addView(row2)
        sectionTitle("OSTATNIA AKTYWNOŚĆ", "Zapisane lokalnie")
        if (events.isEmpty()) noteCard("Na razie spokojnie", "Zdarzenia bezpieczeństwa pojawią się tutaj po aktywowaniu administratora urządzenia.")
        events.take(3).forEach { event ->
            card {
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(label("●", 14f, if (event.type == "DEVICE_UNLOCK_FAILURE") red else mint, true))
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                        addView(label(event.type.replace('_', ' '), 12f, ink, true))
                        addView(label(event.details, 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
                    })
                })
                addView(label(EventLog.formattedTime(event.time), 10f, muted).apply { setPadding(0, dp(9), 0, 0) })
            }
        }
    }

    private fun cameraPage() {
        pageHeading("Aparat", "Zdjęcia zapisywane w galerii")
        card {
            addView(label("PRZEDNIA KAMERA", 10f, mint, true))
            addView(label("Złap ważny moment.", 23f, ink, true).apply { setPadding(0, dp(8), 0, dp(6)) })
            addView(label("AntiThiefPRO użyje przedniej kamery, jeśli jest dostępna. Zdjęcia trafią do Pictures/AntiThiefPRO.", 13f, muted))
            addView(primaryButton("ZRÓB ZDJĘCIE", mint) { takePhoto() })
        }
        sectionTitle("WAŻNE", "Ograniczenia Androida")
        noteCard("Ekran blokady", "Samo wykrycie błędnego PIN-u nie daje aplikacji automatycznego dostępu do aparatu w tle. Funkcję zdjęcia po błędnym odblokowaniu trzeba zweryfikować na telefonie; nie oznaczamy jej jako działającej, dopóki test tego nie potwierdzi.")
        metricWide(photoCount().toString(), "Zdjęcia AntiThiefPRO w galerii", purple)
    }

    private fun protectionPage() {
        pageHeading("Ochrona", "Wykrywanie błędnych prób odblokowania")
        val enabled = adminEnabled()
        card {
            addView(label(if (enabled) "● OCHRONA SYSTEMOWA AKTYWNA" else "○ WYMAGANA AKTYWACJA", 11f, if (enabled) mint else amber, true))
            addView(label(if (enabled) "Administrator urządzenia jest włączony." else "Włącz administratora, aby Android mógł przekazywać aplikacji informacje o nieudanych próbach.", 15f, ink, true).apply { setPadding(0, dp(12), 0, dp(8)) })
            addView(primaryButton(if (enabled) "OTWÓRZ USTAWIENIA ADMINISTRATORA" else "AKTYWUJ ADMINISTRATORA", if (enabled) blue else mint) {
                try {
                    val component = ComponentName(this@MainActivity, AntiThiefAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "AntiThiefPRO rejestruje nieudane próby odblokowania telefonu.")
                    startActivity(intent)
                } catch (_: Exception) { toast("Nie można otworzyć ustawień administratora.") }
            })
        }
        sectionTitle("JAK TO DZIAŁA", "Bez obietnic bez pokrycia")
        noteCard("01 · Wykrywanie", "Android może zgłosić błędną próbę PIN-u, wzoru lub hasła, gdy administrator urządzenia jest aktywny.")
        noteCard("02 · Historia", "Zdarzenia są zapisywane lokalnie i szyfrowane kluczem Android Keystore.")
        noteCard("03 · Zdjęcia", "Zdjęcie z poziomu aplikacji działa po przyznaniu dostępu do aparatu. Automatyczne zdjęcie z ekranu blokady wymaga osobnego testu na Androidzie 16.")
    }

    private fun historyPage() {
        pageHeading("Historia zdarzeń", "Dziennik lokalny i szyfrowany")
        val events = EventLog.read(this).asReversed()
        card {
            addView(label("${events.size} / 200 zdarzeń", 13f, muted, true))
            addView(primaryButton("WYCZYŚĆ HISTORIĘ", red) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Wyczyścić historię?")
                    .setMessage("Ta operacja usunie lokalny dziennik zdarzeń.")
                    .setNegativeButton("Anuluj", null)
                    .setPositiveButton("Wyczyść") { _, _ -> EventLog.clear(this@MainActivity); render("Historia") }
                    .show()
            })
        }
        if (events.isEmpty()) noteCard("Brak wpisów", "Gdy pojawią się zdarzenia, zobaczysz je tutaj.")
        events.forEach { event ->
            card {
                addView(label(event.type.replace('_', ' '), 13f, if (event.type == "DEVICE_UNLOCK_FAILURE") red else ink, true))
                addView(label(EventLog.formattedTime(event.time), 11f, muted).apply { setPadding(0, dp(5), 0, dp(5)) })
                addView(label(event.details, 12f, muted))
            }
        }
    }

    private fun settingsPage() {
        pageHeading("Ustawienia", "Prywatność i stan aplikacji")
        noteCard("Dane lokalne", "Dziennik zdarzeń jest szyfrowany przez Android Keystore. Zdjęcia zapisują się w galerii w folderze Pictures/AntiThiefPRO.")
        card {
            addView(label("UPRAWNIENIA", 10f, mint, true))
            settingLine("Administrator urządzenia", if (adminEnabled()) "Aktywny" else "Nieaktywny", if (adminEnabled()) mint else amber)
            val cameraAllowed = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            settingLine("Dostęp do aparatu", if (cameraAllowed) "Przyznany" else "Wymagany", if (cameraAllowed) mint else amber)
            addView(primaryButton("SPRAWDŹ OCHRONĘ", blue) { render("Ochrona") })
        }
        card {
            addView(label("INFORMACJE", 10f, mint, true))
            addView(label("AntiThiefPRO", 19f, ink, true).apply { setPadding(0, dp(8), 0, dp(3)) })
            addView(label("Wersja 4.0 • Android 8+", 12f, muted))
        }
    }

    private fun takePhoto() {
        if (cameraBusy) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        cameraBusy = true
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
                    CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, capture)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "AntiThiefPRO_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AntiThiefPRO")
                }
                val output = ImageCapture.OutputFileOptions.Builder(
                    contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ).build()
                capture.takePicture(output, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        cameraBusy = false
                        EventLog.add(this@MainActivity, "PHOTO_CAPTURED", "Zapisano zdjęcie aparatem aplikacji")
                        toast("Zdjęcie zapisane w galerii")
                        render("Aparat")
                    }
                    override fun onError(exception: ImageCaptureException) {
                        cameraBusy = false
                        toast("Nie udało się zrobić zdjęcia: ${exception.message ?: "błąd aparatu"}")
                    }
                })
            } catch (e: Exception) {
                cameraBusy = false
                toast("Aparat niedostępny: ${e.message ?: "nieznany błąd"}")
            }
        }, mainExecutor)
    }


    private fun adminEnabled(): Boolean {
        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return manager.isAdminActive(ComponentName(this, AntiThiefAdminReceiver::class.java))
    }

    private fun photoCount(): Int = try {
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?",
            arrayOf("AntiThiefPRO_%"), null
        )?.use { it.count } ?: 0
    } catch (_: Exception) { 0 }

    private fun pageHeading(title: String, subtitle: String) {
        content.addView(label(title, 27f, ink, true).apply { setPadding(dp(3), dp(8), 0, dp(3)) })
        content.addView(label(subtitle, 13f, muted).apply { setPadding(dp(3), 0, 0, dp(13)) })
    }

    private fun sectionTitle(title: String, subtitle: String) {
        content.addView(label(title, 10f, mint, true).apply { setPadding(dp(3), dp(19), 0, dp(4)) })
        content.addView(label(subtitle, 17f, ink, true).apply { setPadding(dp(3), 0, 0, dp(10)) })
    }

    private fun noteCard(title: String, body: String) {
        card {
            addView(label(title, 14f, ink, true))
            addView(label(body, 12f, muted).apply { setPadding(0, dp(7), 0, 0) })
        }
    }

    private fun metric(value: String, caption: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(10), dp(12), dp(6), dp(12))
        setBackground(rounded(raised, 13))
        addView(label(value, 21f, color, true))
        addView(label(caption, 9f, muted).apply { setPadding(0, dp(3), 0, 0) })
    }

    private fun metricWide(value: String, caption: String, color: Int) {
        card {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(value, 28f, color, true))
            addView(label(caption, 12f, muted).apply { setPadding(dp(12), 0, 0, 0) })
        }
    }

    private fun actionTile(symbol: String, title: String, subtitle: String, color: Int, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(15), dp(12), dp(15))
            setBackground(rounded(surface, 18, border))
            setOnClickListener { action() }
            addView(label(symbol, 23f, color, true))
            addView(label(title, 14f, ink, true).apply { setPadding(0, dp(12), 0, dp(3)) })
            addView(label(subtitle, 10f, muted))
        }

    private fun settingLine(name: String, status: String, color: Int) {
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(5))
            addView(label(name, 12f, ink), LinearLayout.LayoutParams(0, -2, 1f))
            addView(label(status, 11f, color, true))
        })
    }

    private fun primaryButton(caption: String, color: Int, action: () -> Unit): TextView =
        label(caption, 11f, background, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(14), dp(13), dp(14))
            setBackground(rounded(color, 13))
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) }
        }

    private fun card(block: LinearLayout.() -> Unit) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackground(rounded(surface, 20, border))
            block()
        }
        content.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
    }

    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun weight() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun gap(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
