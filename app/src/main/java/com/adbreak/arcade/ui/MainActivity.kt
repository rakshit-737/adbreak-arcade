package com.adbreak.arcade.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.adbreak.arcade.R
import com.adbreak.arcade.core.ArcadeGame
import com.adbreak.arcade.core.GameRegistry
import com.adbreak.arcade.data.ArcadeStore
import com.adbreak.arcade.notification.NotificationController
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var store: ArcadeStore
    private lateinit var content: LinearLayout
    private lateinit var notification: NotificationController
    private val requestCode = 7302

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        store = ArcadeStore(this); notification = NotificationController(this); content = findViewById(R.id.screenContainer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activityRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        findViewById<View>(R.id.navHome).setOnClickListener { showHome() }
        findViewById<View>(R.id.navLibrary).setOnClickListener { showLibrary() }
        findViewById<View>(R.id.navScores).setOnClickListener { showScores() }
        findViewById<View>(R.id.navSettings).setOnClickListener { showSettings() }
        showHome()
    }

    private fun showHome() {
        content.removeAllViews(); heading("Make the waiting part yours", "Play a quiet, offline mini-game from the notification shade while your other app stays in front.")
        val game = store.loadGame(); card(game, true)
        text(if (store.active && notification.canPost()) "Notification active — expand it from the shade when you are ready." else "Notification inactive — start a game below when you want a quiet break companion.", 14, false)
        text("How it works", 19, true); text("Choose a game, tap Start, then pull down and expand the AdBreak Arcade notification. Every move is a normal notification button. The app never detects or manipulates ads.", 15, false)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val permission = button("Allow notifications") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode) }; content.addView(permission)
        }
    }

    private fun showLibrary() {
        content.removeAllViews(); heading("The arcade shelf", "Seven small games designed around real notification controls.")
        GameRegistry.definitions.forEach { game -> card(game, game.id == store.selectedGameId) }
    }

    private fun showScores() { content.removeAllViews(); heading("Your run, kept local", "Personal bests stay on this device. No account, analytics, or network access."); val game = store.loadGame(); text(game.render().scoreLine, 22, true); text("Select a game from Library to start a fresh challenge or continue its saved state.", 15, false) }

    private fun showSettings() { content.removeAllViews(); heading("Settings", "Keep the arcade quiet, clear, and under your control."); text("Notification permission", 18, true); text(if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "Allowed — the game notification can appear." else "Not allowed — enable it in Android Settings to play in the notification shade.", 15, false); content.addView(button("Open notification settings") { startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) }); content.addView(button("Stop active notification") { notification.stop(); showHome() }) }

    private fun heading(title: String, subtitle: String) { text(title, 30, true); text(subtitle, 16, false) }
    private fun card(game: ArcadeGame, selected: Boolean) {
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)); setBackgroundResource(if (selected) R.drawable.bg_lilac_pill else R.drawable.bg_card) }
        val title = TextView(this).apply { text = game.title; textSize = 21f; setTextColor(getColor(R.color.arcade_ink)); setTypeface(typeface, android.graphics.Typeface.BOLD) }; block.addView(title)
        val desc = TextView(this).apply { text = game.description; textSize = 15f; setPadding(0, dp(7), 0, dp(12)) }; block.addView(desc)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button(if (selected) "Selected" else "Use this game") { store.select(game.id); notification.show(); showHome() })
        actions.addView(button("Start notification") { store.select(game.id); notification.show(); showHome() })
        block.addView(actions); content.addView(block, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
    }
    private fun text(value: String, size: Int, bold: Boolean) { content.addView(TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(getColor(R.color.arcade_ink)); if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, dp(12)) }) }
    private fun button(label: String, click: () -> Unit) = MaterialButton(this).apply { text = label; setOnClickListener { click() }; isAllCaps = false; minHeight = dp(52) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            notification.show()
            showHome()
        }
    }
}
