package com.example.planova

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applyLanguage()
        applyTheme()
    }

    private fun applyLanguage() {
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", "ru") ?: "ru"

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val darkTheme = prefs.getBoolean("dark_theme", false)

        // ЛОГ для проверки
        android.util.Log.d("THEME", "App применяет тему: $darkTheme")

        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}