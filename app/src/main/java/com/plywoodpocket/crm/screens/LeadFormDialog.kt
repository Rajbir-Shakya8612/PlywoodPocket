package com.plywoodpocket.crm.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.plywoodpocket.crm.models.Lead
import com.plywoodpocket.crm.models.LeadRequest
import com.plywoodpocket.crm.models.LeadStatus
import com.plywoodpocket.crm.utils.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadFormDialog(
    lead: Lead?,
    statuses: List<LeadStatus>,
    defaultStatus: LeadStatus? = null,
    onDismiss: () -> Unit,
    onSubmit: (LeadRequest, Int?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(TextFieldValue(lead?.name ?: "")) }
    var phone by remember { mutableStateOf(TextFieldValue(lead?.phone ?: "")) }
    var email by remember { mutableStateOf(TextFieldValue(lead?.email ?: "")) }
    var address by remember { mutableStateOf(TextFieldValue(lead?.address ?: "")) }
    var statusId by remember { mutableStateOf(lead?.status_id ?: defaultStatus?.id ?: statuses.firstOrNull()?.id ?: 0) }
    var followUpDate by remember { mutableStateOf(lead?.follow_up_date ?: "") }
    var notes by remember { mutableStateOf(TextFieldValue(lead?.notes ?: "")) }
    var description by remember { mutableStateOf(TextFieldValue(lead?.description ?: "")) }
    var latitude by remember { mutableStateOf(lead?.latitude ?: 0.0) }
    var longitude by remember { mutableStateOf(lead?.longitude ?: 0.0) }
    var locationStr by remember { mutableStateOf(lead?.location ?: "") }
    var loadingLocation by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val selectedStatus = statuses.find { it.id == statusId }

    fun fetchLocation(context: Context) {
        loadingLocation = true
        scope.launch {
            val loc: Location? = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                locationStr = LocationHelper.reverseGeocode(context, loc.latitude, loc.longitude) ?: ""
            }
            loadingLocation = false
        }
    }

    LaunchedEffect(Unit) {
        if (lead == null) fetchLocation(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lead == null) "Add Lead" else "Edit Lead") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status*") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    statusId = status.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = followUpDate,
                    onValueChange = { followUpDate = it },
                    label = { Text("Follow-up Date (dd-MM-yyyy)*") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
                            val datePicker = DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("en", "IN"))
                                    cal.set(y, m, d)
                                    followUpDate = sdf.format(cal.time)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            datePicker.show()
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                        }
                    }
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Location: "+ if (loadingLocation) "Fetching..." else locationStr, modifier = Modifier.weight(1f))
                    if (!loadingLocation) {
                        Button(onClick = { fetchLocation(context) }) {
                            Text("Refresh")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(
                    LeadRequest(
                        name = name.text,
                        phone = phone.text,
                        email = email.text,
                        address = address.text,
                        status_id = statusId,
                        follow_up_date = followUpDate,
                        notes = notes.text,
                        description = description.text,
                        latitude = latitude,
                        longitude = longitude,
                        location = locationStr,
                        pincode = null,
                        company = null,
                        additional_info = null,
                        source = null,
                        expected_amount = null
                    ),
                    lead?.id
                )
            }) {
                Text(if (lead == null) "Add Lead" else "Update Lead")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DropdownMenuBox(
    label: String,
    options: List<LeadStatus>,
    selectedId: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.id == selectedId }

    Box {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onSelected(status.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
