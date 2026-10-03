package com.studybloom.app.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.R
import com.studybloom.app.databinding.ActivitySplashBinding
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.NotificationHelper
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity:
 * Entry point of the StudyBloom application.
 * Demonstrates:
 * - Activity lifecycle (onCreate)
 * - ViewBinding
 * - Simple animations
 * - Checking authentication state with Firebase Auth / SharedPreferences
 * - Explicit Intents to transition to the next Activity
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize notification channel for daily reminders
        NotificationHelper.createNotificationChannel(this)

        // Run a gentle fade-in animation on the logo and app title
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.llSplashContent.startAnimation(fadeIn)

        val prefs = SharedPrefsHelper(this)

        // Hold splash screen for 1.5 seconds, then transition
        lifecycleScope.launch {
            delay(1500)

            val isFirebaseLoggedIn = FirebaseService.isUserLoggedIn()
            val isDemoLoggedIn = prefs.isDemoMode

            val targetActivity = if (isFirebaseLoggedIn || isDemoLoggedIn) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }

            val intent = Intent(this@SplashActivity, targetActivity)
            startActivity(intent)
            finish() // Finish Splash so user cannot navigate back to it
        }
    }
}
