package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moviebuzz.app.ui.nav.Routes

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("MOVIEBUZZ") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Your personal movie universe.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Routes.LIBRARY) }) {
                Text("Open your library")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate(Routes.IMPORT) }) {
                Text("Import your history")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate(Routes.DISCOVER) }) {
                Text("Discover films")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate(Routes.STATS) }) {
                Text("View statistics")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate(Routes.AI) }) {
                Text("Ask MOVIEBUZZ AI")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                Text("Settings")
            }
        }
    }
}
