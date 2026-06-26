package com.example.planova.data

data class ResetPasswordRequest(
    val email: String,
    val username: String,
    val newPassword: String
)