package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.EditStepAdapter
import com.example.planova.data.PlanRequest
import com.example.planova.data.PlanResponse
import com.example.planova.data.StepDto
import com.example.planova.data.StepRequest
import com.example.planova.data.UpdatePlanRequest
import com.example.planova.databinding.ActivityEditGoalBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Edit_Goal : BaseActivity() {

    private lateinit var binding: ActivityEditGoalBinding
    private lateinit var prefs: SharedPrefs
    private lateinit var adapter: EditStepAdapter
    private var planId: Long = -1
    private var targetDate: String? = null
    private val steps = mutableListOf<StepDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = SharedPrefs(this)

        // Получаем данные из Intent
        planId = intent.getLongExtra("planId", -1)
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val stepsJson = intent.getStringExtra("stepsJson") ?: "[]"
        targetDate = intent.getStringExtra("targetDate")

        // Заполняем поля
        binding.etPlanTitle.setText(title)
        binding.etPlanDescription.setText(description)

        // Парсим шаги
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<StepDto>>() {}.type
        val loadedSteps: List<StepDto> = try {
            gson.fromJson(stepsJson, type)
        } catch (e: Exception) {
            emptyList()
        }
        steps.clear()
        steps.addAll(loadedSteps)

        // Настраиваем адаптер
        adapter = EditStepAdapter(
            steps,
            onDelete = { position ->
                steps.removeAt(position)
                adapter.notifyItemRemoved(position)
                // обновляем нумерацию
                updateNumbers()
            },
            onTextChange = { position, newText ->
                steps[position] = steps[position].copy(description = newText)
            }
        )

        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = adapter

        // Кнопка "Добавить шаг"
        binding.btnAddStep.setOnClickListener {
            adapter.addStep()
            // прокрутить вниз
            binding.rvSteps.scrollToPosition(adapter.itemCount - 1)
        }

        // Кнопка "Сохранить изменения"
        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        // Кнопка "Назад"
        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun updateNumbers() {
        for (i in steps.indices) {
            steps[i] = steps[i].copy(sortOrder = i + 1)
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveChanges() {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, getString(R.string.login_first), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val title = binding.etPlanTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.edit_title), Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.etPlanDescription.text.toString().trim()
        val stepRequests = steps.mapIndexed { index, step ->
            StepRequest(step.description, index + 1)
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "..."

        if (planId == -1L) {
            // План еще не был сохранен в БД (только что сгенерирован), создаем новый
            val planRequest = PlanRequest(title, description, targetDate, stepRequests)
            ApiClient.apiService.savePlan(userId, planRequest)
                .enqueue(object : Callback<PlanResponse> {
                    override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                        handleResponse(response)
                    }
                    override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                        handleFailure(t)
                    }
                })
        } else {
            // План уже существует, обновляем
            val updateRequest = UpdatePlanRequest(
                title = title,
                description = description,
                status = null,
                targetDate = targetDate,
                steps = stepRequests.map { StepRequest(it.description, it.sortOrder) }
            )
            ApiClient.apiService.updatePlan(userId, planId, updateRequest)
                .enqueue(object : Callback<PlanResponse> {
                    override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                        handleResponse(response)
                    }
                    override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                        handleFailure(t)
                    }
                })
        }
    }

    private fun handleResponse(response: Response<PlanResponse>) {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = getString(R.string.save)
        if (response.isSuccessful) {
            Toast.makeText(this@Edit_Goal, getString(R.string.plan_saved), Toast.LENGTH_SHORT).show()
            val intent = Intent(this@Edit_Goal, My_Goals::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            val error = response.errorBody()?.string() ?: getString(R.string.save_error)
            Toast.makeText(this@Edit_Goal, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFailure(t: Throwable) {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = getString(R.string.save)
        Toast.makeText(this@Edit_Goal, getString(R.string.network_error) + ": ${t.message}", Toast.LENGTH_LONG).show()
    }
}