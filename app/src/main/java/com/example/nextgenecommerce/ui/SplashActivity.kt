package com.example.nextgenecommerce.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.nextgenecommerce.R
import com.example.nextgenecommerce.presentation.MainComposeActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 2500 // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Delayed navigation to MainComposeActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainComposeActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }
}
