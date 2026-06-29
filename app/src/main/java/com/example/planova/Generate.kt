package com.example.planova

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Generate : BaseActivity() {

    private lateinit var logoInnerCircle: ImageView
    private lateinit var cancelButton: MaterialButton
    private lateinit var prefs: SharedPrefs

    private var goal: String = ""
    private var isApiResponded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = SharedPrefs(this)
        logoInnerCircle = findViewById(R.id.logoInnerCircle)
        cancelButton = findViewById(R.id.cancelButton)

        goal = intent.getStringExtra("goal") ?: ""

        if (goal.isEmpty()) {
            Toast.makeText(this, "Введите цель", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startInnerCircleRotation()
        generatePlan()

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun generatePlan() {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ApiClient.apiService.generate(userId, GenerateRequest(goal))
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
                        val errorMsg = response.errorBody()?.string()?.let { parseError(it) }
                            ?: "Ошибка генерации"
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

        val intent = Intent(this, CheckPlan::class.java)
        intent.putExtra("goal", goal)
        intent.putExtra("title", planData.title)
        intent.putExtra("description", planData.description)
        intent.putExtra("targetDate", planData.targetDate)

        val gson = Gson()
        val stepsJson = gson.toJson(planData.steps)
        intent.putExtra("stepsDtoJson", stepsJson)

        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        finish()
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
}