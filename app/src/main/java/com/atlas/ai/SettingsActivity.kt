package com.atlas.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class SettingsActivity : Activity() {
    private val bg = Color.rgb(8, 10, 22)
    private val panel = Color.rgb(22, 26, 49)
    private val accent = Color.rgb(139, 124, 255)
    private val prefs by lazy { getSharedPreferences("atlas", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildSettings()
    }

    private fun buildSettings() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        titleBox.addView(TextView(this).apply {
            text = "Settings"; textSize = 27f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        })
        titleBox.addView(TextView(this).apply {
            text = "Make Atlas work the way you like"; textSize = 12f; setTextColor(Color.rgb(155, 164, 194))
        })
        header.addView(titleBox)
        header.addView(Button(this).apply {
            text = "✕"; textSize = 17f; setTextColor(Color.WHITE); background = rounded(Color.rgb(35, 40, 70), 15)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)

        val scroll = ScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(18)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun section(name: String, description: String) {
            body.addView(TextView(this).apply {
                text = name; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE); setPadding(dp(2), dp(17), 0, dp(3))
            })
            body.addView(TextView(this).apply {
                text = description; textSize = 11f; setTextColor(Color.rgb(151, 160, 190))
                setPadding(dp(2), 0, 0, dp(8))
            })
        }
        fun card(): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(5), dp(13), dp(5))
            background = rounded(panel, 20)
            body.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) })
        }
        fun row(parent: LinearLayout, icon: String, name: String, detail: String, action: () -> Unit) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(11), 0, dp(11)); isClickable = true; isFocusable = true
                setOnClickListener { action() }
            }
            line.addView(TextView(this).apply {
                text = icon; textSize = 19f; gravity = Gravity.CENTER
                setTextColor(Color.rgb(201, 188, 255)); background = rounded(Color.rgb(43, 39, 84), 13)
            }, LinearLayout.LayoutParams(dp(42), dp(42)))
            val labels = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(6), 0)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            labels.addView(TextView(this).apply { text = name; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            labels.addView(TextView(this).apply { text = detail; textSize = 10f; setTextColor(Color.rgb(158, 166, 194)); setPadding(0, dp(2), 0, 0) })
            line.addView(labels)
            line.addView(TextView(this).apply { text = "›"; textSize = 25f; setTextColor(Color.rgb(124, 134, 169)) })
            parent.addView(line)
        }
        fun toggle(parent: LinearLayout, icon: String, name: String, detail: String, key: String, default: Boolean) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)) }
            line.addView(TextView(this).apply { text = icon; textSize = 18f; setTextColor(Color.rgb(201, 188, 255)) }, LinearLayout.LayoutParams(dp(34), -2))
            val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            labels.addView(TextView(this).apply { text = name; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            labels.addView(TextView(this).apply { text = detail; textSize = 10f; setTextColor(Color.rgb(158, 166, 194)) })
            line.addView(labels)
            line.addView(android.widget.Switch(this).apply {
                isChecked = prefs.getBoolean(key, default)
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
            })
            parent.addView(line)
        }

        section("Appearance", "Personalize the wallpaper and language.")
        val appearance = card()
        row(appearance, "✧", "Apply Atlas wallpaper", "Set the new blue and purple abstract wallpaper") {
            try {
                android.app.WallpaperManager.getInstance(this).setResource(R.drawable.atlas_wallpaper)
                Toast.makeText(this, "Atlas wallpaper applied", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "Could not apply wallpaper", Toast.LENGTH_SHORT).show() }
        }
        row(appearance, "▧", "Choose your own wallpaper", "Select an image from your phone") { chooseWallpaper() }
        row(appearance, "文", "Chat language", "Current: " + (prefs.getString("atlas_language", "auto") ?: "auto")) { showLanguagePicker() }

        section("AI assistant", "Manage Atlas AI and your on-device model connection.")
        val ai = card()
        row(ai, "✦", "Open Atlas AI", "Return to the assistant and start chatting") { finish() }
        ai.addView(TextView(this).apply {
            text = "Local AI server address"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(0, dp(10), 0, dp(6))
        })
        val server = EditText(this).apply {
            hint = "http://127.0.0.1:11434"; setSingleLine(true); textSize = 13f
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(139, 146, 174))
            setPadding(dp(12), dp(10), dp(12), dp(10)); setBackground(rounded(Color.rgb(35, 40, 70), 13))
            setText(prefs.getString("local_server_url", "http://127.0.0.1:11434"))
        }
        ai.addView(server, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        ai.addView(Button(this).apply {
            text = "Save AI connection"; textSize = 12f; setTextColor(Color.WHITE); background = rounded(accent, 14)
            setOnClickListener {
                val address = server.text.toString().trim().trimEnd('/')
                if (address.startsWith("http://") || address.startsWith("https://")) {
                    prefs.edit().putString("local_server_url", address).apply()
                    Toast.makeText(this@SettingsActivity, "AI connection saved", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this@SettingsActivity, "Enter a valid server address", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(44)))
        row(ai, "▦", "Device compatibility scan", "Check RAM, processor and recommended AI settings") {
            startActivity(Intent(this, DeviceScanActivity::class.java))
        }

        section("Browser", "Your own Atlas start page with Google-powered web search.")
        val browser = card()
        row(browser, "◎", "Open Atlas Browser", "Search from Atlas or open any website") {
            startActivity(Intent(this, BrowserActivity::class.java))
        }

        section("Home screen & Dock", "Choose how Atlas looks and responds.")
        val home = card()
        row(home, "⌂", "Set as default Home app", "Choose Atlas as your Android launcher") { openDefaultHomeSettings() }
        row(home, "▦", "Customize app dock", "Choose which installed apps appear at the bottom") {
            startActivity(Intent(this, HomeActivity::class.java).putExtra("open_dock_customizer", true))
        }
        toggle(home, "✧", "Dock animations", "Smooth icon movement", "dock_animation", true)
        toggle(home, "♫", "Touch sounds", "Subtle tap sounds", "ui_sounds", true)
        toggle(home, "◌", "Haptic feedback", "Gentle vibration on tap", "haptics", false)
        toggle(home, "◫", "Reduce motion", "Simpler transitions for slower phones", "reduce_motion", false)

        section("Phone settings", "Shortcuts to Android system controls.")
        val phone = card()
        val systemSettings = listOf(
            "Wi-Fi & network" to android.provider.Settings.ACTION_WIFI_SETTINGS,
            "Sound & vibration" to android.provider.Settings.ACTION_SOUND_SETTINGS,
            "Battery" to android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "Apps" to android.provider.Settings.ACTION_APPLICATION_SETTINGS,
            "Storage" to android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "Display" to android.provider.Settings.ACTION_DISPLAY_SETTINGS,
            "Security" to android.provider.Settings.ACTION_SECURITY_SETTINGS,
            "All Android settings" to android.provider.Settings.ACTION_SETTINGS
        )
        systemSettings.forEach { (label, action) -> row(phone, "⚙", label, "Open phone settings") { openSystemSettings(action) } }

        section("About Atlas", "A local-first Android launcher and AI assistant.")
        val about = card()
        row(about, "ⓘ", "About Atlas AI", "Version 1.0 • Response speed depends on device performance") {
            AlertDialog.Builder(this).setTitle("Atlas AI")
                .setMessage("Atlas is an Android launcher and local AI assistant. It does not replace Android itself. AI speed depends on your device's processing power and memory.")
                .setPositiveButton("OK", null).show()
        }
        setContentView(root)
    }

    private fun chooseWallpaper() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(picker, 7011)
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7011 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    contentResolver.openInputStream(uri)?.use { android.app.WallpaperManager.getInstance(this).setStream(it) }
                    Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { Toast.makeText(this, "Could not set wallpaper", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun showLanguagePicker() {
        val names = arrayOf("Auto (device language)", "Roman Urdu", "Urdu", "English", "Arabic", "Bengali", "Chinese (Simplified)", "Chinese (Traditional)", "French", "German", "Hindi", "Indonesian", "Italian", "Japanese", "Korean", "Malay", "Pashto", "Persian", "Portuguese", "Punjabi", "Russian", "Spanish", "Swahili", "Tamil", "Telugu", "Thai", "Turkish", "Vietnamese")
        val codes = arrayOf("auto", "ur-Latn", "ur", "en", "ar", "bn", "zh-CN", "zh-TW", "fr", "de", "hi", "id", "it", "ja", "ko", "ms", "ps", "fa", "pt", "pa", "ru", "es", "sw", "ta", "te", "th", "tr", "vi")
        val current = prefs.getString("atlas_language", "auto") ?: "auto"
        AlertDialog.Builder(this).setTitle("Choose chat language")
            .setSingleChoiceItems(names, codes.indexOf(current).coerceAtLeast(0)) { dialog, which ->
                prefs.edit().putString("atlas_language", codes[which]).apply()
                Toast.makeText(this, "Language updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun openDefaultHomeSettings() {
        try { startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
        catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }

    private fun openSystemSettings(action: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
