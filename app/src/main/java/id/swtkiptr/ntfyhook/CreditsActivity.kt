package id.swtkiptr.ntfyhook

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class CreditsActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Set up repository buttons
        setupRepositoryButtons()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_credits
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_left, R.anim.slide_out_right))
                    finish()
                    true
                }
                R.id.nav_apps -> {
                    val intent = Intent(this, AppSelectionActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_left, R.anim.slide_out_right))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    val intent = Intent(this, NotificationHistoryActivity::class.java)
                    startActivity(intent, createCustomAnimationBundle(R.anim.slide_in_left, R.anim.slide_out_right))
                    finish()
                    true
                }
                R.id.nav_credits -> {
                    // Already on Credits screen
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRepositoryButtons() {
        // Original repository button
        val originalRepoButton = findViewById<MaterialButton>(R.id.original_repo_button)
        originalRepoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eja/ntfy-relay"))
            startActivity(intent)
        }
        
        // Current repository button
        val currentRepoButton = findViewById<MaterialButton>(R.id.current_repo_button)
        currentRepoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/swtkiptr/ntfy-hook"))
            startActivity(intent)
        }
    }
    
    /**
     * Helper function to create animation bundle for activity transitions
     */
    private fun createCustomAnimationBundle(enterAnim: Int, exitAnim: Int): Bundle {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Use the modern API for Android 14+
            android.app.ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
        } else {
            // Use the compatibility approach for older versions
            android.app.ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim).toBundle()
        }
    }
}