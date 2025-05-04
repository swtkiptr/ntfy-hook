
package id.swtkiptr.ntfyhook

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationRecord(
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

class NotificationHistoryManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "ntfyHookHistory"
        private const val KEY_NOTIFICATIONS = "notifications_history"
        private const val MAX_HISTORY_SIZE = 100
        
        @Volatile
        private var INSTANCE: NotificationHistoryManager? = null
        
        fun getInstance(context: Context): NotificationHistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationHistoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun addNotification(appName: String, packageName: String, title: String, text: String) {
        val record = NotificationRecord(appName, packageName, title, text)
        
        val notifications = getNotifications().toMutableList()
        notifications.add(0, record) // Add to the beginning of the list
        
        // Trim list if it gets too large
        if (notifications.size > MAX_HISTORY_SIZE) {
            notifications.subList(MAX_HISTORY_SIZE, notifications.size).clear()
        }
        
        saveNotifications(notifications)
    }
    
    fun getNotifications(): List<NotificationRecord> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationRecord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveNotifications(notifications: List<NotificationRecord>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }
    
    fun clearHistory() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }
}