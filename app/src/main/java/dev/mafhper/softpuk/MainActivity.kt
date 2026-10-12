package dev.mafhper.softpuk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.mafhper.softpuk.ui.SoftPukApp
import dev.mafhper.softpuk.ui.theme.SoftPukTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoftPukTheme {
                SoftPukApp()
            }
        }
    }
}
