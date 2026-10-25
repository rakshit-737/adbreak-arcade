package com.adbreak.arcade.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.adbreak.arcade.BuildConfig
import com.adbreak.arcade.R
import com.adbreak.arcade.core.ArcadeGame
import com.adbreak.arcade.core.GameRegistry
import com.adbreak.arcade.data.ArcadeStore
import com.adbreak.arcade.notification.NotificationController
import com.adbreak.arcade.trigger.SelectableLaunchableApp
import com.adbreak.arcade.trigger.SmartAppTriggerService
import com.adbreak.arcade.trigger.SmartAppTriggerServiceFactory
import com.adbreak.arcade.trigger.SmartTriggerConfig
import com.adbreak.arcade.trigger.SmartTriggerPollResult
import com.adbreak.arcade.trigger.SmartTriggerSettings
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.adbreak.arcade.ui.game.GamePresentationCatalog
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private enum class Screen { HOME, LIBRARY, SCORES, SETTINGS, ABOUT, SMART_TRIGGERS }

    private lateinit var store: ArcadeStore
    private lateinit var content: LinearLayout
    private lateinit var notification: NotificationController
    private var screen = Screen.HOME
    private var pendingStartGameId: String? = null
    private val requestCode = 7302

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        store = ArcadeStore(this)
        notification = NotificationController(this)
        content = findViewById(R.id.screenContainer)
        screen = savedInstanceState?.getString(KEY_SCREEN)?.let { runCatching { Screen.valueOf(it) }.getOrNull() } ?: Screen.HOME

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        findViewById<View>(R.id.navHome).setOnClickListener { showHome() }
        findViewById<View>(R.id.navLibrary).setOnClickListener { showLibrary() }
        findViewById<View>(R.id.navScores).setOnClickListener { showScores() }
        findViewById<View>(R.id.navSettings).setOnClickListener { showSettings() }
        findViewById<View>(R.id.navAbout).setOnClickListener { showAbout() }
        renderCurrentScreen()
    }

    override fun onResume() {
        super.onResume()
        pollSmartTrigger()
        if (::content.isInitialized) renderCurrentScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SCREEN, screen.name)
        super.onSaveInstanceState(outState)
    }

    private fun renderCurrentScreen() {
        when (screen) {
            Screen.HOME -> showHome()
            Screen.LIBRARY -> showLibrary()
            Screen.SCORES -> showScores()
            Screen.SETTINGS -> showSettings()
            Screen.ABOUT -> showAbout()
            Screen.SMART_TRIGGERS -> showSmartTriggers()
        }
        updateNavigation()
    }

    private fun updateNavigation() {
        if (!::content.isInitialized) return
        val selected = when (screen) {
            Screen.SMART_TRIGGERS -> R.id.navSettings
            Screen.HOME -> R.id.navHome
            Screen.LIBRARY -> R.id.navLibrary
            Screen.SCORES -> R.id.navScores
            Screen.SETTINGS -> R.id.navSettings
            Screen.ABOUT -> R.id.navAbout
        }
        listOf(R.id.navHome, R.id.navLibrary, R.id.navScores, R.id.navSettings, R.id.navAbout).forEach { id ->
            findViewById<MaterialButton>(id).apply {
                isSelected = id == selected
                setTextColor(getColor(if (isSelected) R.color.arcade_on_primary_container else R.color.arcade_on_surface_variant))
                iconTint = ColorStateList.valueOf(getColor(if (isSelected) R.color.arcade_violet else R.color.arcade_on_surface_variant))
            }
        }
    }

    private fun progressCard(): View {
        val level = store.arcadeLevel()
        val xp = store.arcadeXpIntoLevel()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundResource(R.drawable.bg_card)
            addView(TextView(context).apply {
                text = "ARCADE LEVEL $level  ·  ${store.totalXp()} XP"
                textSize = 11f
                setTextColor(getColor(R.color.arcade_violet))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "Every meaningful move builds a local run. Nothing is locked behind progression."
                textSize = 14f
                setTextColor(getColor(R.color.arcade_on_surface_variant))
                setPadding(0, dp(6), 0, dp(8))
            })
            addView(android.widget.ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = xp
                layoutParams = LinearLayout.LayoutParams(-1, dp(8))
                contentDescription = "$xp percent toward arcade level ${level + 1}"
            })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(16)) }
        }
    }

    private fun onboardingCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundResource(R.drawable.bg_lilac_pill)
            addView(TextView(context).apply {
                text = "Start small. Play from the shade."
                textSize = 21f
                setTextColor(getColor(R.color.arcade_on_primary_container))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "1  Choose a game\n2  Tap Start notification\n3  Pull down and expand AdBreak Arcade\n4  Use the visible controls"
                textSize = 15f
                setTextColor(getColor(R.color.arcade_on_primary_container))
                setPadding(0, dp(9), 0, dp(12))
            })
            addView(button("Got it — show me the arcade") {
                store.onboardingCompleted = true
                showHome()
            })
            addView(button("Not now") {
                store.onboardingCompleted = true
                showHome()
            })
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(16)) }
        }
    }

    private fun statusBanner(message: String): View = TextView(this).apply {
        text = message
        textSize = 14f
        setTextColor(getColor(R.color.arcade_on_surface_variant))
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundResource(R.drawable.bg_card)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(18)) }
    }

    private fun showHome() {
        screen = Screen.HOME
        updateNavigation()
        content.removeAllViews()
        heading("Make the waiting part yours", "Play an offline mini-game from the notification shade while your other app stays in front.")
        content.addView(progressCard())
        if (!store.onboardingCompleted) content.addView(onboardingCard())
        val game = store.loadGame()
        card(game, true)
        val status = when {
            store.active && notification.canPost() -> "Notification active — expand AdBreak Arcade from the shade when you are ready."
            !notification.canPost() -> "Notifications are unavailable — allow them in Android settings to play from the shade."
            else -> "Notification inactive — start a game below when you want a quiet break companion."
        }
        content.addView(statusBanner(status))
        text("How it works", 19, true)
        text("Choose a game, tap Start notification, then pull down and expand AdBreak Arcade. Every move is a normal notification button. The app never detects, skips, blocks, or manipulates ads.", 15, false)
        val recent = store.recentGameIds().mapNotNull { id -> GameRegistry.definitions.firstOrNull { it.id == id } }
        if (recent.isNotEmpty()) {
            text("Recently played", 19, true)
            recent.take(3).forEach { recentGame -> card(recentGame, recentGame.id == store.selectedGameId) }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            content.addView(button("Allow notifications") {
                pendingStartGameId = store.selectedGameId
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
            })
        }
    }

    private fun showLibrary() {
        screen = Screen.LIBRARY
        updateNavigation()
        content.removeAllViews()
        val count = GameRegistry.definitions.size
        heading("The arcade shelf", "$count offline games designed around real notification controls.")
        val favorites = GameRegistry.definitions.filter { store.isFavorite(it.id) }
        if (favorites.isNotEmpty()) {
            text("Favorites", 19, true)
            favorites.forEach { game -> card(game, game.id == store.selectedGameId) }
        }
        val recentIds = store.recentGameIds()
        val recent = recentIds.mapNotNull { id -> GameRegistry.definitions.firstOrNull { it.id == id } }
        if (recent.isNotEmpty()) {
            text("Recent", 19, true)
            recent.forEach { game -> card(game, game.id == store.selectedGameId) }
        }
        text("All games", 19, true)
        GameRegistry.definitions.forEach { game -> card(game, game.id == store.selectedGameId) }
    }

    private fun showScores() {
        screen = Screen.SCORES
        updateNavigation()
        content.removeAllViews()
        heading("Your run, kept local", "Best runs stay on this device. Nothing is uploaded.")
        content.addView(progressCard())
        GameRegistry.definitions.forEach { definition ->
            val game = store.loadGame(definition.id)
            val stats = store.statsFor(definition.id)
            val presentation = GamePresentationCatalog.forGame(definition.id)
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                setBackgroundResource(R.drawable.bg_card)
            }
            block.addView(TextView(this).apply {
                text = "${presentation.motif}  ${definition.title}"
                textSize = 17f
                setTextColor(getColor(R.color.arcade_ink))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            block.addView(TextView(this).apply {
                text = if (stats.plays == 0) "Not played yet  ·  ${game.render().scoreLine}" else "${stats.plays} plays  ·  ${stats.actions} moves  ·  ${stats.roundsCompleted} completed"
                textSize = 14f
                setTextColor(getColor(R.color.arcade_on_surface_variant))
                setPadding(0, dp(6), 0, 0)
            })
            block.addView(TextView(this).apply {
                text = "${presentation.family.label} family  ·  ${if (store.isFavorite(definition.id)) "Favorite" else "Ready"}"
                textSize = 11f
                setTextColor(getColor(presentation.family.accent))
                setPadding(0, dp(8), 0, 0)
            })
            content.addView(block, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(10)) })
        }
    }

    private fun showSettings() {
        screen = Screen.SETTINGS
        updateNavigation()
        content.removeAllViews()
        heading("Settings", "Keep the arcade quiet, clear, and under your control.")
        text("Notifications", 18, true)
        text(notificationStatus(), 15, false)
        content.addView(button("Open notification settings") { openNotificationSettings() })
        content.addView(button("Stop active notification") { notification.stop(); showSettings() })

        text("Smart App Triggers", 18, true)
        text(if (store.smartTriggersEnabled) "Enabled — selected app launches are checked on the next available lifecycle poll." else "Disabled — no app-usage polling is active.", 15, false)
        content.addView(button("Configure Smart App Triggers") { showSmartTriggers() })

        text("Local game data", 18, true)
        text("Reset starts a fresh round. Local arcade stats and progression remain available here.", 15, false)
        content.addView(button("Reset selected game") { store.resetGame(store.selectedGameId); showSettings() })
        content.addView(button("Clear selected game history") { store.clearGameHistory(store.selectedGameId); showSettings() })
    }

    private fun showSmartTriggers() {
        screen = Screen.SMART_TRIGGERS
        updateNavigation()
        content.removeAllViews()
        val service = triggerService()
        heading("Smart App Triggers", "Prepare the game notification when a selected app becomes foreground-visible, where Android permits a lifecycle poll.")
        text("This is opt-in and best effort. Usage access reveals only selected package foreground transitions to this feature; it does not inspect app content or detect advertisements.", 15, false)
        text("Usage access: ${if (service.hasUsageAccess()) "Granted" else "Needed"}", 17, true)
        content.addView(button("Open Usage Access settings") { startActivity(SmartTriggerSettings.usageAccessIntent()) })

        val enabled = SwitchMaterial(this).apply {
            text = "Enable Smart App Triggers"
            isChecked = store.smartTriggersEnabled
            minHeight = dp(52)
        }
        content.addView(enabled)

        text("Game to prepare", 17, true)
        val gameDefinitions = GameRegistry.definitions
        val gameSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, gameDefinitions.map { it.title }).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(gameDefinitions.indexOfFirst { it.id == store.smartTriggerGameId }.coerceAtLeast(0))
            minimumHeight = dp(52)
        }
        content.addView(gameSpinner)

        text("Cooldown", 17, true)
        val cooldownOptions = listOf(30_000L to "30 seconds", 60_000L to "1 minute", 120_000L to "2 minutes", 300_000L to "5 minutes")
        val cooldownSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, cooldownOptions.map { it.second }).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(cooldownOptions.indexOfFirst { it.first == store.smartTriggerCooldownMillis }.coerceAtLeast(0))
            minimumHeight = dp(52)
        }
        content.addView(cooldownSpinner)

        text("Choose apps", 17, true)
        val search = EditText(this).apply {
            hint = "Search installed apps"
            minHeight = dp(52)
            contentDescription = "Search installed apps"
        }
        content.addView(search)
        val appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(appList)

        val apps = service.discoverApps()
        val selectedPackages = service.selectedPackageNames().toMutableSet()
        fun renderApps(query: String) {
            appList.removeAllViews()
            apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
                .forEach { app -> appList.addView(appRow(app, selectedPackages)) }
            if (appList.childCount == 0) textInto(appList, "No matching launchable apps.", 14, false)
        }
        renderApps("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        content.addView(button("Save selection") {
            store.smartTriggersEnabled = enabled.isChecked
            store.smartTriggerGameId = gameDefinitions[gameSpinner.selectedItemPosition].id
            store.smartTriggerCooldownMillis = cooldownOptions[cooldownSpinner.selectedItemPosition].first
            service.setSelectedPackageNames(selectedPackages.intersect(apps.map { it.packageName }.toSet()))
            service.clearRuntimeHistory()
            showSettings()
        })
        content.addView(button("Disable auto-trigger") {
            store.smartTriggersEnabled = false
            enabled.isChecked = false
            showSettings()
        })
        content.addView(button("Test notification now") {
            startGame(gameDefinitions[gameSpinner.selectedItemPosition].id)
        })
        content.addView(button("Clear selected apps") {
            selectedPackages.clear()
            service.clearSelectedPackageNames()
            renderApps(search.text?.toString().orEmpty())
        })
        content.addView(button("Clear trigger history") { service.clearRuntimeHistory() })
        content.addView(button("Back to Settings") { showSettings() })
    }

    private fun appRow(app: SelectableLaunchableApp, selectedPackages: MutableSet<String>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(8) }
            setImageDrawable(runCatching { packageManager.getApplicationIcon(app.packageName) }.getOrNull() ?: getDrawable(R.drawable.ic_launcher))
            contentDescription = null
        }
        val check = CheckBox(this).apply {
            text = app.label
            contentDescription = "${app.label}, package ${app.packageName}"
            isChecked = app.packageName in selectedPackages
            minHeight = dp(52)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setOnCheckedChangeListener { _, checked -> if (checked) selectedPackages += app.packageName else selectedPackages -= app.packageName }
        }
        row.addView(icon)
        row.addView(check)
        row.setOnClickListener { check.isChecked = !check.isChecked }
        row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        return row
    }

    private fun pollSmartTrigger() {
        if (!::store.isInitialized || !store.smartTriggersEnabled) return
        when (val result = triggerService().poll()) {
            is SmartTriggerPollResult.Triggered -> {
                store.select(store.smartTriggerGameId)
                notification.show()
            }
            else -> Unit
        }
    }

    private fun triggerService(): SmartAppTriggerService = SmartAppTriggerServiceFactory.create(
        this,
        SmartTriggerConfig(cooldownMillis = store.smartTriggerCooldownMillis),
    )

    private fun startGame(gameId: String) {
        store.select(gameId)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingStartGameId = gameId
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
            return
        }
        if (notification.show()) showHome() else showSettings()
    }

    private fun notificationStatus(): String = when {
        Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> "Permission needed — allow notifications to play from the shade."
        !notification.canPost() -> "Notifications or the arcade channel are disabled in Android settings."
        store.active -> if (store.paused) "Active and paused." else "Active and ready in the notification shade."
        else -> "Allowed but not running."
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.content.Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationController.CHANNEL_ID)
        } else {
            android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun showAbout() {
        screen = Screen.ABOUT
        updateNavigation()
        content.removeAllViews()
        heading("About AdBreak Arcade", "A small, offline arcade for the moments between everything else.")
        val intro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundResource(R.drawable.bg_lilac_pill)
        }
        intro.addView(TextView(this).apply {
            text = "Play without leaving your flow"
            textSize = 21f
            setTextColor(getColor(R.color.arcade_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        intro.addView(TextView(this).apply {
            text = "AdBreak Arcade keeps ${GameRegistry.definitions.size} quick games close at hand through a quiet Android notification. It is designed for short, intentional play while another app stays in front."
            textSize = 15f
            setTextColor(getColor(R.color.arcade_ink))
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(intro, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(20)) })
        text("Related topics", 20, true)
        topic("Notification-first play", "Every move is a normal notification control. Expand the notification when you want to play, then return to the app you were already using.")
        topic("Offline by default", "The games, scores, and saved state stay on this device. There is no account, analytics, or network access required.")
        topic("Respectful boundaries", "AdBreak Arcade does not detect, block, skip, overlay, or manipulate advertisements or other apps.")
        topic("Smart App Triggers", "Optional Usage access lets you select apps whose foreground transitions can prepare the notification on a best-effort basis. The feature is off until you enable it.")
        text("Version ${BuildConfig.VERSION_NAME}", 14, false)
    }

    private fun topic(title: String, body: String) {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundResource(R.drawable.bg_card)
        }
        block.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(getColor(R.color.arcade_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        block.addView(TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(getColor(R.color.arcade_ink))
            setPadding(0, dp(6), 0, 0)
        })
        content.addView(block, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) })
    }

    private fun heading(title: String, subtitle: String) {
        content.addView(textView("ADBREAK ARCADE  ·  ${screen.name.replace('_', ' ')}", 11, true).apply {
            setTextColor(getColor(R.color.arcade_violet))
            setPadding(0, 0, 0, dp(8))
        })
        val titleView = textView(title, 30, true)
        ViewCompat.setAccessibilityHeading(titleView, true)
        content.addView(titleView)
        text(subtitle, 16, false)
    }

    private fun card(game: ArcadeGame, selected: Boolean) {
        val presentation = GamePresentationCatalog.forGame(game.id)
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundResource(if (selected) R.drawable.bg_lilac_pill else R.drawable.bg_card)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = presentation.motif
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(getColor(presentation.family.accent))
            setBackgroundResource(R.drawable.bg_violet_pill)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(36)).apply { marginEnd = dp(10) }
        })
        top.addView(TextView(this).apply {
            text = "${presentation.family.label.uppercase()}  ·  ${if (selected) "CURRENT" else "READY"}"
            textSize = 11f
            setTextColor(getColor(presentation.family.accent))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        top.addView(button(if (store.isFavorite(game.id)) "★" else "☆") {
            store.toggleFavorite(game.id)
            if (screen == Screen.HOME) showHome() else showLibrary()
        }.apply {
            contentDescription = if (store.isFavorite(game.id)) "Remove ${game.title} from favorites" else "Add ${game.title} to favorites"
            minWidth = dp(48)
            minHeight = dp(48)
        })
        block.addView(top)
        block.addView(TextView(this).apply {
            text = game.title
            textSize = 21f
            setTextColor(getColor(R.color.arcade_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(14), 0, 0)
        })
        block.addView(TextView(this).apply {
            text = game.description
            textSize = 15f
            setTextColor(getColor(R.color.arcade_on_surface_variant))
            setPadding(0, dp(7), 0, dp(12))
        })
        if (selected) {
            block.addView(TextView(this).apply {
                text = "Selected game · start from the notification"
                textSize = 14f
                setTextColor(getColor(R.color.arcade_on_primary_container))
                setPadding(0, 0, 0, dp(8))
            })
        } else {
            block.addView(button("Use this game") { store.select(game.id); showLibrary() })
        }
        block.addView(button("Start notification") { startGame(game.id) })
        content.addView(block, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
    }

    private fun text(value: String, size: Int, bold: Boolean) {
        content.addView(textView(value, size, bold).apply {
            if (bold && size >= 18) ViewCompat.setAccessibilityHeading(this, true)
            setPadding(0, 0, 0, dp(12))
        })
    }

    private fun textInto(parent: LinearLayout, value: String, size: Int, bold: Boolean) {
        parent.addView(textView(value, size, bold).apply { setPadding(0, dp(8), 0, dp(12)) })
    }

    private fun textView(value: String, size: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(getColor(R.color.arcade_ink))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(label: String, click: () -> Unit) = MaterialButton(this).apply {
        text = label
        setOnClickListener { click() }
        isAllCaps = false
        minHeight = dp(52)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            val gameId = pendingStartGameId
            pendingStartGameId = null
            if (gameId != null && notification.show()) showHome() else showSettings()
        } else if (requestCode == this.requestCode) {
            pendingStartGameId = null
            showSettings()
        }
    }

    companion object {
        private const val KEY_SCREEN = "screen"
    }
}
