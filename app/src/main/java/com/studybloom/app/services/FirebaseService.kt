package com.studybloom.app.services

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.studybloom.app.models.FlashcardSet
import com.studybloom.app.models.Note
import com.studybloom.app.models.Quiz
import com.studybloom.app.models.QuizResult
import com.studybloom.app.models.UserProfile
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Service to encapsulate Firebase operations:
 * - Firebase Authentication (email/password)
 * - Cloud Firestore (notes, flashcardSets, quizzes, quizResults, users)
 * - Firebase Cloud Storage (uploading .txt notes)
 *
 * Includes local in-memory fallback stores so the app can be fully tested
 * even before configuring Firebase credentials.
 */
object FirebaseService {

    // Firebase Instances (wrapped in try-catch to prevent crashes if google-services is unconfigured)
    val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }

    val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    val storage: FirebaseStorage? by lazy {
        try { FirebaseStorage.getInstance() } catch (e: Exception) { null }
    }

    // In-memory demo caches for offline/unconfigured testing
    private val localNotes = mutableListOf<Note>()
    private val localFlashcardSets = mutableListOf<FlashcardSet>()
    private val localQuizzes = mutableListOf<Quiz>()
    private val localQuizResults = mutableListOf<QuizResult>()

    fun getCurrentUserId(): String {
        return auth?.currentUser?.uid ?: "demo_user_101"
    }

    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }

    // ==========================================
    // USER PROFILE
    // ==========================================

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        val db = firestore ?: return true
        return try {
            db.collection("users").document(profile.uid).set(profile).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val db = firestore ?: return UserProfile(uid, "Bloom Scholar", "scholar@studybloom.edu")
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            UserProfile(uid, "Bloom Scholar", "scholar@studybloom.edu")
        }
    }

    // ==========================================
    // NOTES & STORAGE
    // ==========================================

    suspend fun uploadNoteFile(fileUri: Uri, fileName: String): String? {
        val storageRef = storage?.reference ?: return null
        return try {
            val fileRef = storageRef.child("notes/${getCurrentUserId()}/${UUID.randomUUID()}_$fileName")
            fileRef.putFile(fileUri).await()
            val downloadUrl = fileRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveNote(note: Note): String {
        val db = firestore
        val noteId = note.id.ifBlank { UUID.randomUUID().toString() }
        val noteToSave = note.copy(id = noteId, userId = getCurrentUserId())

        if (db != null) {
            try {
                db.collection("notes").document(noteId).set(noteToSave).await()
            } catch (e: Exception) {
                e.printStackTrace()
                localNotes.removeAll { it.id == noteId }
                localNotes.add(noteToSave)
            }
        } else {
            localNotes.removeAll { it.id == noteId }
            localNotes.add(noteToSave)
        }
        return noteId
    }

    suspend fun getNotes(): List<Note> {
        val db = firestore
        val uid = getCurrentUserId()
        if (db != null) {
            try {
                val snapshot = db.collection("notes")
                    .whereEqualTo("userId", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(Note::class.java)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return localNotes.filter { it.userId == uid }
    }

    // ==========================================
    // FLASHCARD SETS
    // ==========================================

    suspend fun saveFlashcardSet(set: FlashcardSet): String {
        val db = firestore
        val setId = set.id.ifBlank { UUID.randomUUID().toString() }
        val setToSave = set.copy(id = setId, userId = getCurrentUserId())

        if (db != null) {
            try {
                db.collection("flashcardSets").document(setId).set(setToSave).await()
            } catch (e: Exception) {
                e.printStackTrace()
                localFlashcardSets.removeAll { it.id == setId }
                localFlashcardSets.add(setToSave)
            }
        } else {
            localFlashcardSets.removeAll { it.id == setId }
            localFlashcardSets.add(setToSave)
        }
        return setId
    }

    suspend fun getFlashcardSets(): List<FlashcardSet> {
        val db = firestore
        val uid = getCurrentUserId()
        if (db != null) {
            try {
                val snapshot = db.collection("flashcardSets")
                    .whereEqualTo("userId", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(FlashcardSet::class.java)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return localFlashcardSets.filter { it.userId == uid }
    }

    suspend fun updateFlashcardProgress(setId: String, masteredDelta: Int, reviewDelta: Int) {
        val db = firestore
        if (db != null) {
            try {
                val docRef = db.collection("flashcardSets").document(setId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val currentMastered = snapshot.getLong("masteredCount") ?: 0
                    val currentReview = snapshot.getLong("reviewCount") ?: 0
                    transaction.update(docRef, "masteredCount", currentMastered + masteredDelta)
                    transaction.update(docRef, "reviewCount", currentReview + reviewDelta)
                }.await()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val set = localFlashcardSets.find { it.id == setId }
        if (set != null) {
            set.masteredCount += masteredDelta
            set.reviewCount += reviewDelta
        }
    }

    // ==========================================
    // QUIZZES
    // ==========================================

    suspend fun saveQuiz(quiz: Quiz): String {
        val db = firestore
        val quizId = quiz.id.ifBlank { UUID.randomUUID().toString() }
        val quizToSave = quiz.copy(id = quizId, userId = getCurrentUserId())

        if (db != null) {
            try {
                db.collection("quizzes").document(quizId).set(quizToSave).await()
            } catch (e: Exception) {
                e.printStackTrace()
                localQuizzes.removeAll { it.id == quizId }
                localQuizzes.add(quizToSave)
            }
        } else {
            localQuizzes.removeAll { it.id == quizId }
            localQuizzes.add(quizToSave)
        }
        return quizId
    }

    suspend fun getQuizzes(): List<Quiz> {
        val db = firestore
        val uid = getCurrentUserId()
        if (db != null) {
            try {
                val snapshot = db.collection("quizzes")
                    .whereEqualTo("userId", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(Quiz::class.java)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return localQuizzes.filter { it.userId == uid }
    }

    // ==========================================
    // QUIZ RESULTS
    // ==========================================

    suspend fun saveQuizResult(result: QuizResult): String {
        val db = firestore
        val resId = result.id.ifBlank { UUID.randomUUID().toString() }
        val resToSave = result.copy(id = resId, userId = getCurrentUserId())

        if (db != null) {
            try {
                db.collection("quizResults").document(resId).set(resToSave).await()
            } catch (e: Exception) {
                e.printStackTrace()
                localQuizResults.add(resToSave)
            }
        } else {
            localQuizResults.add(resToSave)
        }
        return resId
    }

    suspend fun getQuizResults(): List<QuizResult> {
        val db = firestore
        val uid = getCurrentUserId()
        if (db != null) {
            try {
                val snapshot = db.collection("quizResults")
                    .whereEqualTo("userId", uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val list = snapshot.toObjects(QuizResult::class.java)
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return localQuizResults.filter { it.userId == uid }
    }
}
