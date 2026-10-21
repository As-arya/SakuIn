package Sakuin.Project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import Sakuin.Project.ui.SakuinApp
import Sakuin.Project.ui.theme.SakuinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SakuinTheme {
                SakuinApp()
            }
        }
    }
}
