package com.plywoodpocket.crm.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.plywoodpocket.crm.models.*
import com.plywoodpocket.crm.models.Role
import java.util.Calendar
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText

fun isValidIndianPhoneNumber(input: String): Boolean {
    val trimmed = input.removePrefix("+91 ")
    return trimmed.length == 10 && trimmed.all { it.isDigit() }
}

fun formatIndianPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }
    val trimmed = if (digits.startsWith("91") && digits.length > 10) digits.drop(2) else digits
    return if (trimmed.isNotEmpty()) "+91 " + trimmed.take(10) else ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserDialog(
    roles: List<Role>,
    onDismiss: () -> Unit,
    onConfirm: (CreateUserRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<Role?>(null) }
    var isActive by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var dateOfJoining by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var targetLeads by remember { mutableStateOf("") }

    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var roleError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var whatsappError by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance()
                picked.set(year, month, dayOfMonth)
                dateOfJoining = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun validate(): Boolean {
        var valid = true
        nameError = if (name.isBlank()) "Name is required" else null
        emailError = if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Valid email is required" else null
        passwordError = if (password.length < 6) "Password must be at least 6 characters" else null
        roleError = if (selectedRole == null) "Role is required" else null
        phoneError = if (phone.isNotBlank() && !isValidIndianPhoneNumber(phone)) "Enter valid 10-digit Indian number" else null
        whatsappError = if (whatsappNumber.isNotBlank() && !isValidIndianPhoneNumber(whatsappNumber)) "Enter valid 10-digit Indian number" else null
        if (nameError != null || emailError != null || passwordError != null || roleError != null || phoneError != null || whatsappError != null) valid = false
        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New User") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name*") },
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (emailError != null) Text(emailError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password*") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError != null) Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole?.name ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Role*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = roleError != null,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (roleError != null) Text(roleError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active")
                    Switch(
                        checked = isActive ?: false,
                        onCheckedChange = { isActive = it }
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        val formatted = formatIndianPhoneNumber(it)
                        phone = formatted
                        phoneError = if (phone.isNotBlank() && !isValidIndianPhoneNumber(phone)) "Enter valid 10-digit Indian number" else null
                    },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("+91 9876543210") },
                    maxLines = 1
                )
                if (phoneError != null) Text(phoneError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = {
                        val formatted = formatIndianPhoneNumber(it)
                        whatsappNumber = formatted
                        whatsappError = if (whatsappNumber.isNotBlank() && !isValidIndianPhoneNumber(whatsappNumber)) "Enter valid 10-digit Indian number" else null
                    },
                    label = { Text("WhatsApp Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = whatsappError != null,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("+91 9876543210") },
                    maxLines = 1
                )
                if (whatsappError != null) Text(whatsappError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateOfJoining,
                    onValueChange = { dateOfJoining = it },
                    label = { Text("Date of Joining (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetLeads,
                    onValueChange = { targetLeads = it },
                    label = { Text("Target Leads") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        onConfirm(
                            CreateUserRequest(
                                name = name,
                                email = email,
                                password = password,
                                role_id = selectedRole!!.id,
                                is_active = isActive,
                                phone = phone.ifBlank { null },
                                whatsapp_number = whatsappNumber.ifBlank { null },
                                pincode = pincode.ifBlank { null },
                                address = address.ifBlank { null },
                                location = location.ifBlank { null },
                                designation = designation.ifBlank { null },
                                date_of_joining = dateOfJoining.ifBlank { null },
                                target_amount = targetAmount.toDoubleOrNull(),
                                target_leads = targetLeads.toIntOrNull()
                            )
                        )
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: UserProfile,
    roles: List<Role>,
    onDismiss: () -> Unit,
    onConfirm: (UpdateUserPasswordRequest) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<Role?>(user.role) }
    var isActive by remember { mutableStateOf(user.is_active) }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var whatsappNumber by remember { mutableStateOf(user.whatsapp_number ?: "") }
    var pincode by remember { mutableStateOf(user.pincode ?: "") }
    var address by remember { mutableStateOf(user.address ?: "") }
    var location by remember { mutableStateOf(user.location ?: "") }
    var designation by remember { mutableStateOf(user.designation ?: "") }
    var dateOfJoining by remember { mutableStateOf(user.date_of_joining ?: "") }
    var targetAmount by remember { mutableStateOf(user.target_amount?.toString() ?: "") }
    var targetLeads by remember { mutableStateOf(user.target_leads?.toString() ?: "") }

    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var roleError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var whatsappError by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance()
                picked.set(year, month, dayOfMonth)
                dateOfJoining = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun validate(): Boolean {
        var valid = true
        nameError = if (name.isBlank()) "Name is required" else null
        emailError = if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Valid email is required" else null
        roleError = if (selectedRole == null) "Role is required" else null
        phoneError = if (phone.isNotBlank() && !isValidIndianPhoneNumber(phone)) "Enter valid 10-digit Indian number" else null
        whatsappError = if (whatsappNumber.isNotBlank() && !isValidIndianPhoneNumber(whatsappNumber)) "Enter valid 10-digit Indian number" else null
        if (nameError != null || emailError != null || roleError != null || phoneError != null || whatsappError != null) valid = false
        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name*") },
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (emailError != null) Text(emailError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("New Password (leave blank to keep current)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole?.name ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Role*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = roleError != null,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRole = role
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (roleError != null) Text(roleError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active")
                    Switch(
                        checked = isActive ?: false,
                        onCheckedChange = { isActive = it }
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        val formatted = formatIndianPhoneNumber(it)
                        phone = formatted
                        phoneError = if (phone.isNotBlank() && !isValidIndianPhoneNumber(phone)) "Enter valid 10-digit Indian number" else null
                    },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("+91 9876543210") },
                    maxLines = 1
                )
                if (phoneError != null) Text(phoneError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = whatsappNumber,
                    onValueChange = {
                        val formatted = formatIndianPhoneNumber(it)
                        whatsappNumber = formatted
                        whatsappError = if (whatsappNumber.isNotBlank() && !isValidIndianPhoneNumber(whatsappNumber)) "Enter valid 10-digit Indian number" else null
                    },
                    label = { Text("WhatsApp Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = whatsappError != null,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("+91 9876543210") },
                    maxLines = 1
                )
                if (whatsappError != null) Text(whatsappError!!, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateOfJoining,
                    onValueChange = { dateOfJoining = it },
                    label = { Text("Date of Joining (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetLeads,
                    onValueChange = { targetLeads = it },
                    label = { Text("Target Leads") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        onConfirm(
                            UpdateUserPasswordRequest(
                                name = name,
                                email = email,
                                password = password.ifBlank { null },
                                role_id = selectedRole!!.id,
                                is_active = isActive ?: false,
                                phone = phone.ifBlank { null },
                                whatsapp_number = whatsappNumber.ifBlank { null },
                                pincode = pincode.ifBlank { null },
                                address = address.ifBlank { null },
                                location = location.ifBlank { null },
                                designation = designation.ifBlank { null },
                                date_of_joining = dateOfJoining.ifBlank { null },
                                target_amount = targetAmount.toDoubleOrNull(),
                                target_leads = targetLeads.toIntOrNull()
                            )
                        )
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 