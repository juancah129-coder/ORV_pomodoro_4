package com.example.orvpomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.orvpomodoro.ui.PomodoroScreen
import com.example.orvpomodoro.ui.theme.OrvPomodoroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrvPomodoroTheme {
                PomodoroScreen()
            }
        }
    }
}