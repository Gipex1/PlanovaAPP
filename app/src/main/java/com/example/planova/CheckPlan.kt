package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.data.*
import com.example.planova.databinding.ActivityCheckPlanBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckPlan : AppCompatActivity() {

    private lateinit var binding: ActivityCheckPlanBinding
    private lateinit var prefs: SharedPrefs
    private var planId: Long = -1
    private var goal: String = "" // для перегенерации
    private var title: String = ""
    private var description: String = ""
    private var targetDate: String = ""
    private lateinit var steps: List<StepDto>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCheckPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = SharedPrefs(this)

        // Получаем данные
        planId = intent.getLongExtra("planId", -1)
        goal = intent.getStringExtra("goal") ?: ""
        title = intent.getStringExtra("title") ?: "Без названия"
        description = intent.getStringExtra("description") ?: "Описание отсутствует"
        targetDate = intent.getStringExtra("targetDate") ?: ""
        val stepsJson = intent.getStringExtra("stepsJson") ?: "[]"
        val type = object : TypeToken<List<StepDto>>() {}.type
        steps = try {
            Gson().fromJson(stepsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        Log.d("CheckPlan", "stepsJson = $stepsJson")

        // Заполняем UI
        binding.tvPlanTitle.text = title
        binding.tvPlanDescription.text = description

        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = AllStepAdapter(steps)

        // Назад
        binding.backArrow.setOnClickListener { finish() }

        // ===== 1. СОХРАНИТЬ =====
        if (planId != -1L) {
            binding.llSave.visibility = android.view.View.GONE
        } else {
            binding.llSave.setOnClickListener {
                savePlan(title, description, targetDate, steps)
            }
        }

        // ===== 2. ПЕРЕДЕЛАТЬ (повторная генерация) =====
        binding.llReset.setOnClickListener {
            regeneratePlan()
        }

        // ===== 3. РЕДАКТИРОВАТЬ =====
        binding.llEdit.setOnClickListener {
            if (planId == -1L) {
                Toast.makeText(this, "Сначала сохраните план", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, Edit_Goal::class.java)
                intent.putExtra("planId", planId)
                intent.putExtra("title", title)
                intent.putExtra("description", description)
                intent.putExtra("stepsJson", stepsJson)
                startActivity(intent)
            }
        }

        // ===== 4. УДАЛИТЬ =====
        binding.llDelete.setOnClickListener {
            if (planId == -1L) {
                Toast.makeText(this, "Нет плана для удаления", Toast.LENGTH_SHORT).show()
            } else {
                deletePlan(planId)
            }
        }

        // Нижнее меню – переход на список планов
        binding.menuBooks.setOnClickListener {
            startActivity(Intent(this, My_Goals::class.java))
        }

        var next1 = findViewById<ImageView>(R.id.menu_profile)
        next1.setOnClickListener {
            startActivity(Intent(this@CheckPlan, Activity_User::class.java))
        }

        var next2 = findViewById<ImageView>(R.id.menu_book)
        next2.setOnClickListener {
            startActivity(Intent(this@CheckPlan, My_Goals::class.java))
        }

        var next3 = findViewById<ImageView>(R.id.menu_history)
        next3.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- СОХРАНЕНИЕ ----------
    private fun savePlan(title: String, description: String, targetDate: String, steps: List<StepDto>) {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val stepRequests = steps.map { StepRequest(it.description, it.sortOrder) }
        val planRequest = PlanRequest(title, description, targetDate, stepRequests)

        binding.llSave.isEnabled = false

        ApiClient.apiService.savePlan(userId, planRequest)
            .enqueue(object : Callback<PlanResponse> {
                override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                    binding.llSave.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@CheckPlan, "План сохранён!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                        finish()
                    } else {
                        val error = response.errorBody()?.string() ?: "Ошибка сохранения"
                        Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                    binding.llSave.isEnabled = true
                    Toast.makeText(this@CheckPlan, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // ---------- ПЕРЕДЕЛАТЬ (генерация заново) ----------
    private fun regeneratePlan() {
        if (goal.isEmpty()) {
            Toast.makeText(this, "Нечего переделывать: цель не найдена", Toast.LENGTH_SHORT).show()
            return
        }

        // Показываем прогресс (можно сделать видимым ProgressBar)
        binding.llReset.isEnabled = false

        ApiClient.apiService.generate(GenerateRequest(goal))
            .enqueue(object : Callback<GenerateResponse> {
                override fun onResponse(call: Call<GenerateResponse>, response: Response<GenerateResponse>) {
                    binding.llReset.isEnabled = true
                    if (response.isSuccessful) {
                        val newPlan = response.body()
                        if (newPlan != null) {
                            // Обновляем данные на экране
                            title = newPlan.title
                            description = newPlan.description
                            targetDate = newPlan.targetDate
                            steps = newPlan.steps

                            binding.tvPlanTitle.text = title
                            binding.tvPlanDescription.text = description
                            binding.rvSteps.adapter = AllStepAdapter(steps)

                            Toast.makeText(this@CheckPlan, "План переделан!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CheckPlan, "Ошибка генерации", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val error = response.errorBody()?.string() ?: "Ошибка генерации"
                        Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<GenerateResponse>, t: Throwable) {
                    binding.llReset.isEnabled = true
                    Toast.makeText(this@CheckPlan, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // ---------- УДАЛЕНИЕ ----------
    private fun deletePlan(id: Long) {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ApiClient.apiService.deletePlan(userId, id)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CheckPlan, "План удалён", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                        finish()
                    } else {
                        val error = response.errorBody()?.string() ?: "Ошибка удаления"
                        Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@CheckPlan, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}