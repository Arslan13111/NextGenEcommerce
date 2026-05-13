package com.example.nextgenecommerce.presentation.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.R
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Animation states
    var startAnimation by remember { mutableStateOf(false) }

    // Logo scale & alpha animation
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logoAlpha"
    )

    // Title slide-up animation
    val titleOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 60f,
        animationSpec = tween(durationMillis = 900, delayMillis = 400, easing = EaseOutCubic),
        label = "titleOffsetY"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 400),
        label = "titleAlpha"
    )

    // Subtitle animation
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 700),
        label = "subtitleAlpha"
    )
    val subtitleOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(durationMillis = 800, delayMillis = 700, easing = EaseOutCubic),
        label = "subtitleOffsetY"
    )

    // Tagline animation
    val taglineAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.7f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 1000),
        label = "taglineAlpha"
    )

    // Loading indicator animation
    val loadingAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 1200),
        label = "loadingAlpha"
    )

    // Continuous shimmer for decorative ring
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Pulsing glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2800)
        if (viewModel.isUserLoggedIn()) {
            // Give a moment for currentUser to load from Supabase if not ready yet
            if (viewModel.currentUser.value == null) delay(600)
            val user = viewModel.currentUser.value
            val destination = when {
                user?.isDeliveryPartner() == true -> Screen.DeliveryDashboard.route
                user?.isRetailer() == true        -> Screen.RetailerDashboard.route
                user?.isAdmin() == true           -> Screen.AdminDashboard.route
                else                              -> Screen.Home.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF111111),
                        Color(0xFF0A0A0A),
                        Color(0xFF000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle radial glow behind logo
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(glowScale)
                .alpha(logoAlpha * 0.12f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo with decorative ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Rotating decorative ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Inner dark circle
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .background(Color(0xFF0A0A0A), CircleShape)
                )

                // Logo image
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "NextGen Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App title "NextGen"
            Text(
                text = "NextGen",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleOffsetY }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle "E-COMMERCE"
            Text(
                text = "E - C O M M E R C E",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp,
                    fontSize = 18.sp
                ),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .graphicsLayer { translationY = subtitleOffsetY }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Decorative line
            Box(
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .graphicsLayer { translationY = subtitleOffsetY }
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "Shop the Future",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    fontSize = 14.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            Box(modifier = Modifier.alpha(loadingAlpha)) {
                // Custom loading dots
                LoadingDots()
            }
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
