package com.example.planova

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.planova.data.ResetPasswordRequest
import com.example.planova.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnConfirm: Button
    private lateinit var tvResend: TextView

    private lateinit var ivMinLength: ImageView
    private lateinit var ivDigit: ImageView
    private lateinit var ivUpperCase: ImageView
    private lateinit var tvMinLength: TextView
    private lateinit var tvDigit: TextView
    private lateinit var tvUpperCase: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnConfirm = findViewById(R.id.btn_confirm)
        tvResend = findViewById(R.id.tv_resend)

        ivMinLength = findViewById(R.id.imageView5)
        ivDigit = findViewById(R.id.imageView6)
        ivUpperCase = findViewById(R.id.imageView7)
        tvMinLength = findViewById(R.id.textView4)
        tvDigit = findViewById(R.id.textView5)
        tvUpperCase = findViewById(R.id.textView6)

        // Начальное состояние иконок – серые
        setRequirementIcon(ivMinLength, false)
        setRequirementIcon(ivDigit, false)
        setRequirementIcon(ivUpperCase, false)
        val gray = ContextCompat.getColor(this, R.color.dark_text_secondary)
        ivMinLength.setColorFilter(gray)
        ivDigit.setColorFilter(gray)
        ivUpperCase.setColorFilter(gray)
    }

    private fun setRequirementIcon(imageView: ImageView, isMet: Boolean) {
        val drawableRes = if (isMet) R.drawable.ic_accept2 else R.drawable.icc_error
        imageView.setImageDrawable(ContextCompat.getDrawable(this, drawableRes))
    }

    private fun setupTextWatchers() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                updatePasswordRequirements(password)
                checkPasswordsMatch()
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkPasswordsMatch()
            }
        })

        // Очистка ошибок при вводе
        etEmail.addTextChangedListener(clearErrorWatcher(etEmail))
        etUsername.addTextChangedListener(clearErrorWatcher(etUsername))
    }

    private fun clearErrorWatcher(editText: EditText) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            clearFieldError(editText)
        }
    }

    private fun updatePasswordRequirements(password: String) {
        val minLengthOk = password.length >= 8
        val digitOk = password.any { it.isDigit() }
        val upperCaseOk = password.any { it.isUpperCase() }

        updateRequirement(ivMinLength, tvMinLength, minLengthOk)
        updateRequirement(ivDigit, tvDigit, digitOk)
        updateRequirement(ivUpperCase, tvUpperCase, upperCaseOk)
    }

    private fun updateRequirement(imageView: ImageView, textView: TextView, isMet: Boolean) {
        val colorRes = if (isMet) R.color.success else R.color.error
        val textColorRes = if (isMet) R.color.success else R.color.dark_text_secondary
        setRequirementIcon(imageView, isMet)
        imageView.setColorFilter(ContextCompat.getColor(this, colorRes))
        textView.setTextColor(ContextCompat.getColor(this, textColorRes))
    }

    private fun checkPasswordsMatch() {
        val password = etPassword.text.toString()
        val confirm = etConfirmPassword.text.toString()
        if (confirm.isNotEmpty() && password != confirm) {
            etConfirmPassword.background = createErrorDrawable()
        } else {
            etConfirmPassword.background = ContextCompat.getDrawable(this, R.drawable.edit)
        }
    }

    private fun createErrorDrawable(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 10.dpToPx().toFloat()
            setStroke(2.dpToPx(), ContextCompat.getColor(this@ForgotPasswordActivity, R.color.error))
            setColor(ContextCompat.getColor(this@ForgotPasswordActivity, R.color.dark_surface))
        }
    }

    private fun clearFieldError(editText: EditText) {
        val normalBg = ContextCompat.getDrawable(this, R.drawable.edit)
        if (editText.background != normalBg) {
            editText.background = normalBg
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupClickListeners() {
        btnConfirm.setOnClickListener {
            resetPassword()
        }

        tvResend.setOnClickListener {
            finish() // возврат на экран логина
        }
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm = etConfirmPassword.text.toString().trim()

        // Валидация
        if (email.isEmpty()) {
            showError("Введите email")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Неверный формат email")
            return
        }
        if (username.isEmpty()) {
            showError("Введите имя пользователя")
            return
        }
        if (password.isEmpty()) {
            showError("Введите новый пароль")
            return
        }
        if (password.length < 8) {
            showError("Пароль должен быть не менее 8 символов")
            return
        }
        if (!password.any { it.isDigit() }) {
            showError("Пароль должен содержать цифру")
            return
        }
        if (!password.any { it.isUpperCase() }) {
            showError("Пароль должен содержать заглавную букву")
            return
        }
        if (confirm.isEmpty()) {
            showError("Подтвердите пароль")
            return
        }
        if (password != confirm) {
            showError("Пароли не совпадают")
            return
        }

        btnConfirm.isEnabled = false
        btnConfirm.text = "Смена..."

        ApiClient.apiService.resetPassword(ResetPasswordRequest(email, username, password))
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Подтвердить"
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, "Пароль изменён", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val error = response.errorBody()?.string()?.let { parseError(it) } ?: "Ошибка"
                        showError(error)
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Подтвердить"
                    showError("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun parseError(json: String): String {
        return try {
            val obj = com.google.gson.JsonParser().parse(json).asJsonObject
            obj.get("error")?.asString ?: "Ошибка"
        } catch (_: Exception) {
            "Ошибка"
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}