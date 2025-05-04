
package id.swtkiptr.ntfyhook

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class NotificationSender {
    
    fun sendNotification(context: Context?, urlString: String, appName: String, packageName: String, title: String, message: String): Boolean {
        if (urlString.isBlank() || message.isBlank()) {
            Log.d("NotificationSender", "Skipping send: empty URL or message")
            lastSendSuccessful = false
            return false
        }
        
        try {
            var fixedUrl = urlString
            // Make sure the URL is properly formatted
            if (!fixedUrl.startsWith("http://") && !fixedUrl.startsWith("https://")) {
                fixedUrl = "https://$fixedUrl"
            }
            
            val url = URL(fixedUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("X-Title", title)
            connection.doOutput = true
            connection.connectTimeout = 10000

            val outputStream = connection.outputStream
            outputStream.write(message.toByteArray())
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            val success = responseCode in 200..299
            
            if (success && context != null) {
                // Save to history
                saveToHistory(context, appName, packageName, title, message)
            }
            
            if (!success) {
                Log.e("NotificationSender", "Server error response: $responseCode")
            } else {
                Log.d("NotificationSender", "Notification sent successfully with response: $responseCode")
            }
            
            lastSendSuccessful = success
            lastSendTime = System.currentTimeMillis()
            
            return success
        } catch (e: Exception) {
            Log.e("NotificationSender", "Error sending notification: ${e.message}")
            lastSendSuccessful = false
            return false
        }
    }
    
    private fun saveToHistory(context: Context, appName: String, packageName: String, title: String, message: String) {
        try {
            val historyManager = NotificationHistoryManager.getInstance(context)
            historyManager.addNotification(appName, packageName, title, message)
        } catch (e: Exception) {
            Log.e("NotificationSender", "Error saving to history: ${e.message}")
        }
    }
    
    companion object {
        // Track the success of the last notification send
        private var lastSendSuccessful = false
        private var lastSendTime = 0L
        
        fun isLastSendSuccessful(): Boolean {
            // Only consider it successful if it happened within the last hour
            val oneHour = 60 * 60 * 1000
            return lastSendSuccessful && (System.currentTimeMillis() - lastSendTime < oneHour)
        }
        
        fun getAppNameFromPackage(context: Context, packageName: String): String {
            return try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
        }
    }
}