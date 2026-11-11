package com.driver.orderalert

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import java.util.Locale

object Alert {
    private val h = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var ring: Ringtone? = null
    private var repeater: Runnable? = null
    private var speaker: Runnable? = null
    private var lastText = ""
    private var lastAt = 0L

    fun handle(c: Context, text: String, test: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!test && text == lastText && now - lastAt < 5000) return
        lastText = text; lastAt = now
        val s = Store.settings(c)

        val price = Regex("(?:Rp|IDR)\\s*([0-9][0-9.,]*)", RegexOption.IGNORE_CASE).find(text)
            ?.groupValues?.get(1)?.replace(Regex("[.,]"), "")?.toLongOrNull()
        val kms = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*km", RegexOption.IGNORE_CASE).findAll(text)
            .map { it.groupValues[1].replace(',', '.').toDouble() }.toList()
        var pickup = kms.getOrNull(0)
        val trip = kms.getOrNull(1)
        if (pickup == null) {
            Regex("\\b([0-9]+)\\s*m\\b").find(text)?.let { pickup = it.groupValues[1].toDouble() / 1000 }
        }
        val pk = pickup

        val ok = (price == null || price >= s.optLong("minPrice", 0)) &&
                 (pk == null || pk <= s.optDouble("maxPickup", 99.0))
        val near = pk != null && pk <= s.optDouble("nearKm", 0.5)
        val high = price != null && price >= s.optLong("highPrice", 30000)
        val tier = if (near && high) 3 else if (high) 2 else if (near) 1 else 0

        if (!test) {
            Store.addHistory(c, JSONObject().apply {
                put("t", now); put("ok", ok); put("tier", tier)
                put("price", price ?: JSONObject.NULL)
                put("pickup", pk ?: JSONObject.NULL)
                put("trip", trip ?: JSONObject.NULL)
                put("txt", text.take(160))
            })
        }
        if (!ok) return

        stop()
        fire(c, s, tier, pk, price, true)
        if (s.optBoolean("repeat", true)) schedule(c, s, tier, pk, price, 1)
        if (s.optBoolean("bubble", true)) Bubble.show(c.applicationContext)
    }

    fun stop() {
        repeater?.let { h.removeCallbacks(it) }; repeater = null
        speaker?.let { h.removeCallbacks(it) }; speaker = null
        try { ring?.stop() } catch (_: Exception) {}
        ring = null
        tts?.stop()
    }

    private fun schedule(c: Context, s: JSONObject, tier: Int, pk: Double?, price: Long?, n: Int) {
        val gap = if (tier >= 2) 3000L else 5000L
        if (n * gap > 30000L) return
        val r = Runnable { fire(c, s, tier, pk, price, false); schedule(c, s, tier, pk, price, n + 1) }
        repeater = r
        h.postDelayed(r, gap)
    }

    private fun fire(c: Context, s: JSONObject, tier: Int, pk: Double?, price: Long?, voice: Boolean) {
        val pattern = when (tier) {
            0 -> longArrayOf(0, 300)
            1 -> longArrayOf(0, 300, 150, 300)
            2 -> longArrayOf(0, 800, 200, 800)
            else -> longArrayOf(0, 300, 100, 300, 100, 300, 100, 800)
        }
        @Suppress("DEPRECATION")
        (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            .vibrate(VibrationEffect.createWaveform(pattern, -1))

        if (s.optBoolean("sound", true)) {
            val type = when (tier) {
                0 -> RingtoneManager.TYPE_NOTIFICATION
                2 -> RingtoneManager.TYPE_RINGTONE
                else -> RingtoneManager.TYPE_ALARM
            }
            try { ring?.stop() } catch (_: Exception) {}
            val r = RingtoneManager.getRingtone(c, RingtoneManager.getDefaultUri(type))
            if (r != null) {
                r.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                r.play(); ring = r
                h.postDelayed({ if (ring === r) try { r.stop() } catch (_: Exception) {} }, 2500)
            }
        }
        if (voice && s.optBoolean("voice", true)) {
            val sb = StringBuilder(when (tier) {
                1 -> "Order dekat."
                2 -> "Order tarif tinggi."
                3 -> "Order terbaik, dekat dan tarif tinggi."
                else -> "Order cocok."
            })
            pk?.let { sb.append(" Jemput ${String.format(Locale("id", "ID"), "%.1f", it)} kilometer.") }
            price?.let {
                sb.append(if (it % 1000L == 0L) " Tawaran ${it / 1000} ribu rupiah." else " Tawaran $it rupiah.")
            }
            val r = Runnable { speak(c, sb.toString()) }
            speaker = r
            h.postDelayed(r, 1500)
        }
    }

    private fun speak(c: Context, t: String) {
        if (tts == null) {
            tts = TextToSpeech(c.applicationContext) { st ->
                if (st == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("id", "ID")
                    ready = true
                    pending?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "o") }
                    pending = null
                }
            }
        }
        if (ready) tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "o") else pending = t
    }
}
