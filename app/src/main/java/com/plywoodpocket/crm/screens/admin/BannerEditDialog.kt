package com.plywoodpocket.crm.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.plywoodpocket.crm.models.Banner
import java.io.File

@Composable
fun BannerEditDialog(
    banner: Banner?,
    onDismiss: () -> Unit,
    onSave: (title: String, imageFile: File?, isActive: Boolean, order: Int) -> Unit
) {
    var title by remember { mutableStateOf(banner?.title ?: "") }
    var isActive by remember { mutableStateOf(banner?.is_active ?: true) }
    var orderText by remember { mutableStateOf(banner?.order?.toString() ?: "0") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val file = File(context.cacheDir, "picked_image_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            imageFile = file
        }
    }

    fun validateForm(): Boolean {
        var isValid = true
        
        // Title validation (required, max 255 chars)
        if (title.isBlank()) {
            titleError = "Title is required"
            isValid = false
        } else if (title.length > 255) {
            titleError = "Title must be less than 255 characters"
            isValid = false
        } else {
            titleError = null
        }

        // Order validation (must be a valid integer)
        val order = orderText.toIntOrNull()
        if (order == null) {
            orderError = "Order must be a valid number"
            isValid = false
        } else {
            orderError = null
        }

        // Image validation (required for new banner)
        if (banner == null && imageFile == null) {
            isValid = false
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (banner == null) "Add Banner" else "Edit Banner") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = null
                    },
                    label = { Text("Title*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = orderText,
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() }) {
                            orderText = newText
                            orderError = null
                        }
                    },
                    label = { Text("Order") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = orderError != null,
                    supportingText = { orderError?.let { Text(it) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Active")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text(if (banner == null) "Pick Image*" else "Change Image")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Picked Image",
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else if (banner?.image_url != null) {
                        AsyncImage(
                            model = banner.image_url,
                            contentDescription = "Current Banner Image",
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateForm()) {
                        onSave(
                            title.trim(),
                            imageFile,
                            isActive,
                            orderText.toIntOrNull() ?: 0
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
