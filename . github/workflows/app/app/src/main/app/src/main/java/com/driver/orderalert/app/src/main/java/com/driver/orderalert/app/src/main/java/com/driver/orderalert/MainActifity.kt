package com.driver.orderalert

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }

    override fun onResume() {
        super.onResume()
        try { NotificationListenerService.requestRebind(ComponentName(this, OrderListener::class.java)) } catch (_: Exception) {}
    }

    override fun onBackPressed() { moveTaskToBack(true) }

    inner class Bridge {
        @JavascriptInterface fun state(): String {
            val on = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                ?.contains(packageName) == true
            return JSONObject().put("settings", Store.settings(this@MainActivity))
                .put("history", Store.history(this@MainActivity)).put("listener", on).put("overlay", Settings.canDrawOverlays(this@MainActivity)).toString()
        }
        @JavascriptInterface fun save(json: String) = Store.saveSettings(this@MainActivity, json)
        @JavascriptInterface fun clear() = Store.clear(this@MainActivity)
        @JavascriptInterface fun test() = runOnUiThread { Alert.handle(this@MainActivity,
            "Budi menawar Rp 35.000 · Jemput 0,4 km · Tujuan 4,2 km", true) }
        @JavascriptInterface fun stopAlarm() = runOnUiThread { Alert.stop(); Bubble.hide() }
        @JavascriptInterface fun overlay() {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        @JavascriptInterface fun openAccess() {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        @JavascriptInterface fun battery() {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        @JavascriptInterface fun openIndrive() {
            val pkg = Store.settings(this@MainActivity).optString("pkg", "sinet.startup.inDriver")
            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
        }
    }
}
