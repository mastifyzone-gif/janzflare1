package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.onGoogleSignInClicked
import com.example.ui.components.JanzFlareMark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareOrange
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val alphaAnim = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(550))
        progressAnim.animateTo(1f, animationSpec = tween(950))
        delay(120)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBlack, MidnightNavy, Color(0xFF1A1033))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JanzFlareMark(size = 112.dp, animated = true)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "JanzFlare",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your Talent. Your Voice. Your World.",
                style = MaterialTheme.typography.titleMedium,
                color = FlareOrange,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "आपकी प्रतिभा, आपकी आवाज़, आपकी दुनिया।",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC5CCE0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            LinearProgressIndicator(
                progress = { progressAnim.value },
                color = FlareOrange,
                trackColor = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Janz Creative Studio • Empowering Indian Creators",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ObsidianBlack, MidnightNavy, Color(0xFF17112B))
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JanzFlareMark(size = 86.dp, animated = true)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Welcome to JanzFlare",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your Talent. Your Voice. Your World.",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlareOrange,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "आपकी प्रतिभा, आपकी आवाज़, आपकी दुनिया।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB5BED6),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                FeatureHighlightCard(
                    icon = Icons.Default.CloudUpload,
                    title = "Upload Videos & 9:16 Shorts",
                    subtitle = "Share HD videos, regional vlogs, music, tech & Shorts right from your phone."
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightCard(
                    icon = Icons.Default.Bolt,
                    title = "Fair Creator Discovery",
                    subtitle = "New and emerging Indian creators get equal visibility alongside popular channels."
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightCard(
                    icon = Icons.Default.Groups,
                    title = "Real-Time Creator Community",
                    subtitle = "Follow creators, reply in comments, build playlists, and track real analytics."
                )
                Spacer(Modifier.height(10.dp))
                FeatureHighlightCard(
                    icon = Icons.Default.Security,
                    title = "Protected Google Sign-In",
                    subtitle = "Verified Google authentication keeps your creator channel and privacy secure."
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFFB4AB),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlareOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Signing in...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Janz Creative Studio • Safe & Moderated Platform",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141B2D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(FlareOrange.copy(alpha = 0.25f), ElectricViolet.copy(alpha = 0.25f))),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FlareOrange)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA7B2CC))
            }
        }
    }
}
