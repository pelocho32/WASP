package com.example.hourlyreminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "hourly_reminder_prefs"
private const val KEY_REMINDERS = "reminders"

data class Reminder(
    val id: String,
    val label: String,
    val enabled: Boolean
)

class ReminderRepository(private val context: Context) {
    fun loadReminders(): List<Reminder> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            Reminder(
                id = obj.getString("id"),
                label = obj.getString("label"),
                enabled = obj.optBoolean("enabled", true)
            )
        }
    }

    fun saveReminders(reminders: List<Reminder>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            val obj = JSONObject()
            obj.put("id", reminder.id)
            obj.put("label", reminder.label)
            obj.put("enabled", reminder.enabled)
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REMINDERS, array.toString())
            .apply()
    }
}
