package com.atlas.ai

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.speech.*
import java.util.Locale

class VoiceWakeService : Service() {
    companion object {
        const val ACTION_START = "com.atlas.ai.START_WAKE"
        const val ACTION_STOP = "com.atlas.ai.STOP_WAKE"
        private const val CHANNEL = "atlas_voice"
        private const val ID = 4102
    }
    private val handler = Handler(Looper.getMainLooper())
    private var speech: SpeechRecognizer? = null
    private var listening = false
    private var restarting = false
    private var stopped = false

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        foreground()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            if (speech == null) createRecognizer()
            listen()
        } else notifyStatus("Speech recognition unavailable")
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val c = NotificationChannel(CHANNEL, "Atlas voice assistant", NotificationManager.IMPORTANCE_LOW)
            c.description = "Persistent notice while Atlas listens for its wake word"
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }

    private fun makeNotification(message: String): Notification {
        val stop = Intent(this, VoiceWakeService::class.java).setAction(ACTION_STOP)
        val pi = PendingIntent.getService(this, ID, stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Atlas voice assistant")
            .setContentText(message).setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", pi).build()
    }

    private fun foreground() {
        val n = makeNotification("Listening for “Atlas”")
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(ID, n)
    }
    private fun notifyStatus(s: String) { getSystemService(NotificationManager::class.java).notify(ID, makeNotification(s)) }

    private fun createRecognizer() {
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { listening = true; restarting = false; notifyStatus("Listening for “Atlas”") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(e: Int) { listening = false; restart(1200) }
                override fun onResults(b: Bundle?) {
                    listening = false
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { hear(it) }
                    restart(700)
                }
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
        }
    }

    private fun listen() {
        if (listening || restarting || speech == null || stopped) return
        try {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speech?.startListening(i); listening = true
        } catch (_: Exception) { restart(1800) }
    }

    private fun restart(ms: Long) {
        if (restarting || stopped) return
        restarting = true
        handler.postDelayed({ restarting = false; listen() }, ms)
    }

    private fun hear(text: String) {
        val m = Regex("\\batlas\\b", RegexOption.IGNORE_CASE).find(text) ?: return
        val cmd = text.substring(m.range.last + 1).trim().trimStart(',', '.', '!', ':')
        if (cmd.isBlank()) { notifyStatus("Say a command after Atlas"); return }
        command(cmd)
    }

    private fun command(cmd: String) {
        val s = cmd.lowercase(Locale.ROOT)
        try {
            val yt = Regex("^(?:open )?youtube(?: and)? (?:play|search for) (.+)$", RegexOption.IGNORE_CASE).find(cmd)
            if (yt != null) {
                val url = "https://www.youtube.com/results?search_query=" + Uri.encode(yt.groupValues[1].trim())
                launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                notifyStatus("YouTube results opened. Choose a video to play.")
                return
            }
            when {
                s.matches(Regex("(open|launch|start)\\s+(youtube|google|chrome|whatsapp|maps|camera|settings|phone|dialer)")) -> {
                    val app = s.substringAfterLast(" ")
                    when (app) {
                        "settings" -> launch(Intent(android.provider.Settings.ACTION_SETTINGS))
                        "camera" -> launch(Intent("android.media.action.IMAGE_CAPTURE"))
                        "phone", "dialer" -> launch(Intent(Intent.ACTION_DIAL))
                        "youtube" -> openApp("com.google.android.youtube", "https://www.youtube.com")
                        "google" -> openApp("com.google.android.googlequicksearchbox", "https://www.google.com")
                        "chrome" -> openApp("com.android.chrome", "https://www.google.com")
                        "whatsapp" -> openApp("com.whatsapp", "https://wa.me/")
                        "maps" -> openApp("com.google.android.apps.maps", "https://maps.google.com")
                    }
                    notifyStatus("Opening $app")
                }
                s == "volume up" || s == "increase volume" ->
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                s == "volume down" || s == "decrease volume" ->
                    (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                s.startsWith("search ") -> {
                    launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(cmd.substringAfter(" ").trim()))))
                    notifyStatus("Google search opened")
                }
                else -> notifyStatus("Heard: $cmd")
            }
        } catch (_: Exception) { notifyStatus("Couldn't perform that command") }
    }

    private fun openApp(pkg: String, fallback: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        launch(i ?: Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
    }
    private fun launch(i: Intent) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        try { speech?.cancel(); speech?.destroy() } catch (_: Exception) {}
        speech = null
        getSharedPreferences("atlas", MODE_PRIVATE).edit().putBoolean("wake_service_running", false).apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}