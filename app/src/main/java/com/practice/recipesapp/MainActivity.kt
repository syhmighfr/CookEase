package com.practice.recipesapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Optional: show your splash layout

        // Delay for 2 seconds before navigating
        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // User is signed in
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // User not signed in
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Close splash
        }, 2000) // 2000ms = 2 seconds
    }
}