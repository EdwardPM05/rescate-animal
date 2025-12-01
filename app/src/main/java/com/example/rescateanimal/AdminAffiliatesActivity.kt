package com.example.rescateanimal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AdminAffiliatesActivity : AppCompatActivity() {

    private lateinit var navigationHelper: NavigationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_coming_soon)

        setupNavigation()
    }

    private fun setupNavigation() {
        navigationHelper = NavigationHelper(this)
        navigationHelper.setupBottomNavigation()
    }
}