package com.aks.boilerplate

import android.app.Application
import com.aks.boilerplate.di.AppModule

class BoilerplateApplication : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
