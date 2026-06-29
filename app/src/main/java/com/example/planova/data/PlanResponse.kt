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