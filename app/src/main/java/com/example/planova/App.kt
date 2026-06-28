package com.example.planova

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.*

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applyLanguage()
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
}