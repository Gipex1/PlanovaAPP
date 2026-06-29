package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.planova.data.PlanResponse
import com.example.planova.data.StepResponse
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Activity_User : BaseActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvPlansCount: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация UI
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvPlansCount = findViewById(R.id.tvPlansCount)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        progressBar = findViewById(R.id.progressBar)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)

        // Навигационные кнопки
        findViewById<ImageView>(R.id.menu_home).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
        findViewById<ImageView>(R.id.menu_book).setOnClickListener {
            startActivity(Intent(this, My_Goals::class.java))
        }
        findViewById<ImageView>(R.id.menu_history).setOnClickListener {
            startActivity(Intent(this, My_Goals_Complat::class.java))
        }
        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
        findViewById<ImageView>(R.id.ivEditProfile).setOnClickListener {
            Toast.makeText(this, "Редактирование профиля в разработке", Toast.LENGTH_SHORT).show()
        }

        // Загружаем данные пользователя
        loadUserInfo()

        // Загружаем планы и статистику
        loadPlansAndStats()
    }

    private fun loadUserInfo() {
        val prefs = SharedPrefs(this)
        val username = prefs.getUsername() ?: "Пользователь"
        val email = prefs.getEmail() ?: "email@example.com"

        tvUserName.text = username
        tvUserEmail.text = formatEmail(email)
    }

    private fun formatEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex == -1) return email

        val localPart = email.substring(0, atIndex)
        val domain = email.substring(atIndex)

        return if (localPart.length <= 4) {
            localPart.take(1) + "*****" + domain
        } else {
            localPart.take(4) + "*****" + domain
        }
    }

    private fun loadPlansAndStats() {
        val prefs = SharedPrefs(this)
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем список планов (без шагов)
        ApiClient.apiService.getPlans(userId).enqueue(object : Callback<List<PlanResponse>> {
            override fun onResponse(call: Call<List<PlanResponse>>, response: Response<List<PlanResponse>>) {
                if (response.isSuccessful) {
                    val plans = response.body() ?: emptyList()
                    if (plans.isEmpty()) {
                        updateStatistics(emptyList())
                        return
                    }

                    // Загружаем каждый план отдельно, чтобы получить шаги
                    var loadedCount = 0
                    val fullPlans = mutableListOf<PlanResponse>()

                    for (planSummary in plans) {
                        ApiClient.apiService.getPlan(userId, planSummary.id)
                            .enqueue(object : Callback<PlanResponse> {
                                override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                                    loadedCount++
                                    if (response.isSuccessful) {
                                        fullPlans.add(response.body()!!)
                                    } else {
                                        // Если не удалось загрузить детали – добавляем план без шагов (считаем прогресс 0)
                                        fullPlans.add(planSummary)
                                    }
                                    if (loadedCount == plans.size) {
                                        updateStatistics(fullPlans)
                                    }
                                }

                                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                                    loadedCount++
                                    fullPlans.add(planSummary)
                                    if (loadedCount == plans.size) {
                                        updateStatistics(fullPlans)
                                    }
                                }
                            })
                    }
                } else {
                    Toast.makeText(this@Activity_User, "Ошибка загрузки планов", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PlanResponse>>, t: Throwable) {
                Toast.makeText(this@Activity_User, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
                t.printStackTrace()
            }
        })
    }

    private fun updateStatistics(plans: List<PlanResponse>) {
        // Считаем прогресс для каждого плана
        val planProgress = plans.map { plan ->
            val steps = plan.steps ?: emptyList()
            val total = steps.size
            val completed = steps.count { it.completed }
            val progress = if (total > 0) (completed * 100) / total else 0
            plan to progress
        }

        // Активные – прогресс < 100
        val activePlans = planProgress.filter { it.second < 100 }
        val completedPlans = planProgress.filter { it.second == 100 }

        tvPlansCount.text = "${activePlans.size}"
        tvCompletedCount.text = "${completedPlans.size}"

        // Общий прогресс: процент выполненных шагов по активным планам
        var totalSteps = 0
        var completedSteps = 0
        for ((plan, _) in activePlans) {
            val steps = plan.steps ?: emptyList()
            totalSteps += steps.size
            completedSteps += steps.count { it.completed }
        }

        val overallProgress = if (totalSteps > 0) {
            (completedSteps * 100) / totalSteps
        } else {
            0
        }

        progressBar.progress = overallProgress
        tvProgressPercent.text = "$overallProgress%"
    }
}