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
import com.example.planova.databinding.ActivityMyGoalsBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class My_Goals : AppCompatActivity() {
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

        // Настраиваем RecyclerView
        adapter = PlanAdapter(emptyList()) { plan ->
            // Обработка клика по плану
            Toast.makeText(this, "Выбран: ${plan.title}", Toast.LENGTH_SHORT).show()
        }
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter

        // Загружаем планы
        loadPlans(userId)

        var next1 = findViewById<ImageView>(R.id.menu_home)
        next1.setOnClickListener {
            startActivity(Intent(this@My_Goals, Home::class.java))
        }

        var next2 = findViewById<ImageView>(R.id.menu_profile)
        next2.setOnClickListener {
            startActivity(Intent(this@My_Goals, Activity_User::class.java))
        }

        var next3 = findViewById<ImageView>(R.id.menu_history)
        next3.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
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

    private fun calculateProgress(steps: List<StepResponse>?): Int {
        if (steps.isNullOrEmpty()) return 0
        val total = steps.size
        val completed = steps.count { it.isCompleted }
        return if (total > 0) (completed * 100) / total else 0
    }
}