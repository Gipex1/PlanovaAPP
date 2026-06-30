package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.StepAdapter
import com.example.planova.data.*
import com.example.planova.databinding.ActivityCheckPlanBinding
import com.example.planova.network.ApiClient
import com.example.planova.utils.SharedPrefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckPlan : BaseActivity() {

    private lateinit var binding: ActivityCheckPlanBinding
    private lateinit var prefs: SharedPrefs
    private var planId: Long = -1
    private var goal: String = ""
    private var title: String = ""
    private var description: String = ""
    private var targetDate: String = ""
    private var steps: List<StepDto> = emptyList()

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
        title = intent.getStringExtra("title") ?: getString(R.string.namePlan)
        description = intent.getStringExtra("description") ?: getString(R.string.descriptonPlan)
        targetDate = intent.getStringExtra("targetDate") ?: ""

        val stepsDtoJson = intent.getStringExtra("stepsDtoJson") ?: "[]"
        val type = object : TypeToken<List<StepDto>>() {}.type
        steps = try {
            Gson().fromJson(stepsDtoJson, type)
        } catch (e: Exception) {
            emptyList()
        }

        // Заполняем UI
        updateUI()

        // Назад
        binding.backArrow.setOnClickListener { finish() }

        // Сохранить – теперь с проверкой на существующий план
        binding.llSave.setOnClickListener {
            saveOrUpdatePlan()
        }

        // Переделать
        binding.llReset.setOnClickListener {
            regeneratePlan()
        }

        // Редактировать
        binding.llEdit.setOnClickListener {
            val intent = Intent(this, Edit_Goal::class.java)
            intent.putExtra("planId", planId)
            intent.putExtra("title", title)
            intent.putExtra("description", description)
            intent.putExtra("targetDate", targetDate)
            val currentStepsJson = Gson().toJson(steps)
            intent.putExtra("stepsJson", currentStepsJson)
            startActivity(intent)
        }

        // Удалить – теперь с подтверждением
        binding.llDelete.setOnClickListener {
            deletePlan()
        }

        // Нижнее меню
        binding.menuBooks.setOnClickListener {
            startActivity(Intent(this, My_Goals::class.java))
        }
    }

    private fun updateUI() {
        binding.tvPlanTitle.text = title
        binding.tvPlanDescription.text = description
        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = StepAdapter(steps)
    }

    /**
     * Удаление плана с диалогом подтверждения.
     */
    private fun deletePlan() {
        // Если план ещё не сохранён (новый), просто закрываем экран
        if (planId == -1L) {
            finish()
            return
        }

        // Диалог подтверждения
        MaterialAlertDialogBuilder(this)
            .setTitle("Удалить план?")
            .setMessage("Вы уверены, что хотите удалить этот план? Это действие нельзя отменить.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Удалить") { _, _ ->
                performDelete()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Выполняет DELETE-запрос к серверу.
     */
    private fun performDelete() {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, getString(R.string.login_first), Toast.LENGTH_SHORT).show()
            return
        }

        // Блокируем кнопку на время запроса
        binding.llDelete.isEnabled = false

        ApiClient.apiService.deletePlan(userId, planId)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(
                    call: Call<Map<String, String>>,
                    response: Response<Map<String, String>>
                ) {
                    binding.llDelete.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CheckPlan,
                            "План удалён",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Переходим к списку планов
                        startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string()?.let { parseError(it) }
                            ?: "Ошибка удаления"
                        Toast.makeText(this@CheckPlan, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    binding.llDelete.isEnabled = true
                    Toast.makeText(
                        this@CheckPlan,
                        "${getString(R.string.network_error)}: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    t.printStackTrace()
                }
            })
    }

    // Вспомогательный метод для парсинга ошибки
    private fun parseError(json: String): String {
        return try {
            val obj = com.google.gson.JsonParser().parse(json).asJsonObject
            obj.get("error")?.asString ?: "Ошибка"
        } catch (_: Exception) {
            "Ошибка"
        }
    }

    // ============ Остальные методы (saveOrUpdatePlan, regeneratePlan) остаются без изменений ============
    private fun saveOrUpdatePlan() {
        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, getString(R.string.login_first), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val stepRequests = steps.map { StepRequest(it.description, it.sortOrder) }
        val planRequest = PlanRequest(title, description, targetDate, stepRequests)

        binding.llSave.isEnabled = false

        if (planId != -1L) {
            val updateRequest = UpdatePlanRequest(
                title = title,
                description = description,
                status = null,
                targetDate = targetDate,
                steps = stepRequests
            )
            ApiClient.apiService.updatePlan(userId, planId, updateRequest)
                .enqueue(object : Callback<PlanResponse> {
                    override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                        binding.llSave.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@CheckPlan, getString(R.string.plan_updated), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                            finish()
                        } else {
                            val error = response.errorBody()?.string() ?: getString(R.string.update_error)
                            Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                        binding.llSave.isEnabled = true
                        Toast.makeText(this@CheckPlan, getString(R.string.network_error) + ": ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        } else {
            ApiClient.apiService.savePlan(userId, planRequest)
                .enqueue(object : Callback<PlanResponse> {
                    override fun onResponse(call: Call<PlanResponse>, response: Response<PlanResponse>) {
                        binding.llSave.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@CheckPlan, getString(R.string.plan_saved), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                            finish()
                        } else {
                            val error = response.errorBody()?.string() ?: getString(R.string.save_error)
                            Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<PlanResponse>, t: Throwable) {
                        binding.llSave.isEnabled = true
                        Toast.makeText(this@CheckPlan, getString(R.string.network_error) + ": ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }

    private fun regeneratePlan() {
        val userGoal = if (goal.isNotEmpty()) {
            goal
        } else {
            val prefix = "План: "
            if (title.startsWith(prefix)) title.substring(prefix.length) else title
        }

        if (userGoal.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_goal_to_regenerate), Toast.LENGTH_SHORT).show()
            return
        }

        binding.llReset.isEnabled = false

        ApiClient.apiService.generate(prefs.getUserId()!!, GenerateRequest(userGoal))
            .enqueue(object : Callback<GenerateResponse> {
                override fun onResponse(call: Call<GenerateResponse>, response: Response<GenerateResponse>) {
                    binding.llReset.isEnabled = true
                    if (response.isSuccessful) {
                        val newPlan = response.body()
                        if (newPlan != null) {
                            title = newPlan.title
                            description = newPlan.description
                            targetDate = newPlan.targetDate
                            steps = newPlan.steps
                            updateUI()
                            Toast.makeText(this@CheckPlan, getString(R.string.plan_regenerated), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CheckPlan, getString(R.string.generate_error), Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val error = response.errorBody()?.string() ?: getString(R.string.generate_error)
                        Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<GenerateResponse>, t: Throwable) {
                    binding.llReset.isEnabled = true
                    Toast.makeText(this@CheckPlan, getString(R.string.network_error) + ": ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}