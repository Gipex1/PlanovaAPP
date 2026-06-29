package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.PlanAdapter
import com.example.planova.adapter.PlanItem
import com.example.planova.data.PlanResponse
import com.example.planova.data.StepResponse
import com.example.planova.data.StepDto
import com.example.planova.data.StepProgressItem
import com.example.planova.databinding.ActivityMyGoalsBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class My_Goals : BaseActivity() {
    private lateinit var binding: ActivityMyGoalsBinding
    private lateinit var adapter: PlanAdapter
    private lateinit var prefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefs(this)

        // Проверяем, авторизован ли пользователь
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Настраиваем RecyclerView с кликом
        adapter = PlanAdapter(emptyList()) { plan ->
            // Обработка клика по плану – открываем CheckPlan
            openPlanDetail(plan)
        }
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter

        // Загружаем планы
        loadPlans(userId)

        // Нижнее меню
        binding.menuHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        binding.menuProfile.setOnClickListener {
            startActivity(Intent(this, Activity_User::class.java))
        }

        binding.menuHistory.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }

        // Кнопка "+" – переход на генерацию
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
    }

    private fun loadPlans(userId: Long) {
        ApiClient.apiService.getPlans(userId).enqueue(object : Callback<List<PlanResponse>> {
            override fun onResponse(
                call: Call<List<PlanResponse>>,
                response: Response<List<PlanResponse>>
            ) {
                if (response.isSuccessful) {
                    val plans = response.body() ?: emptyList()
                    val items = plans.map { plan ->
                        PlanItem(
                            id = plan.id,
                            title = plan.title,
                            category = plan.category ?: "Общее",
                            progress = calculateProgress(plan.steps)
                        )
                    }
                    adapter.updateItems(items)
                } else {
                    val error = response.errorBody()?.string() ?: "Ошибка загрузки"
                    Toast.makeText(this@My_Goals, error, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PlanResponse>>, t: Throwable) {
                Toast.makeText(this@My_Goals, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openPlanDetail(plan: PlanItem) {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.apiService.getPlan(userId, plan.id)
            .enqueue(object : Callback<PlanResponse> {
                override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()!!
                        val intent = Intent(this@My_Goals, Goal_List::class.java)
                        intent.putExtra("planId", data.id)
                        intent.putExtra("planTitle", data.title)
                        intent.putExtra("description", data.description ?: "")
                        intent.putExtra("targetDate", data.targetDate ?: "")
                        intent.putExtra("category", data.category ?: "Общее")
                        intent.putExtra("progress", calculateProgress(data.steps))
                        intent.putExtra("totalDays", data.steps.size)
                        
                        val stepsProgressJson = Gson().toJson(
                            data.steps.map { StepProgressItem(it.sortOrder, it.description, it.isCompleted) }
                        )
                        intent.putExtra("stepsJson", stepsProgressJson)

                        // Передаем также StepDto для CheckPlan
                        val stepsDtoJson = Gson().toJson(
                            data.steps.map { StepDto(it.description, it.sortOrder) }
                        )
                        intent.putExtra("stepsDtoJson", stepsDtoJson)

                        startActivity(intent)
                    } else {
                        Toast.makeText(this@My_Goals, "Не удалось загрузить план", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                    Toast.makeText(this@My_Goals, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun calculateProgress(steps: List<StepResponse>?): Int {
        if (steps.isNullOrEmpty()) return 0
        val total = steps.size
        val completed = steps.count { it.isCompleted }
        return if (total > 0) (completed * 100) / total else 0
    }
}