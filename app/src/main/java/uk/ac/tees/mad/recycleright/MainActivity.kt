package uk.ac.tees.mad.recycleright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import uk.ac.tees.mad.recycleright.presentation.navigation.AppNavigation
import uk.ac.tees.mad.recycleright.ui.theme.RecycleRightTheme



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecycleRightTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    val navController = rememberNavController()
//                    SplashScreen(navController)
//                }

                AppNavigation()
            }
        }
    }
}

