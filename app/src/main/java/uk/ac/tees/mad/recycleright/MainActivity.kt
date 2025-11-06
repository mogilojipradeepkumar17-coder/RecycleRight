package uk.ac.tees.mad.recycleright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import uk.ac.tees.mad.recycleright.presentation.SplashScreen
import uk.ac.tees.mad.recycleright.ui.theme.RecycleRightTheme

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

//                App
            }
        }
    }
}

