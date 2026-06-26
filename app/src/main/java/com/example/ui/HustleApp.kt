package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.home.HomeScreen
import com.example.ui.session.SessionScreen
import com.example.ui.setup.FirstLaunchScreen

import androidx.compose.runtime.setValue

@Composable
fun HustleApp(appViewModel: AppViewModel = viewModel(), shouldOpenSession: Boolean = false) {
    val navController = rememberNavController()
    val isSetupComplete by appViewModel.isSetupComplete.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(shouldOpenSession, isSetupComplete) {
        if (shouldOpenSession && isSetupComplete == true) {
            navController.navigate("session") {
                popUpTo("home")
            }
        }
    }
    
    if (isSetupComplete == null) {
        // Loading state
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController, 
            startDestination = if (isSetupComplete == true) "home" else "setup"
        ) {
            composable("setup") {
                val context = androidx.compose.ui.platform.LocalContext.current
                var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                FirstLaunchScreen(
                    onNewUser = { name ->
                        appViewModel.completeSetup(name)
                        navController.navigate("home") {
                            popUpTo("setup") { inclusive = true }
                        }
                    },
                    onExistingUser = { uri ->
                        isLoading = true
                        appViewModel.importDataAndCompleteSetup(context, uri) { success ->
                            isLoading = false
                            if (success) {
                                navController.navigate("home") {
                                    popUpTo("setup") { inclusive = true }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Failed to import backup.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onStartSession = {
                        navController.navigate("session")
                    },
                    onNavigateToCalendar = {
                        navController.navigate("calendar")
                    },
                    onNavigateToManualEntry = {
                        navController.navigate("manual_entry")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable("calendar") {
                com.example.ui.calendar.CalendarScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onDayClick = { dateMillis ->
                        navController.navigate("day_details/$dateMillis")
                    }
                )
            }
            composable("day_details/{dateMillis}") { backStackEntry ->
                val dateMillis = backStackEntry.arguments?.getString("dateMillis")?.toLongOrNull() ?: 0L
                com.example.ui.calendar.DayDetailsScreen(
                    dateMillis = dateMillis,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("manual_entry") {
                com.example.ui.manual.ManualEntryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                com.example.ui.settings.SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("session") {
                SessionScreen(
                    onSessionEnded = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

