package com.atlas.ai

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

data class AtlasDeviceProfile(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cores: Int,
    val abis: String,
    val screen: String,
    val freeStorageGb: Double,
    val tier: String,
    val recommendedTokens: Int
)

object AtlasDeviceScanner {
    fun scan(activity: Activity): AtlasDeviceProfile {
        val manager = activity.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        val total = memory.totalMem / (1024L * 1024L)
        val available = memory.availMem / (1024L * 1024L)
        val lowRam = manager.isLowRamDevice
        val tier = when {
            lowRam || total < 1800L -> "LITE"
            total < 3500L -> "BALANCED"
            else -> "FULL"
        }
        val tokens = when (tier) { "LITE" -> 96; "BALANCED" -> 160; else -> 256 }
        val dm = activity.resources.displayMetrics
        val storage = StatFs(Environment.getDataDirectory().path)
        val freeGb = storage.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return AtlasDeviceProfile(
            Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty(),
            Build.VERSION.RELEASE.orEmpty(), Build.VERSION.SDK_INT,
            total, available, Runtime.getRuntime().availableProcessors(),
            Build.SUPPORTED_ABIS.joinToString(", "),
            "${dm.widthPixels} × ${dm.heightPixels} px",
            freeGb, tier, tokens
        )
    }

    fun save(activity: Activity, profile: AtlasDeviceProfile) {
        activity.getSharedPreferences("atlas", Activity.MODE_PRIVATE).edit()
            .putString("device_profile", profile.tier)
            .putInt("recommended_tokens", profile.recommendedTokens)
            .putLong("device_ram_mb", profile.totalRamMb)
            .apply()
    }
}

class DeviceScanActivity : Activity() {
    private val bg = Color.rgb(12, 14, 24)
    private val panel = Color.rgb(25, 28, 43)
    private val accent = Color.rgb(139, 124, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val profile = AtlasDeviceScanner.scan(this)
        AtlasDeviceScanner.save(this, profile)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(16))
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "DEVICE SCAN"
            textSize = 12f
            letterSpacing = .16f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Atlas adapts to your device"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(5))
        })
        root.addView(TextView(this).apply {
            text = "A quick, on-device check. No account or special permission needed."
            textSize = 13f
            setTextColor(Color.LTGRAY)
        })
        val badge = TextView(this).apply {
            text = "  ${profile.tier} PROFILE ACTIVE  "
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(accent, 18)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(badge, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(20); bottomMargin = dp(12) })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun row(label: String, value: String) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(11), dp(14), dp(11))
                background = rounded(panel, 15)
            }
            card.addView(TextView(this).apply { text = label.uppercase(Locale.ROOT); textSize = 10f; setTextColor(Color.rgb(151,155,180)); letterSpacing = .08f })
            card.addView(TextView(this).apply { text = value; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, dp(4), 0, 0) })
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        row("Device", "${profile.manufacturer} ${profile.model}")
        row("Android", "Android ${profile.androidVersion} (API ${profile.sdk})")
        row("RAM", "${profile.totalRamMb} MB total • ${profile.availableRamMb} MB currently free")
        row("Processor", "${profile.cores} CPU cores • ${profile.abis}")
        row("Screen", profile.screen)
        row("Free storage", String.format(Locale.US, "%.2f GB", profile.freeStorageGb))
        row("Atlas mode", when(profile.tier) {
            "LITE" -> "Lite: reduced memory use and shorter AI replies"
            "BALANCED" -> "Balanced: moderate effects and AI reply length"
            else -> "Full: complete available interface effects"
        })
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "DONE"
            textSize = 14f
            setTextColor(Color.WHITE)
            background = rounded(accent, 18)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
