package com.example.todolist_v2

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val gradientWidth = 1000f

        val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")

        val xPosition by infiniteTransition.animateFloat(
            initialValue = -gradientWidth * 8,
            targetValue = screenWidthPx + gradientWidth * 8,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_x_position"
        )

        val shimmerColors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.0f)
        )

        val shimmerBrush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = xPosition, y = 0f),
            end = Offset(x = xPosition + gradientWidth, y = screenHeightPx)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush)
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
                    ){
                        Text("Nie jesteś zalogowany.", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onSignIn) {
                            Text("Zaloguj się / Zarejestruj")
                        }
                    }
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