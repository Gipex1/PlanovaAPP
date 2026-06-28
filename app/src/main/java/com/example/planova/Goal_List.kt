package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planova.adapter.StepProgressAdapter
import com.example.planova.data.StepProgressItem
import com.example.planova.databinding.ActivityGoalListBinding

class Goal_List : AppCompatActivity() {

    private lateinit var binding: ActivityGoalListBinding
    private lateinit var adapter: StepProgressAdapter
    private var planId: Long = -1
    private lateinit var steps: MutableList<StepProgressItem>

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

        // Получаем данные из Intent
        planId = intent.getLongExtra("planId", -1)
        val planTitle = intent.getStringExtra("planTitle") ?: "Название плана"
        val category = intent.getStringExtra("category") ?: "Категория"
        val progress = intent.getIntExtra("progress", 0)
        val totalDays = intent.getIntExtra("totalDays", 7)

        // Заполняем заголовок и категорию
        binding.titleGenerating.text = planTitle
        binding.titleCategory.text = category

        // Обновляем прогресс
        binding.progressBar.progress = progress
        binding.tvProgress.text = "$progress%"
        binding.tvDaysInfo.text = "день ${(progress / 100.0 * totalDays).toInt()} из $totalDays"

        // Получаем шаги из Intent (если переданы) или используем тестовые
        val stepsJson = intent.getStringExtra("stepsJson") ?: ""
        steps = if (stepsJson.isNotEmpty()) {
            try {
                val gson = com.google.gson.Gson()
                // ЯВНО УКАЗЫВАЕМ ТИП
                val type = object : com.google.gson.reflect.TypeToken<List<StepProgressItem>>() {}.type
                val list: List<StepProgressItem> = gson.fromJson(stepsJson, type)
                list.toMutableList()
            } catch (e: Exception) {
                getTestSteps().toMutableList()
            }
        } else {
            getTestSteps().toMutableList()
        }

        // Настройка RecyclerView
        adapter = StepProgressAdapter(steps) { position ->
            val step = steps[position]
            Toast.makeText(
                this,
                "Шаг ${step.day}: ${if (step.isCompleted) "Выполнен" else "Не выполнен"}",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: отправить запрос на сервер для обновления шага
            // Обновляем прогресс после изменения статуса
            updateProgress()
        }
        binding.rvSteps.layoutManager = LinearLayoutManager(this)
        binding.rvSteps.adapter = adapter

        // Кнопка "Открыть план" – переход на CheckPlan
        binding.btnSave.setOnClickListener {
            if (planId == -1L) {
                Toast.makeText(this, "ID плана не передан", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, CheckPlan::class.java)
            intent.putExtra("planId", planId)
            intent.putExtra("title", intent.getStringExtra("planTitle") ?: "")
            intent.putExtra("description", intent.getStringExtra("description") ?: "")
            intent.putExtra("targetDate", intent.getStringExtra("targetDate") ?: "")
            intent.putExtra("stepsJson", intent.getStringExtra("stepsJson") ?: "[]")
            startActivity(intent)
        }

        // Назад
        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun updateProgress() {
        val total = steps.size
        val completed = steps.count { it.isCompleted }
        val progress = if (total > 0) (completed * 100) / total else 0
        binding.progressBar.progress = progress
        binding.tvProgress.text = "$progress%"
        val totalDays = intent.getIntExtra("totalDays", 7)
        binding.tvDaysInfo.text = "день ${(progress / 100.0 * totalDays).toInt()} из $totalDays"
    }

    private fun getTestSteps(): List<StepProgressItem> {
        return listOf(
            StepProgressItem(1, "Изучить основы Kotlin", true),
            StepProgressItem(2, "Создать первый проект", false),
            StepProgressItem(3, "Настроить Retrofit", false),
            StepProgressItem(4, "Реализовать API", false),
            StepProgressItem(5, "Протестировать приложение", false)
        )
    }
}