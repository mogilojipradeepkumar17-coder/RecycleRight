package uk.ac.tees.mad.recycleright.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uk.ac.tees.mad.recycleright.presentation.screens.SplashScreen
import uk.ac.tees.mad.recycleright.presentation.screens.LoginScreen
import uk.ac.tees.mad.recycleright.presentation.screens.MainScreen
import uk.ac.tees.mad.recycleright.presentation.screens.Screen
import uk.ac.tees.mad.recycleright.presentation.screens.SignUpScreen
import uk.ac.tees.mad.recycleright.presentation.viewmodel.AuthViewModel

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    val authViewModel = hiltViewModel<AuthViewModel>()

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {


        composable(
            route = Screen.SplashScreen.route
        ) {
            SplashScreen(
                navController=navController,
                authViewModel=authViewModel,
            )
        }

        composable(
            route = Screen.LoginScreen.route
        ) {
            LoginScreen(
                navController=navController,
                authViewModel=authViewModel
            )
        }

        composable(
            route = Screen.SignUpScreen.route
        ) {
            SignUpScreen(
                navController=navController,
                authViewModel=authViewModel
            )
        }

        composable(
            route = Screen.MainScreen.route
        ) {
            MainScreen(
                logout = {
                    authViewModel.logout {
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }

}