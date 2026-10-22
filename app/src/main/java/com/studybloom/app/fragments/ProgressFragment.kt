package com.studybloom.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.studybloom.app.activities.ResultActivity
import com.studybloom.app.adapters.RecentQuizAdapter
import com.studybloom.app.databinding.FragmentProgressBinding
import com.studybloom.app.services.FirebaseService
import kotlinx.coroutines.launch

/**
 * ProgressFragment:
 * Provides a clean, aesthetic progress dashboard for students.
 * Demonstrates:
 * - Data calculations in Kotlin (average, max, percentages)
 * - RecyclerView for historical test results
 * - Dynamic ProgressBar updates
 * - Cloud Firestore queries
 */
class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private lateinit var recentQuizAdapter: RecentQuizAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recentQuizAdapter = RecentQuizAdapter(emptyList()) { result ->
            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_QUIZ_TITLE, result.quizTitle)
                putExtra(ResultActivity.EXTRA_QUIZ_ID, result.quizId)
                putExtra(ResultActivity.EXTRA_SUBJECT, result.subject)
                putExtra(ResultActivity.EXTRA_SCORE, result.score)
                putExtra(ResultActivity.EXTRA_TOTAL, result.totalQuestions)
                putExtra(ResultActivity.EXTRA_PERCENTAGE, result.percentage)
                putExtra(ResultActivity.EXTRA_CORRECT, result.correctCount)
                putExtra(ResultActivity.EXTRA_INCORRECT, result.incorrectCount)
            }
            startActivity(intent)
        }
        binding.rvRecentQuizzes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentQuizAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        loadProgressData()
    }

    private fun loadProgressData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = FirebaseService.getQuizResults()
                val flashcardSets = FirebaseService.getFlashcardSets()

                // Calculate metrics
                val totalQuizzes = results.size
                val avgScore = if (results.isNotEmpty()) {
                    results.map { it.percentage }.average().toInt()
                } else 0

                val highestScore = if (results.isNotEmpty()) {
                    results.maxOf { it.percentage }
                } else 0

                val totalCardsMastered = flashcardSets.sumOf { it.masteredCount }

                // Update UI Views
                binding.tvTotalQuizzesCount.text = totalQuizzes.toString()
                binding.tvAverageScoreVal.text = "$avgScore%"
                binding.tvHighestScoreVal.text = "$highestScore%"
                binding.tvCardsMasteredCount.text = totalCardsMastered.toString()

                // Calculate overall mastery rate
                val masteryRate = if (totalQuizzes > 0 || totalCardsMastered > 0) {
                    ((avgScore * 0.7) + (totalCardsMastered * 3).coerceAtMost(30)).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                binding.tvMasteryPercent.text = "$masteryRate%"
                binding.pbMastery.progress = masteryRate

                // Recent Quizzes
                recentQuizAdapter.updateResults(results)
                binding.tvProgressEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
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
