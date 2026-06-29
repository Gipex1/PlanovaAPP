package com.example.planova.data

data class StepResponse(
    val id: Long,
    val description: String,
    val sortOrder: Int,
    val completed: Boolean  // ← оставляем completed
)