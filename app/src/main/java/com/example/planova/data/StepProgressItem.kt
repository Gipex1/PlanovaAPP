package com.example.planova.data

data class StepProgressItem(
    val id: Long,
    val day: Int,
    val description: String,
    var isCompleted: Boolean = false
)