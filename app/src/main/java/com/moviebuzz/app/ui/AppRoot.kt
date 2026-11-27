package com.moviebuzz.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moviebuzz.app.ui.nav.MovieBuzzNavHost
import com.moviebuzz.app.ui.nav.Routes
import com.moviebuzz.app.ui.nav.bottomDestinations

@Stable
class MovieBuzzAppState(
    val navController: NavHostController
) {
    fun navigateToMovie(movieId: Long) {
        navController.navigate(Routes.movieDetail(movieId))
    }

    /** Switch between bottom bar tabs without stacking duplicates. */
    fun navigateToTopLevel(route: String) {
        val controller = navController
        controller.navigate(route) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberAppState(): MovieBuzzAppState {
    val navController = rememberNavController()
    return remember { MovieBuzzAppState(navController) }
}

@Composable
fun MovieBuzzRoot(appState: MovieBuzzAppState) {
    val backStackEntry by appState.navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            selected = currentRoute == dest.route,
                            onClick = { appState.navigateToTopLevel(dest.route) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            MovieBuzzNavHost(navController = appState.navController)
        }
    }
}
