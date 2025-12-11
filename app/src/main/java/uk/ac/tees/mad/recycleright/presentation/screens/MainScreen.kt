package uk.ac.tees.mad.recycleright.presentation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uk.ac.tees.mad.recycleright.presentation.components.HomeTopBar
import uk.ac.tees.mad.recycleright.presentation.components.ReminderTopBar
import uk.ac.tees.mad.recycleright.presentation.navigation.bottom_navigation.BottomNavScreen
import uk.ac.tees.mad.recycleright.presentation.navigation.bottom_navigation.bottomNavScreens
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.HomeScreen
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.ItemDetailsScreen
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.ProfileScreen
import uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen.ReminderScreen
import uk.ac.tees.mad.recycleright.presentation.viewmodel.HomeViewModel
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ItemDetailsViewModel
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ProfileViewModel
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ReminderViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    logout: () -> Unit,
) {

    val bottomNavController = rememberNavController()
    val navBackStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry.value?.destination?.route ?: BottomNavScreen.Home.route

    val context = LocalContext.current

    val homeViewModel= hiltViewModel<HomeViewModel>()
    val reminderViewModel=hiltViewModel<ReminderViewModel>()
    val itemDetailViewModel=hiltViewModel<ItemDetailsViewModel>()
    val profileViewModel=hiltViewModel<ProfileViewModel>()

    Scaffold(

        topBar = {
            when (currentRoute) {
                BottomNavScreen.Home.route -> {
                    HomeTopBar(
                        navigateToProfileScreen = {
                            bottomNavController.navigate(BottomNavScreen.Profile.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                BottomNavScreen.Reminder.route -> {
                    ReminderTopBar()
                }
            }
        },
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
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = BottomNavScreen.Home.route
            ) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onItemClick = { itemId ->
                        bottomNavController.navigate("item_details/$itemId")
                    }
                )
            }


            composable(
                route = BottomNavScreen.Reminder.route
            ) {
                ReminderScreen(
                    viewModel = reminderViewModel
                )
            }

            composable(
                route = BottomNavScreen.Profile.route
            ) {
                ProfileScreen(
                    onLogout = {
                        // clear the the items
                        homeViewModel.clearAllItemsPer()
                            // clear the reminder
                        reminderViewModel.clearAllReminders()
                        logout()
                    }
                )
            }

            composable(
                route = "item_details/{itemId}",
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                ItemDetailsScreen(
                    itemId = itemId,
                    onBackClick = {
                        bottomNavController.popBackStack()
                    }
                )

            }
        }
    }

}