package com.example.todolist_v2

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object ToDo : Screen("todo_screen", "Zadania", Icons.Default.CheckCircle)
    object Shopping : Screen("shopping_screen", "Zakupy", Icons.Default.ShoppingCart)
    object Logging : Screen("logging_screen", "Login", Icons.Default.List)
}

private val navigationItems = listOf(Screen.ToDo, Screen.Shopping, Screen.Logging)

@Composable
fun MainAppScreen(
    todoViewModel: TodoViewModel,
    shoppingViewModel: ShoppingViewModel,
    onSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        bottomBar = { if (!isLandscape) BottomNavigationBar(navController = navController) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            NavigationGraph(
                modifier = Modifier.weight(1f),
                navController = navController,
                todoViewModel = todoViewModel,
                shoppingViewModel = shoppingViewModel,
                onSignIn = onSignIn
            )
            if (isLandscape) {
                AppNavigationRail(navController = navController)
            }
        }
    }
}

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    todoViewModel: TodoViewModel,
    shoppingViewModel: ShoppingViewModel,
    onSignIn: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ToDo.route,
        modifier = modifier
    ) {
        composable(Screen.ToDo.route) {
            ToDoScreen(viewModel = todoViewModel)
        }
        composable(Screen.Shopping.route) {
            val selectedListId by todoViewModel.selectedListId.collectAsState()
            ShoppingScreen(
                viewModel = shoppingViewModel, // Używamy przekazanej instancji
                selectedListId = selectedListId
            )
        }
        composable(Screen.Logging.route) {
            LoggingScreen(onSignIn = onSignIn)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        navigationItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavigationRail(navController: NavController) {
    NavigationRail {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        navigationItems.forEach { screen ->
            NavigationRailItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}