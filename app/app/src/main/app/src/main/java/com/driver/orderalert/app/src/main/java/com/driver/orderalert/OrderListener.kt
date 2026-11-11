package com.driver.orderalert

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class OrderListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val s = Store.settings(this)
        if (!s.optBoolean("active", true)) return
        if (sbn.packageName != s.optString("pkg", "sinet.startup.inDriver")) return
        val e = sbn.notification.extras
        val text = listOf("android.title", "android.text", "android.bigText")
            .mapNotNull { e.getCharSequence(it)?.toString() }
            .distinct().joinToString(" · ")
        if (text.isBlank()) return
        Alert.handle(this, text)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == Store.settings(this).optString("pkg", "sinet.startup.inDriver")) {
            Alert.stop(); Bubble.hide()
        }
    }
}
