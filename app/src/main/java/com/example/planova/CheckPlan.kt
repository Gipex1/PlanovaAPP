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

        // 🔥 Исправлено: читаем правильный ключ "stepsDtoJson", а не "stepsJson"
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
            // Сериализуем текущие шаги (они могли измениться после регенерации)
            val currentStepsJson = Gson().toJson(steps)
            intent.putExtra("stepsJson", currentStepsJson)
            startActivity(intent)
        }

        // Удалить
        binding.llDelete.setOnClickListener {
            deletePlan(planId)
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
        // Адаптер StepAdapter должен использовать sortOrder как день
        binding.rvSteps.adapter = StepAdapter(steps)
    }

    // 🔥 Новый метод: сохранение или обновление
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
            // ✅ Обновление существующего плана
            val updateRequest = UpdatePlanRequest(
                title = title,
                description = description,
                status = null,           // оставляем без изменений
                targetDate = targetDate,
                steps = stepRequests      // передаём обновлённые шаги
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
            // ✅ Создание нового плана
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
        if (goal.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_goal_to_regenerate), Toast.LENGTH_SHORT).show()
            return
        }

        binding.llReset.isEnabled = false

        ApiClient.apiService.generate(prefs.getUserId()!!, GenerateRequest(goal))
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

    private fun deletePlan(id: Long) {
        if (id == -1L) {
            Toast.makeText(this, getString(R.string.plan_not_saved), Toast.LENGTH_SHORT).show()
            return
        }

        val userId = prefs.getUserId()
        if (userId == null) {
            Toast.makeText(this, getString(R.string.login_first), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ApiClient.apiService.deletePlan(userId, id)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CheckPlan, getString(R.string.plan_deleted), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@CheckPlan, My_Goals::class.java))
                        finish()
                    } else {
                        val error = response.errorBody()?.string() ?: getString(R.string.delete_error)
                        Toast.makeText(this@CheckPlan, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@CheckPlan, getString(R.string.network_error) + ": ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}