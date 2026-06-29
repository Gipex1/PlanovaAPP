package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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

        // Проверка авторизации
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Адаптер
        adapter = PlanAdapter(emptyList()) { plan ->
            openPlanDetail(plan)
        }
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter

        // Нижнее меню
        binding.menuHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
        binding.menuProfile.setOnClickListener {
            startActivity(Intent(this, Activity_User::class.java))
        }
        binding.menuHistory.setOnClickListener {
            startActivity(Intent(this, My_Goals_Complat::class.java))
        }
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
        binding.tvCompleted.setOnClickListener {
            startActivity(Intent(this, My_Goals_Complat::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = prefs.getUserId()
        if (userId != null) {
            loadPlans(userId)   // обновляем список при возврате
        }
    }

    private fun loadPlans(userId: Long) {
        ApiClient.apiService.getPlans(userId).enqueue(object : Callback<List<PlanResponse>> {
            override fun onResponse(call: Call<List<PlanResponse>>, response: Response<List<PlanResponse>>) {
                if (response.isSuccessful) {
                    val plans = response.body() ?: emptyList()
                    if (plans.isEmpty()) {
                        adapter.updateItems(emptyList())
                        return
                    }

                    // Загружаем каждый план отдельно, чтобы получить шаги
                    var loadedCount = 0
                    val items = mutableListOf<PlanItem>()

                    plans.forEach { planSummary ->
                        ApiClient.apiService.getPlan(userId, planSummary.id)
                            .enqueue(object : Callback<PlanResponse> {
                                override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                                    loadedCount++
                                    if (response.isSuccessful) {
                                        val fullPlan = response.body()!!
                                        val progress = calculateProgress(fullPlan.steps)
                                        items.add(
                                            PlanItem(
                                                id = fullPlan.id,
                                                title = fullPlan.title,
                                                category = fullPlan.category ?: "Общее",
                                                progress = progress
                                            )
                                        )
                                    } else {
                                        // Если не удалось загрузить детали – добавляем с прогрессом 0
                                        items.add(
                                            PlanItem(
                                                id = planSummary.id,
                                                title = planSummary.title,
                                                category = planSummary.category ?: "Общее",
                                                progress = 0
                                            )
                                        )
                                    }

                                    // Когда все планы загружены – обновляем адаптер
                                    if (loadedCount == plans.size) {
                                        // Фильтруем активные (progress < 100)
                                        val activeItems = items.filter { it.progress < 100 }
                                        adapter.updateItems(activeItems)
                                    }
                                }

                                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                                    loadedCount++
                                    items.add(
                                        PlanItem(
                                            id = planSummary.id,
                                            title = planSummary.title,
                                            category = planSummary.category ?: "Общее",
                                            progress = 0
                                        )
                                    )
                                    if (loadedCount == plans.size) {
                                        val activeItems = items.filter { it.progress < 100 }
                                        adapter.updateItems(activeItems)
                                    }
                                }
                            })
                    }
                } else {
                    Toast.makeText(this@My_Goals, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PlanResponse>>, t: Throwable) {
                Toast.makeText(this@My_Goals, "Ошибка сети", Toast.LENGTH_SHORT).show()
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

                        val stepsProgressJson = Gson().toJson(
                            data.steps.map { StepProgressItem(
                                id = it.id,
                                day = it.sortOrder,
                                description = it.description,
                                isCompleted = it.completed
                            ) }
                        )
                        intent.putExtra("stepsJson", stepsProgressJson)

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
        val completed = steps.count { it.completed }
        return if (total > 0) (completed * 100) / total else 0
    }
}