package com.abi.project.activities
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.abi.project.R

class HomeActivity : ComponentActivity() {
    private lateinit var welcomeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        welcomeTextView = findViewById(R.id.welcomeTextView)

        // Get username from intent
        val username = intent.getStringExtra("USERNAME") ?: "User"
        welcomeTextView.text = "Welcome, $username!"
    }
}

// Update LoginActivity.kt - Modify the login success block to pass username
// In LoginActivity.kt, update the login success part:

