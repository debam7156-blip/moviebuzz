package com.moviebuzz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviebuzz.app.ui.MovieBuzzRoot
import com.moviebuzz.app.ui.rememberAppState
import com.moviebuzz.app.ui.theme.MovieBuzzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MovieBuzzTheme {
                val appState = rememberAppState()
                MovieBuzzRoot(appState)
            }
        }
    }
}
