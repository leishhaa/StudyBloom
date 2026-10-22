package com.studybloom.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Beginner-friendly helper to manage simple local preferences using Android SharedPreferences.
 * Used for UI states, subject history, and notification toggles.
 * Note: Cloud data is stored in Firebase Firestore.
 */
class SharedPrefsHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "studybloom_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_LAST_SUBJECT = "last_selected_subject"
    }

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var lastSubject: String
        get() = prefs.getString(KEY_LAST_SUBJECT, "General Science") ?: "General Science"
        set(value) = prefs.edit().putString(KEY_LAST_SUBJECT, value).apply()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ONBOARDING_DONE)
            .apply()
    }
}
