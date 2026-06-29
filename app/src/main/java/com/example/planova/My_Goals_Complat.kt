package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.PlanAdapter
import com.example.planova.adapter.PlanItem
import com.example.planova.data.PlanResponse
import com.example.planova.data.StepResponse
import com.example.planova.databinding.ActivityMyGoalsComplatBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class My_Goals_Complat : BaseActivity() {
    private lateinit var binding: ActivityMyGoalsComplatBinding
    private lateinit var adapter: PlanAdapter
    private lateinit var prefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyGoalsComplatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefs(this)

        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Адаптер
        adapter = PlanAdapter(emptyList()) { plan ->
            // При клике на завершённый план можно открыть его детали (или показать Toast)
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
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        binding.tvActive.setOnClickListener {
            startActivity(Intent(this, My_Goals::class.java))
        }
        var help = findViewById<ImageView>(R.id.menu_book)
        help.setOnClickListener {
            startActivity(Intent(this, My_Goals::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = prefs.getUserId()
        if (userId != null) {
            loadPlans(userId)
        }
    }

    private fun loadPlans(userId: Long) {
        // Сначала получаем список планов (без шагов)
        ApiClient.apiService.getPlans(userId).enqueue(object : Callback<List<PlanResponse>> {
            override fun onResponse(
                call: Call<List<PlanResponse>>,
                response: Response<List<PlanResponse>>
            ) {
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
                                        // Фильтруем только завершённые (progress == 100)
                                        val completedItems = items.filter { it.progress == 100 }
                                        adapter.updateItems(completedItems)
                                    }
                                }

                                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                                    loadedCount++
                                    Log.e("My_Goals_Complat", "Ошибка загрузки плана ${planSummary.id}: ${t.message}")
                                    items.add(
                                        PlanItem(
                                            id = planSummary.id,
                                            title = planSummary.title,
                                            category = planSummary.category ?: "Общее",
                                            progress = 0
                                        )
                                    )
                                    if (loadedCount == plans.size) {
                                        val completedItems = items.filter { it.progress == 100 }
                                        adapter.updateItems(completedItems)
                                    }
                                }
                            })
                    }
                } else {
                    Toast.makeText(this@My_Goals_Complat, "Ошибка загрузки списка", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PlanResponse>>, t: Throwable) {
                Toast.makeText(this@My_Goals_Complat, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateProgress(steps: List<StepResponse>?): Int {
        if (steps.isNullOrEmpty()) return 0
        val total = steps.size
        val completed = steps.count { it.completed }   // сервер возвращает completed
        return if (total > 0) (completed * 100) / total else 0
    }

    private fun openPlanDetail(plan: PlanItem) {
        Toast.makeText(this, "План «${plan.title}» завершён", Toast.LENGTH_SHORT).show()
    }
}