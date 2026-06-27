package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val next = findViewById<View>(R.id.blackCircle)
        next.setOnClickListener {
            val editTextGoal = findViewById<EditText>(R.id.etGoal)
            val goal = editTextGoal.text.toString().trim()

            if (goal.isNotEmpty()) {

                // В Home.kt перед startActivity(Intent(this, Generate::class.java))
                val userId = com.example.planova.utils.SharedPrefs(this).getUserId()
                if (userId == null) {
                    Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(this, Generate::class.java)
                intent.putExtra("goal", goal)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Введите цель", Toast.LENGTH_SHORT).show()
            }
        }

        var next1 = findViewById<ImageView>(R.id.menu_profile)
        next1.setOnClickListener {
            startActivity(Intent(this@Home, Activity_User::class.java))
        }

        var next2 = findViewById<ImageView>(R.id.menu_book)
        next2.setOnClickListener {
            startActivity(Intent(this@Home, My_Goals::class.java))
        }

        var next3 = findViewById<ImageView>(R.id.menu_history)
        next3.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }

        var setting = findViewById<ImageView>(R.id.ivSettings)
        setting.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }

    }
}