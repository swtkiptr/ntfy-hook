package id.swtkiptr.ntfyhook

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var searchView: SearchView
    private lateinit var selectAllButton: MaterialButton
    private lateinit var deselectAllButton: MaterialButton
    private lateinit var selectedCountText: TextView
    private lateinit var appSelectionManager: AppSelectionManager
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var savingProgressIndicator: LinearProgressIndicator
    private val selectedPackages = mutableSetOf<String>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var saveJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_selection_activity)
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Apps"
        
        // Initialize components
        appSelectionManager = AppSelectionManager(this)
        recyclerView = findViewById(R.id.app_recycler_view)
        searchView = findViewById(R.id.app_search_view)
        selectAllButton = findViewById(R.id.select_all_button)
        deselectAllButton = findViewById(R.id.deselect_all_button)
        selectedCountText = findViewById(R.id.selected_count_text)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        savingProgressIndicator = findViewById(R.id.saving_progress_indicator)
        
        // Initially hide the saving progress indicator
        savingProgressIndicator.visibility = View.GONE
        
        // Get already selected packages
        selectedPackages.clear() // Ensure we start with a clean slate
        selectedPackages.addAll(appSelectionManager.getSelectedPackages())
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Update selected count
        updateSelectedCount()
        
        // Load installed apps and set up adapter
        loadInstalledApps()
        
        // Set up search functionality
        setupSearch()
        
        // Set up batch selection buttons
        setupBatchSelection()
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Handle back button press with modern approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })
    }
    
    override fun onResume() {
        super.onResume()
        
        // Set the selected navigation item to Apps
        bottomNavigation.selectedItemId = R.id.nav_apps
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
                    // Already on Apps screen
                    true
                }
                R.id.nav_history -> {
                    // Navigate to NotificationHistoryActivity
                    val intent = Intent(this, NotificationHistoryActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_right, R.anim.slide_out_left))
                    finish() // Close this activity
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
    
    private fun loadInstalledApps() {
        // Show loading indicator
        findViewById<View>(R.id.loading_progress).visibility = View.VISIBLE
        
        coroutineScope.launch(Dispatchers.IO) {
            // Load all installed apps on background thread
            val apps = appSelectionManager.getInstalledApps()
            
            withContext(Dispatchers.Main) {
                // Hide loading indicator
                findViewById<View>(R.id.loading_progress).visibility = View.GONE
                
                // Show empty view if no apps
                if (apps.isEmpty()) {
                    findViewById<View>(R.id.empty_view).visibility = View.VISIBLE
                }
                
                // Set up adapter with installed apps
                adapter = AppAdapter(apps) { packageName, isSelected ->
                    handleAppSelection(packageName, isSelected)
                }
                recyclerView.adapter = adapter
            }
        }
    }
    
    private fun handleAppSelection(packageName: String, isSelected: Boolean) {
        // Show saving indicator
        savingProgressIndicator.visibility = View.VISIBLE
        
        if (isSelected) {
            selectedPackages.add(packageName)
        } else {
            selectedPackages.remove(packageName)
        }
        
        // Update the selected count text immediately
        updateSelectedCount()
        
        // Cancel any pending save operations
        saveJob?.cancel()
        
        // Create a new save job with debounce
        saveJob = coroutineScope.launch {
            delay(300) // Wait for 300ms to debounce rapid selections
            
            // Save selection
            appSelectionManager.saveSelectedPackages(selectedPackages.toSet())
            
            // Hide the saving indicator after saving completes
            withContext(Dispatchers.Main) {
                savingProgressIndicator.visibility = View.GONE
            }
        }
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })
    }
    
    private fun setupBatchSelection() {
        selectAllButton.setOnClickListener {
            // Show saving indicator
            savingProgressIndicator.visibility = View.VISIBLE
            
            // Get all available packages from adapter's current filtered list
            val currentPackages = adapter.getCurrentPackages()
            selectedPackages.addAll(currentPackages)
            
            // Update UI to reflect changes
            adapter.notifyDataSetChanged()
            updateSelectedCount()
            
            // Save selection on background thread
            coroutineScope.launch {
                appSelectionManager.saveSelectedPackages(selectedPackages.toSet())
                
                // Hide the saving indicator after saving completes
                withContext(Dispatchers.Main) {
                    savingProgressIndicator.visibility = View.GONE
                    Toast.makeText(this@AppSelectionActivity, "${currentPackages.size} apps selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        deselectAllButton.setOnClickListener {
            // Show saving indicator
            savingProgressIndicator.visibility = View.VISIBLE
            
            // Get all available packages from adapter's current filtered list
            val currentPackages = adapter.getCurrentPackages()
            val previousSize = selectedPackages.size
            
            // Remove only the currently visible packages
            selectedPackages.removeAll(currentPackages.toSet())
            
            // Update UI to reflect changes
            adapter.notifyDataSetChanged()
            updateSelectedCount()
            
            // Save selection on background thread
            coroutineScope.launch {
                appSelectionManager.saveSelectedPackages(selectedPackages.toSet())
                
                // Hide the saving indicator after saving completes
                withContext(Dispatchers.Main) {
                    savingProgressIndicator.visibility = View.GONE
                    val removed = previousSize - selectedPackages.size
                    Toast.makeText(this@AppSelectionActivity, "$removed apps deselected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateSelectedCount() {
        selectedCountText.text = "${selectedPackages.size} selected"
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                saveAndFinish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveAndFinish() {
        // Show saving indicator
        savingProgressIndicator.visibility = View.VISIBLE
        
        val previousCount = appSelectionManager.getSelectedPackages().size
        
        coroutineScope.launch {
            // Use synchronous save to ensure it completes before finishing
            withContext(Dispatchers.IO) {
                appSelectionManager.saveSelectedPackagesSynchronously(selectedPackages.toSet())
            }
            
            withContext(Dispatchers.Main) {
                // Hide saving indicator
                savingProgressIndicator.visibility = View.GONE
                
                if (selectedPackages.size != previousCount) {
                    Toast.makeText(this@AppSelectionActivity, "${selectedPackages.size} apps selected", Toast.LENGTH_SHORT).show()
                }
                
                finish()
                // Apply custom animation when finishing
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }
    }
    
    /**
     * Helper function to create animation bundle for activity transitions
     */
    private fun createCustomAnimationBundle(enterAnim: Int, exitAnim: Int): Bundle {
        return ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
    }
    
    private inner class AppAdapter(
        private val apps: List<AppInfo>,
        private val onItemCheckedChanged: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>(), Filterable {
        
        private var filteredApps = ArrayList(apps)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.app_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = filteredApps[position]
            
            // Prevent checkbox listener from triggering while binding
            holder.checkBox.setOnCheckedChangeListener(null)
            
            // Set the checkbox state first before setting up any listeners
            holder.checkBox.isChecked = selectedPackages.contains(app.packageName)
            
            holder.bind(app)
        }
        
        override fun getItemCount() = filteredApps.size
        
        fun getCurrentPackages(): List<String> {
            return filteredApps.map { it.packageName }
        }
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val appNameView: TextView = view.findViewById(R.id.app_name)
            private val packageNameView: TextView = view.findViewById(R.id.package_name)
            val checkBox: MaterialCheckBox = view.findViewById(R.id.app_checkbox)
            private val iconView: ImageView = view.findViewById(R.id.app_icon)
            
            fun bind(app: AppInfo) {
                appNameView.text = app.appName
                packageNameView.text = app.packageName
                
                // Try to load app icon
                try {
                    val icon = packageManager.getApplicationIcon(app.packageName)
                    iconView.setImageDrawable(icon)
                } catch (e: Exception) {
                    iconView.setImageResource(android.R.drawable.sym_def_app_icon)
                }
                
                // Handle checkbox changes after we've set the initial state
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    @Suppress("DEPRECATION")
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val currentApp = filteredApps[position]
                        onItemCheckedChanged(currentApp.packageName, isChecked)
                    }
                }
                
                // Make the whole item clickable to toggle selection
                itemView.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }
            }
        }
        
        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.lowercase() ?: ""
                    
                    val filteredList = if (query.isEmpty()) {
                        ArrayList(apps)
                    } else {
                        ArrayList(apps.filter { app ->
                            app.appName.lowercase().contains(query) || 
                            app.packageName.lowercase().contains(query)
                        })
                    }
                    
                    val results = FilterResults()
                    results.values = filteredList
                    return results
                }
                
                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    filteredApps = results?.values as ArrayList<AppInfo>
                    notifyDataSetChanged()
                }
            }
        }
    }
}