package pl.antithiefpro

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("anti_thief_settings", Context.MODE_PRIVATE) }
    private lateinit var pageContent: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private var currentPage = "Główna"

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
        val entries = listOf("Główna" to "⌂", "Historia" to "◷", "Mapa" to "⌖", "Ustawienia" to "⚙")
        entries.forEach { (name, symbol) ->
            val selected = currentPage == name || (name == "Mapa" && currentPage == "Lokalizacja")
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(6), dp(5), dp(4))
                background = if (selected) rounded(Color.rgb(24, 51, 49), 14) else transparent()
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true
                isFocusable = true
                setOnClickListener { tapAnimation(this); showPage(if (name == "Mapa") "Lokalizacja" else name) }
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
            addView(actionButton("Sprawdź uprawnienia lokalizacji", purple) { requestProtectionPermissions() })
        })
        pageContent.addView(moduleCard("◷", "Oś czasu incydentu", "Zdarzenia uporządkowane według czasu", blue) { showPage("Historia") })
    }

    private fun syncPage() {
        pageContent.addView(pageHeading("Synchronizacja", "Wysyłka raportów i kopie zapasowe", "⇧", mint))
        pageContent.addView(moduleCard("✉", "Adres odbiorcy", prefs.getString("recipient_email", "")?.ifBlank { "Nie skonfigurowano" } ?: "Nie skonfigurowano", blue) { showPage("Ustawienia") })
        pageContent.addView(moduleCard("☁", "Kolejka offline", "Moduł wysyłki oczekuje na implementację", mint) { showPage("Diagnostyka") })
        pageContent.addView(moduleCard("▣", "Google Drive", "Wymaga autoryzacji i konfiguracji", purple) { showPage("Ustawienia") })
        pageContent.addView(noteCard("Wysyłka e-mail i synchronizacja z Google Drive nie są jeszcze aktywne."))
    }

    private fun diagnosticsPage() {
        pageContent.addView(pageHeading("Diagnostyka", "Sprawdź gotowość urządzenia", "⬡", amber))
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
        val events = EventLog.read(this).asReversed()
        if (events.isEmpty()) {
            pageContent.addView(noteCard("Nie ma jeszcze żadnych zdarzeń w dzienniku."))
        } else {
            events.forEach { event ->
                pageContent.addView(card(tile, 16, border).apply {
                    addView(label(EventLog.formattedTime(event.time), 11f, muted, true))
                    addView(label(event.type.replace('_', ' '), 15f, white, true).apply { setPadding(0, dp(6), 0, dp(4)) })
                    addView(label(event.details, 12f, muted))
                })
            }
        }
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
        listOf("Dom" to "Ustawienia dla zaufanej lokalizacji", "Poza domem" to "Rozszerzone monitorowanie", "Podróż" to "Dostęp do narzędzi odzyskiwania", "Maksymalna ochrona" to "Wszystkie dostępne kontrole").forEach { (name, desc) ->
            pageContent.addView(moduleCard("◈", name, desc, if (name == "Maksymalna ochrona") mint else blue) {
                Toast.makeText(this, "Profil „$name” będzie dostępny po wdrożeniu reguł automatyzacji.", Toast.LENGTH_SHORT).show()
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
