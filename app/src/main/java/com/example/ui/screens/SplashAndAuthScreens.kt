package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.onGoogleSignInClicked
import com.example.ui.components.JanzFlareMark
import com.example.ui.components.LegalPoliciesDialog
import com.example.ui.components.LegalPolicyTab
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
        alphaAnim.animateTo(1f, animationSpec = tween(500))
        progressAnim.animateTo(1f, animationSpec = tween(900))
        delay(120)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBlack, MidnightNavy, Color(0xFF150E2A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle atmospheric flare behind the logo
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FlareOrange.copy(alpha = 0.16f),
                            ElectricViolet.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JanzFlareMark(size = 132.dp, animated = true)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "JanzFlare",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your Talent. Your Voice. Your World.",
                style = MaterialTheme.typography.titleMedium,
                color = FlareOrange,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(34.dp))
            LinearProgressIndicator(
                progress = { progressAnim.value },
                color = FlareOrange,
                trackColor = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .width(156.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
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
    var openPolicyTab by remember { mutableStateOf<LegalPolicyTab?>(null) }

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
                        colors = listOf(ObsidianBlack, MidnightNavy, Color(0xFF150E2A))
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            // Soft ambient glow behind center mark
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                FlareOrange.copy(alpha = 0.14f),
                                ElectricViolet.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                JanzFlareMark(size = 116.dp, animated = true)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "JanzFlare",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
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
                    color = Color(0xFFB5BED6),
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFFB4AB),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(44.dp))

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
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Sign in with Google to watch, create & subscribe",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.labelMedium,
                        color = FlareOrange,
                        modifier = Modifier
                            .clickable { openPolicyTab = LegalPolicyTab.PRIVACY }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("auth_privacy_policy_link")
                    )
                    Text("•", color = Color.White.copy(alpha = 0.45f))
                    Text(
                        text = "Terms",
                        style = MaterialTheme.typography.labelMedium,
                        color = FlareOrange,
                        modifier = Modifier
                            .clickable { openPolicyTab = LegalPolicyTab.TERMS }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("auth_terms_link")
                    )
                    Text("•", color = Color.White.copy(alpha = 0.45f))
                    Text(
                        text = "Cookies",
                        style = MaterialTheme.typography.labelMedium,
                        color = FlareOrange,
                        modifier = Modifier
                            .clickable { openPolicyTab = LegalPolicyTab.COOKIES }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("auth_cookies_link")
                    )
                }
            }
        }
    }

    openPolicyTab?.let { tab ->
        LegalPoliciesDialog(
            initialTab = tab,
            onDismiss = { openPolicyTab = null }
        )
    }
}
