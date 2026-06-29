package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.planova.data.LoginRequest
import com.example.planova.data.UserResponse
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : BaseActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var switchRemember: Switch
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    private lateinit var prefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        prefs = SharedPrefs(this)

        loadSavedCredentials()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.editText1)
        etPassword = findViewById(R.id.editText2)
        btnLogin = findViewById(R.id.button)
        switchRemember = findViewById(R.id.switch1)
        tvForgotPassword = findViewById(R.id.textView4)
        tvRegister = findViewById(R.id.textView6)
    }

    private fun loadSavedCredentials() {
        val savedEmail = prefs.getEmail() ?: ""
        val savedPassword = prefs.getPassword() ?: ""
        val remember = prefs.isRemember()

        if (remember && savedEmail.isNotEmpty()) {
            etEmail.setText(savedEmail)
            etPassword.setText(savedPassword)
            switchRemember.isChecked = true
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                clearFieldError(etEmail)
                clearFieldError(etPassword)
            }
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
    }

    private fun clearFieldError(editText: EditText) {
        val normalBg = ContextCompat.getDrawable(this, R.drawable.edit)
        if (editText.background != normalBg) {
            editText.background = normalBg
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateEmail(email) || !validatePassword(password)) return

        btnLogin.isEnabled = false
        btnLogin.text = "Вход..."

        ApiClient.apiService.login(LoginRequest(email, password))
            .enqueue(object : Callback<Map<String, Long>> {
                override fun onResponse(
                    call: Call<Map<String, Long>>,
                    response: Response<Map<String, Long>>
                ) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Войти"

                    if (response.isSuccessful) {
                        val userId = response.body()?.get("userId")
                        if (userId != null) {
                            // Сохраняем userId и email сразу (пароль – если нужно)
                            prefs.saveUserData(
                                userId = userId,
                                username = "", // временно, пока не загрузим с сервера
                                email = email,
                                password = password,
                                remember = switchRemember.isChecked
                            )
                            // Теперь запрашиваем данные пользователя, чтобы получить username
                            fetchUserProfile(userId)
                        } else {
                            showError("Не удалось получить userId")
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()?.let { parseError(it) }
                            ?: "Ошибка входа"
                        showFieldError(etPassword, errorMsg)
                    }
                }

                override fun onFailure(call: Call<Map<String, Long>>, t: Throwable) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Войти"
                    showError("Ошибка сети: ${t.message}")
                }
            })
    }

    /**
     * Запрашивает профиль пользователя, чтобы получить username и обновить SharedPrefs.
     */
    private fun fetchUserProfile(userId: Long) {
        ApiClient.apiService.getMe(userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        val username = user.username ?: ""
                        // Обновляем запись в SharedPrefs с корректным username
                        prefs.saveUserData(
                            userId = userId,
                            username = username,
                            email = prefs.getEmail() ?: "",
                            password = prefs.getPassword(),
                            remember = switchRemember.isChecked
                        )
                    }
                }
                // В любом случае переходим в Home, даже если username не загрузился
                startActivity(Intent(this@MainActivity, Home::class.java))
                finish()
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                // Если не удалось загрузить профиль, всё равно переходим в Home
                startActivity(Intent(this@MainActivity, Home::class.java))
                finish()
            }
        })
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            showFieldError(etEmail, "Введите email")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(etEmail, "Неверный формат email")
            return false
        }
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.isEmpty()) {
            showFieldError(etPassword, "Введите пароль")
            return false
        }
        if (password.length < 6) {
            showFieldError(etPassword, "Пароль должен быть не менее 6 символов")
            return false
        }
        return true
    }

    private fun showFieldError(editText: EditText, message: String) {
        editText.background = createErrorDrawable()
        Snackbar.make(btnLogin, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun createErrorDrawable(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 10.dpToPx().toFloat()
            setStroke(2.dpToPx(), ContextCompat.getColor(this@MainActivity, R.color.error))
            setColor(ContextCompat.getColor(this@MainActivity, R.color.dark_surface))
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun parseError(json: String): String {
        return try {
            val obj = com.google.gson.JsonParser().parse(json).asJsonObject
            obj.get("error")?.asString ?: "Ошибка"
        } catch (_: Exception) {
            "Ошибка"
        }
    }

    private fun showError(msg: String) {
        Snackbar.make(btnLogin, msg, Snackbar.LENGTH_LONG).show()
    }
}