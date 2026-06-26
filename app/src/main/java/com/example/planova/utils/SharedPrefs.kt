package com.example.planova.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("planova", Context.MODE_PRIVATE)

    fun saveUserData(userId: Long, email: String, password: String?, remember: Boolean) {
        with(prefs.edit()) {
            putLong("userId", userId)
            putString("email", email)
            if (password != null) putString("password", password)
            putBoolean("remember", remember)
            apply()
        }
    }

    fun getUserId(): Long? = if (prefs.contains("userId")) prefs.getLong("userId", 0) else null

    fun getEmail(): String? = prefs.getString("email", null)

    fun getPassword(): String? = prefs.getString("password", null)

    fun isRemember(): Boolean = prefs.getBoolean("remember", false)

    fun clear() = prefs.edit().clear().apply()
}