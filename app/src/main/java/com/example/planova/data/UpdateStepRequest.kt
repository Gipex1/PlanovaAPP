package com.example.planova.data

data class UpdateStepRequest(
    val description: String?,
    val isCompleted: Boolean?,   // ← было isCompleted
    val sortOrder: Int?
)