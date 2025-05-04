
package id.swtkiptr.ntfyhook

import android.app.Notification
import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class ntfyNotificationListener : NotificationListenerService() {

    private val sender = NotificationSender()
    private lateinit var appSelectionManager: AppSelectionManager
    private lateinit var connectionStatusManager: ConnectionStatusManager

    override fun onCreate() {
        super.onCreate()
        Log.d("ntfyNotificationListener", "NotificationListener created")
        try {
            appSelectionManager = AppSelectionManager(this)
            connectionStatusManager = ConnectionStatusManager.getInstance(this)
            
            // Ensure we tell the system we're connected on creation
            if (isConnected()) {
                Log.d("ntfyNotificationListener", "Listener already connected on create")
                connectionStatusManager.setListenerConnected(true)
            }
        } catch (e: Exception) {
            Log.e("ntfyNotificationListener", "Error in onCreate: ${e.message}")
        }
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("ntfyNotificationListener", "Listener connected")
        try {
            // Make sure connectionStatusManager is initialized
            if (!::connectionStatusManager.isInitialized) {
                connectionStatusManager = ConnectionStatusManager.getInstance(this)
            }
            connectionStatusManager.setListenerConnected(true)
            
            // Force a re-request for active notifications to make sure we don't miss anything
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Request active notifications to ensure we're receiving everything
                requestRebind(ComponentName(this, ntfyNotificationListener::class.java))
            }
        } catch (e: Exception) {
            Log.e("ntfyNotificationListener", "Error in onListenerConnected: ${e.message}")
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("ntfyNotificationListener", "Listener disconnected")
        connectionStatusManager.setListenerConnected(false)
        
        // Try to reconnect if service should be active
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val active = sharedPreferences.getBoolean("ACTIVE", false)
        if (active) {
            Log.d("ntfyNotificationListener", "Attempting to reconnect listener")
            requestRebind(ComponentName(this, ntfyNotificationListener::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE,"")
        val text = extras.getString(Notification.EXTRA_TEXT,"")
        
        Log.d("ntfyNotificationListener", "Notification from $packageName: Title: $title, Text: $text")

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val url = sharedPreferences.getString("URL", "")
        val active: Boolean = sharedPreferences.getBoolean("ACTIVE", false)
        
        // Check if the app is in the selected list
        val selectedApps = appSelectionManager.getSelectedPackages()
        val isAppSelected = selectedApps.contains(packageName)
        
        // Skip forwarding if URL empty or no apps are selected
        if (url.isNullOrEmpty()) {
            Log.d("ntfyNotificationListener", "URL not configured, skipping notification")
            return
        }
        
        if (selectedApps.isNotEmpty() && !isAppSelected) {
            Log.d("ntfyNotificationListener", "Skipping notification from $packageName (not in selected apps)")
            return
        }
        
        // Only forward if active and validation passes
        if (active) {
            val appName = NotificationSender.getAppNameFromPackage(this, packageName)
            Log.d("ntfyNotificationListener", "Forwarding notification from $appName ($packageName)")
            
            Thread {
                try {
                    val success = sender.sendNotification(this, url, appName, packageName, title ?: "", text ?: "")
                    if (success) {
                        Log.d("ntfyNotificationListener", "Notification sent successfully")
                    } else {
                        Log.e("ntfyNotificationListener", "Failed to send notification")
                    }
                } catch (e: Exception) {
                    Log.e("ntfyNotificationListener", "Error sending notification: ${e.message}")
                }
            }.start()
        }
    }

    // Helper method to check if the listener is connected
    private fun isConnected(): Boolean {
        return try {
            // If we can access active notifications, we're connected
            val activeNotifications = activeNotifications
            Log.d("ntfyNotificationListener", "Active notifications check: ${activeNotifications.size}")
            true
        } catch (e: Exception) {
            Log.d("ntfyNotificationListener", "Not connected: ${e.message}")
            false
        }
    }
}