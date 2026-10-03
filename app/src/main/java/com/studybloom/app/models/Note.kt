package com.studybloom.app.models

import java.io.Serializable

/**
 * Represents a study note created by the user or imported from a .txt file.
 * Stored in Firestore under the 'notes' collection.
 */
data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val content: String = "",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
