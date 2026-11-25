package com.atlas.ai

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.net.Uri

class BrowserActivity : Activity() {
    private val bg = Color.rgb(8, 10, 18)
    private val panel = Color.rgb(24, 27, 42)
    private val accent = Color.rgb(137, 121, 255)
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private var privateMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        privateMode = getSharedPreferences("atlas_browser", MODE_PRIVATE).getBoolean("private_mode", true)
        buildBrowser()
        if (savedInstanceState == null) loadUrl(getSharedPreferences("atlas_browser", MODE_PRIVATE).getString("homepage", "https://duckduckgo.com/") ?: "https://duckduckgo.com/")
    }

    private fun buildBrowser() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(8))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "ATLAS  /  BROWSER"
            textSize = 17f
            letterSpacing = .08f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(TextView(this).apply {
            text = "● PRIVATE"
            textSize = 10f
            letterSpacing = .08f
            setTextColor(Color.rgb(112, 218, 174))
            gravity = Gravity.CENTER
        })
        root.addView(header)

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(3), dp(5), dp(3))
            background = rounded(panel, 18)
        }
        address = EditText(this).apply {
            hint = "Search DuckDuckGo or enter address"
            setHintTextColor(Color.rgb(143, 147, 168))
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 1
            setSingleLine(true)
            setPadding(dp(8), dp(9), dp(4), dp(9))
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    loadAddress(address.text.toString()); true
                } else false
            }
        }
        bar.addView(address, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(Button(this).apply {
            text = "GO"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = rounded(accent, 14)
            setOnClickListener { click(); loadAddress(address.text.toString()) }
        }, LinearLayout.LayoutParams(dp(48), dp(40)))
        root.addView(bar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(panel)
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)).apply { topMargin = dp(8) })
        web = WebView(this).apply {
            setBackgroundColor(bg)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = !privateMode
            settings.databaseEnabled = !privateMode
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = if (privateMode) android.webkit.WebSettings.LOAD_NO_CACHE else android.webkit.WebSettings.LOAD_DEFAULT
            settings.userAgentString = settings.userAgentString
            isSaveEnabled = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    this@BrowserActivity.title.text = "Loading…"
                    address.setText(url ?: "")
                    this@BrowserActivity.progress.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    address.setText(url ?: "")
                    this@BrowserActivity.title.text = view?.title?.take(42)?.ifBlank { "Ready" } ?: "Ready"
                    this@BrowserActivity.progress.visibility = View.GONE
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    this@BrowserActivity.progress.progress = newProgress
                    this@BrowserActivity.progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        title = TextView(this).apply {
            text = "Private search by DuckDuckGo"
            textSize = 10f
            setTextColor(Color.rgb(147, 151, 174))
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        root.addView(title)

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5))
            background = rounded(panel, 22)
        }
        dockButton(dock, "⌂", "HOME") { finish() }
        dockButton(dock, "‹", "BACK") { if (web.canGoBack()) web.goBack() }
        dockButton(dock, "↻", "RELOAD") { web.reload() }
        dockButton(dock, "＋", "NEW") { address.setText(""); loadUrl("https://duckduckgo.com/"); address.requestFocus() }
        dockButton(dock, "⚙", "SETTINGS") { showBrowserSettings() }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(5) })
        setContentView(root)
    }

    private fun dockButton(parent: LinearLayout, glyph: String, label: String, action: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { click(); action() }
        }
        item.addView(TextView(this).apply { text = glyph; textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        item.addView(TextView(this).apply { text = label; textSize = 8f; letterSpacing = .04f; gravity = Gravity.CENTER; setTextColor(Color.rgb(155, 159, 182)) })
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun loadAddress(raw: String) {
        val value = raw.trim()
        if (value.isBlank()) return
        val prefs = getSharedPreferences("atlas_browser", MODE_PRIVATE)
        val engine = prefs.getString("search_engine", "DuckDuckGo") ?: "DuckDuckGo"
        val searchBase = when (engine) {
            "Google" -> "https://www.google.com/search?q="
            "Bing" -> "https://www.bing.com/search?q="
            "Brave" -> "https://search.brave.com/search?q="
            else -> "https://duckduckgo.com/?q="
        }
        val target = when {
            value.startsWith("https://") || value.startsWith("http://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> searchBase + Uri.encode(value)
        }
        loadUrl(target)
    }
    private fun loadUrl(url: String) { web.loadUrl(url) }
    private fun click() { window.decorView.playSoundEffect(SoundEffectConstants.CLICK) }

    private fun showBrowserSettings() {
        val prefs = getSharedPreferences("atlas_browser", MODE_PRIVATE)
        val options = arrayOf("Change homepage", "Choose search engine",
            if (privateMode) "Turn off private mode" else "Turn on private mode",
            "Clear browsing data", "Open homepage", "Open Android app settings")
        AlertDialog.Builder(this).setTitle("Customize Atlas Browser").setItems(options) { _, which ->
            when (which) {
                0 -> {
                    val field = EditText(this).apply {
                        setSingleLine(true)
                        setText(prefs.getString("homepage", "https://duckduckgo.com/"))
                        hint = "https://example.com"
                    }
                    AlertDialog.Builder(this).setTitle("Browser homepage").setView(field)
                        .setPositiveButton("Save") { _, _ ->
                            val value = field.text.toString().trim()
                            if (value.startsWith("https://") || value.startsWith("http://")) {
                                prefs.edit().putString("homepage", value).apply()
                                Toast.makeText(this, "Homepage saved", Toast.LENGTH_SHORT).show()
                            } else Toast.makeText(this, "Enter a valid http(s) address", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("Cancel", null).show()
                }
                1 -> {
                    val engines = arrayOf("DuckDuckGo", "Google", "Bing", "Brave")
                    val current = engines.indexOf(prefs.getString("search_engine", "DuckDuckGo")).coerceAtLeast(0)
                    AlertDialog.Builder(this).setTitle("Search engine")
                        .setSingleChoiceItems(engines, current) { dialog, index ->
                            prefs.edit().putString("search_engine", engines[index]).apply()
                            Toast.makeText(this, engines[index] + " selected", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }.setNegativeButton("Cancel", null).show()
                }
                2 -> {
                    privateMode = !privateMode
                    prefs.edit().putBoolean("private_mode", privateMode).apply()
                    web.settings.domStorageEnabled = !privateMode
                    web.settings.databaseEnabled = !privateMode
                    web.settings.cacheMode = if (privateMode) android.webkit.WebSettings.LOAD_NO_CACHE else android.webkit.WebSettings.LOAD_DEFAULT
                    if (privateMode) clearData()
                    Toast.makeText(this, if (privateMode) "Private mode enabled (best-effort)" else "Private mode disabled", Toast.LENGTH_SHORT).show()
                }
                3 -> { clearData(); Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show() }
                4 -> loadUrl(prefs.getString("homepage", "https://duckduckgo.com/") ?: "https://duckduckgo.com/")
                5 -> startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }.setNegativeButton("Close", null).show()
    }

    private fun clearData() {
        web.clearCache(true)
        web.clearHistory()
        web.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onDestroy() {
        if (::web.isInitialized) { web.stopLoading(); web.destroy() }
        super.onDestroy()
    }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}