package com.example.nextgenecommerce.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.Address
import com.example.nextgenecommerce.presentation.viewmodel.AddressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesScreen(
    navController: NavController,
    viewModel: AddressViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val operationSuccess by viewModel.operationSuccess.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(operationSuccess) {
        if (operationSuccess != null) {
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Addresses",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingAddress = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, "Add address") },
                text = { Text("Add Address") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (addresses.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No saved addresses",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your delivery address to make checkout faster",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(addresses, key = { it.id }) { address ->
                        AddressCard(
                            address = address,
                            onSetDefault = { viewModel.setDefaultAddress(address) },
                            onEdit = {
                                editingAddress = address
                                showAddEditDialog = true
                            },
                            onDelete = { showDeleteDialog = address }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // Error snackbar
    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    // Add / Edit Sheet
    if (showAddEditDialog) {
        AddressFormSheet(
            existingAddress = editingAddress,
            onDismiss = { showAddEditDialog = false },
            onSave = { address ->
                if (editingAddress == null) {
                    viewModel.addAddress(address)
                } else {
                    viewModel.updateAddress(address)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation
    showDeleteDialog?.let { address ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Address") },
            text = { Text("Are you sure you want to delete this address?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAddress(address)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressFormSheet(
    existingAddress: Address?,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var label by remember { mutableStateOf(existingAddress?.label ?: "Home") }
    var fullName by remember { mutableStateOf(existingAddress?.fullName ?: "") }
    var phone by remember { mutableStateOf(existingAddress?.phone ?: "") }
    var addressLine1 by remember { mutableStateOf(existingAddress?.addressLine1 ?: "") }
    var addressLine2 by remember { mutableStateOf(existingAddress?.addressLine2 ?: "") }
    var city by remember { mutableStateOf(existingAddress?.city ?: "") }
    var province by remember { mutableStateOf(existingAddress?.province ?: "") }
    var postalCode by remember { mutableStateOf(existingAddress?.postalCode ?: "") }

    val isValid = fullName.isNotBlank() && phone.isNotBlank() && addressLine1.isNotBlank() && city.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (existingAddress == null) "Add New Address" else "Edit Address",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Label chips
            Text("Address Label", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home" to Icons.Default.Home, "Office" to Icons.Default.Business, "Other" to Icons.Default.LocationOn).forEach { (l, icon) ->
                    FilterChip(
                        selected = label == l,
                        onClick = { label = l },
                        label = { Text(l) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF111111),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            AddressField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", icon = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
            AddressField(value = phone, onValueChange = { phone = it }, label = "Phone", placeholder = "03XX-XXXXXXX", icon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next))
            AddressField(value = addressLine1, onValueChange = { addressLine1 = it }, label = "Address Line 1", icon = Icons.Default.Home,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next))
            AddressField(value = addressLine2, onValueChange = { addressLine2 = it }, label = "Address Line 2 (Optional)", icon = Icons.Default.AddHome, required = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddressField(value = city, onValueChange = { city = it }, label = "City", icon = Icons.Default.LocationCity,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f))
                AddressField(value = province, onValueChange = { province = it }, label = "Province", placeholder = "Punjab...", icon = Icons.Default.Map, required = false,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f))
            }
            AddressField(value = postalCode, onValueChange = { postalCode = it }, label = "Postal Code", icon = Icons.Default.Pin, required = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))

            Button(
                onClick = {
                    if (isValid) {
                        onSave(Address(
                            id = existingAddress?.id ?: "",
                            userId = existingAddress?.userId ?: "",
                            label = label,
                            fullName = fullName.trim(),
                            phone = phone.trim(),
                            addressLine1 = addressLine1.trim(),
                            addressLine2 = addressLine2.trim(),
                            city = city.trim(),
                            province = province.trim(),
                            postalCode = postalCode.trim(),
                            country = "Pakistan",
                            isDefault = existingAddress?.isDefault ?: false
                        ))
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (existingAddress == null) "Save Address" else "Update Address", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String = "",
    required: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }) else null,
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF111111),
            focusedLabelColor = Color(0xFF111111),
            focusedLeadingIconColor = Color(0xFF111111)
        )
    )
}

@Composable
fun AddressCard(
    address: Address,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = address.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = address.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (address.isDefault) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Default",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = address.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(address.addressLine1)
                    if (address.addressLine2.isNotBlank()) append(", ${address.addressLine2}")
                    append("\n${address.city}")
                    if (address.province.isNotBlank()) append(", ${address.province}")
                    if (address.postalCode.isNotBlank()) append(" ${address.postalCode}")
                    append("\n${address.country}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!address.isDefault) {
                    // Set Default action
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B5E20).copy(alpha = 0.10f))
                            .clickable(
                                indication = androidx.compose.material.ripple.rememberRipple(bounded = true),
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onSetDefault() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Set Default",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Edit action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .clickable(
                            indication = androidx.compose.material.ripple.rememberRipple(bounded = true),
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { onEdit() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Edit, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!address.isDefault) {
                    Spacer(modifier = Modifier.width(6.dp))
                    // Delete action
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                            .clickable(
                                indication = androidx.compose.material.ripple.rememberRipple(bounded = true),
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { onDelete() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Delete",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
