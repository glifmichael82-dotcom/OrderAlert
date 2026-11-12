package com.driver.orderalert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Store {
    private const val DEF = """{"active":true,"sound":true,"voice":true,"repeat":true,"bubble":true,"nearKm":0.5,"highPrice":30000,"maxPickup":1.5,"minPrice":10000,
"pkg":"sinet.startup.inDriver","zones":[{"name":"Umum","maxPickup":1.5,"minPrice":10000}]}"""
    private fun p(c: Context) = c.getSharedPreferences("oa", 0)
    fun settings(c: Context) = JSONObject(p(c).getString("s", null) ?: DEF)
    fun saveSettings(c: Context, json: String) { p(c).edit().putString("s", json).apply() }
    fun history(c: Context) = JSONArray(p(c).getString("h", "[]"))
    fun addHistory(c: Context, o: JSONObject) {
        val old = history(c)
        val n = JSONArray()
        val start = maxOf(0, old.length() - 299)
        for (i in start until old.length()) n.put(old.get(i))
        n.put(o)
        p(c).edit().putString("h", n.toString()).apply()
    }
    fun clear(c: Context) { p(c).edit().putString("h", "[]").apply() }
}
