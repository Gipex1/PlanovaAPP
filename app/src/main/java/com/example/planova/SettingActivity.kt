package com.example.planova

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingActivity : AppCompatActivity() {

    // UI элементы - делаем nullable вместо lateinit
    private var backArrow: ImageView? = null
    private var llLanguage: LinearLayout? = null
    private var llNotifications: LinearLayout? = null
    private var llTheme: LinearLayout? = null
    private var llReset: LinearLayout? = null
    private var llInfo: LinearLayout? = null
    private var switchNotifications: Switch? = null
    private var switchTheme: Switch? = null
    private var tvLanguage: TextView? = null

    // SharedPreferences - делаем nullable
    private var sharedPref: SharedPreferences? = null

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_THEME = "dark_theme"
        private const val KEY_LANGUAGE = "language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем SharedPreferences сразу
        sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Применяем тему
        applyTheme()

        setContentView(R.layout.activity_setting)

        initViews()
        loadSettings()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initViews() {
        backArrow = findViewById(R.id.backArrow)
        llLanguage = findViewById(R.id.llLanguage)
        llNotifications = findViewById(R.id.llNotifications)
        llTheme = findViewById(R.id.llTheme)
        llReset = findViewById(R.id.llReset)
        llInfo = findViewById(R.id.llInfo)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchTheme = findViewById(R.id.switchTheme)
        tvLanguage = findViewById(R.id.tvLanguage)
    }

    private fun loadSettings() {
        val prefs = sharedPref ?: return
        switchNotifications?.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        switchTheme?.isChecked = prefs.getBoolean(KEY_THEME, false)
        tvLanguage?.text = prefs.getString(KEY_LANGUAGE, "Русский")
    }

    private fun setupClickListeners() {
        backArrow?.setOnClickListener {
            animateClick(it)
            finish()
        }

        llLanguage?.setOnClickListener {
            animateClick(it)
            showLanguageDialog()
        }

        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean(KEY_NOTIFICATIONS, isChecked)?.apply()
            Toast.makeText(this, if (isChecked) "Уведомления включены" else "Уведомления выключены", Toast.LENGTH_SHORT).show()
            switchNotifications?.let { animateSwitch(it) }
        }

        switchTheme?.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean(KEY_THEME, isChecked)?.apply()
            applyTheme()
            Toast.makeText(this, if (isChecked) "Тёмная тема включена" else "Светлая тема включена", Toast.LENGTH_SHORT).show()
            switchTheme?.let { animateSwitch(it) }
        }

        llReset?.setOnClickListener {
            animateClick(it)
            showResetDialog()
        }

        llInfo?.setOnClickListener {
            animateClick(it)
            showInfoDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Русский", "English", "Қазақша", "Deutsch", "Français")
        val currentLanguage = tvLanguage?.text?.toString() ?: "Русский"
        var selectedIndex = languages.indexOf(currentLanguage)
        if (selectedIndex == -1) selectedIndex = 0

        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите язык")
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                tvLanguage?.text = selectedLanguage
                sharedPref?.edit()?.putString(KEY_LANGUAGE, selectedLanguage)?.apply()
                Toast.makeText(this, "Язык: $selectedLanguage", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Сброс данных")
            .setMessage("Вы уверены, что хотите сбросить все данные? Это действие необратимо.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Сбросить") { _, _ ->
                sharedPref?.edit()?.clear()?.apply()
                switchNotifications?.isChecked = true
                switchTheme?.isChecked = false
                tvLanguage?.text = "Русский"
                Toast.makeText(this, "Все данные сброшены", Toast.LENGTH_LONG).show()
                recreate()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showInfoDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Информация")
            .setMessage("""
                Приложение v1.0.0
                
                Разработчик: Your Name
                Email: your@email.com
                
                © 2024 Все права защищены
            """.trimIndent())
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun applyTheme() {
        val prefs = sharedPref
        val darkTheme = if (prefs != null) {
            prefs.getBoolean(KEY_THEME, false)
        } else {
            false
        }

        AppCompatDelegate.setDefaultNightMode(
            if (darkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun animateSwitch(switch: Switch) {
        switch.animate()
            .setDuration(300)
            .withEndAction { switch.rotation = 0f }
            .start()
    }

    private fun animateClick(view: android.view.View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setupBottomNavigation() {
        val menuHome = findViewById<ImageView>(R.id.menu_home)
        val menuBook = findViewById<ImageView>(R.id.menu_book)
        val menuHistory = findViewById<ImageView>(R.id.menu_history)
        val menuProfile = findViewById<ImageView>(R.id.menu_profile)

        // Подсвечиваем текущий пункт
        menuProfile?.setColorFilter(ContextCompat.getColor(this, android.R.color.white))

        menuHome?.setOnClickListener {
            animateClick(it)
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        }

        menuBook?.setOnClickListener {
            animateClick(it)
            Toast.makeText(this, "Book", Toast.LENGTH_SHORT).show()
        }

        menuHistory?.setOnClickListener {
            animateClick(it)
            Toast.makeText(this, "History", Toast.LENGTH_SHORT).show()
        }

        menuProfile?.setOnClickListener {
            animateClick(it)
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("notifications", switchNotifications?.isChecked ?: false)
        outState.putBoolean("theme", switchTheme?.isChecked ?: false)
        outState.putString("language", tvLanguage?.text?.toString() ?: "Русский")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        switchNotifications?.isChecked = savedInstanceState.getBoolean("notifications")
        switchTheme?.isChecked = savedInstanceState.getBoolean("theme")
        tvLanguage?.text = savedInstanceState.getString("language")
    }
}