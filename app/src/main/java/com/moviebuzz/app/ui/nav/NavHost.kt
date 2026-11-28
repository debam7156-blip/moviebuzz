package com.moviebuzz.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moviebuzz.app.ui.screens.AiScreen
import com.moviebuzz.app.ui.screens.DiscoverScreen
import com.moviebuzz.app.ui.screens.HomeScreen
import com.moviebuzz.app.ui.screens.ImportIssuesScreen
import com.moviebuzz.app.ui.screens.ImportScreen
import com.moviebuzz.app.ui.screens.LibraryScreen
import com.moviebuzz.app.ui.screens.MovieDetailScreen
import com.moviebuzz.app.ui.screens.SearchScreen
import com.moviebuzz.app.ui.screens.SettingsScreen
import com.moviebuzz.app.ui.screens.StatsScreen
import com.moviebuzz.app.ui.screens.collections.CollectionDetailScreen
import com.moviebuzz.app.ui.screens.collections.CollectionsScreen

object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val COLLECTIONS = "collections"
    const val COLLECTION_DETAIL = "collection/{listId}"
    const val STATS = "stats"
    const val AI = "ai"
    const val IMPORT = "import"
    const val IMPORT_ISSUES = "import-issues"
    const val SETTINGS = "settings"
    const val MOVIE_DETAIL = "movie/{movieId}"
    fun movieDetail(movieId: Long) = "movie/$movieId"
    fun collectionDetail(listId: Long) = "collection/$listId"
}

/** Bottom navigation destinations (order matters for the bar). */
data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Filled.Home),
    BottomDestination(Routes.DISCOVER, "Discover", Icons.Filled.Movie),
    BottomDestination(Routes.SEARCH, "Search", Icons.Filled.Search),
    BottomDestination(Routes.COLLECTIONS, "Collections", Icons.Filled.CollectionsBookmark),
    BottomDestination(Routes.LIBRARY, "Library", Icons.Filled.Star)
)

@Composable
fun MovieBuzzNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.DISCOVER) { DiscoverScreen(navController) }
        composable(Routes.SEARCH) { SearchScreen(navController) }
        composable(Routes.LIBRARY) { LibraryScreen(navController) }
        composable(Routes.COLLECTIONS) { CollectionsScreen(navController) }
        composable(
            Routes.COLLECTION_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: 0L
            CollectionDetailScreen(navController, listId)
        }
        composable(Routes.STATS) { StatsScreen(navController) }
        composable(Routes.AI) { AiScreen(navController) }
        composable(Routes.IMPORT) { ImportScreen(navController) }
        composable(Routes.IMPORT_ISSUES) { ImportIssuesScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(
            Routes.MOVIE_DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.LongType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
            MovieDetailScreen(navController, movieId)
        }
    }
}
