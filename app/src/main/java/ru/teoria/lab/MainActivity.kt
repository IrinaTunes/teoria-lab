package ru.teoria.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.teoria.lab.ui.AppRoot
import ru.teoria.lab.ui.TeoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeoriaTheme {
                AppRoot()
            }
        }
    }
}
