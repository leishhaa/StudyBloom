package com.studybloom.app.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.R
import com.studybloom.app.databinding.ActivityFlashcardBinding
import com.studybloom.app.models.Flashcard
import com.studybloom.app.models.FlashcardSet
import com.studybloom.app.services.FirebaseService
import kotlinx.coroutines.launch

/**
 * FlashcardActivity:
 * Presents flashcards one at a time with an interactive flip animation.
 * Demonstrates:
 * - Intent extras serialization
 * - View animation (card flip transitions)
 * - State management in Kotlin (current card index, front/back side)
 * - Dynamic ProgressBar updates
 * - Firestore progress tracking (mastered vs review counts)
 */
class FlashcardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashcardBinding

    private var flashcardSet: FlashcardSet? = null
    private var cards: List<Flashcard> = emptyList()
    private var currentIndex = 0
    private var isShowingFront = true

    companion object {
        const val EXTRA_FLASHCARD_SET = "extra_flashcard_set"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve FlashcardSet passed via Intent extras
        @Suppress("DEPRECATION")
        flashcardSet = intent.getSerializableExtra(EXTRA_FLASHCARD_SET) as? FlashcardSet

        cards = flashcardSet?.cards ?: emptyList()

        if (cards.isEmpty()) {
            Toast.makeText(this, "No flashcards found in this set!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDeckTitle.text = flashcardSet?.title ?: getString(R.string.flashcards_title)
        binding.pbDeckProgress.max = cards.size

        setupListeners()
        displayCard(currentIndex)
    }

    private fun setupListeners() {
        binding.btnBackFlashcards.setOnClickListener {
            finish()
        }

        // Tap card to flip between Question and Answer
        binding.cardFlashcard.setOnClickListener {
            flipCard()
        }

        // Previous card button
        binding.btnPreviousCard.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                isShowingFront = true
                displayCard(currentIndex)
            } else {
                Toast.makeText(this, "You are at the first card!", Toast.LENGTH_SHORT).show()
            }
        }

        // Next card button
        binding.btnNextCard.setOnClickListener {
            if (currentIndex < cards.size - 1) {
                currentIndex++
                isShowingFront = true
                displayCard(currentIndex)
            } else {
                Toast.makeText(this, getString(R.string.flashcard_completed_msg), Toast.LENGTH_LONG).show()
            }
        }

        // "I knew this 🌸" button
        binding.btnKnewThis.setOnClickListener {
            cards[currentIndex].isMastered = true
            updateProgress(masteredDelta = 1, reviewDelta = 0)
            Toast.makeText(this, "Mastered! 🌸 Keep going!", Toast.LENGTH_SHORT).show()
            moveToNextCardAuto()
        }

        // "Need review 🌷" button
        binding.btnNeedReview.setOnClickListener {
            cards[currentIndex].isMastered = false
            updateProgress(masteredDelta = 0, reviewDelta = 1)
            Toast.makeText(this, "Marked for review 🌷", Toast.LENGTH_SHORT).show()
            moveToNextCardAuto()
        }
    }

    private fun moveToNextCardAuto() {
        if (currentIndex < cards.size - 1) {
            currentIndex++
            isShowingFront = true
            displayCard(currentIndex)
        } else {
            Toast.makeText(this, getString(R.string.flashcard_completed_msg), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Flips the flashcard between Question and Answer with animation.
     */
    private fun flipCard() {
        val flipExit = AnimationUtils.loadAnimation(this, R.anim.card_flip_exit)
        val flipEnter = AnimationUtils.loadAnimation(this, R.anim.card_flip_enter)

        binding.cardFlashcard.startAnimation(flipExit)

        flipExit.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                isShowingFront = !isShowingFront
                renderCardContent()
                binding.cardFlashcard.startAnimation(flipEnter)
            }
        })
    }

    private fun displayCard(index: Int) {
        val card = cards[index]
        binding.tvCardCounter.text = "Card ${index + 1} of ${cards.size}"
        binding.pbDeckProgress.progress = index + 1
        binding.btnPreviousCard.isEnabled = (index > 0)

        isShowingFront = true
        renderCardContent()
    }

    private fun renderCardContent() {
        val card = cards[currentIndex]
        if (isShowingFront) {
            // Front: Question
            binding.tvCardSideBadge.text = "QUESTION 🌸"
            binding.tvCardSideBadge.setTextColor(ContextCompat.getColor(this, R.color.bloom_pink_primary))
            binding.tvCardContent.text = card.question
            binding.tvFlipHint.text = getString(R.string.tap_to_flip_hint)
            binding.cardFlashcard.strokeColor = ContextCompat.getColor(this, R.color.bloom_pink_soft)
            binding.cardFlashcard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bloom_surface))
        } else {
            // Back: Answer
            binding.tvCardSideBadge.text = "ANSWER ✨"
            binding.tvCardSideBadge.setTextColor(ContextCompat.getColor(this, R.color.bloom_lavender_dark))
            binding.tvCardContent.text = card.answer
            binding.tvFlipHint.text = "Tap to view question 🌸"
            binding.cardFlashcard.strokeColor = ContextCompat.getColor(this, R.color.bloom_lavender)
            binding.cardFlashcard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bloom_card_lavender))
        }
    }

    private fun updateProgress(masteredDelta: Int, reviewDelta: Int) {
        val setId = flashcardSet?.id ?: return
        if (setId.isBlank()) return

        lifecycleScope.launch {
            FirebaseService.updateFlashcardProgress(setId, masteredDelta, reviewDelta)
        }
    }
}
