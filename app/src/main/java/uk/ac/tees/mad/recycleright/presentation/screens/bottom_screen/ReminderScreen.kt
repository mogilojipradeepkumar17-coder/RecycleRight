package uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.ac.tees.mad.recycleright.data.model.Reminder
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ReminderUiState
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ReminderViewModel

@Composable
fun ReminderScreen(
    viewModel: ReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Optional: Show a message that notifications won't work
            // You can add a Snackbar here if you want
        }
    }

    // Request permission when screen opens (only on Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Header Card
            ReminderHeaderCard(
                onAddClick = { showAddDialog = true }
            )

            // Reminders List
            when (val state = uiState) {
                is ReminderUiState.Loading -> {
                    LoadingView()
                }
                is ReminderUiState.Success -> {
                    if (state.reminders.isEmpty()) {
                        EmptyRemindersView(
                            onAddClick = { showAddDialog = true }
                        )
                    } else {
                        RemindersList(
                            reminders = state.reminders,
                            onToggle = { viewModel.toggleReminder(it) },
                            onEdit = {
                                editingReminder = it
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteReminder(it) }
                        )
                    }
                }
                is ReminderUiState.Error -> {
                    ErrorView(message = state.message)
                }
            }
        }

        // Add/Edit Reminder Dialog
        if (showAddDialog) {
            AddReminderDialog(
                existingReminder = editingReminder,
                onDismiss = {
                    showAddDialog = false
                    editingReminder = null
                },
                onSave = { reminder ->
                    if (editingReminder != null) {
                        viewModel.updateReminder(reminder)
                    } else {
                        viewModel.addReminder(reminder)
                    }
                    showAddDialog = false
                    editingReminder = null
                }
            )
        }
    }
}


@Preview
@Composable
private fun ReminderHeaderCardPreview() {
    ReminderHeaderCard({})
}

@Composable
fun ReminderHeaderCard(
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔔 My Reminders",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set bin days and eco challenges",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Reminder",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RemindersList(
    reminders: List<Reminder>,
    onToggle: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            ReminderCard(
                reminder = reminder,
                onToggle = { onToggle(reminder) },
                onEdit = { onEdit(reminder) },
                onDelete = { onDelete(reminder) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day indicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (reminder.isEnabled) Color(0xFF4CAF50) else Color.Gray
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reminder.getDayString().take(3),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Reminder info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.isEnabled) Color(0xFF212121) else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reminder.getDayString()} at ${reminder.getTimeString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reminder.isEnabled) Color(0xFF4CAF50) else Color.Gray
                )
                if (reminder.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFF44336)
                )
            },
            title = { Text("Delete Reminder?") },
            text = { Text("Are you sure you want to delete '${reminder.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
private fun EmptyRemindersViewPreview() {
    EmptyRemindersView({})
}

@Composable
fun EmptyRemindersView(
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Reminders Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set reminders for bin days and Eco challenges",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier=Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Reminder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    existingReminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var description by remember { mutableStateOf(existingReminder?.description ?: "") }
    var selectedDay by remember { mutableStateOf(existingReminder?.dayOfWeek ?: 1) }
    var selectedHour by remember { mutableStateOf(existingReminder?.hourOfDay ?: 9) }
    var selectedMinute by remember { mutableStateOf(existingReminder?.minute ?: 0) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    var expandedDay by remember { mutableStateOf(false) }
    var expandedHour by remember { mutableStateOf(false) }
    var expandedMinute by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingReminder != null) "Edit Reminder" else "Add Reminder")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Recycling Bin Day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g., Put out blue bin") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Day selector
                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { expandedDay = it }
                ) {
                    OutlinedTextField(
                        value = days[selectedDay - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        days.forEachIndexed { index, day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = index + 1
                                    expandedDay = false
                                }
                            )
                        }
                    }
                }

                // Time selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hour
                    ExposedDropdownMenuBox(
                        expanded = expandedHour,
                        onExpandedChange = { expandedHour = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", selectedHour),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hour") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHour) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedHour,
                            onDismissRequest = { expandedHour = false }
                        ) {
                            hours.forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", hour)) },
                                    onClick = {
                                        selectedHour = hour
                                        expandedHour = false
                                    }
                                )
                            }
                        }
                    }

                    // Minute
                    ExposedDropdownMenuBox(
                        expanded = expandedMinute,
                        onExpandedChange = { expandedMinute = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", selectedMinute),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Minute") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMinute) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMinute,
                            onDismissRequest = { expandedMinute = false }
                        ) {
                            minutes.chunked(5).flatten().forEach { minute ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", minute)) },
                                    onClick = {
                                        selectedMinute = minute
                                        expandedMinute = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val reminder = Reminder(
                            id = existingReminder?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            dayOfWeek = selectedDay,
                            hourOfDay = selectedHour,
                            minute = selectedMinute,
                            isEnabled = existingReminder?.isEnabled ?: true
                        )
                        onSave(reminder)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF4CAF50))
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFF44336).copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}