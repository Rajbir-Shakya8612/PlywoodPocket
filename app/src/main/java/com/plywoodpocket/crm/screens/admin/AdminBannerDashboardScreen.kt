package com.plywoodpocket.crm.screens.admin

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.plywoodpocket.crm.models.Banner
import com.plywoodpocket.crm.viewmodel.BannerAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBannerDashboardScreen(
    viewModel: BannerAdminViewModel,
    onBack: () -> Unit
) {
    val banners = viewModel.banners
    val isLoading = viewModel.isLoading
    val error = viewModel.error
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(24.dp)

    var showDeleteConfirmation by remember { mutableStateOf<Banner?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
                .background(Color.Black)
        )
        TopAppBar(
            title = {
                Text(
                    text = "Banner Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.error = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                banners.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "No Banners",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No banners found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(banners) { banner ->
                            BannerRow(
                                banner = banner,
                                onEdit = {
                                    viewModel.editingBanner = banner
                                    viewModel.showDialog = true
                                },
                                onDelete = { showDeleteConfirmation = banner }
                            )
                        }
                    }
                }
            }

            if (viewModel.showDialog) {
                BannerEditDialog(
                    banner = viewModel.editingBanner,
                    onDismiss = {
                        viewModel.showDialog = false
                        viewModel.editingBanner = null
                        viewModel.pickedImageUri = null
                    },
                    onSave = { title, imageFile, isActive, order ->
                        if (viewModel.editingBanner == null) {
                            if (imageFile != null) {
                                viewModel.createBanner(title, imageFile, isActive, order)
                            }
                        } else {
                            viewModel.updateBanner(
                                viewModel.editingBanner!!.id,
                                title,
                                imageFile,
                                isActive,
                                order
                            )
                        }
                    }
                )
            }

            if (showDeleteConfirmation != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = null },
                    title = { Text("Delete Banner") },
                    text = { Text("Are you sure you want to delete this banner?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmation?.let { banner ->
                                    viewModel.deleteBanner(banner.id)
                                }
                                showDeleteConfirmation = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteConfirmation = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            FloatingActionButton(
                onClick = {
                    viewModel.editingBanner = null
                    viewModel.showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 64.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Banner", tint = Color.White)
            }
        }
    }
}

@Composable
fun BannerRow(
    banner: Banner,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = banner.image_url,
                contentDescription = banner.title,
                modifier = Modifier
                    .size(80.dp)
                    .weight(0.3f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(0.7f)
            ) {
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Order: ${banner.order}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (banner.is_active) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (banner.is_active) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
