package Sakuin.Project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import Sakuin.Project.ui.SakuinApp
import Sakuin.Project.ui.navigation.HomeRoute
import Sakuin.Project.ui.navigation.OnboardingRoute
import Sakuin.Project.ui.theme.SakuinTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer { MainViewModel((application as SakuinApplication).container.settingsRepository) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.onboardingDone.value == null }
        enableEdgeToEdge()
        setContent {
            val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
            val done = onboardingDone ?: return@setContent
            // Decided once per launch; later changes are handled by navigation.
            val startDestination = remember { if (done) HomeRoute else OnboardingRoute }
            SakuinTheme {
                SakuinApp(startDestination = startDestination)
            }
        }
    }
}
