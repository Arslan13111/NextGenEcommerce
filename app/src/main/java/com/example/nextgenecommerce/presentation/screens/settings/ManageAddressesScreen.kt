package com.example.nextgenecommerce.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.Address
import com.example.nextgenecommerce.presentation.viewmodel.AddressViewModel

private val HomeColor   = Color(0xFF1565C0)
private val OfficeColor = Color(0xFFE65100)
private val OtherColor  = Color(0xFF6A1B9A)

private fun labelColor(label: String) = when (label) {
    "Home"   -> HomeColor
    "Office" -> OfficeColor
    else     -> OtherColor
}

private fun labelIcon(label: String) = when (label) {
    "Home"   -> Icons.Default.Home
    "Office" -> Icons.Default.Business
    else     -> Icons.Default.LocationOn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesScreen(
    navController: NavController,
    viewModel: AddressViewModel = hiltViewModel()
) {
    val addresses        by viewModel.addresses.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val error            by viewModel.error.collectAsState()
    val operationSuccess by viewModel.operationSuccess.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingAddress   by remember { mutableStateOf<Address?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(operationSuccess) {
        if (operationSuccess != null) viewModel.clearSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Addresses",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingAddress = null; showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor  = MaterialTheme.colorScheme.background,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, "Add address", modifier = Modifier.size(26.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                addresses.isEmpty() -> EmptyAddressState(
                    onAdd = { editingAddress = null; showAddEditDialog = true }
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "${addresses.size} saved address${if (addresses.size == 1) "" else "es"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(addresses, key = { it.id }) { address ->
                        AddressCard(
                            address  = address,
                            onSetDefault = { viewModel.setDefaultAddress(address) },
                            onEdit   = { editingAddress = address; showAddEditDialog = true },
                            onDelete = { showDeleteDialog = address }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    // Error
    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text  = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    // Add / Edit sheet
    if (showAddEditDialog) {
        AddressFormSheet(
            existingAddress = editingAddress,
            onDismiss = { showAddEditDialog = false },
            onSave = { address ->
                if (editingAddress == null) viewModel.addAddress(address)
                else viewModel.updateAddress(address)
                showAddEditDialog = false
            }
        )
    }

    // Delete confirmation
    showDeleteDialog?.let { address ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete address?") },
            text  = { Text("This address will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAddress(address); showDeleteDialog = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyAddressState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No saved addresses",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Save your home or office address\nfor a faster checkout experience.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAdd,
            shape   = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Address", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ── Address card ──────────────────────────────────────────────────────────────
@Composable
fun AddressCard(
    address: Address,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = labelColor(address.label)

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Coloured header strip ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.08f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            labelIcon(address.label),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            address.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        Text(
                            address.fullName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (address.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = accent, modifier = Modifier.size(12.dp))
                            Text(
                                "Default",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = accent
                            )
                        }
                    }
                }
            }

            // ── Address details ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Phone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Phone, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        address.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Full address
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Place, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Text(
                        buildString {
                            append(address.addressLine1)
                            if (address.addressLine2.isNotBlank()) append(", ${address.addressLine2}")
                            append(", ${address.city}")
                            if (address.province.isNotBlank()) append(", ${address.province}")
                            if (address.postalCode.isNotBlank()) append(" ${address.postalCode}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Action row ────────────────────────────────────────────────────
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!address.isDefault) {
                    ActionChip(
                        label = "Set default",
                        icon  = Icons.Default.CheckCircle,
                        tint  = Color(0xFF2E7D32),
                        bg    = Color(0xFF2E7D32).copy(alpha = 0.08f),
                        onClick = onSetDefault
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                ActionChip(
                    label = "Edit",
                    icon  = Icons.Default.Edit,
                    tint  = MaterialTheme.colorScheme.primary,
                    bg    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    onClick = onEdit
                )
                if (!address.isDefault) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ActionChip(
                        label = "Delete",
                        icon  = Icons.Default.Delete,
                        tint  = MaterialTheme.colorScheme.error,
                        bg    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tint
        )
    }
}

// ── Add / Edit full-screen dialog (avoids ModalBottomSheet keyboard conflicts) ─
@Composable
private fun AddressFormSheet(
    existingAddress: Address?,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var label        by remember { mutableStateOf(existingAddress?.label ?: "Home") }
    var fullName     by remember { mutableStateOf(existingAddress?.fullName ?: "") }
    var phone        by remember { mutableStateOf(existingAddress?.phone ?: "") }
    var addressLine1 by remember { mutableStateOf(existingAddress?.addressLine1 ?: "") }
    var addressLine2 by remember { mutableStateOf(existingAddress?.addressLine2 ?: "") }
    var city         by remember { mutableStateOf(existingAddress?.city ?: "") }
    var province     by remember { mutableStateOf(existingAddress?.province ?: "") }
    var postalCode   by remember { mutableStateOf(existingAddress?.postalCode ?: "") }

    val isValid = fullName.isNotBlank() && phone.isNotBlank() &&
            addressLine1.isNotBlank() && city.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Fixed top bar ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBack, "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        if (existingAddress == null) "Add New Address" else "Edit Address",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Scrollable form (imePadding here keeps fields above keyboard) ─
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Label chips
                    Text(
                        "Address Label",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Home"   to Icons.Default.Home,
                            "Office" to Icons.Default.Business,
                            "Other"  to Icons.Default.LocationOn
                        ).forEach { (l, icon) ->
                            val accent = labelColor(l)
                            FilterChip(
                                selected    = label == l,
                                onClick     = { label = l },
                                label       = {
                                    Text(l, fontWeight = if (label == l) FontWeight.Bold else FontWeight.Normal)
                                },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                                colors      = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor   = accent,
                                    selectedLabelColor       = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }

                    AddressField(
                        value = fullName, onValueChange = { fullName = it },
                        label = "Full Name", icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next
                        )
                    )
                    AddressField(
                        value = phone, onValueChange = { phone = it },
                        label = "Phone", placeholder = "03XX-XXXXXXX", icon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next
                        )
                    )
                    AddressField(
                        value = addressLine1, onValueChange = { addressLine1 = it },
                        label = "Address Line 1", icon = Icons.Default.Home,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next
                        )
                    )
                    AddressField(
                        value = addressLine2, onValueChange = { addressLine2 = it },
                        label = "Address Line 2 (Optional)", icon = Icons.Default.AddHome,
                        required = false,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AddressField(
                            value = city, onValueChange = { city = it },
                            label = "City", icon = Icons.Default.LocationCity,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        AddressField(
                            value = province, onValueChange = { province = it },
                            label = "Province", placeholder = "Punjab", icon = Icons.Default.Map,
                            required = false,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AddressField(
                        value = postalCode, onValueChange = { postalCode = it },
                        label = "Postal Code", icon = Icons.Default.Pin, required = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (isValid) {
                                onSave(
                                    Address(
                                        id           = existingAddress?.id ?: "",
                                        userId       = existingAddress?.userId ?: "",
                                        label        = label,
                                        fullName     = fullName.trim(),
                                        phone        = phone.trim(),
                                        addressLine1 = addressLine1.trim(),
                                        addressLine2 = addressLine2.trim(),
                                        city         = city.trim(),
                                        province     = province.trim(),
                                        postalCode   = postalCode.trim(),
                                        country      = "Pakistan",
                                        isDefault    = existingAddress?.isDefault ?: false
                                    )
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = MaterialTheme.colorScheme.onBackground,
                            disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (existingAddress == null) "Save Address" else "Update Address",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String = "",
    required: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = {
            Text(if (required) label else label)
        },
        placeholder   = if (placeholder.isNotEmpty()) ({
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }) else null,
        leadingIcon   = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        singleLine    = true,
        keyboardOptions = keyboardOptions,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = MaterialTheme.colorScheme.onBackground,
            focusedLabelColor     = MaterialTheme.colorScheme.onBackground,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
