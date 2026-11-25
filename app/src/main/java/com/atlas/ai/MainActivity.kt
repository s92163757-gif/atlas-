package com.atlas.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 14, 24)
    private val panel = Color.rgb(25, 28, 43)
    private val panel2 = Color.rgb(34, 37, 56)
    private val accent = Color.rgb(139, 124, 255)
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var statusText: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val localHistory = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        AtlasDeviceScanner.save(this, AtlasDeviceScanner.scan(this))
        buildScreen()
        addMessage("ATLAS", "Hey! Main Atlas hoon. Local AI server start rakho aur mujhse chat karo. Voice button se commands bhi de sakte ho.\n\nAtlas response speed may depend on your device's performance and computing processing power.")
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundResource(R.drawable.atlas_wallpaper)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(TextView(this).apply {
            text = "ATLAS"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.12f
        })
        brand.addView(TextView(this).apply {
            text = "YOUR AI • YOUR PHONE"
            textSize = 10f
            setTextColor(Color.rgb(160, 163, 185))
            letterSpacing = 0.12f
        })
        header.addView(brand)
        header.addView(Button(this).apply {
            text = "🌐"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = rounded(panel2, 16)
            setOnClickListener { startActivity(Intent(this@MainActivity, BrowserActivity::class.java)) }
        })
        header.addView(Button(this).apply {
            text = "🎧"
            textSize = 17f
            setTextColor(Color.WHITE)
            background = rounded(panel2, 16)
            setOnClickListener { toggleAlwaysListening() }
        })
        header.addView(Button(this).apply {
            text = "⚙"
            textSize = 20f
            setTextColor(Color.WHITE)
            background = rounded(panel2, 16)
            setOnClickListener { showSettings() }
        })
        root.addView(header)

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(panel, 16)
        }
        status.addView(View(this).apply {
            background = rounded(Color.rgb(73, 210, 145), 50)
        }, LinearLayout.LayoutParams(dp(8), dp(8)))
        statusText = TextView(this).apply {
            text = "  LOCAL AI  •  127.0.0.1:11434"
            textSize = 11f
            setTextColor(Color.rgb(190, 194, 215))
            letterSpacing = 0.04f
        }
        status.addView(statusText)
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

        scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(14)) }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(6), dp(6), dp(6))
            background = rounded(panel, 24)
        }
        input = EditText(this).apply {
            hint = "Message Atlas..."
            setHintTextColor(Color.rgb(130, 134, 157))
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 4
            setPadding(dp(10), dp(9), dp(8), dp(9))
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(Button(this).apply {
            text = "🎙"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(68, 72, 105), 18)
            setOnClickListener { startVoiceInput() }
        })
        sendButton = Button(this).apply {
            text = "➤"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = rounded(accent, 18)
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(dp(52), dp(48)).apply { leftMargin = dp(6) })
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        root.addView(TextView(this).apply {
            text = "Voice commands: “open YouTube”, “open settings”, “volume up”"
            textSize = 10f
            setTextColor(Color.rgb(125, 129, 151))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = rounded(Color.argb(220, 23, 27, 52), 30)
            elevation = dp(12).toFloat()
        }
        fun dockItem(glyph: String, label: String, action: () -> Unit) {
            val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(Color.argb(90, 83, 91, 145), 17)
                setPadding(dp(2), dp(4), dp(2), dp(3))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (prefs.getBoolean("dock_animation", true) && !prefs.getBoolean("reduce_motion", false)) {
                        animate().cancel()
                        scaleX = 0.94f; scaleY = 0.94f; alpha = 0.82f
                        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170)
                            .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)).start()
                    }
                    if (prefs.getBoolean("haptics", false))
                        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    if (prefs.getBoolean("ui_sounds", false))
                        window.decorView.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    action()
                }
            }
            item.addView(TextView(this).apply { text = glyph; textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
            item.addView(TextView(this).apply { text = label; textSize = 8.5f; gravity = Gravity.CENTER; maxLines = 1; setTextColor(Color.rgb(205, 210, 235)) })
            dock.addView(item, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(2); rightMargin = dp(2) })
        }
        dockItem("⌂", "HOME") { scroll.smoothScrollTo(0, 0) }
        dockItem("🌐", "BROWSER") { startActivity(Intent(this, BrowserActivity::class.java)) }
        dockItem("✦", "ATLAS AI") { input.requestFocus(); val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager; imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }
        dockItem("⚙", "SETTINGS") { showSettings() }
        dockItem("▦", "DEVICE") { startActivity(Intent(this, DeviceScanActivity::class.java)) }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(66)).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun addMessage(who: String, body: String) {
        val isUser = who == "YOU"
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (isUser) Color.rgb(54, 47, 91) else panel, 18)
        }
        card.addView(TextView(this).apply {
            text = who
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(if (isUser) Color.rgb(199, 190, 255) else accent)
        })
        card.addView(TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(Color.rgb(245, 246, 252))
            setPadding(0, dp(5), 0, 0)
        })
        val params = LinearLayout.LayoutParams(if (isUser) (resources.displayMetrics.widthPixels * 0.82).toInt() else -1, -2)
        params.gravity = if (isUser) Gravity.END else Gravity.START
        params.topMargin = dp(6)
        params.bottomMargin = dp(6)
        messages.addView(card, params)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun toggleAlwaysListening() {
        val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
        if (prefs.getBoolean("wake_service_running", false)) {
            stopService(Intent(this, VoiceWakeService::class.java))
            prefs.edit().putBoolean("wake_service_running", false).apply()
            Toast.makeText(this, "Atlas wake listening stopped", Toast.LENGTH_SHORT).show()
            return
        }
        val permissions = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        val missing = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 7010) else startWakeService()
    }

    private fun startWakeService() {
        try {
            val serviceIntent = Intent(this, VoiceWakeService::class.java).setAction(VoiceWakeService.ACTION_START)
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
            getSharedPreferences("atlas", MODE_PRIVATE).edit().putBoolean("wake_service_running", true).apply()
            Toast.makeText(this, "Atlas is listening. Say: Atlas, open YouTube", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start listening: " + (e.message ?: "permission required"), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7010) {
            if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) startWakeService()
            else Toast.makeText(this, "Microphone and notification permissions are needed for always-listening.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command to Atlas")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 7001)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice recognition is not available. Check Google speech services.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7001 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input.setText(spoken)
                sendMessage()
            }
        } else if (requestCode == 7011 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    contentResolver.openInputStream(uri)?.use { android.app.WallpaperManager.getInstance(this).setStream(it) }
                    Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't set wallpaper: " + (e.message ?: "unsupported image"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun runPhoneCommand(prompt: String): Boolean {
        val command = prompt.trim()
        val lower = command.lowercase(Locale.ROOT)
        fun done(message: String) {
            addMessage("YOU", command)
            addMessage("ATLAS", message)
        }
        try {
            when {
                lower.matches(Regex("(open|launch|start)\\s+(youtube|google|chrome|whatsapp|maps|camera|settings|phone|dialer)")) -> {
                    val app = lower.substringAfterLast(" ")
                    when (app) {
                        "settings" -> startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                        "camera" -> startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                        "phone", "dialer" -> startActivity(Intent(Intent.ACTION_DIAL))
                        "youtube" -> openPackage("com.google.android.youtube", "https://www.youtube.com")
                        "google" -> openPackage("com.google.android.googlequicksearchbox", "https://www.google.com")
                        "chrome" -> openPackage("com.android.chrome", "https://www.google.com")
                        "whatsapp" -> openPackage("com.whatsapp", "https://wa.me/")
                        "maps" -> openPackage("com.google.android.apps.maps", "https://maps.google.com")
                    }
                    done("Opening $app.")
                    return true
                }
                lower == "volume up" || lower == "increase volume" -> {
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    done("Volume up.")
                    return true
                }
                lower == "volume down" || lower == "decrease volume" -> {
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    done("Volume down.")
                    return true
                }
                lower == "mute" || lower == "mute media" -> {
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    done("Media muted.")
                    return true
                }
                lower == "unmute" || lower == "unmute media" -> {
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    done("Media unmuted.")
                    return true
                }
                lower.startsWith("call ") -> {
                    val number = command.substringAfter(" ").trim()
                    if (number.isBlank()) return false
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
                    done("Dialler opened. Review the number and tap call yourself.")
                    return true
                }
                lower.startsWith("search ") -> {
                    val query = command.substringAfter(" ").trim()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
                    done("Searching the web for: $query")
                    return true
                }
            }
        } catch (_: Exception) {
            done("I couldn't perform that action. Your phone may not have a compatible app.")
            return true
        }
        return false
    }

    private fun openPackage(packageName: String, fallback: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        if (runPhoneCommand(prompt)) return
        addMessage("YOU", prompt)
        localHistory.put(JSONObject().put("role", "user").put("content", prompt))
        while (localHistory.length() > 12) localHistory.remove(0)
        sendButton.isEnabled = false
        sendButton.text = "…"
        statusText.text = "  CONNECTING TO LOCAL AI…"
        executor.execute {
            val answer = requestLocal()
            runOnUiThread {
                sendButton.isEnabled = true
                sendButton.text = "➤"
                statusText.text = "  LOCAL AI  •  127.0.0.1:11434"
                addMessage("ATLAS", if (answer.startsWith("ERROR:")) answer.removePrefix("ERROR:") else answer)
                if (!answer.startsWith("ERROR:")) localHistory.put(JSONObject().put("role", "assistant").put("content", answer))
            }
        }
    }

    private fun requestLocal(): String {
        var connection: HttpURLConnection? = null
        return try {
            val base = getSharedPreferences("atlas", MODE_PRIVATE)
                .getString("local_server_url", "http://127.0.0.1:11434")!!
                .trim().trimEnd('/')
            connection = (URL(base + "/v1/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 15000
            }
            val modelCode = connection.responseCode
            val modelBody = (if (modelCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (modelCode !in 200..299) return "ERROR:Local server HTTP $modelCode. Check that the API server is running."
            val models = JSONObject(modelBody).optJSONArray("data")
            val model = models?.optJSONObject(0)?.optString("id").orEmpty()
            if (model.isBlank()) return "ERROR:No loaded model found. Load the GGUF model in LLM AI Server first."
            connection.disconnect()
            connection = (URL(base + "/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 180000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val requestMessages = JSONArray()
            requestMessages.put(JSONObject().put("role", "system").put("content", languageInstruction()))
            for (i in 0 until localHistory.length()) requestMessages.put(localHistory.getJSONObject(i))
            val payload = JSONObject().put("model", model).put("messages", requestMessages).put("stream", false).put("max_tokens", getSharedPreferences("atlas", MODE_PRIVATE).getInt("recommended_tokens", 160))
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (code !in 200..299) "ERROR:Local AI HTTP $code: " + response.take(350)
            else JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                ?: "I couldn't read the local model response. Try again."
        } catch (e: Exception) {
            "ERROR:Can't reach local AI server. Keep LLM AI Server running on this phone. Details: " + (e.message ?: "connection failed")
        } finally {
            connection?.disconnect()
        }
    }

    private fun languageInstruction(): String {
        val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
        val selected = prefs.getString("atlas_language", "auto") ?: "auto"
        val autoLocale = Locale.getDefault()
        val language = when (selected) {
            "auto" -> when {
                autoLocale.country.equals("PK", true) -> "Roman Urdu (Urdu language written in Latin/Roman letters)"
                autoLocale.language == "ur" -> "Urdu"
                else -> autoLocale.getDisplayLanguage(Locale.ENGLISH)
            }
            "ur-Latn" -> "Roman Urdu (Urdu language written in Latin/Roman letters)"
            "zh-CN" -> "Simplified Chinese"
            "zh-TW" -> "Traditional Chinese"
            else -> Locale.forLanguageTag(selected).getDisplayLanguage(Locale.ENGLISH)
        }
        return "Reply naturally in $language. Keep using this language unless the user asks to switch. " +
            "If the user writes in another language and clearly wants that language, follow their request. " +
            "Do not claim the user's exact location or IP address."
    }

    private fun showLanguagePicker() {
        val names = arrayOf("Auto (device language)", "Roman Urdu", "Urdu", "English", "Arabic", "Bengali", "Chinese (Simplified)", "Chinese (Traditional)", "French", "German", "Hindi", "Indonesian", "Italian", "Japanese", "Korean", "Malay", "Pashto", "Persian", "Portuguese", "Punjabi", "Russian", "Spanish", "Swahili", "Tamil", "Telugu", "Thai", "Turkish", "Vietnamese")
        val codes = arrayOf("auto", "ur-Latn", "ur", "en", "ar", "bn", "zh-CN", "zh-TW", "fr", "de", "hi", "id", "it", "ja", "ko", "ms", "ps", "fa", "pt", "pa", "ru", "es", "sw", "ta", "te", "th", "tr", "vi")
        val prefs = getSharedPreferences("atlas", MODE_PRIVATE)
        val selected = prefs.getString("atlas_language", "auto") ?: "auto"
        val index = codes.indexOf(selected).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Choose Atlas language")
            .setSingleChoiceItems(names, index) { dialog, which ->
                prefs.edit().putString("atlas_language", codes[which]).apply()
                Toast.makeText(this, "Language set to " + names[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDefaultHomeSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            } catch (_: Exception) {
                Toast.makeText(this, "Open Android Settings > Apps > Default apps > Home app, then choose Atlas.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun chooseWallpaper() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(picker, 7011)
    }

    private fun openSystemSettings(action: String) {
        try { startActivity(Intent(action)) }
        catch (_: Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }

    private fun showSettings() { startActivity(Intent(this, SettingsActivity::class.java)) }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
