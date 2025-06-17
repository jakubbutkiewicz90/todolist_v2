package com.example.todolist_v2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todolist_v2.ui.theme.Todolist_v2Theme
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val todoViewModel: TodoViewModel by viewModels()
    private val syncViewModel: SyncViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ownerId by todoViewModel.currentOwnerId.collectAsState()

            LaunchedEffect(ownerId) {
                val localUserIds = setOf("default_user", "default_shopping_user")
                if (ownerId !in localUserIds) {
                    syncViewModel.performSync(ownerId)
                }
            }

            Todolist_v2Theme {
                MainAppScreen(
                    todoViewModel = todoViewModel,
                    shoppingViewModel = hiltViewModel(),
                    onSignIn = {
                        val providers = arrayListOf(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()
                        )
                        val signInIntent = AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setTheme(R.style.FirebaseUITheme)
                            .build()
                        signInLauncher.launch(signInIntent)
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val ownerId = todoViewModel.currentOwnerId.value
        val localUserIds = setOf("default_user", "default_shopping_user")

        if (ownerId !in localUserIds) {
            Log.d("AppSync", "Aplikacja zatrzymana, uruchamiam synchronizację dla użytkownika: $ownerId")
            syncViewModel.performSync(ownerId)
        } else {
            Log.d("AppSync", "Aplikacja zatrzymana, brak zalogowanego użytkownika, pomijam synchronizację.")
        }
    }

    override fun onRestart() {
        super.onRestart()

        val ownerId = todoViewModel.currentOwnerId.value
        val localUserIds = setOf("default_user", "default_shopping_user")

        if (ownerId !in localUserIds) {
            Log.d("AppSync", "Aplikacja zatrzymana, uruchamiam synchronizację dla użytkownika: $ownerId")
            syncViewModel.performSync(ownerId)
        } else {
            Log.d("AppSync", "Aplikacja zatrzymana, brak zalogowanego użytkownika, pomijam synchronizację.")
        }
    }
}