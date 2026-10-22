package Sakuin.Project.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import Sakuin.Project.SakuinApplication
import Sakuin.Project.di.AppContainer

/**
 * Creates a screen ViewModel from the [AppContainer]. Inside [create], `this` is the
 * [CreationExtras], so `createSavedStateHandle()` is available for navigation arguments.
 */
@Composable
inline fun <reified VM : ViewModel> sakuinViewModel(
    crossinline create: CreationExtras.(AppContainer) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer { create((this[APPLICATION_KEY] as SakuinApplication).container) }
    },
)
