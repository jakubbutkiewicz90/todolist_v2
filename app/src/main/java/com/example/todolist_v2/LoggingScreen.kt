package com.example.todolist_v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun LoggingScreen(
    onSignIn: () -> Unit
) {
    val currentUser by rememberFirebaseAuthUserState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.purple_smoke_wallpaper_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentUser != null) {

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Zalogowano jako:", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentUser?.email ?: "Użytkownik anonimowy", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            AuthUI.getInstance().signOut(context)
                        }) {
                            Text("Wyloguj")
                        }
                    }
                }
            } else {
                Text("Nie jesteś zalogowany.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSignIn) {
                    Text("Zaloguj się / Zarejestruj")
                }
            }
        }
    }
}

@Composable
fun rememberFirebaseAuthUserState(): State<FirebaseUser?> {
    return produceState<FirebaseUser?>(initialValue = FirebaseAuth.getInstance().currentUser) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            value = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        awaitDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
    }
}