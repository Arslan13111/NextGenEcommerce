package com.example.nextgenecommerce.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.nextgenecommerce.presentation.navigation.BottomNavItem
import com.example.nextgenecommerce.presentation.navigation.NavGraph
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.theme.NextGenEcommerceTheme
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.SendNotificationViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ThemeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Enable edge-to-edge to ensure WindowInsets (like IME) are correctly dispatched
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val darkModePref by themeViewModel.darkMode.collectAsStateWithLifecycle()
            val useSystemTheme by themeViewModel.useSystemTheme.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            val darkMode = if (useSystemTheme) systemInDarkTheme else darkModePref

            NextGenEcommerceTheme(darkTheme = darkMode) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val cartViewModel = hiltViewModel<CartViewModel>()
    val authViewModel = hiltViewModel<AuthViewModel>()
    val sendNotificationViewModel = hiltViewModel<SendNotificationViewModel>()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Start Realtime notification listener once the user is authenticated
    LaunchedEffect(currentUser?.id) {
        currentUser?.let { user ->
            val role = when {
                user.isAdmin() -> "admin"
                user.isRetailer() -> "retailer"
                user.isDeliveryPartner() -> "delivery_partner"
                else -> "customer"
            }
            sendNotificationViewModel.startListeningForIncoming(role, user.id)
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Keyboard visibility detection
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val bottomNavItems = remember {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Wishlist,
            BottomNavItem.Search,
            BottomNavItem.Cart,
            BottomNavItem.Profile
        )
    }

    val showBottomBar by remember(currentRoute, isKeyboardVisible) {
        derivedStateOf {
            currentRoute in bottomNavItems.map { it.route } && !isKeyboardVisible
        }
    }

    val showScaffold by remember(currentRoute) {
        derivedStateOf {
            currentRoute != Screen.Splash.route
        }
    }

    val floatingBottomNavInset = 92.dp
    val navHostModifier = when (currentRoute) {
        Screen.Splash.route,
        Screen.Login.route,
        Screen.Register.route,
        Screen.OrderSuccess.route -> Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()

        Screen.Profile.route -> Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = floatingBottomNavInset)

        Screen.Wishlist.route,
        Screen.Search.route -> Modifier
            .fillMaxSize()
            .padding(bottom = floatingBottomNavInset)

        Screen.Notifications.route,
        Screen.Vault.route -> Modifier
            .fillMaxSize()
            .navigationBarsPadding()

        else -> Modifier.fillMaxSize()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                cartViewModel = cartViewModel,
                modifier = navHostModifier.padding(innerPadding),
                startDestination = Screen.Splash.route
            )
        }

        // Overlay Bottom Bar - Persistent container to prevent layout shifts
        androidx.compose.animation.AnimatedVisibility(
            visible = showBottomBar && showScaffold,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                NavigationBar(
                    containerColor = Color(0xFF111111),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = null,
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color(0xFF888888),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}
