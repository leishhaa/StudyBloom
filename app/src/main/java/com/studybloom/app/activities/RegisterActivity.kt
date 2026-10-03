package com.studybloom.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.databinding.ActivityRegisterBinding
import com.studybloom.app.models.UserProfile
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * RegisterActivity:
 * Handles user account registration.
 * Demonstrates:
 * - Firebase Authentication user creation
 * - Saving user profile document to Cloud Firestore
 * - Form validation (passwords match, min length)
 * - Explicit Intents
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: SharedPrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefsHelper(this)

        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish() // Return to LoginActivity
        }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Form validations
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseService.auth
        if (auth == null) {
            // Firebase not configured fallback
            Toast.makeText(this, "Account created in Demo Mode 🌸", Toast.LENGTH_SHORT).show()
            prefs.isDemoMode = true
            prefs.demoUserName = name
            prefs.demoUserEmail = email
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Create user in Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: ""

                // 2. Save user profile document to Cloud Firestore
                val userProfile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )
                FirebaseService.saveUserProfile(userProfile)

                prefs.isDemoMode = false
                prefs.demoUserName = name
                prefs.demoUserEmail = email

                Toast.makeText(this@RegisterActivity, "Welcome to StudyBloom, $name! 🌸", Toast.LENGTH_LONG).show()

                // 3. Navigate to MainActivity
                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}
