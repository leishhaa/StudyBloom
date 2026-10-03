package com.studybloom.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.activities.NotesActivity
import com.studybloom.app.databinding.FragmentHomeBinding
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch

/**
 * HomeFragment:
 * Welcomes the student, displays summary revision metrics,
 * and presents quick action cards to upload notes, generate flashcards, or take a quiz.
 * Demonstrates:
 * - Fragment lifecycle (onCreateView, onViewCreated, onResume)
 * - ViewBinding in Fragments
 * - Coroutine calls to Cloud Firestore
 * - Explicit Intents with Intent extras
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPrefsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = SharedPrefsHelper(requireContext())

        setupGreeting()
        setupActionListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh stats from Firestore whenever user returns to Home
        loadStats()
    }

    private fun setupGreeting() {
        val userName = if (prefs.isDemoMode) {
            prefs.demoUserName
        } else {
            FirebaseService.auth?.currentUser?.displayName ?: prefs.demoUserName
        }
        binding.tvGreeting.text = "Hi, $userName 🌸"
    }

    private fun setupActionListeners() {
        // 1. Generate Flashcards Card
        binding.cardActionFlashcards.setOnClickListener {
            val intent = Intent(requireContext(), NotesActivity::class.java).apply {
                putExtra(NotesActivity.EXTRA_INITIAL_ACTION, NotesActivity.ACTION_GENERATE_FLASHCARDS)
            }
            startActivity(intent)
        }

        // 2. Take a Quiz Card
        binding.cardActionQuiz.setOnClickListener {
            val intent = Intent(requireContext(), NotesActivity::class.java).apply {
                putExtra(NotesActivity.EXTRA_INITIAL_ACTION, NotesActivity.ACTION_GENERATE_QUIZ)
            }
            startActivity(intent)
        }

        // 3. Upload Notes Card
        binding.cardActionUpload.setOnClickListener {
            val intent = Intent(requireContext(), NotesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch stats from Firestore
                val flashcardSets = FirebaseService.getFlashcardSets()
                val quizResults = FirebaseService.getQuizResults()

                binding.tvStatFlashcards.text = flashcardSets.size.toString()
                binding.tvStatQuizzes.text = quizResults.size.toString()

                if (quizResults.isNotEmpty()) {
                    val avg = quizResults.map { it.percentage }.average().toInt()
                    binding.tvStatAvgScore.text = "$avg%"
                } else {
                    binding.tvStatAvgScore.text = "0%"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
