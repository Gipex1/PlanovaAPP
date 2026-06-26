package com.example.planova.data

data class PlanResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val category: String?,
    val targetDate: String?,
    val steps: List<StepResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class StepResponse(
    val id: Long,
    val description: String,
    val sortOrder: Int,
    val isCompleted: Boolean
)