package com.studybloom.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.activities.LoginActivity
import com.studybloom.app.databinding.FragmentProfileBinding
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.NotificationHelper
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch

/**
 * ProfileFragment:
 * Manages user profile information, study counts, and settings
 * (such as daily study reminder notifications via WorkManager).
 * Demonstrates:
 * - SharedPreferences toggles
 * - WorkManager scheduling and cancellation
 * - Firebase Authentication sign-out
 * - Explicit Intent clearing task backstack
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPrefsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsHelper(requireContext())

        setupProfileHeader()
        setupSettings()
        setupLogout()
    }

    override fun onResume() {
        super.onResume()
        loadStudyCounts()
        binding.tvLastSubjectFocus.text = prefs.lastSubject
    }

    private fun setupProfileHeader() {
        val userName = if (prefs.isDemoMode) {
            prefs.demoUserName
        } else {
            FirebaseService.auth?.currentUser?.displayName
                ?: FirebaseService.auth?.currentUser?.email?.substringBefore("@")
                ?: prefs.demoUserName
        }

        val userEmail = if (prefs.isDemoMode) {
            prefs.demoUserEmail
        } else {
            FirebaseService.auth?.currentUser?.email ?: prefs.demoUserEmail
        }

        binding.tvProfileName.text = userName
        binding.tvProfileEmail.text = userEmail
        binding.tvAvatarInitial.text = userName.take(1).uppercase()
    }

    private fun setupSettings() {
        // Daily Reminder Switch setup
        binding.switchReminder.isChecked = prefs.isDailyReminderEnabled
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDailyReminderEnabled = isChecked
            if (isChecked) {
                NotificationHelper.scheduleDailyReminder(requireContext())
                Toast.makeText(requireContext(), "Daily revision reminder scheduled! 🌸", Toast.LENGTH_SHORT).show()
            } else {
                NotificationHelper.cancelDailyReminder(requireContext())
                Toast.makeText(requireContext(), "Daily reminders paused 🌷", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStudyCounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = FirebaseService.getNotes()
                val flashcardSets = FirebaseService.getFlashcardSets()
                val quizzes = FirebaseService.getQuizzes()

                binding.tvProfileNotesCount.text = notes.size.toString()
                binding.tvProfileFlashcardsCount.text = flashcardSets.size.toString()
                binding.tvProfileQuizzesCount.text = quizzes.size.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            // Sign out from Firebase Auth
            FirebaseService.auth?.signOut()
            prefs.clearSession()

            Toast.makeText(requireContext(), "Logged out. See you soon! 🌸", Toast.LENGTH_SHORT).show()

            // Navigate back to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
