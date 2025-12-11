package uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen

import android.Manifest
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.model.RecycleCategory
import uk.ac.tees.mad.recycleright.presentation.viewmodel.HomeUiState
import uk.ac.tees.mad.recycleright.presentation.viewmodel.HomeViewModel
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ScanningState
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scanningState by viewModel.scanningState.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Camera temp file
    val tempFile = remember {
        File.createTempFile("barcode_", ".jpg", context.cacheDir).apply {
            deleteOnExit()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            // Show processing state immediately
            viewModel.startImageProcessing()

            processBarcodeFromImage(
                context = context,
                uri = imageUri!!,
                onBarcodeDetected = { barcode ->
                    viewModel.searchByBarcode(barcode)
                },
                onNoBarcodeFound = {
                    // Clear scanning state before showing error
                    viewModel.clearScanningState()
                    Toast.makeText(
                        context,
                        "No barcode detected. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = { error ->
                    // Clear scanning state before showing error
                    viewModel.clearScanningState()
                    Toast.makeText(
                        context,
                        "Failed to process image: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            // Photo was cancelled or failed
            viewModel.clearScanningState()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            imageUri = uri
            cameraLauncher.launch(uri)
        } else {
            showPermissionDialog = true
            viewModel.clearScanningState()
        }
    }

    // Handle scanning state changes
    LaunchedEffect(scanningState) {
        when (val state = scanningState) {
            is ScanningState.Success -> {
                Toast.makeText(
                    context,
                    "✓ Found: ${state.item.name}",
                    Toast.LENGTH_SHORT
                ).show()
                // Auto-clear after brief delay
                kotlinx.coroutines.delay(1500)
                viewModel.clearScanningState()
            }
            is ScanningState.Error -> {
                Toast.makeText(
                    context,
                    "✗ ${state.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Auto-clear after showing error
                kotlinx.coroutines.delay(2000)
                viewModel.clearScanningState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Search & Scan Section
            SearchAndScanCard(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onClearSearch = { viewModel.clearSearch() },
                onScanClick = {
                    // Prevent multiple concurrent scans
                    if (scanningState is ScanningState.Idle) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                isScanning = scanningState !is ScanningState.Idle
            )

            // Results Section
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingView()
                }
                is HomeUiState.Success -> {
                    ItemsList(
                        items = state.items,
                        searchQuery = searchQuery,
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onItemClick = onItemClick,
                    )
                }
                is HomeUiState.Error -> {
                    ErrorView(message = state.message)
                }
            }
        }

        // Scanning Dialog - Show for all scanning states
        when (scanningState) {
            is ScanningState.ProcessingImage -> {
                ScanningDialog(message = "Detecting barcode...")
            }
            is ScanningState.FetchingProduct -> {
                ScanningDialog(message = "Looking up product information...")
            }
            else -> {}
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
            },
            title = { Text("Camera Permission Required") },
            text = {
                Text("RecycleRight needs camera access to scan barcodes. Please enable it in Settings.")
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK", color = Color(0xFF4CAF50))
                }
            }
        )
    }
}

@Composable
fun SearchAndScanCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onScanClick: () -> Unit,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App Title
            Text(
                text = "♻️ RecycleRight",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Search or scan to check recyclability",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search items...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = true,
                enabled = !isScanning
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scan Button
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isScanning) "Processing..." else "Scan Barcode",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ItemsList(
    items: List<RecyclableItem>,
    searchQuery: String,
    onFavoriteClick: (RecyclableItem) -> Unit,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            ItemsHeader(
                count = items.size,
                searchQuery = searchQuery,
                items = items
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(items, key = { it.id }) { item ->
            RecyclableItemCard(
                item = item,
                onFavoriteClick = { onFavoriteClick(item) },
                onClick = { onItemClick(item.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ItemsHeader(
    count: Int,
    searchQuery: String,
    items: List<RecyclableItem>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (searchQuery.isBlank()) {
                "All Items ($count)"
            } else {
                "Results ($count)"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val recyclable = items.count { it.category == RecycleCategory.RECYCLABLE }
            val waste = items.count { it.category == RecycleCategory.GENERAL_WASTE }
            val compost = items.count { it.category == RecycleCategory.COMPOSTABLE }

            if (recyclable > 0) {
                CategoryChip(recyclable, Color(0xFF4CAF50), Icons.Default.Recycling)
            }
            if (waste > 0) {
                CategoryChip(waste, Color(0xFFF44336), Icons.Default.Delete)
            }
            if (compost > 0) {
                CategoryChip(compost, Color(0xFFFF9800), Icons.Default.Nature)
            }
        }
    }
}

@Composable
fun CategoryChip(
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecyclableItemCard(
    item: RecyclableItem,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(item.category.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.category) {
                        RecycleCategory.RECYCLABLE -> Icons.Default.Recycling
                        RecycleCategory.GENERAL_WASTE -> Icons.Default.Delete
                        RecycleCategory.COMPOSTABLE -> Icons.Default.Nature
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(item.category.color)
                )
                if (item.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
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

@Composable
fun ScanningDialog(message: String = "Processing...") {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal during scan */ },
        icon = {
            CircularProgressIndicator(
                color = Color(0xFF4CAF50),
                modifier = Modifier.size(40.dp)
            )
        },
        title = { Text("Scanning") },
        text = { Text(message) },
        confirmButton = { /* No button during scanning */ }
    )
}

// Barcode detection helper
private fun processBarcodeFromImage(
    context: android.content.Context,
    uri: Uri,
    onBarcodeDetected: (String) -> Unit,
    onNoBarcodeFound: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val bitmap = android.provider.MediaStore.Images.Media.getBitmap(
            context.contentResolver,
            uri
        )
        val image = InputImage.fromBitmap(bitmap, 0)

        BarcodeScanning.getClient()
            .process(image)
            .addOnSuccessListener { barcodes ->
                when {
                    barcodes.isEmpty() -> onNoBarcodeFound()
                    else -> {
                        val barcode = barcodes.first().rawValue
                        if (barcode.isNullOrEmpty()) {
                            onNoBarcodeFound()
                        } else {
                            onBarcodeDetected(barcode)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Unknown error")
            }
    } catch (e: Exception) {
        onError(e.message ?: "Failed to load image")
    }
}