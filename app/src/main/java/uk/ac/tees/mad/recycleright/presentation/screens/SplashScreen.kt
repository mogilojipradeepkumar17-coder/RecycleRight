package uk.ac.tees.mad.recycleright.presentation.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import uk.ac.tees.mad.recycleright.R
import uk.ac.tees.mad.recycleright.presentation.viewmodel.AuthViewModel

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    //val auth = FirebaseAuth.getInstance()
    //val isLoggedIn = auth.currentUser != null


    val isLoggedIn=authViewModel.isLoggedIn()
    val scale = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = { OvershootInterpolator(4f).getInterpolation(it) }
        )
    )

    val alpha = animateFloatAsState(
        targetValue = if (scale.value == 1f) 1f else 0f,
        animationSpec = tween(durationMillis = 600)
    )

    LaunchedEffect(true) {
        delay(2000) 
        if (isLoggedIn) {
            navController.navigate(Screen.MainScreen.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.LoginScreen.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.oie_png),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Recycle Smart, Live Green",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = alpha.value),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
