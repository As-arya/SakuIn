package Sakuin.Project

import android.app.Application
import Sakuin.Project.di.AppContainer

class SakuinApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
