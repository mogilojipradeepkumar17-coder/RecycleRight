package uk.ac.tees.mad.recycleright.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Text(
                text = "Set Reminders",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = darkGreen
        ),
        actions = {
//            IconButton(onClick = { /* Navigate to notifications */ }) {
//                Icon(
//                    imageVector = Icons.Default.Notifications,
//                    contentDescription = "Notifications",
//                    tint = Color.White
//                )
//            }
        }
    )
}