package com.atlas.ai

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : Activity() {
    private val bg = Color.rgb(10, 13, 27)
    private val surface = Color.argb(218, 25, 31, 55)
    private val accent = Color.rgb(157, 139, 255)
    private lateinit var appsArea: LinearLayout
    private lateinit var search: EditText
    private var allApps: List<AppEntry> = emptyList()
    private val prefs by lazy { getSharedPreferences("atlas_home", MODE_PRIVATE) }

    data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 13, 27)
        window.navigationBarColor = Color.rgb(10, 13, 27)
        window.decorView.systemUiVisibility = 0
        allApps = loadApps()
        buildHome()
        if (intent.getBooleanExtra("open_dock_customizer", false)) window.decorView.post { customizeDock() }
    }

    override fun onResume() {
        super.onResume()
        if (::appsArea.isInitialized) {
            allApps = loadApps()
            renderApps(search.text.toString())
        }
    }

    private fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        return resolved.mapNotNull {
            val name = it.loadLabel(packageManager)?.toString()?.takeIf { s -> s.isNotBlank() } ?: return@mapNotNull null
            AppEntry(name, it.activityInfo.packageName, it.loadIcon(packageManager))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    private fun buildHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            setBackgroundResource(R.drawable.atlas_wallpaper)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        heading.addView(TextView(this).apply {
            text = "ATLAS"
            textSize = 25f
            letterSpacing = .16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        heading.addView(TextView(this).apply {
            text = "YOUR SPACE. YOUR APPS."
            textSize = 9f
            letterSpacing = .14f
            setTextColor(Color.rgb(178, 183, 211))
        })
        top.addView(heading)
        top.addView(actionChip("⚙", "Settings") { openMain("settings") })
        root.addView(top)

        val clock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(15), dp(17), dp(15))
            background = rounded(Color.argb(120, 255, 255, 255), 22)
        }
        clock.addView(TextView(this).apply {
            text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
            textSize = 12f
            setTextColor(Color.rgb(205, 210, 233))
        })
        clock.addView(TextView(this).apply {
            text = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            textSize = 42f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.WHITE)
        })
        clock.addView(TextView(this).apply {
            text = "Welcome back"
            textSize = 12f
            setTextColor(Color.rgb(198, 202, 226))
        })
        root.addView(clock, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        search = EditText(this).apply {
            hint = "Search your apps"
            setHintTextColor(Color.rgb(183, 187, 211))
            setTextColor(Color.WHITE)
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            background = rounded(surface, 18)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderApps(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        val appHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(2), dp(15), dp(2), dp(8)) }
        appHeader.addView(TextView(this).apply {
            text = "Your apps"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        appHeader.addView(TextView(this).apply {
            text = "${allApps.size} installed"
            textSize = 11f
            setTextColor(Color.rgb(184, 188, 214))
        })
        root.addView(appHeader)

        val scroll = ScrollView(this).apply { isFillViewport = false; overScrollMode = View.OVER_SCROLL_NEVER }
        appsArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(2), 0, dp(12)) }
        scroll.addView(appsArea)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        renderApps("")

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(Color.argb(225, 34, 39, 67), 25)
            elevation = dp(8).toFloat()
        }
        val favorites = prefs.getStringSet("dock_apps", defaultDock()).orEmpty()
            .mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg } }.take(3)
        favorites.forEach { app -> dockAppItem(dock, app) { launchApp(app.packageName) } }
        dockItem(dock, "✦", "AI") { openMain("ai") }
        dockItem(dock, "◎", "BROWSER") { startActivity(Intent(this, BrowserActivity::class.java)); overridePendingTransition(0, 0) }
        dockItem(dock, "＋", "EDIT") { customizeDock() }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(70)).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun renderApps(query: String) {
        if (!::appsArea.isInitialized) return
        appsArea.removeAllViews()
        val filtered = allApps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
        if (filtered.isEmpty()) {
            appsArea.addView(TextView(this).apply { text = "No apps found"; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, dp(30), 0, dp(30)) })
            return
        }
        val columns = 4
        filtered.chunked(columns).forEach { rowApps ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
            rowApps.forEach { app ->
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(9), dp(3), dp(9))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { launchApp(app.packageName) }
                    setOnLongClickListener { showAppActions(app); true }
                }
                tile.addView(ImageView(this).apply {
                    setImageDrawable(app.icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                tile.addView(TextView(this).apply {
                    text = app.label
                    textSize = 10f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(Color.WHITE)
                    setPadding(0, dp(5), 0, 0)
                }, LinearLayout.LayoutParams(-1, -2))
                row.addView(tile, LinearLayout.LayoutParams(0, dp(82), 1f))
            }
            repeat(columns - rowApps.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(82), 1f)) }
            appsArea.addView(row)
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            } else Toast.makeText(this, "App launch shortcut is unavailable", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { Toast.makeText(this, "Couldn't open this app", Toast.LENGTH_SHORT).show() }
    }

    private fun showAppActions(app: AppEntry) {
        AlertDialog.Builder(this).setTitle(app.label)
            .setItems(arrayOf("Open app", "App info", "Add to dock")) { _, which ->
                when (which) {
                    0 -> launchApp(app.packageName)
                    1 -> startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                    2 -> {
                        val set = prefs.getStringSet("dock_apps", defaultDock()).orEmpty().toMutableSet()
                        set.add(app.packageName); prefs.edit().putStringSet("dock_apps", set).apply()
                        Toast.makeText(this, "Added to your dock", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun customizeDock() {
        val defaults = defaultDock()
        val selected = prefs.getStringSet("dock_apps", defaults).orEmpty().toMutableSet()
        val labels = allApps.map { it.label }.toTypedArray()
        val checked = BooleanArray(allApps.size) { selected.contains(allApps[it].packageName) }
        AlertDialog.Builder(this).setTitle("Customize your dock")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                if (isChecked) selected.add(allApps[which].packageName) else selected.remove(allApps[which].packageName)
            }
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putStringSet("dock_apps", selected).apply()
                Toast.makeText(this, "Dock updated", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun defaultDock(): Set<String> {
        val wanted = listOf("com.google.android.dialer", "com.android.dialer", "com.google.android.apps.messaging",
            "com.whatsapp", "com.android.chrome", "com.google.android.youtube")
        val installed = allApps.map { it.packageName }.toSet()
        return wanted.filter { installed.contains(it) }.take(4).toSet()
    }

    private fun dockAppItem(parent: LinearLayout, app: AppEntry, action: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            setOnLongClickListener { showAppActions(app); true }
        }
        item.addView(ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER },
            LinearLayout.LayoutParams(dp(29), dp(29)))
        item.addView(TextView(this).apply {
            text = app.label; textSize = 7f; maxLines = 1; gravity = Gravity.CENTER
            ellipsize = android.text.TextUtils.TruncateAt.END; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(dp(48), -2))
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun dockItem(parent: LinearLayout, glyph: String, label: String, action: () -> Unit) {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; isFocusable = true; setOnClickListener { action() } }
        item.addView(TextView(this).apply { text = glyph; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        item.addView(TextView(this).apply { text = label; textSize = 8f; gravity = Gravity.CENTER; setTextColor(Color.rgb(202, 205, 227)) })
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun actionChip(glyph: String, label: String, action: () -> Unit): View =
        TextView(this).apply { text = "$glyph  $label"; textSize = 11f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(surface, 16); setOnClickListener { action() } }

    private fun openMain(section: String) {
        if (section == "settings") startActivity(Intent(this, SettingsActivity::class.java))
        else startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(0, 0)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
