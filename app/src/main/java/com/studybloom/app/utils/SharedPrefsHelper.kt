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
        private const val KEY_DAILY_REMINDER = "daily_reminder_enabled"
        private const val KEY_DEMO_USER_NAME = "demo_user_name"
        private const val KEY_DEMO_USER_EMAIL = "demo_user_email"
        private const val KEY_IS_DEMO_MODE = "is_demo_mode"
    }

    var isOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var lastSubject: String
        get() = prefs.getString(KEY_LAST_SUBJECT, "General Science") ?: "General Science"
        set(value) = prefs.edit().putString(KEY_LAST_SUBJECT, value).apply()

    var isDailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDER, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDER, value).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DEMO_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DEMO_MODE, value).apply()

    var demoUserName: String
        get() = prefs.getString(KEY_DEMO_USER_NAME, "Bloom Scholar") ?: "Bloom Scholar"
        set(value) = prefs.edit().putString(KEY_DEMO_USER_NAME, value).apply()

    var demoUserEmail: String
        get() = prefs.getString(KEY_DEMO_USER_EMAIL, "scholar@studybloom.edu") ?: "scholar@studybloom.edu"
        set(value) = prefs.edit().putString(KEY_DEMO_USER_EMAIL, value).apply()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_DEMO_MODE)
            .apply()
    }
}
