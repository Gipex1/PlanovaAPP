package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.StepProgressAdapter
import com.example.planova.data.*
import com.example.planova.databinding.ActivityGoalListBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Goal_List : AppCompatActivity() {

    private lateinit var binding: ActivityGoalListBinding
    private lateinit var adapter: StepProgressAdapter
    private lateinit var prefs: SharedPrefs
    private var planId: Long = -1
    private lateinit var steps: MutableList<StepProgressItem>
    private var planTitle: String = ""
    private var description: String = ""
    private var targetDate: String = ""
    private var stepsDtoJson: String = "[]"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGoalListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = SharedPrefs(this)

        planId = intent.getLongExtra("planId", -1)
        planTitle = intent.getStringExtra("planTitle") ?: "Название плана"
        description = intent.getStringExtra("description") ?: ""
        targetDate = intent.getStringExtra("targetDate") ?: ""

        binding.titleGenerating.text = planTitle
        binding.titleCategory.text = intent.getStringExtra("category") ?: "Общее"

        // Инициализируем steps из Intent, если план новый
        steps = mutableListOf()
        if (planId == -1L) {
            // Для нового плана берём шаги из Intent (ключ "stepsJson" или "stepsDtoJson")
            val stepsJson = intent.getStringExtra("stepsJson") ?: "[]"
            val type = object : TypeToken<List<StepDto>>() {}.type
            val stepDtos: List<StepDto> = try {
                Gson().fromJson(stepsJson, type)
            } catch (e: Exception) {
                emptyList()
            }
            steps = stepDtos.mapIndexed { index, stepDto ->
                StepProgressItem(
                    id = -1L, // временный id
                    day = stepDto.sortOrder,
                    description = stepDto.description,
                    isCompleted = false
                )
            }.sortedBy { it.day }.toMutableList()
            // Обновляем JSON для передачи в CheckPlan
            updateStepsJson()
            updateProgress()
        }

        adapter = StepProgressAdapter(steps) { position, newStatus ->
            toggleStepStatus(position, newStatus)
        }
        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = adapter

        binding.btnSave.setOnClickListener {
            val intent = Intent(this, CheckPlan::class.java)
            intent.putExtra("planId", planId)
            intent.putExtra("title", planTitle)
            intent.putExtra("description", description)
            intent.putExtra("targetDate", targetDate)
            intent.putExtra("stepsDtoJson", stepsDtoJson)
            startActivity(intent)
        }

        binding.backArrow.setOnClickListener {
            finish()
        }

        if (planId != -1L) {
            loadPlanFromServer()
        }
    }

    override fun onResume() {
        super.onResume()
        // Если нужно обновлять при возврате – раскомментируй
        // if (planId != -1L) {
        //     loadPlanFromServer()
        // }
    }

    private fun loadPlanFromServer() {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.apiService.getPlan(userId, planId)
            .enqueue(object : Callback<PlanResponse> {
                override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()!!
                        planTitle = data.title
                        description = data.description ?: ""
                        targetDate = data.targetDate ?: ""
                        binding.titleGenerating.text = planTitle

                        val newSteps = data.steps.map {
                            StepProgressItem(
                                id = it.id,
                                day = it.sortOrder,
                                description = it.description,
                                isCompleted = it.completed
                            )
                        }.sortedBy { it.day }.toMutableList()

                        steps = newSteps
                        adapter.updateItems(steps)
                        updateStepsJson()
                        updateProgress()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Goal_List", "Load plan error: $errorBody")
                        Toast.makeText(this@Goal_List, "Не удалось загрузить план", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                    Log.e("Goal_List", "Network error: ${t.message}")
                    Toast.makeText(this@Goal_List, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleStepStatus(position: Int, newStatus: Boolean) {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show()
            return
        }

        val step = steps[position]
        val request = UpdateStepRequest(
            description = step.description,
            isCompleted = newStatus,
            sortOrder = step.day
        )

        Log.d("Goal_List", "➡️ Sending update: stepId=${step.id}, isCompleted=$newStatus")

        binding.rvSteps.isEnabled = false

        ApiClient.apiService.updateStep(userId, planId, step.id, request)
            .enqueue(object : Callback<StepResponse> {
                override fun onResponse(call: Call<StepResponse>, response: Response<StepResponse>) {
                    binding.rvSteps.isEnabled = true
                    Log.d("Goal_List", "✅ Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        steps[position] = steps[position].copy(isCompleted = newStatus)
                        adapter.updateItems(steps)
                        updateStepsJson()
                        updateProgress()
                        Log.d("Goal_List", "✅ Step ${step.id} updated successfully")
                        Toast.makeText(this@Goal_List, "Статус обновлён", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Goal_List", "❌ Server error: $errorBody")
                        Toast.makeText(this@Goal_List, "Ошибка: $errorBody", Toast.LENGTH_LONG).show()
                        adapter.updateItemStatus(position, !newStatus)
                    }
                }

                override fun onFailure(call: Call<StepResponse>, t: Throwable) {
                    binding.rvSteps.isEnabled = true
                    Log.e("Goal_List", "❌ Network failure: ${t.message}")
                    Toast.makeText(this@Goal_List, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
                    adapter.updateItemStatus(position, !newStatus)
                }
            })
    }

    private fun updateStepsJson() {
        val stepDtos = steps.map {
            StepDto(it.description, it.day)
        }
        stepsDtoJson = Gson().toJson(stepDtos)
    }

    private fun updateProgress() {
        val total = steps.size
        val completed = steps.count { it.isCompleted }
        val progress = if (total > 0) (completed * 100) / total else 0
        binding.progressBar.progress = progress
        binding.tvProgress.text = "$progress%"
        binding.tvDaysInfo.text = "день $completed из $total"
    }
}