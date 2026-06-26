package com.example.planova.data

data class GenerateResponse(
    val title: String,
    val description: String,
    val targetDate: String,
    val steps: List<StepDto>
)

data class StepDto(
    val description: String,
    val sortOrder: Int
)