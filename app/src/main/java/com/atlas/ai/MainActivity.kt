package com.atlas.ai

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.os.Bundle
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
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(16, 17, 26)
    private val panel = Color.rgb(27, 29, 43)
    private val accent = Color.rgb(139, 124, 255)
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private val executor = Executors.newSingleThreadExecutor()
    private val localHistory = JSONArray()
    private val history = JSONArray() // retained for the optional Gemini request helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()
        addMessage("Atlas", "Hey! I'm Atlas AI. Start the local model server, then chat with me offline. Server: http://127.0.0.1:11434")
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(TextView(this).apply {
            text = "ATLAS AI"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        })
        brand.addView(TextView(this).apply {
            text = "Your personal AI assistant"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        })
        header.addView(brand)
        header.addView(Button(this).apply {
            text = "Settings"
            setTextColor(Color.WHITE)
            background = rounded(accent, 18)
            setOnClickListener { showSettings() }
        })
        root.addView(header)
        root.addView(View(this).apply { setBackgroundColor(Color.rgb(48, 49, 66)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(8) })

        scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(12)) }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(6), dp(6))
            background = rounded(panel, 24)
        }
        input = EditText(this).apply {
            hint = "Message Atlas..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 4
            setPadding(dp(10), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(Button(this).apply {
            text = "Voice"
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(67, 72, 100), 20)
            setOnClickListener { startVoiceInput() }
        })
        sendButton = Button(this).apply {
            text = "Send"
            setTextColor(Color.WHITE)
            background = rounded(accent, 20)
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton)
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun addMessage(who: String, body: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(if (who == "You") Color.rgb(48, 43, 82) else panel, 18)
        }
        card.addView(TextView(this).apply {
            text = who
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (who == "You") Color.rgb(190, 181, 255) else accent)
        })
        card.addView(TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, dp(4), 0, 0)
        })
        messages.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(6) })
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Atlas")
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 7001)
        } catch (_: Exception) {
            Toast.makeText(this, "Voice input is not available on this phone", Toast.LENGTH_LONG).show()
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
        }
    }

    private fun runOfflineCommand(prompt: String): Boolean {
        val command = prompt.trim().lowercase()
        val target = when {
            command.matches(Regex("(open|launch|start)\\s+youtube")) -> "https://www.youtube.com"
            command.matches(Regex("(open|launch|start)\\s+google")) -> "https://www.google.com"
            command.matches(Regex("(open|launch|start)\\s+chrome")) -> "https://www.google.com"
            command.matches(Regex("(open|launch|start)\\s+whatsapp")) -> "https://wa.me/"
            command.matches(Regex("(open|launch|start)\\s+maps")) -> "https://maps.google.com"
            else -> null
        } ?: return false
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
            addMessage("You", prompt)
            addMessage("Atlas", "Opening " + target.substringAfter("://").substringBefore("/"))
            true
        } catch (_: Exception) {
            addMessage("Atlas", "I couldn't open that app or website.")
            true
        }
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        if (runOfflineCommand(prompt)) return
        addMessage("You", prompt)
        localHistory.put(JSONObject().put("role", "user").put("content", prompt))
        sendButton.isEnabled = false
        sendButton.text = "..."
        executor.execute {
            val answer = requestLocal()
            runOnUiThread {
                sendButton.isEnabled = true
                sendButton.text = "Send"
                if (answer.startsWith("ERROR:")) {
                    addMessage("Atlas", answer.removePrefix("ERROR:"))
                } else {
                    addMessage("Atlas", answer)
                    localHistory.put(JSONObject().put("role", "assistant").put("content", answer))
                }
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
            if (modelCode !in 200..299) return "ERROR:Local model server returned HTTP $modelCode. Make sure the server is running."
            val models = JSONObject(modelBody).optJSONArray("data")
            val model = models?.optJSONObject(0)?.optString("id").orEmpty()
            if (model.isBlank()) return "ERROR:No loaded model found. Load your Qwen model and keep the local server running."
            connection.disconnect()
            connection = (URL(base + "/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 120000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().put("model", model).put("messages", localHistory).put("stream", false).put("max_tokens", 512)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                "ERROR:Local AI error (HTTP $code): " + response.take(500)
            } else {
                JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: "I could not read the local model response. Please try again."
            }
        } catch (e: Exception) {
            "ERROR:Cannot reach the local AI server. Open LLM AI Server, start API/WebUI, and keep it running. Details: " + (e.message ?: "connection failed")
        } finally {
            connection?.disconnect()
        }
    }
    private fun requestGemini(key: String): String {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + key)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val payload = JSONObject().put("contents", history)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val detail = try { JSONObject(response).optJSONObject("error")?.optString("message") ?: response } catch (_: Exception) { response }
                "ERROR:" + detail
            } else {
                val json = JSONObject(response)
                json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    ?: "I couldn't read the response. Please try again."
            }
        } catch (e: Exception) {
            "ERROR:" + (e.message ?: "Connection failed. Check your internet and API key.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun showSettings() {
        val field = EditText(this).apply {
            hint = "Paste Gemini API key"
            setSingleLine(true)
            inputType = 0x00000081
            setText(getSharedPreferences("atlas", MODE_PRIVATE).getString("gemini_key", ""))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(TextView(this@MainActivity).apply {
                text = "Get a free key from Google AI Studio. The key is saved only on this phone; never share it or commit it to GitHub."
                textSize = 13f
                setTextColor(Color.DKGRAY)
            })
            addView(field)
        }
        AlertDialog.Builder(this)
            .setTitle("Atlas AI Settings")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                getSharedPreferences("atlas", MODE_PRIVATE).edit().putString("gemini_key", field.text.toString().trim()).apply()
                Toast.makeText(this, "API key saved on this device", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear key") { _, _ ->
                getSharedPreferences("atlas", MODE_PRIVATE).edit().remove("gemini_key").apply()
            }
            .show()
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
