package uk.ac.tees.mad.recycleright.presentation.screens.bottom_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import uk.ac.tees.mad.recycleright.data.model.RecyclableItem
import uk.ac.tees.mad.recycleright.data.model.RecycleCategory
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ItemDetailsUiState
import uk.ac.tees.mad.recycleright.presentation.viewmodel.ItemDetailsViewModel

@Composable
fun ItemDetailsScreen(
    modifier: Modifier = Modifier,
    itemId: String,
    onBackClick: () -> Unit,
    viewModel: ItemDetailsViewModel = hiltViewModel()
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle()


    // load the item
    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        when (val state = uiState.value) {
            ItemDetailsUiState.Loading -> {
                LoadingView()
            }

            is ItemDetailsUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadItem(itemId) }
                )
            }

            is ItemDetailsUiState.Success -> {
                ItemDetailsContent(
                    item = state.item,
                    onFavoriteClick = { viewModel.toggleFavorite(state.item) }
                )
            }
        }
    }

}

@Composable
fun ItemDetailsContent(
    modifier: Modifier = Modifier,
    item: RecyclableItem,
    onFavoriteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Item Details",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color(0xFF2E7D32)
        )

//        Text("Item Details", fontSize = 24.sp,modifier=Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        // Image Card
        if (!item.imageUrl.isNullOrEmpty()) {
            ImageCard(imageUrl = item.imageUrl)
        }

        // Main Info Card
        MainInfoCard(
            item = item,
            onFavoriteClick = onFavoriteClick
        )

        // Category Card
        CategoryCard(category = item.category)

        // Description Card
        if (item.description.isNotEmpty()) {
            InfoCard(
                title = "Description",
                icon = Icons.Default.Info,
                content = item.description
            )
        }

        // Recycling Tips Card
        if (item.tips.isNotEmpty()) {
            InfoCard(
                title = "Recycling Tips",
                icon = Icons.Default.Lightbulb,
                content = item.tips,
                backgroundColor = Color(0xFFFFF9C4)
            )
        }

        // Barcode Info Card
        if (!item.barcode.isNullOrEmpty()) {
            BarcodeCard(barcode = item.barcode)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

}

@Composable
fun ImageCard(imageUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Product Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun MainInfoCard(
    item: RecyclableItem,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            }

            // Favorite Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = if (item.isFavorite) {
                        "Remove from favorites"
                    } else {
                        "Add to favorites"
                    },
                    tint = if (item.isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: RecycleCategory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color(category.color).copy(alpha = 0.1f)
//        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(category.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category) {
                        RecycleCategory.RECYCLABLE -> Icons.Default.CheckCircle
                        RecycleCategory.GENERAL_WASTE -> Icons.Default.Cancel
                        RecycleCategory.COMPOSTABLE -> Icons.Default.Eco
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(category.color)
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: String,
    backgroundColor: Color = Color.White
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF424242),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
            )
        }
    }
}

@Composable
fun BarcodeCard(barcode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Barcode",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = barcode,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Text(
                text = "Loading item details...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFF44336).copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
