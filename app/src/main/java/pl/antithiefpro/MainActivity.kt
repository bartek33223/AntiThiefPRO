package pl.antithiefpro

import android.Manifest
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ContentValues
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("anti_thief_settings", Context.MODE_PRIVATE) }
    private lateinit var body: LinearLayout
    private lateinit var nav: LinearLayout
    private var page = "Główna"
    private var activeRecording: Recording? = null
    private var recording = false
    private var busy = false
    private var eventSearch = ""
    private var mediaSearch = ""
    private var mediaFilter = "Wszystkie"

    private val bg = Color.rgb(7, 13, 24)
    private val panel = Color.rgb(15, 27, 44)
    private val tile = Color.rgb(21, 37, 58)
    private val edge = Color.rgb(39, 59, 83)
    private val mint = Color.rgb(91, 245, 190)
    private val blue = Color.rgb(108, 184, 255)
    private val violet = Color.rgb(188, 160, 255)
    private val orange = Color.rgb(255, 190, 100)
    private val red = Color.rgb(255, 115, 128)
    private val white = Color.rgb(242, 247, 255)
    private val muted = Color.rgb(145, 165, 190)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(12))
        }
        header.addView(label("A", 25f, mint, true).apply {
            gravity = Gravity.CENTER; background = shape(Color.rgb(22, 66, 60), 16)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        })
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(label("AntiThiefPRO", 21f, white, true))
            addView(label("PERSONAL SECURITY SYSTEM", 9f, muted, true).apply { letterSpacing = .12f })
        }
        header.addView(brand)
        header.addView(button("⚙", violet) { show("Ustawienia") }.apply { layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)) })
        root.addView(header)
        val scroll = ScrollView(this).apply {
            isFillViewport = true; clipToPadding = false
            setPadding(dp(15), 0, dp(15), dp(10))
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
        scroll.addView(body); root.addView(scroll)
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5)); background = shape(Color.rgb(12, 21, 35), 18)
        }
        root.addView(LinearLayout(this).apply { setPadding(dp(8), dp(3), dp(8), dp(8)); addView(nav) })
        setContentView(root)
        show("Główna", false)
    }

    private fun show(destination: String, animate: Boolean = true) {
        page = destination
        body.removeAllViews()
        nav.removeAllViews()
        listOf("Główna" to "⌂", "Aparat" to "▣", "Moduły" to "▦", "Historia" to "◷", "Ustawienia" to "⚙").forEach { item ->
            val selected = page == item.first || (item.first == "Moduły" && page in listOf("Antykradzież", "Lokalizacja", "Synchronizacja", "Diagnostyka", "Profile"))
            nav.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(4), dp(7), dp(4), dp(7))
                background = shape(if (selected) Color.rgb(25, 56, 52) else Color.TRANSPARENT, 12)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnClickListener { show(item.first) }
                addView(label(item.second, 21f, if (selected) mint else muted, true).apply { gravity = Gravity.CENTER })
                addView(label(item.first, 9f, if (selected) mint else muted, true).apply { gravity = Gravity.CENTER })
            })
        }
        when (destination) {
            "Główna" -> home()
            "Aparat" -> cameraPage()
            "Historia" -> historyPage()
            "Antykradzież" -> protectionPage()
            "Lokalizacja" -> locationPage()
            "Moduły" -> modulesPage()
            "Synchronizacja" -> syncPage()
            "Diagnostyka" -> diagnosticsPage()
            "Profile" -> profilesPage()
            "Ustawienia" -> settingsPage()
            else -> home()
        }
        if (animate) { body.alpha = 0f; body.translationY = dp(7).toFloat(); body.animate().alpha(1f).translationY(0f).setDuration(180).start() }
    }

    private fun home() {
        val events = EventLog.read(this).asReversed()
        val failed = events.count { it.type == "DEVICE_UNLOCK_FAILURE" }
        card().apply {
            addView(row().apply {
                addView(label("CENTRUM BEZPIECZEŃSTWA", 11f, white, true), weight())
                addView(chip(if (adminOn()) "ADMIN AKTYWNY" else "KONFIGURUJ", if (adminOn()) mint else orange))
            })
            addView(label("Ochrona telefonu.", 26f, white, true).apply { setPadding(0, dp(16), 0, dp(4)) })
            addView(label("Narzędzia bezpieczeństwa, multimedia i zdarzenia w jednym miejscu.", 13f, muted))
            addView(row().apply {
                setPadding(0, dp(17), 0, dp(4))
                addView(metric(failed.toString(), "Błędne próby", red), weight())
                addView(gap(7))
                addView(metric(events.size.toString(), "Zdarzenia", blue), weight())
                addView(gap(7))
                addView(metric(if (recording) "REC" else "GOTOWE", "Aparat", mint), weight())
            })
            addView(button("Otwórz aparat", mint) { show("Aparat") })
            addView(button("Ustaw ochronę", blue) { show("Antykradzież") })
            body.addView(this)
        }
        heading("SZYBKIE MODUŁY")
        val first = row()
        first.addView(module("▣", "Aparat", "Przednia kamera", violet) { show("Aparat") }, weight())
        first.addView(gap(8))
        first.addView(module("⌕", "Antykradzież", "Próby odblokowania", red) { show("Antykradzież") }, weight())
        body.addView(first)
        val second = row().apply { setPadding(0, dp(8), 0, 0) }
        second.addView(module("⌖", "Lokalizacja", "Pozycja telefonu", blue) { show("Lokalizacja") }, weight())
        second.addView(gap(8))
        second.addView(module("◷", "Historia", "Dziennik zdarzeń", orange) { show("Historia") }, weight())
        body.addView(second)
        heading("OSTATNIE ZDARZENIA")
        if (events.isEmpty()) body.addView(note("Brak zdarzeń. Po aktywacji administratora Androida błędne próby mogą trafiać do historii."))
        events.take(4).forEach { event ->
            card().apply {
                addView(label(event.type.replace('_', ' '), 13f, white, true))
                addView(label(EventLog.formattedTime(event.time), 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
                addView(label(event.details, 12f, muted).apply { setPadding(0, dp(4), 0, 0) })
                body.addView(this)
            }
        }
    }

    private fun cameraPage() {
        title("Aparat i multimedia", "Zdjęcia i filmy z przedniej kamery", "▣", violet)
        card().apply {
            addView(label("PRZEDNIA KAMERA", 12f, mint, true))
            addView(label("Zdjęcia i filmy zapisują się w galerii, bez otwierania zewnętrznej aplikacji aparatu.", 13f, muted).apply { setPadding(0, dp(8), 0, dp(5)) })
            addView(button("📸 ZRÓB ZDJĘCIE", mint) { capturePhoto() })
            addView(button(if (recording) "■ ZATRZYMAJ FILM" else "● NAGRAJ FILM", if (recording) red else blue) { if (recording) stopVideo() else startVideo() })
            addView(note("Android ogranicza dostęp do kamery z tła. Błędne hasła są rejestrowane w historii, ale nie obiecuję automatycznego zdjęcia, gdy telefon jest zablokowany."))
            body.addView(this)
        }
        heading("ZAPISANE PLIKI")
        row().apply {
            listOf("Wszystkie", "Zdjęcia", "Filmy").forEach { f ->
                addView(button(if (mediaFilter == f) "✓ " + f else f, if (mediaFilter == f) mint else blue) { mediaFilter = f; show("Aparat") }, weight())
                addView(gap(4))
            }
            body.addView(this)
        }
        val search = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(white); setHintTextColor(muted)
            hint = "Szukaj nazwy pliku"; setText(mediaSearch); setPadding(dp(12), dp(11), dp(12), dp(11))
            background = shape(tile, 12, edge)
        }
        body.addView(search)
        body.addView(button("Szukaj", violet) { mediaSearch = search.text.toString().trim(); show("Aparat") })
        val files = mediaFiles().filter { f ->
            (mediaFilter == "Wszystkie" || (mediaFilter == "Zdjęcia" && !f.second) || (mediaFilter == "Filmy" && f.second)) && f.first.contains(mediaSearch, true)
        }
        body.addView(label(files.size.toString() + " plików", 12f, muted).apply { setPadding(0, dp(12), 0, dp(7)) })
        if (files.isEmpty()) body.addView(note("Nie znaleziono plików dla tego filtra."))
        files.forEach { f ->
            card().apply {
                addView(label(if (f.second) "▶  " + f.first else "▣  " + f.first, 12f, white, true))
                addView(button("Otwórz", blue) { openFile(f.third) })
                addView(button("Udostępnij", violet) { shareFile(f.third) })
                body.addView(this)
            }
        }
    }

    private fun mediaFiles(): List<Triple<String, Boolean, Uri>> {
        val out = mutableListOf<Triple<String, Boolean, Uri>>()
        val sources = listOf(
            Triple(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)),
            Triple(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME))
        )
        sources.forEach { (uri, video, columns) ->
            try {
                contentResolver.query(uri, columns, columns[1] + " LIKE ?", arrayOf("AntiThiefPRO%"), columns[0] + " DESC")?.use { c ->
                    while (c.moveToNext()) out.add(Triple(c.getString(1) ?: "plik", video, Uri.withAppendedPath(uri, c.getLong(0).toString())))
                }
            } catch (_: Exception) { }
        }
        return out
    }

    private fun capturePhoto() {
        if (!has(Manifest.permission.CAMERA)) { requestPermissions(arrayOf(Manifest.permission.CAMERA), 810); return }
        if (busy) return
        busy = true
        cameraProvider { provider ->
            try {
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, capture)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "AntiThiefPRO_photo_" + System.currentTimeMillis() + ".jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AntiThiefPRO")
                }
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build(),
                    mainExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                            busy = false; EventLog.add(this@MainActivity, "PHOTO_CAPTURED", "Zapisano zdjęcie przednią kamerą")
                            toast("Zdjęcie zapisane"); show("Aparat")
                        }
                        override fun onError(error: ImageCaptureException) { busy = false; toast("Błąd zdjęcia: " + error.message) }
                    })
            } catch (e: Exception) { busy = false; toast("Aparat niedostępny: " + e.message) }
        }
    }

    private fun startVideo() {
        if (!has(Manifest.permission.CAMERA)) { requestPermissions(arrayOf(Manifest.permission.CAMERA), 810); return }
        if (busy || recording) return
        busy = true
        cameraProvider { provider ->
            try {
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                val video = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, video)
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "AntiThiefPRO_video_" + System.currentTimeMillis() + ".mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AntiThiefPRO")
                }
                val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
                activeRecording = video.output.prepareRecording(this, output).start(mainExecutor) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> { recording = true; busy = false; EventLog.add(this, "VIDEO_STARTED", "Nagrywanie przednią kamerą"); show("Aparat") }
                        is VideoRecordEvent.Finalize -> {
                            recording = false; activeRecording = null; busy = false
                            if (!event.hasError()) { EventLog.add(this, "VIDEO_SAVED", "Zapisano film"); toast("Film zapisany") } else toast("Błąd zapisu filmu")
                            show("Aparat")
                        }
                    }
                }
            } catch (e: Exception) { busy = false; toast("Nie można nagrać filmu: " + e.message) }
        }
    }

    private fun stopVideo() { activeRecording?.stop() }

    private fun cameraProvider(ready: (ProcessCameraProvider) -> Unit) {
        try {
            val future = ProcessCameraProvider.getInstance(this)
            future.addListener({ try { ready(future.get()) } catch (_: Exception) { busy = false; toast("Aparat niedostępny") } }, mainExecutor)
        } catch (_: Exception) { busy = false; toast("Nie można uruchomić aparatu") }
    }

    private fun openFile(uri: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
        catch (_: Exception) { toast("Brak aplikacji do otwarcia pliku") }
    }

    private fun shareFile(uri: Uri) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(contentResolver.getType(uri) ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Udostępnij plik"))
    }

    private fun protectionPage() {
        title("Antykradzież", "Błędne próby i konfiguracja ochrony", "⌕", red)
        card().apply {
            addView(row().apply {
                addView(label("Administrator urządzenia", 14f, white, true), weight())
                addView(chip(if (adminOn()) "AKTYWNY" else "WYŁĄCZONY", if (adminOn()) mint else orange))
            })
            addView(label("Zapisane nieudane próby: " + EventLog.read(this@MainActivity).count { it.type == "DEVICE_UNLOCK_FAILURE" }, 13f, muted).apply { setPadding(0, dp(10), 0, 0) })
            addView(button(if (adminOn()) "Otwórz ustawienia administratora" else "Włącz administratora", mint) { enableAdmin() })
            addView(button("Nadaj uprawnienia", blue) { requestProtectionPermissions() })
            addView(note("Aplikacja nie blokuje telefonu po pierwszej próbie. Czasowe blokady ekranu kontroluje Android. Aby raportować błędne hasła, ustaw PIN, hasło lub wzór blokady i aktywuj administratora urządzenia."))
            body.addView(this)
        }
        heading("WAŻNE O OCHRONIE")
        body.addView(note("Android 14 i nowsze ogranicza uruchamianie aparatu z tła. Dlatego nie da się zagwarantować automatycznego zdjęcia po błędnym haśle, gdy telefon jest zablokowany. Zdjęcia ręczne używają przedniej kamery, jeśli jest dostępna."))
        body.addView(module("◷", "Historia prób", "Sprawdź zapisane zdarzenia", blue) { show("Historia") })
    }

    private fun locationPage() {
        title("Lokalizacja", "Ostatnia znana pozycja telefonu", "⌖", blue)
        card().apply {
            addView(label("Pozycja jest pobierana na żądanie.", 13f, muted))
            addView(button("Pobierz lokalizację", blue) {
                if (!has(Manifest.permission.ACCESS_FINE_LOCATION) && !has(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 811)
                } else try {
                    val manager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                    val location = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER).firstNotNullOfOrNull { p ->
                        try { if (manager.isProviderEnabled(p)) manager.getLastKnownLocation(p) else null } catch (_: SecurityException) { null }
                    }
                    if (location == null) toast("Brak ostatniej pozycji. Włącz lokalizację.")
                    else {
                        val coords = "Szerokość: " + location.latitude + "\nDługość: " + location.longitude
                        EventLog.add(this@MainActivity, "LOCATION_READ", coords)
                        AlertDialog.Builder(this@MainActivity).setTitle("Pozycja telefonu").setMessage(coords).setPositiveButton("OK", null).show()
                    }
                } catch (_: Exception) { toast("Nie udało się pobrać lokalizacji") }
            })
            body.addView(this)
        }
    }

    private fun modulesPage() {
        title("Wszystkie moduły", "Szybki dostęp do narzędzi", "▦", mint)
        listOf(
            Triple("▣", "Aparat i multimedia", "Zdjęcia i filmy") to "Aparat",
            Triple("⌕", "Antykradzież", "Nieudane próby odblokowania") to "Antykradzież",
            Triple("⌖", "Lokalizacja", "Pozycja telefonu") to "Lokalizacja",
            Triple("◷", "Historia", "Dziennik zdarzeń") to "Historia",
            Triple("⇧", "Synchronizacja", "Raport e-mail") to "Synchronizacja",
            Triple("⬡", "Diagnostyka", "Stan uprawnień") to "Diagnostyka",
            Triple("⌁", "Profile ochrony", "Zapisane konfiguracje") to "Profile",
            Triple("⚙", "Ustawienia", "Opcje aplikacji") to "Ustawienia"
        ).forEach { (item, dest) -> body.addView(module(item.first, item.second, item.third, blue) { show(dest) }) }
    }

    private fun historyPage() {
        title("Historia zdarzeń", "Lokalny dziennik aplikacji", "◷", blue)
        val search = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(white); setHintTextColor(muted)
            hint = "Szukaj zdarzeń"; setText(eventSearch); setPadding(dp(12), dp(11), dp(12), dp(11)); background = shape(tile, 12, edge)
        }
        body.addView(search)
        body.addView(button("Filtruj", violet) { eventSearch = search.text.toString().trim(); show("Historia") })
        val events = EventLog.read(this).asReversed().filter { it.type.contains(eventSearch, true) || it.details.contains(eventSearch, true) }
        body.addView(label(events.size.toString() + " zdarzeń", 12f, muted).apply { setPadding(0, dp(12), 0, dp(8)) })
        if (events.isEmpty()) body.addView(note("Brak zdarzeń pasujących do wyszukiwania."))
        events.forEach { event ->
            card().apply {
                addView(label(event.type.replace('_', ' '), 14f, white, true))
                addView(label(EventLog.formattedTime(event.time), 11f, muted).apply { setPadding(0, dp(4), 0, dp(5)) })
                addView(label(event.details, 12f, muted))
                body.addView(this)
            }
        }
        body.addView(button("Wyczyść historię", red) {
            AlertDialog.Builder(this).setTitle("Wyczyścić historię?")
                .setMessage("Usunie to lokalne wpisy dziennika.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Wyczyść") { _, _ -> EventLog.clear(this); EventLog.add(this, "HISTORY_CLEARED", "Ręcznie wyczyszczono historię"); show("Historia") }.show()
        })
        body.addView(button("Eksportuj historię CSV", mint) { exportCsv() })
    }

    private fun exportCsv() {
        val csv = buildString {
            append("data;typ;szczegoly\n")
            EventLog.read(this@MainActivity).forEach { e ->
                append("\"" + EventLog.formattedTime(e.time) + "\";\"" + e.type.replace("\"", "\"\"") + "\";\"" + e.details.replace("\"", "\"\"").replace("\n", " ") + "\"\n")
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "AntiThiefPRO_historia.csv")
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Downloads.RELATIVE_PATH, "Download")
        }
        try {
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) toast("Nie udało się utworzyć CSV")
            else { contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }; toast("CSV zapisany w Pobranych") }
        } catch (e: Exception) { toast("Eksport nieudany: " + e.message) }
    }

    private fun syncPage() {
        title("Synchronizacja", "Przygotuj raport do wysłania", "⇧", mint)
        card().apply {
            addView(note("Raport otwiera aplikację pocztową. Wiadomość wysyłasz ręcznie; automatyczna wysyłka nie jest skonfigurowana."))
            addView(button("Utwórz raport e-mail", mint) {
                val recipient = prefs.getString("recipient_email", "") ?: ""
                val report = EventLog.read(this@MainActivity).takeLast(50).joinToString("\n") {
                    EventLog.formattedTime(it.time) + " | " + it.type + " | " + it.details
                }
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(recipient))).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Raport AntiThiefPRO"); putExtra(Intent.EXTRA_TEXT, report)
                }
                try { startActivity(intent) } catch (_: Exception) { toast("Brak aplikacji pocztowej") }
            })
            body.addView(this)
        }
        body.addView(button("Ustaw adres e-mail", blue) { show("Ustawienia") })
    }

    private fun diagnosticsPage() {
        title("Diagnostyka", "Stan uprawnień i modułów", "⬡", orange)
        listOf(
            "Administrator urządzenia" to adminOn(),
            "Aparat" to has(Manifest.permission.CAMERA),
            "Lokalizacja" to (has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)),
            "Powiadomienia" to (Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS))
        ).forEach { (name, ok) ->
            card().apply {
                addView(row().apply { addView(label(name, 14f, white, true), weight()); addView(chip(if (ok) "GOTOWE" else "BRAK", if (ok) mint else orange)) })
                body.addView(this)
            }
        }
        body.addView(button("Poproś o uprawnienia", blue) { requestProtectionPermissions() })
        body.addView(button("Dodaj zdarzenie testowe", violet) { EventLog.add(this, "TEST_EVENT", "Ręczny test aplikacji"); show("Diagnostyka") })
    }

    private fun profilesPage() {
        title("Profile ochrony", "Wybierz aktywny profil", "⌁", violet)
        val active = prefs.getString("active_profile", "Dom") ?: "Dom"
        listOf("Dom", "Poza domem", "Podróż", "Maksymalna ochrona").forEach { profile ->
            body.addView(module(if (active == profile) "✓" else "◈", profile, if (active == profile) "AKTYWNY" else "Ustaw profil", if (active == profile) mint else blue) {
                prefs.edit().putString("active_profile", profile).apply()
                EventLog.add(this, "PROFILE_CHANGED", "Wybrano profil: " + profile); show("Profile")
            })
        }
    }

    private fun settingsPage() {
        title("Ustawienia", "Konfiguracja aplikacji", "⚙", blue)
        body.addView(module("⬡", "Administrator urządzenia", if (adminOn()) "Aktywny" else "Nieaktywny", mint) { enableAdmin() })
        body.addView(module("▣", "Uprawnienia", "Aparat, lokalizacja, powiadomienia", violet) { requestProtectionPermissions() })
        heading("ADRES E-MAIL RAPORTÓW")
        val email = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(white); setHintTextColor(muted)
            hint = "adres@email.pl"; setText(prefs.getString("recipient_email", "") ?: "")
            setPadding(dp(12), dp(12), dp(12), dp(12)); background = shape(tile, 12, edge)
        }
        body.addView(email)
        body.addView(button("Zapisz adres", mint) {
            val value = email.text.toString().trim()
            if (value.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                prefs.edit().putString("recipient_email", value).apply()
                EventLog.add(this, "EMAIL_SAVED", "Zapisano adres odbiorcy"); toast("Adres zapisany"); show("Ustawienia")
            } else email.error = "Wpisz poprawny adres e-mail"
        })
        body.addView(button("Profile ochrony", violet) { show("Profile") })
        body.addView(button("Otwórz ustawienia systemowe aplikacji", blue) {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
        })
    }

    private fun title(name: String, subtitle: String, symbol: String, color: Int) {
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(2), dp(8), dp(2), dp(14))
            addView(label(symbol, 31f, color, true))
            addView(label(name, 24f, white, true).apply { setPadding(0, dp(6), 0, dp(3)) })
            addView(label(subtitle, 12f, muted))
        })
    }

    private fun heading(value: String) {
        body.addView(label(value, 11f, muted, true).apply { setPadding(dp(3), dp(20), dp(3), dp(9)); letterSpacing = .08f })
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = shape(panel, 17, edge)
        setPadding(dp(15), dp(15), dp(15), dp(15))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
    }

    private fun module(icon: String, name: String, desc: String, color: Int, action: () -> Unit): View = card().apply {
        addView(label(icon, 24f, color, true))
        addView(label(name, 14f, white, true).apply { setPadding(0, dp(8), 0, dp(4)) })
        addView(label(desc, 11f, muted))
        isClickable = true; setOnClickListener { action() }
    }

    private fun metric(value: String, caption: String, color: Int) = card().apply {
        setPadding(dp(10), dp(10), dp(8), dp(10))
        addView(label(value, 19f, color, true))
        addView(label(caption, 10f, muted).apply { setPadding(0, dp(4), 0, 0) })
    }

    private fun note(value: String): View = card().apply { addView(label(value, 12f, muted)) }

    private fun button(value: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = value; textSize = 13f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER; setTextColor(if (color == mint) Color.rgb(5, 32, 25) else white)
        setPadding(dp(12), dp(12), dp(12), dp(12)); background = shape(color, 13)
        isClickable = true; isFocusable = true; setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) }
    }

    private fun chip(value: String, color: Int) = TextView(this).apply {
        text = value; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (color == mint) Color.rgb(5, 32, 25) else Color.rgb(36, 28, 8))
        setPadding(dp(8), dp(6), dp(8), dp(6)); background = shape(color, 20)
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun weight() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun gap(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun shape(color: Int, radius: Int, outline: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (outline != null) setStroke(dp(1), outline)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun has(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun adminOn(): Boolean = try {
        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        manager.isAdminActive(ComponentName(this, AntiThiefAdminReceiver::class.java))
    } catch (_: Exception) { false }

    private fun enableAdmin() {
        val component = ComponentName(this, AntiThiefAdminReceiver::class.java)
        startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "AntiThiefPRO rejestruje błędne próby odblokowania.")
        })
    }

    private fun requestProtectionPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.distinct().filterNot { has(it) }
        if (missing.isEmpty()) toast("Wszystkie wymagane uprawnienia są już nadane.")
        else requestPermissions(missing.toTypedArray(), 812)
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) show(page, false)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (page != "Główna") show("Główna") else super.onBackPressed()
    }
}
