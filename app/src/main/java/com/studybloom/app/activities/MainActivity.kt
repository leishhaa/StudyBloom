package com.studybloom.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.studybloom.app.R
import com.studybloom.app.databinding.ActivityMainBinding
import com.studybloom.app.fragments.HomeFragment
import com.studybloom.app.fragments.LibraryFragment
import com.studybloom.app.fragments.ProfileFragment
import com.studybloom.app.fragments.ProgressFragment

/**
 * MainActivity:
 * The main container for the application's fragments.
 * Demonstrates:
 * - BottomNavigationView setup
 * - Fragment transactions using FragmentManager (replace, commit)
 * - Handling Intent extras to navigate to specific tabs
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EXTRA_NAV_TARGET = "extra_nav_target"
        const val TARGET_PROGRESS = "target_progress"
        const val TARGET_LIBRARY = "target_library"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up BottomNavigationView item selected listener
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_library -> {
                    loadFragment(LibraryFragment())
                    true
                }
                R.id.nav_progress -> {
                    loadFragment(ProgressFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Check if an Intent extra requested a specific tab (e.g. from ResultActivity)
        val target = intent.getStringExtra(EXTRA_NAV_TARGET)
        if (target == TARGET_PROGRESS) {
            binding.bottomNavigation.selectedItemId = R.id.nav_progress
        } else if (target == TARGET_LIBRARY) {
            binding.bottomNavigation.selectedItemId = R.id.nav_library
        } else {
            // Default to HomeFragment on first load
            if (savedInstanceState == null) {
                loadFragment(HomeFragment())
            }
        }
    }

    /**
     * Replaces the current fragment inside fragmentContainer.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Helper to select a tab programmatically from hosted fragments.
     */
    fun selectNavigationItem(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }
}
