package uk.ac.tees.mad.recycleright.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.ac.tees.mad.recycleright.presentation.navigation.bottom_navigation.BottomNavScreen
import uk.ac.tees.mad.recycleright.presentation.navigation.bottom_navigation.bottomNavScreens
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.HomeScreen
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.ProfileScreen
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.ReminderScreen

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    logout:()->Unit,
) {

    val bottomNavController = rememberNavController()
    val navBackStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry.value?.destination?.route ?: BottomNavScreen.Home.route

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = {
                            Text(screen.title)
                        },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.title)
                        }
                    )
                }
            }
        }
    ){innerPadding->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(
                route= BottomNavScreen.Home.route
            ) {
                HomeScreen()
            }


            composable(
                route= BottomNavScreen.Reminder.route
            ) {
                ReminderScreen()
            }

            composable(
                route= BottomNavScreen.Profile.route
            ) {
                ProfileScreen()
            }
        }
    }

}