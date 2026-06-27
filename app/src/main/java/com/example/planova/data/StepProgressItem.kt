package com.example.planova.data

data class StepProgressItem(
    val day: Int,
    val description: String,
    val isCompleted: Boolean = false
)