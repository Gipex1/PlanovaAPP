package com.example.planova.data

data class UpdatePlanRequest(
    val title: String?,
    val description: String?,
    val status: String?,
    val targetDate: String?,
    val steps: List<StepRequest>?
)