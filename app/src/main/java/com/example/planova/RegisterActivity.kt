package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.planova.data.RegisterRequest
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLoginLink: TextView

    // Требования к паролю
    private lateinit var ivMinLength: ImageView
    private lateinit var ivDigit: ImageView
    private lateinit var ivUpperCase: ImageView
    private lateinit var tvMinLength: TextView
    private lateinit var tvDigit: TextView
    private lateinit var tvUpperCase: TextView

    private lateinit var prefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        prefs = SharedPrefs(this)

        setupRequirementTexts()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.editText1)
        etEmail = findViewById(R.id.editText2)
        etPassword = findViewById(R.id.editText3)
        etConfirmPassword = findViewById(R.id.editText4)
        btnRegister = findViewById(R.id.button)
        tvLoginLink = findViewById(R.id.textView8)

        ivMinLength = findViewById(R.id.imageView5)
        ivDigit = findViewById(R.id.imageView6)
        ivUpperCase = findViewById(R.id.imageView7)
        tvMinLength = findViewById(R.id.textView4)
        tvDigit = findViewById(R.id.textView5)
        tvUpperCase = findViewById(R.id.textView6)

        // начальные иконки (серые)
        setRequirementIcon(ivMinLength, false)
        setRequirementIcon(ivDigit, false)
        setRequirementIcon(ivUpperCase, false)
        ivMinLength.setColorFilter(ContextCompat.getColor(this, R.color.dark_text_secondary))
        ivDigit.setColorFilter(ContextCompat.getColor(this, R.color.dark_text_secondary))
        ivUpperCase.setColorFilter(ContextCompat.getColor(this, R.color.dark_text_secondary))
    }

    private fun setupRequirementTexts() {
        tvMinLength.text = "Минимум 8 символов"
        tvDigit.text = "Хотя бы одна цифра"
        tvUpperCase.text = "Хотя бы одна заглавная буква"
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
                if (etConfirmPassword.text.isNotEmpty()) {
                    checkPasswordsMatch()
                }
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkPasswordsMatch()
            }
        })

        etUsername.addTextChangedListener(clearErrorWatcher(etUsername))
        etEmail.addTextChangedListener(clearErrorWatcher(etEmail))
    }

    private fun clearErrorWatcher(editText: EditText) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (editText.background?.constantState !=
                ContextCompat.getDrawable(this@RegisterActivity, R.drawable.edit)?.constantState
            ) {
                editText.background = ContextCompat.getDrawable(this@RegisterActivity, R.drawable.edit)
            }
        }
    }

    private fun updatePasswordRequirements(password: String) {
        val minLengthOk = password.length >= 8
        val digitOk = password.any { it.isDigit() }
        val upperCaseOk = password.any { it.isUpperCase() }

        updateRequirementWithAnimation(ivMinLength, tvMinLength, minLengthOk)
        updateRequirementWithAnimation(ivDigit, tvDigit, digitOk)
        updateRequirementWithAnimation(ivUpperCase, tvUpperCase, upperCaseOk)
    }

    private fun updateRequirementWithAnimation(imageView: ImageView, textView: TextView, isMet: Boolean) {
        // Можно без анимации, если хочешь упростить – просто меняем иконку и цвет
        setRequirementIcon(imageView, isMet)
        val colorRes = if (isMet) R.color.success else R.color.error
        val textColorRes = if (isMet) R.color.success else R.color.dark_text_secondary
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
            setStroke(2.dpToPx(), ContextCompat.getColor(this@RegisterActivity, R.color.error))
            setColor(ContextCompat.getColor(this@RegisterActivity, R.color.dark_surface))
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            performRegistration()
        }
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun performRegistration() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateUsername(username)) return
        if (!validateEmail(email)) return
        if (!validatePassword(password)) return
        if (!validateConfirmPassword(password, confirmPassword)) return

        // Вызов API
        btnRegister.isEnabled = false
        btnRegister.text = "Регистрация..."

        ApiClient.apiService.register(RegisterRequest(username, email, password))
            .enqueue(object : Callback<Map<String, Long>> {
                override fun onResponse(
                    call: Call<Map<String, Long>>,
                    response: Response<Map<String, Long>>
                ) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Зарегистрироваться"

                    if (response.isSuccessful) {
                        val userId = response.body()?.get("userId")
                        if (userId != null) {
                            // сохраняем userId
                            prefs.saveUserData(
                                userId = userId,
                                email = email,
                                password = password,
                                remember = true
                            )
                            // переход на экран подтверждения (или сразу на Home)
                            val intent = Intent(this@RegisterActivity, Home::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        } else {
                            showError("Не удалось получить userId")
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()?.let { parseError(it) }
                            ?: "Ошибка регистрации"
                        showError(errorMsg)
                    }
                }

                override fun onFailure(call: Call<Map<String, Long>>, t: Throwable) {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Зарегистрироваться"
                    showError("Ошибка сети: ${t.message}")
                }
            })
    }

    // Валидация (такая же, как была)
    private fun validateUsername(username: String): Boolean {
        if (username.isEmpty()) {
            showFieldError(etUsername, "Введите имя пользователя")
            return false
        }
        if (username.length < 3) {
            showFieldError(etUsername, "Минимум 3 символа")
            return false
        }
        resetFieldBackground(etUsername)
        return true
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
        resetFieldBackground(etEmail)
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.isEmpty()) {
            showFieldError(etPassword, "Введите пароль")
            return false
        }
        if (password.length < 8) {
            showFieldError(etPassword, "Пароль должен быть не менее 8 символов")
            return false
        }
        if (!password.any { it.isDigit() }) {
            showFieldError(etPassword, "Пароль должен содержать хотя бы одну цифру")
            return false
        }
        if (!password.any { it.isUpperCase() }) {
            showFieldError(etPassword, "Пароль должен содержать заглавную букву")
            return false
        }
        resetFieldBackground(etPassword)
        return true
    }

    private fun validateConfirmPassword(password: String, confirm: String): Boolean {
        if (confirm.isEmpty()) {
            showFieldError(etConfirmPassword, "Подтвердите пароль")
            return false
        }
        if (password != confirm) {
            showFieldError(etConfirmPassword, "Пароли не совпадают")
            return false
        }
        resetFieldBackground(etConfirmPassword)
        return true
    }

    private fun showFieldError(editText: EditText, message: String) {
        editText.background = createErrorDrawable()
        Snackbar.make(btnRegister, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun resetFieldBackground(editText: EditText) {
        editText.background = ContextCompat.getDrawable(this, R.drawable.edit)
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
        Snackbar.make(btnRegister, msg, Snackbar.LENGTH_LONG).show()
    }
}