package id.swtkiptr.ntfyhook

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

class AppSelectionManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "ntfyHookAppSelection"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApplications.map { appInfo ->
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            AppInfo(appInfo.packageName, appName, isSystemApp)
        }.sortedBy { it.appName }
    }
    
    fun getSelectedPackages(): Set<String> {
        val json = prefs.getString(KEY_SELECTED_PACKAGES, null)
        return if (json != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptySet()
        }
    }
    
    fun saveSelectedPackages(selectedPackages: Set<String>) {
        coroutineScope.launch {
            saveSelectedPackagesAsync(selectedPackages)
        }
    }
    
    // For batch operations that need to be done synchronously
    fun saveSelectedPackagesSynchronously(selectedPackages: Set<String>) {
        val json = gson.toJson(selectedPackages)
        prefs.edit().putString(KEY_SELECTED_PACKAGES, json).apply()
    }
    
    private suspend fun saveSelectedPackagesAsync(selectedPackages: Set<String>) {
        withContext(Dispatchers.IO) {
            val json = gson.toJson(selectedPackages)
            prefs.edit().putString(KEY_SELECTED_PACKAGES, json).apply()
            
            // Notify the NotificationListenerService that selection has changed
            notifySelectionChanged()
        }
    }
    
    // Throttling mechanism to avoid frequent service restarts
    private var lastNotifyTime = 0L
    private val throttleTime = 2000L // 2 seconds
    
    private suspend fun notifySelectionChanged() {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotifyTime > throttleTime) {
                lastNotifyTime = currentTime
                
                // This is handled automatically when service is restarted/refreshed
                val isActive = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    .getBoolean("ACTIVE", false)
                
                if (isActive) {
                    // If service is active, restart it to apply changes
                    val intent = Intent(context, ntfyHookService::class.java)
                    context.stopService(intent)
                    context.startService(intent)
                }
            }
        }
    }
}