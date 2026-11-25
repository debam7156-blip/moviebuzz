package com.moviebuzz.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.moviebuzz.app.ui.screens.HomeScreen
import com.moviebuzz.app.ui.screens.DiscoverScreen
import com.moviebuzz.app.ui.screens.LibraryScreen
import com.moviebuzz.app.ui.screens.StatsScreen
import com.moviebuzz.app.ui.screens.AiScreen
import com.moviebuzz.app.ui.screens.MovieDetailScreen
import com.moviebuzz.app.ui.screens.ImportScreen
import com.moviebuzz.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val LIBRARY = "library"
    const val STATS = "stats"
    const val AI = "ai"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val MOVIE_DETAIL = "movie/{movieId}"
    fun movieDetail(movieId: Long) = "movie/$movieId"
}

@Composable
fun MovieBuzzNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.DISCOVER) { DiscoverScreen(navController) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.STATS) { StatsScreen(navController) }
        composable(Routes.AI) { AiScreen(navController) }
        composable(Routes.IMPORT) { ImportScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.MOVIE_DETAIL) { MovieDetailScreen(navController) }
    }
}
