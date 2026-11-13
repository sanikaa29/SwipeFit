package com.swipefit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swipefit.ui.navigation.SwipeFitNavHost
import com.swipefit.ui.theme.SwipeFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; icon colors follow the light/dark theme automatically.
        enableEdgeToEdge()
        setContent {
            SwipeFitTheme {
                SwipeFitNavHost()
            }
        }
    }
}
