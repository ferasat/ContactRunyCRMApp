package com.example.contactrunycrmapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.contactrunycrmapp.R

/**
 * Hosts the MainFragment. Activity is intentionally thin and uses view binding for the layout.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
