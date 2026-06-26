package com.example.planova

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.planova.data.GenerateRequest
import com.example.planova.data.GenerateResponse
import com.example.planova.data.StepDto
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Generate : AppCompatActivity() {

    private lateinit var logoInnerCircle: ImageView
    private lateinit var cancelButton: MaterialButton

    private lateinit var prefs: SharedPrefs

    private var goal: String = ""
    private var isApiResponded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate)

        prefs = SharedPrefs(this)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logoInnerCircle = findViewById(R.id.logoInnerCircle)
        cancelButton = findViewById(R.id.cancelButton)

        // Получаем цель
        goal = intent.getStringExtra("goal") ?: ""
        Log.d("Generate", "goal = '$goal'") // посмотри, что приходит

        // Валидация
        if (!validateGoal(goal)) {
            Toast.makeText(this, "Введите осмысленную цель (минимум 3 буквы)", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Запускаем вращение
        startInnerCircleRotation()

        // Отправляем запрос
        generatePlan(goal)

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun validateGoal(goal: String): Boolean {
        if (goal.length < 3) return false
        return goal.any { it.isLetter() }
    }

    private fun generatePlan(goal: String) {
        val userId = prefs.getUserId()
        Log.d("Generate", "userId = $userId")
        if (userId == null) {
            showError("Сначала войдите в аккаунт")
            return
        }

        ApiClient.apiService.generate(GenerateRequest(goal))
            .enqueue(object : Callback<GenerateResponse> {
                override fun onResponse(
                    call: Call<GenerateResponse>,
                    response: Response<GenerateResponse>
                ) {
                    if (response.isSuccessful) {
                        val planData = response.body()
                        if (planData != null) {
                            onApiResponse(planData)
                        } else {
                            showError("Пустой ответ от сервера")
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string()
                            ?.let { parseError(it) } ?: "Ошибка генерации"
                        showError(errorMsg)
                    }
                }

                override fun onFailure(call: Call<GenerateResponse>, t: Throwable) {
                    showError("Ошибка сети: ${t.message}")
                }
            })
    }

    private fun onApiResponse(planData: GenerateResponse) {
        if (isApiResponded) return
        isApiResponded = true

        accelerateInnerCircleRotation()
        proceedToNextScreen(planData)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun parseError(json: String): String {
        return try {
            val obj = com.google.gson.JsonParser().parse(json).asJsonObject
            obj.get("error")?.asString ?: "Ошибка"
        } catch (_: Exception) {
            "Ошибка"
        }
    }

    // ========== АНИМАЦИИ ==========

    private fun startInnerCircleRotation() {
        val rotate = ObjectAnimator.ofFloat(logoInnerCircle, "rotation", 0f, 360f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotate.start()
    }

    private fun accelerateInnerCircleRotation() {
        logoInnerCircle.animate().cancel()
        val fastRotate = ObjectAnimator.ofFloat(logoInnerCircle, "rotation", 0f, 360f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        fastRotate.start()
    }

    // ========== ПЕРЕХОД ==========

    private fun proceedToNextScreen(planData: GenerateResponse) {
        val intent = Intent(this, CheckPlan::class.java)
        intent.putExtra("goal", goal)
        intent.putExtra("title", planData.title)
        intent.putExtra("description", planData.description)
        intent.putExtra("targetDate", planData.targetDate)

        val gson = com.google.gson.Gson()
        val stepsJson = gson.toJson(planData.steps)
        intent.putExtra("stepsJson", stepsJson)

        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отменяем анимацию, чтобы не было утечек
        logoInnerCircle.animate().cancel()
    }
}