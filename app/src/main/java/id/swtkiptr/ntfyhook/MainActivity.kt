package id.swtkiptr.ntfyhook

import android.Manifest
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var connectionStatusManager: ConnectionStatusManager
    private lateinit var appSelectionManager: AppSelectionManager
    
    // UI components
    private lateinit var statusText: TextView
    private lateinit var serverUrlText: TextView
    private lateinit var selectedAppsCountText: TextView
    private lateinit var switchButton: SwitchMaterial
    private lateinit var statusIndicator: View
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var urlInput: TextInputEditText
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStatusDisplay()
            handler.postDelayed(this, 5000) // Update every 5 seconds
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with service start if active
            if (sharedPreferences.getBoolean("ACTIVE", false)) {
                startntfyHookService()
            }
        } else {
            // Permission denied, inform user and reset switch
            Toast.makeText(this, 
                "Notification permission is required for this app to work properly", 
                Toast.LENGTH_LONG).show()
            switchButton.isChecked = false
            val editor = sharedPreferences.edit()
            editor.putBoolean("ACTIVE", false)
            editor.apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Initialize components
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        connectionStatusManager = ConnectionStatusManager.getInstance(this)
        appSelectionManager = AppSelectionManager(this)
        
        // Initialize UI components
        statusText = findViewById(R.id.status_text)
        serverUrlText = findViewById(R.id.server_url_text)
        selectedAppsCountText = findViewById(R.id.selected_apps_count)
        switchButton = findViewById(R.id.switch1)
        statusIndicator = findViewById(R.id.status_indicator)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Check if notification listener is enabled
        val enabled = isNotificationListenerEnabled(this)
        if (!enabled) {
            Toast.makeText(this, 
                "Please enable notification access for this app", 
                Toast.LENGTH_LONG).show()
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } else {
            // If enabled but showing as disconnected, try to rebind
            forceRebindNotificationListener()
        }

        // Set up URL input
        setupUrlInput()
        
        // Set up switch button with validation
        setupSwitchButton()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
        updateSelectedAppsCount()
        
        // Start periodic updates
        handler.post(updateRunnable)
        
        // Check if active state matches service state
        validateServiceState()
        
        // Set the selected navigation item to Home
        bottomNavigation.selectedItemId = R.id.nav_home
    }
    
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
    
    private fun setupUrlInput() {
        val url = sharedPreferences.getString("URL", "")
        urlInput = findViewById<TextInputEditText>(R.id.url_input)
        urlInput.setText(url)
        
        // Set initial URL input state based on service status
        val active = sharedPreferences.getBoolean("ACTIVE", false)
        urlInput.isEnabled = !active
        
        // Set the correct visual state for URL input layout based on active state
        val urlInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.url_input_layout)
        if (active) {
            urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
            urlInputLayout.hint = "ntfy URL (locked while service is active)"
        } else {
            urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
            urlInputLayout.hint = "Enter ntfy URL"
        }
        
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val newUrl = urlInput.text.toString().trim()
                val editor = sharedPreferences.edit()
                editor.putString("URL", newUrl)
                editor.apply()
                
                serverUrlText.text = "URL: $newUrl"
                
                // Validate service state if URL changed
                validateServiceState()
                
                Toast.makeText(this, "URL Updated", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }
    
    private fun setupSwitchButton() {
        val active = sharedPreferences.getBoolean("ACTIVE", false)
        switchButton.isChecked = active
        
        switchButton.setOnCheckedChangeListener { _, isChecked ->
            // Validate before enabling
            if (isChecked) {
                val url = sharedPreferences.getString("URL", "")
                val selectedApps = appSelectionManager.getSelectedPackages()
                
                if (url.isNullOrEmpty()) {
                    Toast.makeText(this, "Please enter a ntfy server URL first", Toast.LENGTH_SHORT).show()
                    switchButton.isChecked = false
                    return@setOnCheckedChangeListener
                }
                
                if (selectedApps.isEmpty()) {
                    Toast.makeText(this, "Please select at least one app first", Toast.LENGTH_SHORT).show()
                    switchButton.isChecked = false
                    return@setOnCheckedChangeListener
                }
                
                // Check if notification listener service is enabled
                if (!isNotificationListenerEnabled(this)) {
                    Toast.makeText(this, "Please enable notification access for this app first", Toast.LENGTH_LONG).show()
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivity(intent)
                    switchButton.isChecked = false
                    return@setOnCheckedChangeListener
                }
                
                // Lock URL input when service is active
                urlInput.isEnabled = false
                // Show visual feedback that URL is locked
                val urlInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.url_input_layout)
                urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
                urlInputLayout.hint = "ntfy URL (locked while service is active)"
                Toast.makeText(this, "URL locked while service is active", Toast.LENGTH_SHORT).show()
            } else {
                // Unlock URL input when service is inactive
                urlInput.isEnabled = true
                // Reset visual feedback
                val urlInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.url_input_layout)
                urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
                urlInputLayout.hint = "Enter ntfy URL"
            }
            
            // If validation passes or we're turning off, save the state
            val editor = sharedPreferences.edit()
            editor.putBoolean("ACTIVE", isChecked)
            editor.apply()
            
            // Start or stop the service
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                stopntfyHookService()
                updateStatusDisplay() // <-- update status after stopping
            }
            updateStatusDisplay() // <-- always update status after toggle
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on Home screen
                    true
                }
                R.id.nav_apps -> {
                    val intent = Intent(this, AppSelectionActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_right, R.anim.slide_out_left))
                    false
                }
                R.id.nav_history -> {
                    val intent = Intent(this, NotificationHistoryActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_right, R.anim.slide_out_left))
                    false
                }
                R.id.nav_credits -> {
                    val intent = Intent(this, CreditsActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_right, R.anim.slide_out_left))
                    false
                }
                else -> false
            }
        }
    }

    /**
     * Helper function to create animation bundle for activity transitions
     */
    private fun createCustomAnimationBundle(enterAnim: Int, exitAnim: Int): Bundle {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Use the modern API for Android 14+
            ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
        } else {
            // Use the compatibility approach for older versions
            ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
        }
    }
    
    private fun updateStatusDisplay() {
        val isRunning = connectionStatusManager.isServiceRunning()
        val isConnected = connectionStatusManager.isServerConnected()
        val url = sharedPreferences.getString("URL", "Not configured")
        
        // Update status display
        val statusMsg = connectionStatusManager.getConnectionStatusText()
        statusText.text = statusMsg
        
        // Update status indicator color based on state
        statusIndicator.backgroundTintList = when {
            !isRunning -> ContextCompat.getColorStateList(this, R.color.status_error)
            !isConnected -> ContextCompat.getColorStateList(this, R.color.status_warning)
            else -> ContextCompat.getColorStateList(this, R.color.status_success)
        }
        
        // Set status text color based on state
        statusText.setTextColor(
            when {
                !isRunning -> ContextCompat.getColor(this, R.color.status_error)
                !isConnected -> ContextCompat.getColor(this, R.color.status_warning)
                else -> ContextCompat.getColor(this, R.color.status_success)
            }
        )
        
        // Update URL text
        serverUrlText.text = "URL: $url"
    }
    
    private fun updateSelectedAppsCount() {
        val selectedApps = appSelectionManager.getSelectedPackages()
        selectedAppsCountText.text = "Selected apps: ${selectedApps.size}"
    }
    
    private fun validateServiceState() {
        val url = sharedPreferences.getString("URL", "")
        val selectedApps = appSelectionManager.getSelectedPackages()
        val active = sharedPreferences.getBoolean("ACTIVE", false)
        
        if (active && (url.isNullOrEmpty() || selectedApps.isEmpty())) {
            // Auto-disable if requirements not met
            val editor = sharedPreferences.edit()
            editor.putBoolean("ACTIVE", false)
            editor.apply()
            
            switchButton.isChecked = false
            stopntfyHookService()
            
            Toast.makeText(this,
                "Service disabled: " + 
                if (url.isNullOrEmpty()) "URL not configured" 
                else "No apps selected", 
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return listeners.contains(packageName)
    }
    
    // Implementation of missing methods
    
    private fun startntfyHookService() {
        // Start the notification listener service
        val intent = Intent(this, ntfyHookService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d("MainActivity", "Starting ntfyHookService")
    }
    
    private fun stopntfyHookService() {
        // Stop the notification listener service
        val intent = Intent(this, ntfyHookService::class.java)
        stopService(intent)
        Log.d("MainActivity", "Stopping ntfyHookService")
    }
    
    private fun forceRebindNotificationListener() {
        // Toggle notification listener service binding to force a reconnection
        val componentName = ComponentName(this, ntfyHookService::class.java)
        
        // Disable then re-enable to force a rebind
        toggleNotificationListenerService(false, componentName)
        toggleNotificationListenerService(true, componentName)
        
        Log.d("MainActivity", "Forced rebind of notification listener")
    }
    
    private fun toggleNotificationListenerService(enable: Boolean, componentName: ComponentName) {
        val pm = packageManager
        pm.setComponentEnabledSetting(
            componentName,
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
    
    private fun checkAndRequestNotificationPermission() {
        // For Android 13+, check and request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startntfyHookService()
            } else {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For older Android versions, no runtime permission needed
            startntfyHookService()
        }
    }
}