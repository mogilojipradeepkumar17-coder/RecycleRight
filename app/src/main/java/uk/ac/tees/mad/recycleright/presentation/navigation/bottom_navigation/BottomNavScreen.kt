package uk.ac.tees.mad.recycleright.presentation.navigation.bottom_navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavScreen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )


//    object Motivation : BottomNavScreen(
//        route = "motivation",
//        title = "Motivation",
//        icon = Icons.Default.FormatQuote
//    )

    object Reminder : BottomNavScreen(
        route = "reminders",
        title = "Reminders",
        icon = Icons.Default.AddCircle
    )
    object Profile : BottomNavScreen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
}

val bottomNavScreens = listOf(
    BottomNavScreen.Home,
    BottomNavScreen.Reminder,
    BottomNavScreen.Profile
)