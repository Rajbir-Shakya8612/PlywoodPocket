package com.plywoodpocket.crm.viewmodel

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plywoodpocket.crm.api.ApiService
import com.plywoodpocket.crm.models.Banner
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class BannerAdminViewModel(private val apiService: ApiService) : ViewModel() {

    var banners by mutableStateOf<List<Banner>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    var editingBanner by mutableStateOf<Banner?>(null)
    var showDialog by mutableStateOf(false)
    var pickedImageUri by mutableStateOf<Uri?>(null)

    init {
        fetchBanners()
    }

    fun fetchBanners() {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val response = apiService.getBanners()
                if (response.isSuccessful) {
                    banners = response.body()?.banners ?: emptyList()
                } else {
                    val errorBody = response.errorBody()?.string()
                    error = "Failed to load banners: $errorBody"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    fun openAddBannerDialog() {
        editingBanner = null
        pickedImageUri = null
        showDialog = true
    }

    fun createBanner(title: String, imageFile: File, isActive: Boolean, order: Int) {
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val titlePart = MultipartBody.Part.createFormData("title", title)
                val imagePart = MultipartBody.Part.createFormData(
                    "image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                val isActivePart = (if (isActive) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
                val orderPart = order.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val linkPart = null // Since link is optional

                val response = apiService.createBanner(
                    titlePart, imagePart, linkPart, isActivePart, orderPart
                )

                if (response.isSuccessful) {
                    fetchBanners()
                    showDialog = false
                    pickedImageUri = null
                } else {
                    val errorBody = response.errorBody()?.string()
                    error = "Failed to create banner: $errorBody"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateBanner(id: Int, title: String, imageFile: File?, isActive: Boolean, order: Int) {
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val methodPart = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
                val titlePart = MultipartBody.Part.createFormData("title", title)
                val imagePart = imageFile?.let {
                    MultipartBody.Part.createFormData(
                        "image", it.name, it.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                }
                val isActivePart = (if (isActive) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
                val orderPart = order.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val linkPart = null // Since link is optional

                val response = apiService.updateBanner(
                    id, methodPart, titlePart, imagePart, linkPart, isActivePart, orderPart
                )

                if (response.isSuccessful) {
                    fetchBanners()
                    showDialog = false
                    editingBanner = null
                    pickedImageUri = null
                } else {
                    val errorBody = response.errorBody()?.string()
                    error = "Failed to update banner: $errorBody"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteBanner(id: Int) {
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val response = apiService.deleteBanner(id)
                if (response.isSuccessful) {
                    fetchBanners()
                } else {
                    val errorBody = response.errorBody()?.string()
                    error = "Failed to delete banner: $errorBody"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Unknown error occurred"
            } finally {
                isLoading = false
            }
        }
    }
}
