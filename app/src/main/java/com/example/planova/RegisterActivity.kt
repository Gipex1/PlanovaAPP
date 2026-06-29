package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.planova.data.RegisterRequest
import com.example.planova.databinding.ActivityRegisterBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefs(this)

        setupTextWatchers()
        setupClickListeners()
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                clearFieldError(binding.editText1)
                clearFieldError(binding.editText2)
                clearFieldError(binding.editText3)
                clearFieldError(binding.editText4)
            }
        }
        binding.editText1.addTextChangedListener(watcher)
        binding.editText2.addTextChangedListener(watcher)
        binding.editText3.addTextChangedListener(watcher)
        binding.editText4.addTextChangedListener(watcher)
    }

    private fun clearFieldError(editText: EditText) {
        val normalBg = ContextCompat.getDrawable(this, R.drawable.edit)
        if (editText.background != normalBg) {
            editText.background = normalBg
        }
    }

    private fun setupClickListeners() {
        binding.button.setOnClickListener {
            performRegistration()
        }
        binding.textView8.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val username = binding.editText1.text.toString().trim()
        val email = binding.editText2.text.toString().trim()
        val password = binding.editText3.text.toString().trim()
        val confirmPassword = binding.editText4.text.toString().trim()

        if (!validateInput(username, email, password, confirmPassword)) return

        binding.button.isEnabled = false
        binding.button.text = "Регистрация..."

        ApiClient.apiService.register(RegisterRequest(username, email, password))
            .enqueue(object : Callback<Map<String, Long>> {
                override fun onResponse(
                    call: Call<Map<String, Long>>,
                    response: Response<Map<String, Long>>
                ) {
                    binding.button.isEnabled = true
                    binding.button.text = "Зарегистрироваться"

                    if (response.isSuccessful) {
                        val userId = response.body()?.get("userId")
                        if (userId != null) {
                            prefs.saveUserData(
                                userId = userId,
                                username = username,
                                email = email,
                                password = password,
                                remember = true
                            )
                            Toast.makeText(
                                this@RegisterActivity,
                                "Регистрация успешна!",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@RegisterActivity, Home::class.java))
                            finish()
                        } else {
                            showError("Не удалось получить userId")
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()?.let { parseError(it) }
                            ?: "Ошибка регистрации"
                        showFieldError(binding.editText2, errorMsg)
                    }
                }

                override fun onFailure(call: Call<Map<String, Long>>, t: Throwable) {
                    binding.button.isEnabled = true
                    binding.button.text = "Зарегистрироваться"
                    showError("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun validateInput(username: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            showFieldError(binding.editText1, "Введите имя пользователя")
            isValid = false
        } else if (username.length < 3) {
            showFieldError(binding.editText1, "Имя должно быть не менее 3 символов")
            isValid = false
        }

        if (email.isEmpty()) {
            showFieldError(binding.editText2, "Введите email")
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(binding.editText2, "Неверный формат email")
            isValid = false
        }

        if (password.isEmpty()) {
            showFieldError(binding.editText3, "Введите пароль")
            isValid = false
        } else if (password.length < 6) {
            showFieldError(binding.editText3, "Пароль должен быть не менее 6 символов")
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            showFieldError(binding.editText4, "Подтвердите пароль")
            isValid = false
        } else if (password != confirmPassword) {
            showFieldError(binding.editText4, "Пароли не совпадают")
            isValid = false
        }

        return isValid
    }

    private fun showFieldError(editText: EditText, message: String) {
        editText.background = createErrorDrawable()
        Snackbar.make(binding.button, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun createErrorDrawable(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 10.dpToPx().toFloat()
            setStroke(2.dpToPx(), ContextCompat.getColor(this@RegisterActivity, R.color.error))
            setColor(ContextCompat.getColor(this@RegisterActivity, R.color.dark_surface))
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
        Snackbar.make(binding.button, msg, Snackbar.LENGTH_LONG).show()
    }
}