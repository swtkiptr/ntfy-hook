
package id.swtkiptr.ntfyhook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask

class ntfyHookService : Service() {
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "ntfy_hook_service_channel"
    private var keepAliveTimer: Timer? = null
    private lateinit var notificationSender: NotificationSender
    private lateinit var connectionStatusManager: ConnectionStatusManager

    override fun onCreate() {
        super.onCreate()
        Log.d("ntfyHookService", "Service created")
        try {
            // Initialize components first
            notificationSender = NotificationSender()
            connectionStatusManager = ConnectionStatusManager.getInstance(applicationContext)
            
            // Create notification channel immediately in onCreate
            // This helps prevent issues when startForeground is called
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Error initializing service: ${e.message}")
            e.printStackTrace() // Add stack trace for better debugging
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ntfyHookService", "Service starting")

        try {
            // Ensure notification channel exists before startForeground
            createNotificationChannel()

            // Always ensure connectionStatusManager is initialized
            if (!::connectionStatusManager.isInitialized) {
                connectionStatusManager = ConnectionStatusManager.getInstance(applicationContext)
            }
            connectionStatusManager.setServiceRunning(true)
            
            // Create and post the foreground notification (this is critical!)
            val initialNotification = createInitialNotification()
            startForeground(NOTIFICATION_ID, initialNotification)

            Handler(Looper.getMainLooper()).post {
                try {
                    val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    val url = sharedPreferences.getString("URL", "")

                    // Update notification with proper URL
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val updatedNotification = createForegroundNotification(url ?: "Not configured")
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                    
                    // Log that we've updated the notification
                    Log.d("ntfyHookService", "Updated foreground notification with URL: ${url ?: "Not configured"}")

                    // Start keep-alive mechanism
                    startKeepAliveTimer()

                    // Check server connection
                    if (!url.isNullOrEmpty()) {
                        connectionStatusManager.checkServerConnection(url)
                    }
                } catch (e: Exception) {
                    Log.e("ntfyHookService", "Error in post-start initialization: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Fatal error starting service: ${e.message}")
            e.printStackTrace()
            stopSelf() // Stop the service if we can't start it properly
        }

        return START_STICKY
    }
    
    private fun createInitialNotification(): Notification {
        try {
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ntfy Hook")
                .setContentText("Starting service...")
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Error creating initial notification: ${e.message}")
            e.printStackTrace()
            
            // Fallback notification with minimal requirements
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ntfy Hook")
                .setContentText("Service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use built-in Android icon as fallback
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build()
        }
    }
    
    private fun startKeepAliveTimer() {
        try {
            keepAliveTimer?.cancel()
            keepAliveTimer = Timer()
            keepAliveTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                        val url = sharedPreferences.getString("URL", "")
                        if (!url.isNullOrEmpty() && ::connectionStatusManager.isInitialized) {
                            connectionStatusManager.checkServerConnection(url)
                        }
                    } catch (e: Exception) {
                        Log.e("ntfyHookService", "Error in keep-alive: ${e.message}")
                    }
                }
            }, 0, 5 * 60 * 1000) // Check every 5 minutes
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Error setting up keep-alive timer: ${e.message}")
        }
    }
    
    private fun createForegroundNotification(url: String): Notification {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT 
                else PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ntfy Hook is running")
                .setContentText("ntfy : $url")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build()
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Error creating foreground notification: ${e.message}")
            e.printStackTrace()
            
            // Fallback notification with minimal requirements
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ntfy Hook")
                .setContentText("Service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use built-in Android icon as fallback
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "ntfy Hook Service"
                val descriptionText = "Keeps the notification hook service running"
                // Increase importance to IMPORTANCE_DEFAULT to ensure the notification is visible
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    // Make sure notification is visible on lock screen
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    // Enable lights/vibration if needed
                    enableLights(false)
                    enableVibration(false)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // Check if channel already exists
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel == null) {
                    notificationManager.createNotificationChannel(channel)
                    Log.d("ntfyHookService", "Notification channel created")
                } else {
                    // Update the existing channel to ensure proper settings
                    notificationManager.deleteNotificationChannel(CHANNEL_ID)
                    notificationManager.createNotificationChannel(channel)
                    Log.d("ntfyHookService", "Notification channel updated")
                }
            } catch (e: Exception) {
                Log.e("ntfyHookService", "Error creating notification channel: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    override fun onDestroy() {
        Log.d("ntfyHookService", "Service destroyed")
        try {
            keepAliveTimer?.cancel()
            keepAliveTimer = null
            if (!::connectionStatusManager.isInitialized) {
                connectionStatusManager = ConnectionStatusManager.getInstance(applicationContext)
            }
            connectionStatusManager.setServiceRunning(false)
        } catch (e: Exception) {
            Log.e("ntfyHookService", "Error destroying service: ${e.message}")
        }
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}