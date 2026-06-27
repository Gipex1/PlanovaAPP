package com.example.planova

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Activity_User : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var next1 = findViewById<ImageView>(R.id.menu_home)
        next1.setOnClickListener {
            startActivity(Intent(this@Activity_User, Home::class.java))
        }

        var next2 = findViewById<ImageView>(R.id.menu_book)
        next2.setOnClickListener {
            startActivity(Intent(this@Activity_User, My_Goals::class.java))
        }

        var next3 = findViewById<ImageView>(R.id.menu_history)
        next3.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }

        var setting = findViewById<ImageView>(R.id.ivSettings)
        setting.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        var editProf = findViewById<ImageView>(R.id.ivEditProfile)
        editProf.setOnClickListener {
            Toast.makeText(this, "Еще в разработке", Toast.LENGTH_SHORT).show()
        }
    }
}