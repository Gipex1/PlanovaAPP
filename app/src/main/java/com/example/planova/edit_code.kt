package com.example.planova

import android.content.Intent
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class edit_code : BaseActivity() {

    private lateinit var codeFields: List<EditText>
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvResend: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvTimer: TextView

    private val correctCode = "12345"
    private val enteredCode = CharArray(5)

    private var timer: CountDownTimer? = null
    private val delay = 30_000L

    private var errorState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_code)

        btnConfirm = findViewById(R.id.btn_confirm)
        tvResend = findViewById(R.id.tv_resend)
        tvTitle = findViewById(R.id.tv_title)
        tvTimer = findViewById(R.id.tv_timer)

        codeFields = listOf(
            findViewById(R.id.et_code_1),
            findViewById(R.id.et_code_2),
            findViewById(R.id.et_code_3),
            findViewById(R.id.et_code_4),
            findViewById(R.id.et_code_5)
        )

        setupOtp()
        setupTitle()
        startTimer()

        btnConfirm.setOnClickListener { checkCode() }
        tvResend.setOnClickListener {
            if (canResend()) {
                // Здесь вызывайте API повторной отправки кода
                resetTimer()
                Snackbar.make(btnConfirm, "Код отправлен повторно", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ================= OTP =================

    private fun setupOtp() {
        codeFields.forEachIndexed { index, et ->
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    clearErrorVisual()

                    val text = s.toString()

                    if (text.length > 1) {
                        fillAll(text)
                        return
                    }

                    if (text.isNotEmpty()) {
                        enteredCode[index] = text[0]
                        animateField(et)

                        if (index < 4) {
                            codeFields[index + 1].requestFocus()
                        } else {
                            hideKeyboard()
                        }
                    } else {
                        enteredCode[index] = ' '
                    }
                }
            })

            et.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (et.text.isEmpty() && index > 0) {
                        codeFields[index - 1].requestFocus()
                        codeFields[index - 1].setText("")
                    }
                }
                false
            }
        }
    }

    private fun fillAll(code: String) {
        code.take(5).forEachIndexed { i, c ->
            enteredCode[i] = c
            codeFields[i].setText(c.toString())
            animateField(codeFields[i])
        }
        hideKeyboard()
    }

    // ================= ANIMATION =================

    private fun animateField(view: View) {
        val anim = ScaleAnimation(
            0.85f, 1f,
            0.85f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 120
            interpolator = OvershootInterpolator()
        }
        view.startAnimation(anim)
    }

    // ================= CHECK =================

    private fun checkCode() {
        val code = enteredCode.concatToString()
        if (code == correctCode) {
            success()
        } else {
            error()
        }
    }

    // ================= ERROR =================

    private fun error() {
        errorState = true
        val errorBackground = createErrorDrawable()

        codeFields.forEach {
            it.background = errorBackground
        }

        shake()

        Snackbar.make(btnConfirm, "Неверный код", Snackbar.LENGTH_SHORT).show()
    }

    private fun clearErrorVisual() {
        if (!errorState) return
        errorState = false

        val normalBackground = ContextCompat.getDrawable(this, R.drawable.otp_box_bg)
        codeFields.forEach {
            it.background = normalBackground
        }
    }

    private fun createErrorDrawable(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 14.dpToPx().toFloat()
            setStroke(2.dpToPx(), ContextCompat.getColor(this@edit_code, R.color.error))
            setColor(ContextCompat.getColor(this@edit_code, R.color.dark_surface)) // #0A100F
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ================= SUCCESS =================

    private fun success() {
        val successBackground = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 14.dpToPx().toFloat()
            setStroke(2.dpToPx(), ContextCompat.getColor(this@edit_code, R.color.success))
            setColor(ContextCompat.getColor(this@edit_code, R.color.dark_surface))
        }

        codeFields.forEach {
            it.background = successBackground
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Home::class.java))
            finish()
        }, 400)
    }

    // ================= SHAKE =================

    private fun shake() {
        val anim = TranslateAnimation(-15f, 15f, 0f, 0f).apply {
            duration = 70
            repeatCount = 4
            repeatMode = Animation.REVERSE
        }
        codeFields.forEach { it.startAnimation(anim) }
    }

    // ================= TIMER =================

    private fun startTimer() {
        timer?.cancel()
        updateResendState(false)

        tvTimer.visibility = View.VISIBLE
        tvTimer.text = "Повторная отправка через ${delay / 1000} с"
        tvTimer.alpha = 1f

        timer = object : CountDownTimer(delay, 1000) {
            override fun onTick(ms: Long) {
                val sec = ms / 1000
                tvTimer.text = "Повторная отправка через ${sec} с"
                // Убрана пульсирующая анимация
            }

            override fun onFinish() {
                tvTimer.visibility = View.GONE
                updateResendState(true)
            }
        }.start()
    }

    private fun resetTimer() {
        startTimer()
    }

    private fun canResend(): Boolean = tvResend.isClickable && tvResend.alpha == 1f

    private fun updateResendState(enabled: Boolean) {
        tvResend.isClickable = enabled
        tvResend.alpha = if (enabled) 1f else 0.5f
    }

    // ================= HELPERS =================

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun setupTitle() {
        tvTitle.text = "Введите код подтверждения"
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}