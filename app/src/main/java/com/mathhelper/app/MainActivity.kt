package com.mathhelper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mathhelper.app.ui.nav.AppNavHost
import com.mathhelper.app.ui.theme.MathHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MathHelperTheme {
                AppNavHost()
            }
        }
    }
}
