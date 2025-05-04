
package id.swtkiptr.ntfyhook

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ConnectionStatusManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "ntfyHookConnectionStatus"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_SERVER_CONNECTED = "server_connected"
        private const val KEY_LISTENER_CONNECTED = "listener_connected"
        private const val KEY_LAST_CONNECTION_TIME = "last_connection_time"
        
        // Singleton instance to observe from anywhere in the app
        @Volatile
        private var INSTANCE: ConnectionStatusManager? = null
        
        fun getInstance(context: Context): ConnectionStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionStatusManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun isServiceRunning(): Boolean {
        // Check ACTIVE flag from shared preferences as the source of truth
        val mainPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isActive = mainPrefs.getBoolean("ACTIVE", false)
        
        // If the main ACTIVE flag is true but our service status is false,
        // update our service status to match
        if (isActive && !prefs.getBoolean(KEY_SERVICE_RUNNING, false)) {
            setServiceRunning(true)
            return true
        }
        // If the main ACTIVE flag is false but our service status is true,
        // update our service status to match
        else if (!isActive && prefs.getBoolean(KEY_SERVICE_RUNNING, false)) {
            setServiceRunning(false)
            return false
        }
        
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false)
    }
    
    fun setServiceRunning(running: Boolean) {
        Log.d("ConnectionStatusManager", "Setting service running state to: $running")
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply()
        if (!running) {
            // Reset connection states when service stops
            prefs.edit()
                .putBoolean(KEY_SERVER_CONNECTED, false)
                .putBoolean(KEY_LISTENER_CONNECTED, false)
                .apply()
        }
    }
    
    fun isServerConnected(): Boolean {
        return prefs.getBoolean(KEY_SERVER_CONNECTED, false)
    }
    
    fun setServerConnected(connected: Boolean) {
        Log.d("ConnectionStatusManager", "Setting server connected state to: $connected")
        prefs.edit().putBoolean(KEY_SERVER_CONNECTED, connected).apply()
        if (connected) {
            prefs.edit().putLong(KEY_LAST_CONNECTION_TIME, System.currentTimeMillis()).apply()
        }
    }
    
    // Update isListenerConnected to check if the notification service is actually enabled
    fun isListenerConnected(): Boolean {
        // If the app's notification listener service is enabled at the system level,
        // we can consider it connected regardless of what our saved preference says
        val notificationManager = NotificationManagerCompat.getEnabledListenerPackages(context)
        val isEnabledInSystem = notificationManager.contains(context.packageName)
        
        // If the system says it's enabled, update our preference and return true
        if (isEnabledInSystem) {
            prefs.edit().putBoolean(KEY_LISTENER_CONNECTED, true).apply()
            return true
        }
        
        // Otherwise, return whatever was saved in preferences
        return prefs.getBoolean(KEY_LISTENER_CONNECTED, false)
    }
    
    fun setListenerConnected(connected: Boolean) {
        Log.d("ConnectionStatusManager", "Setting listener connected state to: $connected")
        prefs.edit().putBoolean(KEY_LISTENER_CONNECTED, connected).apply()
    }
    
    fun getLastConnectionTime(): Long {
        return prefs.getLong(KEY_LAST_CONNECTION_TIME, 0)
    }
    
    fun checkServerConnection(serverUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var url = serverUrl
                // Make sure the URL is properly formatted
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                
                // If we can send notifications, consider the server connected
                if (NotificationSender.isLastSendSuccessful()) {
                    Log.d("ConnectionStatusManager", "Server considered connected based on successful notification send")
                    setServerConnected(true)
                    return@launch
                }
                
                // Otherwise check connection directly
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.connect()
                
                val responseCode = connection.responseCode
                // Consider any response as success - ntfy might return various codes
                val isConnected = responseCode > 0
                
                setServerConnected(isConnected)
                Log.d("ConnectionStatusManager", "Server connection check: $isConnected (code: $responseCode)")
            } catch (e: IOException) {
                Log.e("ConnectionStatusManager", "Connection error: ${e.message}")
                // If we can't connect but last notification send was successful,
                // still consider server connected
                if (NotificationSender.isLastSendSuccessful()) {
                    setServerConnected(true)
                } else {
                    setServerConnected(false)
                }
            }
        }
    }
    
    fun getConnectionStatusText(): String {
        val serviceRunning = isServiceRunning()
        val serverConnected = isServerConnected()
        
        // First, check if we're running at all
        if (!serviceRunning) {
            return "Service not running"
        }
        
        // Then, check server connection - this is more important
        if (!serverConnected) {
            return "Service running, server disconnected"
        }
        
        // If the service is running and the server is connected, just show connected
        // (We'll ignore listener connected status since notifications are working)
        return "Connected to server"
    }
    
    fun resetStatus() {
        Log.d("ConnectionStatusManager", "Resetting all connection status values")
        prefs.edit()
            .putBoolean(KEY_SERVICE_RUNNING, false)
            .putBoolean(KEY_SERVER_CONNECTED, false)
            .putBoolean(KEY_LISTENER_CONNECTED, false)
            .apply()
    }
}