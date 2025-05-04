package id.swtkiptr.ntfyhook

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHistoryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var historyManager: NotificationHistoryManager
    private lateinit var adapter: NotificationHistoryAdapter
    private lateinit var emptyStateContainer: View
    private lateinit var notificationCountText: TextView
    private lateinit var notificationTimespanText: TextView
    private lateinit var chipAll: Chip
    private lateinit var chipToday: Chip
    private lateinit var chipByApp: Chip
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Keep original list for filtering
    private var allNotifications = listOf<NotificationRecord>()
    private var filteredNotifications = listOf<NotificationRecord>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notification History"
        
        // Initialize components
        historyManager = NotificationHistoryManager.getInstance(this)
        recyclerView = findViewById(R.id.notification_history_recycler_view)
        emptyStateContainer = findViewById(R.id.empty_state_container)
        notificationCountText = findViewById(R.id.notification_count)
        notificationTimespanText = findViewById(R.id.notification_timespan)
        chipAll = findViewById(R.id.chip_all)
        chipToday = findViewById(R.id.chip_today)
        chipByApp = findViewById(R.id.chip_by_app)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Set up filter chips
        setupFilterChips()
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Load data
        loadNotificationHistory()
        
        // Handle back button press with modern approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        })
    }
    
    override fun onResume() {
        super.onResume()
        
        // Set the selected navigation item to History
        bottomNavigation.selectedItemId = R.id.nav_history
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_left, R.anim.slide_out_right))
                    finish() // Close this activity
                    true
                }
                R.id.nav_apps -> {
                    // Navigate to AppSelectionActivity
                    val intent = Intent(this, AppSelectionActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_left, R.anim.slide_out_right))
                    finish() // Close this activity
                    true
                }
                R.id.nav_history -> {
                    // Already on History screen
                    true
                }
                R.id.nav_credits -> {
                    // Navigate to CreditsActivity
                    val intent = Intent(this, CreditsActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_right, R.anim.slide_out_left))
                    finish() // Close this activity
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Helper function to create animation bundle for activity transitions
     */
    private fun createCustomAnimationBundle(enterAnim: Int, exitAnim: Int): Bundle {
        return ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
    }
    
    private fun navigateBack() {
        finish()
        // Apply custom animation when finishing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    
    private fun setupFilterChips() {
        chipAll.setOnClickListener {
            filteredNotifications = allNotifications
            updateNotificationList()
        }
        
        chipToday.setOnClickListener {
            // Filter for today's notifications
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            filteredNotifications = allNotifications.filter { it.timestamp >= startOfDay }
            updateNotificationList()
        }
        
        chipByApp.setOnClickListener {
            // Group by app by sorting
            filteredNotifications = allNotifications.sortedBy { it.appName }
            updateNotificationList()
        }
    }
    
    private fun loadNotificationHistory() {
        allNotifications = historyManager.getNotifications()
        filteredNotifications = allNotifications
        
        updateStatistics()
        updateNotificationList()
    }
    
    private fun updateStatistics() {
        val count = allNotifications.size
        notificationCountText.text = "$count notifications hook"
        
        if (count > 0) {
            // Calculate timespan
            val oldestTimestamp = allNotifications.minByOrNull { it.timestamp }?.timestamp ?: 0L
            val newestTimestamp = allNotifications.maxByOrNull { it.timestamp }?.timestamp ?: 0L
            
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val oldestDate = formatter.format(Date(oldestTimestamp))
            val newestDate = formatter.format(Date(newestTimestamp))
            
            notificationTimespanText.text = if (oldestDate == newestDate) {
                "All from $oldestDate"
            } else {
                "From $oldestDate to $newestDate"
            }
        } else {
            notificationTimespanText.text = "No history available"
        }
    }
    
    private fun updateNotificationList() {
        adapter = NotificationHistoryAdapter(filteredNotifications)
        recyclerView.adapter = adapter
        
        // Show empty view if no notifications
        if (filteredNotifications.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigateBack()
                true
            }
            R.id.action_clear_history -> {
                historyManager.clearHistory()
                loadNotificationHistory()
                Snackbar.make(recyclerView, "Notification history cleared", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private inner class NotificationHistoryAdapter(private val notifications: List<NotificationRecord>) : 
            RecyclerView.Adapter<NotificationHistoryAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appNameText: TextView = view.findViewById(R.id.app_name_text)
            val titleText: TextView = view.findViewById(R.id.notification_title_text)
            val contentText: TextView = view.findViewById(R.id.notification_content_text)
            val timeText: TextView = view.findViewById(R.id.notification_time_text)
            val expandButton: MaterialButton = view.findViewById(R.id.expand_button)
            
            // Track expanded state for this ViewHolder
            var isExpanded = false
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.notification_history_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = notifications[position]
            holder.appNameText.text = notification.appName
            holder.titleText.text = notification.title
            holder.timeText.text = notification.getFormattedTime()
            
            // Setup content text with expand/collapse functionality
            val content = notification.text
            if (content.length > 120 && !holder.isExpanded) {
                holder.contentText.text = content.take(120) + "..."
                holder.expandButton.visibility = View.VISIBLE
                holder.expandButton.text = "See more"
                holder.contentText.maxLines = 3
            } else {
                holder.contentText.text = content
                holder.expandButton.text = if (holder.isExpanded) "Show less" else "See more"
                holder.contentText.maxLines = if (holder.isExpanded) Int.MAX_VALUE else 3
                holder.expandButton.visibility = if (content.length > 120) View.VISIBLE else View.GONE
            }
            
            holder.expandButton.setOnClickListener {
                holder.isExpanded = !holder.isExpanded
                notifyItemChanged(position)
            }
        }
        
        override fun getItemCount() = notifications.size
    }
}