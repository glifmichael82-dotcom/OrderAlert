package com.driver.orderalert

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

object Bubble {
    private val h = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var timeout: Runnable? = null

    fun show(c: Context) = h.post {
        if (view != null || !Settings.canDrawOverlays(c)) return@post
        try {
            val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val d = c.resources.displayMetrics.density
            val size = (88 * d).toInt()
            val tv = TextView(c).apply {
                text = "🚗\nBUKA"
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF16A34A.toInt()) }
                setOnClickListener {
                    Alert.stop(); hide()
                    val pkg = Store.settings(c).optString("pkg", "sinet.startup.inDriver")
                    c.packageManager.getLaunchIntentForPackage(pkg)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { c.startActivity(it) }
                }
            }
            val lp = WindowManager.LayoutParams(size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.END
                x = (12 * d).toInt(); y = (200 * d).toInt()
            }
            wm.addView(tv, lp)
            view = tv
            val t = Runnable { hide() }
            timeout = t
            h.postDelayed(t, 60000)
        } catch (_: Exception) { view = null }
    }

    fun hide() { h.post {
        timeout?.let { h.removeCallbacks(it) }; timeout = null
        view?.let { v ->
            try { (v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) } catch (_: Exception) {}
        }
        view = null
    } }
}
