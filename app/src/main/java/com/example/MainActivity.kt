package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.EditorViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as NextCutApplication
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModelFactory(app.repository)
            )
            MyApplicationTheme {
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: EditorViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { projectId ->
                    navController.navigate("editor/$projectId") {
                        viewModel.selectedClipId = null
                        viewModel.selectedTextId = null
                        viewModel.selectedAudioId = null
                        viewModel.selectedEffectId = null
                        viewModel.selectedTransitionSourceId = null
                        viewModel.selectedTransitionTargetId = null
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "editor/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            EditorScreen(
                viewModel = viewModel,
                projectId = projectId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
