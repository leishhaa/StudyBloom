package com.studybloom.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.databinding.ActivityLoginBinding
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * LoginActivity:
 * Handles user authentication via Firebase Authentication.
 * Demonstrates:
 * - User input validation
 * - Asynchronous operations with Kotlin Coroutines
 * - Error handling and Toast user feedback
 * - Explicit Intents
 * - SharedPreferences demo session fallback
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefsHelper(this)

        // Login button click listener
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        // Navigate to RegisterActivity
        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Demo Mode for university evaluations / offline usage
        binding.btnDemoMode.setOnClickListener {
            prefs.isDemoMode = true
            prefs.demoUserName = "Bloom Scholar"
            prefs.demoUserEmail = "scholar@studybloom.edu"

            Toast.makeText(this, "Logged in as Bloom Scholar (Demo Mode) 🌸", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Input validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseService.auth
        if (auth == null) {
            Toast.makeText(this, "Firebase is not configured yet. Using Demo Mode 🌸", Toast.LENGTH_LONG).show()
            prefs.isDemoMode = true
            prefs.demoUserName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            prefs.demoUserEmail = email
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Show loading spinner
        setLoading(true)

        lifecycleScope.launch {
            try {
                // Firebase Authentication sign in
                auth.signInWithEmailAndPassword(email, password).await()
                prefs.isDemoMode = false

                Toast.makeText(this@LoginActivity, "Welcome back! 🌸", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Display error message to the user
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${e.localizedMessage ?: "Invalid credentials"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnDemoMode.isEnabled = !isLoading
    }
}
