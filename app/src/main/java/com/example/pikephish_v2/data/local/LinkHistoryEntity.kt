package com.example.pikephish_v2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_history")
data class LinkHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String?,
    val domain: String?,
    val isPhishing: Boolean,
    val confidence: Double,
    val prediction: String?,    // ← nullable
    val reason: String?,        // ← nullable

    val checkedAt: Long,
    val source: String
)
