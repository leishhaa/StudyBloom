package com.studybloom.app.models

import java.io.Serializable

/**
 * Stores basic user profile information in Firestore 'users/{uid}'.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
