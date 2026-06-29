package com.example.planova

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.planova.utils.NotificationHelper
import java.util.*

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Применяем язык
        val prefs = newBase.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", "ru") ?: "ru"

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
        // Создаём канал уведомлений при первом запуске
        NotificationHelper(this).createNotificationChannel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()  // <--- ВАЖНО!
        super.onCreate(savedInstanceState)
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val darkTheme = prefs.getBoolean("dark_theme", false)

        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}