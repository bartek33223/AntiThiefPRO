package pl.antithiefpro

import android.Manifest
import android.content.ContentValues
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.StatFs
import android.provider.Settings
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("anti_thief_settings", Context.MODE_PRIVATE) }
    private lateinit var pageContent: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private var currentPage = "Główna"
    private val photoRequestCode = 520
    private val videoRequestCode = 521
    private val csvRequestCode = 522
    private var pendingMediaUri: Uri? = null
    private var locationStatusView: TextView? = null
    private var mediaFilter = "Wszystkie"
    private var mediaSearch = ""
    private var historySearch = ""

    private val bg = Color.rgb(11, 16, 25)
    private val panel = Color.rgb(18, 31, 44)
    private val tile = Color.rgb(21, 33, 48)
    private val border = Color.rgb(37, 53, 72)
    private val mint = Color.rgb(98, 245, 189)
    private val muted = Color.rgb(145, 166, 185)
    private val white = Color.rgb(245, 248, 252)
    private val blue = Color.rgb(123, 200, 255)
    private val purple = Color.rgb(192, 165, 255)
    private val amber = Color.rgb(255, 202, 122)
    private val red = Color.rgb(255, 138, 138)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildShell()
        showPage("Główna", false)
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
        }
        val logo = TextView(this).apply {
            text = "⬡"
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(mint)
            background = rounded(Color.rgb(23, 58, 54), 18)
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
        }
        header.addView(logo)
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBox.addView(label("AntiThiefPRO", 22f, white, true))
        titleBox.addView(label("CENTRUM BEZPIECZEŃSTWA", 10f, muted, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(3), 0, 0)
        })
        header.addView(titleBox)
        header.addView(iconButton("⚙", "Ustawienia") { showPage("Ustawienia") })
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(16), 0, dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        scroll.addView(pageContent)
        root.addView(scroll)

        bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(7), dp(6), dp(9))
            background = rounded(Color.rgb(15, 24, 36), 20)
        }
        val navWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            addView(bottomNav)
        }
        root.addView(navWrap)
        setContentView(root)
        buildBottomNav()
    }

    private fun buildBottomNav() {
        bottomNav.removeAllViews()
        val entries = listOf("Główna" to "⌂", "Zdjęcia" to "▣", "Moduły" to "▦", "Historia" to "◷", "Ustawienia" to "⚙")
        entries.forEach { (name, symbol) ->
            val selected = currentPage == name || (name == "Moduły" && currentPage in listOf("Lokalizacja", "Mapa", "Antykradzież", "Synchronizacja", "Diagnostyka", "Profile"))
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(6), dp(5), dp(4))
                background = if (selected) rounded(Color.rgb(24, 51, 49), 14) else transparent()
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true
                isFocusable = true
                setOnClickListener { tapAnimation(this); showPage(name) }
            }
            item.addView(label(symbol, 22f, if (selected) mint else muted, true).apply { gravity = Gravity.CENTER })
            item.addView(label(name, 10f, if (selected) mint else muted, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            bottomNav.addView(item)
        }
    }

    private fun showPage(name: String, animate: Boolean = true) {
        currentPage = name
        buildBottomNav()
        pageContent.removeAllViews()
        when (name) {
            "Główna" -> homePage()
            "Antykradzież" -> antiTheftPage()
            "Lokalizacja", "Mapa" -> locationPage()
            "Zdjęcia" -> photosPage()
            "Moduły" -> modulesPage()
            "Synchronizacja" -> syncPage()
            "Diagnostyka", "Testy" -> diagnosticsPage()
            "Historia" -> historyPage()
            "Ustawienia" -> settingsPage()
            "Profile" -> profilesPage()
            else -> homePage()
        }
        if (animate) {
            pageContent.alpha = 0f
            pageContent.translationY = dp(12).toFloat()
            pageContent.animate().alpha(1f).translationY(0f).setDuration(240)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun homePage() {
        val admin = isAdminActive()
        val camera = hasPermission(Manifest.permission.CAMERA)
        val location = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val eventCount = EventLog.read(this).size
        val readyCount = listOf(admin, camera, location).count { it }

        card(panel, 22, border).apply {
            addView(horizontal().apply {
                addView(label("OCHRONA URZĄDZENIA", 12f, white, true), weighted())
                addView(pill(if (admin) "ADMIN AKTYWNY" else "KONFIGURACJA", if (admin) mint else amber))
            })
            val statusRow = horizontal().apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(18), 0, dp(12))
                addView(label(if (admin) "⬡" else "◇", 48f, if (admin) mint else amber, true).apply {
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
                    background = rounded(if (admin) Color.rgb(21, 60, 52) else Color.rgb(62, 49, 32), 40)
                })
                val textCol = vertical().apply {
                    setPadding(dp(14), 0, 0, 0)
                    addView(label(if (admin && camera && location) "Podstawowa ochrona gotowa" else "Skonfiguruj ochronę", 17f, white, true))
                    addView(label("Sprawdź uprawnienia i stan urządzenia", 12f, muted).apply { setPadding(0, dp(5), 0, 0) })
                }
                addView(textCol, weighted())
            }
            addView(statusRow)
            addView(actionButton(if (admin) "Sprawdź stan ochrony" else "Włącz administratora urządzenia", mint) {
                if (!admin) enableAdmin() else showPage("Diagnostyka")
            })
            pageContent.addView(this)
        }

        val stats = horizontal()
        stats.addView(statCard("▤", eventCount.toString(), "Zdarzenia", mint) { showPage("Historia") }, weighted())
        stats.addView(space(8))
        stats.addView(statCard("✓", "$readyCount/3", "Kontrole", blue) { showPage("Diagnostyka") }, weighted())
        stats.addView(space(8))
        stats.addView(statCard("⚙", if (admin) "OK" else "—", "Administrator", purple) { showPage("Ustawienia") }, weighted())
        pageContent.addView(stats)

        sectionTitle("Moduły ochrony")
        val grid1 = horizontal()
        grid1.addView(moduleCard("⌕", "Antykradzież", "Zdarzenia i reguły", blue) { showPage("Antykradzież") }, weighted())
        grid1.addView(space(9))
        grid1.addView(moduleCard("⌖", "Lokalizacja", "Pozycje i historia", purple) { showPage("Lokalizacja") }, weighted())
        pageContent.addView(grid1)
        val grid2 = horizontal().apply { setPadding(0, dp(9), 0, 0) }
        grid2.addView(moduleCard("⇧", "Synchronizacja", "Raporty i kopie", mint) { showPage("Synchronizacja") }, weighted())
        grid2.addView(space(9))
        grid2.addView(moduleCard("⬡", "Diagnostyka", "Testy i uprawnienia", amber) { showPage("Diagnostyka") }, weighted())
        pageContent.addView(grid2)

        sectionTitle("Ostatnie zdarzenia")
        val events = EventLog.read(this).asReversed().take(3)
        if (events.isEmpty()) {
            card(tile, 18, border).apply {
                addView(label("✓", 24f, mint, true))
                addView(label("Brak zarejestrowanych zdarzeń", 14f, white, true).apply { setPadding(0, dp(8), 0, dp(4)) })
                addView(label("Tutaj pojawią się zdarzenia zapisane przez aplikację.", 12f, muted))
                pageContent.addView(this)
            }
        } else {
            events.forEach { event ->
                card(tile, 16, border).apply {
                    addView(horizontal().apply {
                        addView(label("●", 16f, amber, true).apply { setPadding(0, 0, dp(12), 0) })
                        val col = vertical().apply {
                            addView(label(event.type.replace('_', ' '), 13f, white, true))
                            addView(label(EventLog.formattedTime(event.time), 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
                        }
                        addView(col, weighted())
                    })
                    pageContent.addView(this)
                }
            }
        }
    }

    private fun antiTheftPage() {
        pageContent.addView(pageHeading("Antykradzież", "Zdarzenia, reguły i ustawienia ochrony", "⌕", blue))
        pageContent.addView(moduleCard("◷", "Historia prób odblokowania", "Zarejestrowane zdarzenia systemowe", blue) { showPage("Historia") })
        pageContent.addView(moduleCard("⚙", "Administrator urządzenia", if (isAdminActive()) "Aktywny" else "Wymaga aktywacji", mint) { enableAdmin() })
        pageContent.addView(moduleCard("▣", "Uprawnienia ochrony", "Aparat i lokalizacja", purple) { requestProtectionPermissions() })
        pageContent.addView(moduleCard("⌁", "Profile ochrony", "Reguły i harmonogramy", amber) { showPage("Profile") })
        pageContent.addView(noteCard("Niektóre funkcje, takie jak automatyczne zdjęcia, nagrywanie i wysyłanie raportów, wymagają osobnej implementacji."))
    }

    private fun locationPage() {
        pageContent.addView(pageHeading("Lokalizacja", "Pozycje powiązane ze zdarzeniami", "⌖", purple))
        pageContent.addView(card(tile, 20, border).apply {
            addView(label("⌖", 46f, purple, true).apply { gravity = Gravity.CENTER })
            addView(label("Historia lokalizacji", 17f, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(6)) })
            addView(label("Gdy lokalizacja zostanie zapisana przez obsługiwaną funkcję, pojawi się tutaj wraz z czasem zdarzenia.", 12f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(8), 0, dp(8), dp(12))
            })
            locationStatusView = label("Pozycja nie została jeszcze pobrana.", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(dp(8), dp(12), dp(8), dp(8)) }
            addView(locationStatusView)
            addView(actionButton("Pobierz aktualną lokalizację", purple) { fetchLocation() })
            addView(actionButton("Sprawdź uprawnienia", blue) { requestProtectionPermissions() })
        })
        pageContent.addView(moduleCard("◷", "Oś czasu incydentu", "Zdarzenia uporządkowane według czasu", blue) { showPage("Historia") })
    }

    private fun syncPage() {
        pageContent.addView(pageHeading("Synchronizacja", "Wysyłka raportów i kopie zapasowe", "⇧", mint))
        pageContent.addView(moduleCard("✉", "Adres odbiorcy", prefs.getString("recipient_email", "")?.ifBlank { "Nie skonfigurowano" } ?: "Nie skonfigurowano", blue) { showPage("Ustawienia") })
        pageContent.addView(moduleCard("☁", "Kolejka offline", "Moduł wysyłki oczekuje na implementację", mint) { showPage("Diagnostyka") })
        pageContent.addView(moduleCard("▣", "Google Drive", "Wymaga autoryzacji i konfiguracji", purple) { showPage("Ustawienia") })
        pageContent.addView(actionButton("Przygotuj raport e-mail", mint) { prepareEmailReport() })
        pageContent.addView(noteCard("Raport otworzy się w aplikacji pocztowej. Wiadomość trzeba wysłać ręcznie. Automatyczna wysyłka w tle wymaga konfiguracji serwera poczty. Google Drive nie jest jeszcze podłączony."))
    }

    private fun modulesPage() {
        pageContent.addView(pageHeading("Wszystkie moduły", "Każda funkcja w osobnym miejscu", "▦", mint))
        val modules = listOf(
            Triple("▣", "Zdjęcia", "Rób i przeglądaj zdjęcia") to "Zdjęcia",
            Triple("⌖", "Lokalizacja", "Pobierz pozycję telefonu") to "Lokalizacja",
            Triple("⌕", "Antykradzież", "Zdarzenia i reguły") to "Antykradzież",
            Triple("✉", "E-mail i synchronizacja", "Przygotuj raport") to "Synchronizacja",
            Triple("◷", "Historia", "Zapisane zdarzenia") to "Historia",
            Triple("⬡", "Diagnostyka", "Uprawnienia i stan") to "Diagnostyka",
            Triple("⌁", "Profile ochrony", "Konfiguracje ochrony") to "Profile",
            Triple("⚙", "Ustawienia", "Adres e-mail i uprawnienia") to "Ustawienia"
        )
        modules.forEach { (item, destination) ->
            pageContent.addView(moduleCard(item.first, item.second, item.third, blue) { showPage(destination) })
        }
    }

    private fun photosPage() {
        pageContent.addView(pageHeading("Zdjęcia", "Fotografie zapisane w aplikacji", "▣", purple))
        pageContent.addView(actionButton("📷 Zrób zdjęcie", mint) { launchCapture(false) })
        pageContent.addView(actionButton("🎥 Nagraj film", blue) { launchCapture(true) })
        pageContent.addView(noteCard("Aparat i kamera uruchamiają się jawnie przez aplikację aparatu Androida. Materiały zapisują się w galerii telefonu."))
        val filters = horizontal()
        listOf("Wszystkie", "Zdjęcia", "Filmy").forEach { filter ->
            filters.addView(actionButton(if (mediaFilter == filter) "✓ $filter" else filter, if (mediaFilter == filter) mint else blue) {
                mediaFilter = filter; showPage("Zdjęcia")
            }, weighted())
            filters.addView(space(5))
        }
        pageContent.addView(filters)
        val search = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(white); setHintTextColor(muted)
            hint = "Szukaj po nazwie pliku"; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(tile, 12, border); setText(mediaSearch)
        }
        pageContent.addView(search)
        pageContent.addView(actionButton("Szukaj", purple) { mediaSearch = search.text.toString().trim(); showPage("Zdjęcia") })
        val media = queryCapturedMedia().filter {
            val kindOk = when (mediaFilter) { "Zdjęcia" -> !it.isVideo; "Filmy" -> it.isVideo; else -> true }
            kindOk && it.name.contains(mediaSearch, true)
        }
        pageContent.addView(label("${media.size} materiałów", 12f, muted).apply { setPadding(0, dp(10), 0, dp(8)) })
        if (media.isEmpty()) pageContent.addView(noteCard("Brak materiałów pasujących do filtra."))
        media.forEach { item ->
            pageContent.addView(card(tile, 16, border).apply {
                addView(horizontal().apply {
                    addView(label(if (item.isVideo) "▶" else "▣", 30f, if (item.isVideo) blue else mint, true).apply { setPadding(0, 0, dp(12), 0) })
                    addView(vertical().apply {
                        addView(label(item.name, 13f, white, true))
                        addView(label("${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.dateSeconds * 1000))} · ${formatBytes(item.size)}", 11f, muted).apply { setPadding(0, dp(5), 0, 0) })
                    }, weighted())
                })
                addView(actionButton("Otwórz", blue) { openMedia(item) })
                addView(actionButton("Udostępnij", mint) { shareMedia(item) })
                addView(actionButton("Usuń", red) {
                    AlertDialog.Builder(this@MainActivity).setTitle("Usunąć materiał?")
                        .setMessage(item.name).setNegativeButton("Anuluj", null)
                        .setPositiveButton("Usuń") { _, _ ->
                            contentResolver.delete(item.uri, null, null)
                            EventLog.add(this@MainActivity, "MEDIA_DELETED", "Usunięto ${item.name}")
                            showPage("Zdjęcia")
                        }.show()
                })
            })
        }
    }

    private data class CapturedMedia(val uri: Uri, val name: String, val dateSeconds: Long, val size: Long, val isVideo: Boolean)

    private fun launchCapture(isVideo: Boolean) {
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val kind = if (isVideo) "video" else "photo"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "AntiThiefPRO_${kind}_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}")
            put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/AntiThiefPRO" else "Pictures/AntiThiefPRO")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = try { contentResolver.insert(collection, values) } catch (_: Exception) { null }
        if (uri == null) {
            Toast.makeText(this, "Nie udało się przygotować pliku. Sprawdź wolne miejsce.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(if (isVideo) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (isVideo) putExtra(MediaStore.EXTRA_DURATION_LIMIT, 180)
        }
        if (intent.resolveActivity(packageManager) == null) {
            contentResolver.delete(uri, null, null)
            Toast.makeText(this, "Nie znaleziono zgodnej aplikacji aparatu.", Toast.LENGTH_LONG).show()
            return
        }
        pendingMediaUri = uri
        try { startActivityForResult(intent, if (isVideo) videoRequestCode else photoRequestCode) }
        catch (_: Exception) {
            contentResolver.delete(uri, null, null); pendingMediaUri = null
            Toast.makeText(this, "Nie udało się uruchomić aparatu.", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryCapturedMedia(): List<CapturedMedia> {
        val result = mutableListOf<CapturedMedia>()
        fun readCollection(collection: Uri, isVideo: Boolean) {
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.RELATIVE_PATH)
            try {
                contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val pathCol = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol) ?: ""
                        val path = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                        if (!name.startsWith("AntiThiefPRO_") && !path.contains("AntiThiefPRO", true)) continue
                        result.add(CapturedMedia(Uri.withAppendedPath(collection, cursor.getLong(idCol).toString()), name, cursor.getLong(dateCol), cursor.getLong(sizeCol), isVideo))
                    }
                }
            } catch (_: Exception) { }
        }
        readCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false)
        readCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
        return result.sortedByDescending { it.dateSeconds }
    }

    private fun openMedia(item: CapturedMedia) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) { Toast.makeText(this, "Brak aplikacji do otwarcia tego pliku.", Toast.LENGTH_LONG).show() }
    }

    private fun shareMedia(item: CapturedMedia) {
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Udostępnij materiał"))
        } catch (_: Exception) { Toast.makeText(this, "Nie udało się udostępnić pliku.", Toast.LENGTH_LONG).show() }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun fetchLocation() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestProtectionPermissions()
            locationStatusView?.text = "Nadaj uprawnienie lokalizacji, a następnie spróbuj ponownie."
            return
        }
        try {
            val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) {
                locationStatusView?.text = "Włącz lokalizację w ustawieniach telefonu."
                return
            }
            var last: Location? = null
            for (provider in providers) {
                try {
                    val candidate = manager.getLastKnownLocation(provider)
                    if (candidate != null && (last == null || candidate.time > last!!.time)) last = candidate
                } catch (_: SecurityException) { }
            }
            if (last != null) {
                showLocation(last!!)
                return
            }
            locationStatusView?.text = "Pobieranie pozycji… poczekaj chwilę."
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try { manager.removeUpdates(this) } catch (_: Exception) { }
                    showLocation(location)
                }
            }
            var registered = false
            for (provider in providers) {
                try {
                    manager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
                    registered = true
                } catch (_: SecurityException) { }
            }
            if (!registered) locationStatusView?.text = "Nie udało się rozpocząć pobierania lokalizacji."
            Toast.makeText(this, "Czekam na pozycję GPS lub sieciową.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            locationStatusView?.text = "Błąd pobierania lokalizacji. Sprawdź ustawienia telefonu."
        }
    }

    private fun showLocation(location: Location) {
        val accuracy = if (location.hasAccuracy()) "\nDokładność: około ${location.accuracy.toInt()} m" else ""
        locationStatusView?.text = "Szerokość: ${location.latitude}\nDługość: ${location.longitude}$accuracy"
        EventLog.add(this, "LOCATION_READ", "Pobrano pozycję: ${location.latitude}, ${location.longitude}")
    }

    private fun prepareEmailReport() {
        val recipient = prefs.getString("recipient_email", "")?.trim().orEmpty()
        if (recipient.isBlank()) {
            Toast.makeText(this, "Najpierw zapisz adres odbiorcy w Ustawieniach.", Toast.LENGTH_LONG).show()
            showPage("Ustawienia")
            return
        }
        val events = EventLog.read(this).takeLast(30).joinToString("\n") {
            "${EventLog.formattedTime(it.time)} | ${it.type} | ${it.details}"
        }.ifBlank { "Brak zapisanych zdarzeń." }
        val body = "Raport AntiThiefPRO\nUrządzenie: ${Build.MANUFACTURER} ${Build.MODEL}\n\nOstatnie zdarzenia:\n$events"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, "Raport AntiThiefPRO")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Brak aplikacji pocztowej obsługującej wysyłanie e-mail.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestCsvExport() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "AntiThiefPRO_historia.csv")
        }, csvRequestCode)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == photoRequestCode || requestCode == videoRequestCode) {
            val uri = pendingMediaUri
            pendingMediaUri = null
            if (uri != null) {
                if (resultCode == RESULT_OK) {
                    try {
                        if (Build.VERSION.SDK_INT >= 29) {
                            contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                        }
                        EventLog.add(this, if (requestCode == videoRequestCode) "VIDEO_CAPTURED" else "PHOTO_CAPTURED", "Zapisano materiał: $uri")
                        Toast.makeText(this, if (requestCode == videoRequestCode) "Zapisano film w galerii." else "Zapisano zdjęcie w galerii.", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Nie udało się zaktualizować pliku.", Toast.LENGTH_LONG).show()
                    }
                } else contentResolver.delete(uri, null, null)
                showPage("Zdjęcia")
            }
        } else if (requestCode == csvRequestCode && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openOutputStream(data.data!!)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write("czas;typ;szczegóły\n")
                    EventLog.read(this).forEach { event ->
                        val safeDetails = event.details.replace("\"","'").replace("\n", " ")
                        writer.write("${EventLog.formattedTime(event.time)};${event.type};$safeDetails\n")
                    }
                }
                Toast.makeText(this, "Wyeksportowano historię CSV.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(this, "Nie udało się zapisać pliku CSV.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun diagnosticsPage() {
        pageContent.addView(pageHeading("Diagnostyka", "Sprawdź gotowość urządzenia", "⬡", amber))
        val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Brak danych"
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = if (Build.VERSION.SDK_INT >= 23) cm.activeNetwork != null else @Suppress("DEPRECATION") (cm.activeNetworkInfo?.isConnected == true)
        val freeBytes = StatFs(filesDir.absolutePath).availableBytes
        val riskEvents = EventLog.read(this).takeLast(20).count { it.type.contains("FAILED") || it.type.contains("UNLOCK", true) }
        val risk = when { riskEvents >= 8 -> "Wysokie"; riskEvents >= 3 -> "Podwyższone"; else -> "Niskie" }
        pageContent.addView(moduleCard("◉", "Poziom ryzyka", risk, if (risk == "Wysokie") red else if (risk == "Podwyższone") amber else mint) { showPage("Historia") })
        pageContent.addView(moduleCard("▤", "Wolne miejsce aplikacji", formatBytes(freeBytes), blue) { })
        pageContent.addView(moduleCard("▰", "Bateria", batteryPercent, mint) { })
        pageContent.addView(moduleCard("⌁", "Internet", if (connected) "Połączenie aktywne" else "Brak połączenia", if (connected) mint else amber) { })
        pageContent.addView(moduleCard("⌘", "Debugowanie USB", if (Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1) "Włączone" else "Wyłączone", amber) { })
        pageContent.addView(moduleCard("⚙", "Opcje programistyczne", if (Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1) "Włączone" else "Wyłączone", amber) { })
        pageContent.addView(moduleCard("◷", "Automatyczna data i godzina", if (Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1) "Włączona" else "Wyłączona", blue) { })
        pageContent.addView(actionButton("Zapisz zdarzenie testowe", purple) {
            EventLog.add(this, "SAFE_TEST_EVENT", "Ręczny test działania aplikacji")
            Toast.makeText(this, "Dodano testowe zdarzenie do historii.", Toast.LENGTH_SHORT).show()
            showPage("Diagnostyka")
        })
        pageContent.addView(actionButton("Eksportuj historię CSV", mint) { requestCsvExport() })
        pageContent.addView(actionButton("Usuń całą historię", red) {
            AlertDialog.Builder(this).setTitle("Usunąć historię?")
                .setMessage("Zostaną usunięte wszystkie lokalne wpisy dziennika.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Usuń") { _, _ ->
                    EventLog.clear(this)
                    EventLog.add(this, "HISTORY_CLEARED", "Historia została wyczyszczona ręcznie")
                    showPage("Diagnostyka")
                }.show()
        })
        val checks = listOf(
            Triple("Administrator urządzenia", isAdminActive(), "Włącz obsługę administratora"),
            Triple("Aparat", hasPermission(Manifest.permission.CAMERA), "Nadaj uprawnienie aparatu"),
            Triple("Lokalizacja", hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION), "Nadaj uprawnienie lokalizacji")
        )
        checks.forEach { (name, ok, action) ->
            pageContent.addView(card(tile, 16, border).apply {
                addView(horizontal().apply {
                    addView(label(if (ok) "✓" else "!", 22f, if (ok) mint else amber, true).apply { setPadding(0, 0, dp(12), 0) })
                    addView(vertical().apply {
                        addView(label(name, 14f, white, true))
                        addView(label(if (ok) "Gotowe" else "Wymaga działania", 12f, if (ok) mint else amber).apply { setPadding(0, dp(4), 0, 0) })
                    }, weighted())
                })
                if (!ok) {
                    addView(actionButton(action, blue) {
                        if (name == "Administrator urządzenia") enableAdmin() else requestProtectionPermissions()
                    })
                }
            })
        }
        pageContent.addView(moduleCard("▤", "Dziennik zdarzeń", "${EventLog.read(this).size} zapisanych wpisów", blue) { showPage("Historia") })
    }

    private fun historyPage() {
        pageContent.addView(pageHeading("Historia zdarzeń", "Wpisy zapisane lokalnie na urządzeniu", "◷", blue))
        val search = EditText(this).apply {
            setSingleLine(true); textSize = 14f; setTextColor(white); setHintTextColor(muted)
            hint = "Szukaj w historii"; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(tile, 12, border); setText(historySearch)
        }
        pageContent.addView(search)
        pageContent.addView(actionButton("Filtruj", purple) { historySearch = search.text.toString().trim(); showPage("Historia") })
        val allEvents = EventLog.read(this).asReversed()
        val events = allEvents.filter { it.type.contains(historySearch, true) || it.details.contains(historySearch, true) }
        if (events.isEmpty()) {
            pageContent.addView(noteCard("Nie ma jeszcze żadnych zdarzeń w dzienniku."))
        } else {
            events.forEach { event ->
                pageContent.addView(card(tile, 16, border).apply {
                    addView(label(EventLog.formattedTime(event.time), 11f, muted, true))
                    addView(label(event.type.replace('_', ' '), 15f, white, true).apply { setPadding(0, dp(6), 0, dp(4)) })
                    addView(label(event.details, 12f, muted))
                    addView(actionButton("Oznacz jako fałszywy alarm", amber) {
                        EventLog.add(this@MainActivity, "FALSE_ALARM_MARKED", "Oznaczono wpis ${EventLog.formattedTime(event.time)} (${event.type}) jako fałszywy alarm")
                        Toast.makeText(this@MainActivity, "Dodano oznaczenie do historii.", Toast.LENGTH_SHORT).show()
                        showPage("Historia")
                    })
                })
            }
        }
        pageContent.addView(actionButton("Eksportuj historię CSV", mint) { requestCsvExport() })
        pageContent.addView(actionButton("Odśwież historię", blue) { showPage("Historia") })
    }

    private fun settingsPage() {
        pageContent.addView(pageHeading("Ustawienia", "Konfiguracja AntiThiefPRO", "⚙", blue))
        pageContent.addView(moduleCard("⬡", "Administrator urządzenia", if (isAdminActive()) "Aktywny" else "Nieaktywny", mint) { enableAdmin() })
        pageContent.addView(moduleCard("▣", "Uprawnienia aparatu i lokalizacji", "Zarządzaj zgodami Androida", purple) { requestProtectionPermissions() })
        pageContent.addView(sectionTitleView("Adres e-mail odbiorcy"))
        val email = EditText(this).apply {
            setSingleLine(true)
            textSize = 14f
            setTextColor(white)
            setHintTextColor(muted)
            hint = "twoj@email.pl"
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(21, 33, 48), 14, border)
            setText(prefs.getString("recipient_email", "") ?: "")
        }
        pageContent.addView(email)
        pageContent.addView(actionButton("Zapisz adres e-mail", mint) {
            val value = email.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                email.error = "Wpisz poprawny adres e-mail"
            } else {
                prefs.edit().putString("recipient_email", value).apply()
                EventLog.add(this, "EMAIL_SAVED", "Zapisano adres odbiorcy")
                Toast.makeText(this, "Zapisano adres odbiorcy", Toast.LENGTH_SHORT).show()
                showPage("Ustawienia")
            }
        })
        pageContent.addView(moduleCard("⌁", "Profile ochrony", "Dom, Poza domem, Podróż", amber) { showPage("Profile") })
        pageContent.addView(moduleCard("◷", "Historia zdarzeń", "Zobacz zapisane wpisy", blue) { showPage("Historia") })
    }

    private fun profilesPage() {
        pageContent.addView(pageHeading("Profile ochrony", "Wybierz konfigurację dopasowaną do sytuacji", "⌁", amber))
        val activeProfile = prefs.getString("active_profile", "Dom") ?: "Dom"
        listOf("Dom" to "Ustawienia dla zaufanej lokalizacji", "Poza domem" to "Rozszerzone kontrole", "Podróż" to "Narzędzia odzyskiwania", "Maksymalna ochrona" to "Wszystkie dostępne kontrole").forEach { (name, desc) ->
            pageContent.addView(moduleCard(if (activeProfile == name) "✓" else "◈", name, if (activeProfile == name) "AKTYWNY · $desc" else desc, if (activeProfile == name) mint else blue) {
                prefs.edit().putString("active_profile", name).apply()
                EventLog.add(this, "PROFILE_CHANGED", "Wybrano profil: $name")
                showPage("Profile")
            })
        }
    }

    private fun pageHeading(title: String, subtitle: String, symbol: String, color: Int): View {
        val box = vertical().apply {
            setPadding(0, dp(8), 0, dp(16))
            addView(label(symbol, 34f, color, true))
            addView(label(title, 24f, white, true).apply { setPadding(0, dp(8), 0, dp(4)) })
            addView(label(subtitle, 13f, muted))
        }
        return box
    }

    private fun sectionTitle(title: String) {
        pageContent.addView(sectionTitleView(title))
    }

    private fun sectionTitleView(title: String) = label(title, 16f, white, true).apply {
        setPadding(dp(2), dp(22), dp(2), dp(10))
    }

    private fun statCard(symbol: String, value: String, caption: String, color: Int, click: () -> Unit): View {
        val card = vertical().apply {
            background = rounded(tile, 16, border)
            setPadding(dp(12), dp(12), dp(8), dp(12))
            addView(label(symbol, 20f, color, true))
            addView(label(value, 21f, white, true).apply { setPadding(0, dp(8), 0, dp(2)) })
            addView(label(caption, 10f, muted, true))
            setOnClickListener { tapAnimation(this); click() }
            isClickable = true
        }
        return card
    }

    private fun moduleCard(symbol: String, title: String, subtitle: String, color: Int, click: () -> Unit): View {
        return card(tile, 17, border).apply {
            addView(horizontal().apply {
                addView(label(symbol, 26f, color, true).apply { setPadding(0, 0, dp(8), 0) }, weighted())
                addView(label("↗", 17f, muted, true))
            })
            addView(label(title, 14f, white, true).apply { setPadding(0, dp(12), 0, dp(4)) })
            addView(label(subtitle, 11f, muted))
            isClickable = true
            setOnClickListener { tapAnimation(this); click() }
        }
    }

    private fun noteCard(message: String): View = card(tile, 16, border).apply {
        addView(label(message, 13f, muted))
    }

    private fun card(color: Int, radius: Int, stroke: Int): LinearLayout = vertical().apply {
        background = rounded(color, radius, stroke)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        }
        clipToOutline = true
    }

    private fun actionButton(title: String, color: Int, action: () -> Unit): View {
        val button = TextView(this).apply {
            text = title
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(if (color == mint) Color.rgb(8, 35, 29) else white)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(color, 14)
            isClickable = true
            isFocusable = true
            setOnClickListener { tapAnimation(this); action() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
        }
        return button
    }

    private fun iconButton(symbol: String, description: String, action: () -> Unit): View = TextView(this).apply {
        text = symbol
        contentDescription = description
        textSize = 23f
        gravity = Gravity.CENTER
        setTextColor(muted)
        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        isClickable = true
        setOnClickListener { tapAnimation(this); action() }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = true
    }

    private fun pill(textValue: String, color: Int): TextView = TextView(this).apply {
        text = textValue
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (color == mint) Color.rgb(8, 35, 29) else Color.rgb(55, 39, 10))
        setPadding(dp(9), dp(6), dp(9), dp(6))
        background = rounded(color, 20)
    }

    private fun horizontal() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun vertical() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    private fun weighted() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun space(width: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun transparent() = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun tapAnimation(view: View) {
        val downX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.975f, 1f)
        val downY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.975f, 1f)
        AnimatorSet().apply {
            playTogether(downX, downY)
            duration = 170
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun hasPermission(permission: String): Boolean =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun isAdminActive(): Boolean {
        val manager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return manager.isAdminActive(ComponentName(this, AntiThiefAdminReceiver::class.java))
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
        val needed = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.distinct().filter { !hasPermission(it) }
        if (missing.isEmpty()) {
            Toast.makeText(this, "Wszystkie wymagane zgody z tej listy są już nadane.", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(missing.toTypedArray(), 410)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::pageContent.isInitialized) showPage(currentPage, false)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (currentPage != "Główna") showPage("Główna") else super.onBackPressed()
    }
}
