package uk.ac.tees.mad.recycleright.presentation.screens

sealed class Screen(val route:String) {
    object SplashScreen:Screen("splash")
    object SignUpScreen:Screen("sign_up")
    object LoginScreen:Screen("login")
    object MainScreen:Screen("main")
}