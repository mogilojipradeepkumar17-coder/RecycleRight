package uk.ac.tees.mad.recycleright.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


val primaryGreen = Color(0xFF4CAF50)
val lightGreen = Color(0xFFE8F5E9)
val darkGreen = Color(0xFF2E7D32)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(modifier: Modifier = Modifier,navigateToProfileScreen:()->Unit,) {
    TopAppBar(
        title = {
            Text(
                text = "RecycleRight",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = darkGreen
        ),
        actions = {
            IconButton(onClick = {
                navigateToProfileScreen()
            }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White
                )
            }
        }
    )
}