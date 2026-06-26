package com.example.planova.data

data class UpdateStepRequest(
    val description: String?,
    val isCompleted: Boolean?,
    val sortOrder: Int?
)