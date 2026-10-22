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
            Toast.makeText(this, "Authentication service is unavailable. Please check your connection.", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Create user in Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: ""

                // Update Firebase Auth user display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                authResult.user?.updateProfile(profileUpdates)?.await()

                // 2. Save user profile document to Cloud Firestore
                val userProfile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )
                FirebaseService.saveUserProfile(userProfile)

                Toast.makeText(this@RegisterActivity, "Account created for $name.", Toast.LENGTH_LONG).show()

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
