package com.example.planova.data

data class PlanRequest(
    val title: String,
    val description: String?,
    val targetDate: String?,
    val steps: List<StepRequest>
)

data class StepRequest(
    val description: String,
    val sortOrder: Int
)