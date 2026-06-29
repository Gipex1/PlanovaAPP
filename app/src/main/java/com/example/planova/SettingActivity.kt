package com.example.planova

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class SettingActivity : BaseActivity() {

    private var backArrow: ImageView? = null
    private var llLanguage: LinearLayout? = null
    private var llNotifications: LinearLayout? = null
    private var llTheme: LinearLayout? = null
    private var llReset: LinearLayout? = null
    private var llInfo: LinearLayout? = null
    private var switchNotifications: Switch? = null
    private var switchTheme: Switch? = null
    private var tvLanguage: TextView? = null
    private var sharedPref: SharedPreferences? = null

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_THEME = "dark_theme"
        private const val KEY_LANGUAGE = "language"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        val currentLang = prefs.getString(KEY_LANGUAGE, "ru") ?: "ru"
        tvLanguage?.text = when (currentLang) {
            "en" -> "English"
            "kk" -> "Қазақша"
            "de" -> "Deutsch"
            "fr" -> "Français"
            else -> "Русский"
        }
    }

    private fun setupClickListeners() {
        backArrow?.setOnClickListener {
            animateClick(it)
            finishWithAnimation()
        }

        llLanguage?.setOnClickListener {
            animateClick(it)
            showLanguageDialog()
        }

        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean(KEY_NOTIFICATIONS, isChecked)?.apply()
            Toast.makeText(
                this,
                if (isChecked) getString(R.string.notifications_enabled)
                else getString(R.string.notifications_disabled),
                Toast.LENGTH_SHORT
            ).show()
            switchNotifications?.let { animateSwitch(it) }
        }

        switchTheme?.setOnCheckedChangeListener { _, isChecked ->
            sharedPref?.edit()?.putBoolean(KEY_THEME, isChecked)?.apply()

            // Применяем тему
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(
                this,
                if (isChecked) getString(R.string.dark_theme_enabled)
                else getString(R.string.dark_theme_disabled),
                Toast.LENGTH_SHORT
            ).show()

            switchTheme?.let { animateSwitch(it) }
            restartApp()
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
        val langCodes = arrayOf("ru", "en", "kk", "de", "fr")
        val currentLang = sharedPref?.getString(KEY_LANGUAGE, "ru") ?: "ru"
        var selectedIndex = langCodes.indexOf(currentLang)
        if (selectedIndex == -1) selectedIndex = 0

        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите язык")
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val selectedLangCode = langCodes[which]

                sharedPref?.edit()?.putString(KEY_LANGUAGE, selectedLangCode)?.apply()
                setLocale(selectedLangCode)
                tvLanguage?.text = languages[which]
                restartApp()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.reset_title))
            .setMessage(getString(R.string.reset_message))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.reset)) { _, _ ->
                sharedPref?.edit()?.clear()?.apply()
                switchNotifications?.isChecked = true
                switchTheme?.isChecked = false
                Toast.makeText(this, getString(R.string.data_reset), Toast.LENGTH_LONG).show()
                restartApp()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showInfoDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.info))
            .setMessage("""
                ${getString(R.string.app_name)} v1.0.0
                
                ${getString(R.string.developer)}: Your Name
                Email: your@email.com
                
                © 2024 ${getString(R.string.all_rights)}
            """.trimIndent())
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }

    private fun animateSwitch(switch: Switch) {
        switch.animate()
            .rotation(360f)
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

        menuProfile?.setColorFilter(ContextCompat.getColor(this, android.R.color.white))

        menuHome?.setOnClickListener {
            animateClick(it)
            startActivity(Intent(this, Home::class.java))
            finishWithAnimation()
        }

        menuBook?.setOnClickListener {
            animateClick(it)
            startActivity(Intent(this, My_Goals::class.java))
        }

        menuHistory?.setOnClickListener {
            animateClick(it)
            startActivity(Intent(this, My_Goals_Complat::class.java))
        }

        menuProfile?.setOnClickListener {
            animateClick(it)
            startActivity(Intent(this, Activity_User::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("notifications", switchNotifications?.isChecked ?: false)
        outState.putBoolean("theme", switchTheme?.isChecked ?: false)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        switchNotifications?.isChecked = savedInstanceState.getBoolean("notifications")
        switchTheme?.isChecked = savedInstanceState.getBoolean("theme")
    }
}