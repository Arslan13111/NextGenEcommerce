package com.example.nextgenecommerce.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.example.nextgenecommerce.presentation.navigation.BottomNavItem
import com.example.nextgenecommerce.presentation.navigation.NavGraph
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.theme.NextGenEcommerceTheme
import com.example.nextgenecommerce.presentation.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkMode by themeViewModel.darkMode.collectAsStateWithLifecycle()

            NextGenEcommerceTheme(darkTheme = darkMode) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Wishlist,
        BottomNavItem.Search,
        BottomNavItem.Cart,
        BottomNavItem.Profile
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val showScaffold = currentRoute != Screen.Splash.route

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && showScaffold,
                enter = expandVertically(
                    expandFrom = androidx.compose.ui.Alignment.Bottom,
                    animationSpec = tween(durationMillis = 180)
                ),
                exit = shrinkVertically(
                    shrinkTowards = androidx.compose.ui.Alignment.Bottom,
                    animationSpec = tween(durationMillis = 150)
                )
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = if (currentRoute == Screen.Splash.route) Modifier else Modifier.padding(innerPadding),
            startDestination = Screen.Splash.route
        )
    }
}
