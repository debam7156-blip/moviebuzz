package com.moviebuzz.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.moviebuzz.app.ui.nav.MovieBuzzNavHost

@Stable
class MovieBuzzAppState(
    val navController: NavHostController
) {
    fun navigateToMovie(movieId: Long) {
        navController.navigate("movie/$movieId")
    }
}

@Composable
fun rememberAppState(): MovieBuzzAppState {
    val navController = rememberNavController()
    return remember { MovieBuzzAppState(navController) }
}

@Composable
fun MovieBuzzRoot(appState: MovieBuzzAppState) {
    MovieBuzzNavHost(navController = appState.navController)
}
