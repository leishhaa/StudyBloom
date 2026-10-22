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
            Toast.makeText(this, "Authentication service is unavailable. Please check your connection.", Toast.LENGTH_LONG).show()
            return
        }

        // Show loading spinner
        setLoading(true)

        lifecycleScope.launch {
            try {
                // Firebase Authentication sign in
                auth.signInWithEmailAndPassword(email, password).await()

                Toast.makeText(this@LoginActivity, "Logged in successfully.", Toast.LENGTH_SHORT).show()
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
    }
}
