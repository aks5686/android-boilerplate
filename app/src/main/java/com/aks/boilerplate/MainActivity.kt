package com.aks.boilerplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aks.boilerplate.features.auth.presentation.LoginScreen
import com.aks.boilerplate.ui.theme.BoilerplateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appModule = (application as BoilerplateApplication).appModule

        setContent {
            BoilerplateTheme {
                LoginScreen(
                    viewModel = appModule.provideLoginViewModel(),
                    onLoginSuccess = { /* TODO: navigate to home once it exists */ },
                )
            }
        }
    }
}
