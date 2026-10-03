package com.studybloom.app.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.databinding.ActivityResultBinding
import com.studybloom.app.models.Quiz
import com.studybloom.app.models.QuizResult
import com.studybloom.app.services.FirebaseService
import kotlinx.coroutines.launch

/**
 * ResultActivity:
 * Displays the summary of the student's quiz attempt, calculates grade percentage,
 * presents a motivational message, and persists the result to Cloud Firestore.
 * Demonstrates:
 * - Intent extras retrieval
 * - Conditional UI feedback
 * - Navigating back to specific Fragment tabs via Intent extras
 * - Firestore data persistence
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    private var quizTitle: String = ""
    private var quizId: String = ""
    private var subject: String = ""
    private var score: Int = 0
    private var total: Int = 0
    private var percentage: Int = 0
    private var correct: Int = 0
    private var incorrect: Int = 0
    private var quizObject: Quiz? = null

    companion object {
        const val EXTRA_QUIZ_TITLE = "extra_quiz_title"
        const val EXTRA_QUIZ_ID = "extra_quiz_id"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_TOTAL = "extra_total"
        const val EXTRA_PERCENTAGE = "extra_percentage"
        const val EXTRA_CORRECT = "extra_correct"
        const val EXTRA_INCORRECT = "extra_incorrect"
        const val EXTRA_QUIZ_OBJECT = "extra_quiz_object"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve passed parameters
        quizTitle = intent.getStringExtra(EXTRA_QUIZ_TITLE) ?: "StudyBloom Quiz"
        quizId = intent.getStringExtra(EXTRA_QUIZ_ID) ?: ""
        subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "General"
        score = intent.getIntExtra(EXTRA_SCORE, 0)
        total = intent.getIntExtra(EXTRA_TOTAL, 0)
        percentage = intent.getIntExtra(EXTRA_PERCENTAGE, 0)
        correct = intent.getIntExtra(EXTRA_CORRECT, 0)
        incorrect = intent.getIntExtra(EXTRA_INCORRECT, 0)
        @Suppress("DEPRECATION")
        quizObject = intent.getSerializableExtra(EXTRA_QUIZ_OBJECT) as? Quiz

        displayResults()
        saveResultToFirestore()
        setupButtons()
    }

    private fun displayResults() {
        binding.tvQuizResultTopic.text = quizTitle
        binding.tvScoreFraction.text = "$score / $total"
        binding.tvScorePercentage.text = "$percentage%"
        binding.tvCorrectCount.text = "Correct: $correct"
        binding.tvIncorrectCount.text = "Incorrect: $incorrect"

        // Whimsical motivational messages
        val message = when {
            percentage >= 80 -> "Blooming brilliantly! You have thoroughly mastered this topic! 🌸✨"
            percentage >= 50 -> "Growing stronger every day! Keep watering your knowledge and shine! 🌷"
            else -> "Every seed takes time to sprout! Review flashcards and watch yourself bloom! 🌱🌸"
        }
        binding.tvMotivationalMessage.text = message
    }

    private fun saveResultToFirestore() {
        lifecycleScope.launch {
            val result = QuizResult(
                id = "",
                userId = FirebaseService.getCurrentUserId(),
                quizId = quizId,
                quizTitle = quizTitle,
                subject = subject,
                score = score,
                totalQuestions = total,
                percentage = percentage,
                correctCount = correct,
                incorrectCount = incorrect,
                timestamp = System.currentTimeMillis()
            )
            FirebaseService.saveQuizResult(result)
        }
    }

    private fun setupButtons() {
        // Try Again: Restarts the same quiz
        binding.btnTryAgain.setOnClickListener {
            if (quizObject != null) {
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_QUIZ, quizObject)
                }
                startActivity(intent)
                finish()
            } else {
                finish()
            }
        }

        // Back to Home: Navigates to MainActivity clearing stack
        binding.btnBackToHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        // View Progress: Navigates to MainActivity with the Progress tab opened
        binding.btnViewProgress.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.TARGET_PROGRESS)
            }
            startActivity(intent)
            finish()
        }
    }
}
