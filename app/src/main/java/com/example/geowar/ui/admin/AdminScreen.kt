package com.example.geowar.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geowar.data.auth.TargetResponse
import com.example.geowar.data.auth.UserResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val targets by viewModel.targets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTargetDialog by remember { mutableStateOf(false) }
    
    // COLORI CYBERPUNK (ripresi dal AuthScreen per coerenza)
    val darkBg = Color(0xFF0A0E17)
    val neonBlue = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF4081)
    val activeColor = neonBlue // Colore principale per l'Admin
    val surfaceColor = Color(0xFF151A25) // Leggermente più chiaro del background

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = darkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ADMIN TERMINAL",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, 
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ricarica", tint = activeColor)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = neonPink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkBg
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) { // Tab Target
                ExtendedFloatingActionButton(
                    onClick = { showAddTargetDialog = true },
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.Black) },
                    text = { Text("NEW TARGET", fontWeight = FontWeight.Bold, color = Color.Black) },
                    containerColor = activeColor,
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(darkBg)
        ) {
            
            // --- HEADER STATS ---
            DashboardStats(
                userCount = users.size, 
                targetCount = targets.size,
                primaryColor = activeColor,
                secondaryColor = neonPink,
                surfaceColor = surfaceColor
            )

            // --- TABS STILE CYBER ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = darkBg,
                contentColor = activeColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = activeColor,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("USERS DATABASE", fontWeight = FontWeight.Bold) },
                    icon = { Icon(if (selectedTab == 0) Icons.Default.Group else Icons.Outlined.Person, null) },
                    selectedContentColor = activeColor,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("TARGET MATRIX", fontWeight = FontWeight.Bold) },
                    icon = { Icon(if (selectedTab == 1) Icons.Default.Map else Icons.Outlined.Place, null) },
                    selectedContentColor = activeColor,
                    unselectedContentColor = Color.Gray
                )
            }

            // --- CONTENT ---
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = activeColor,
                        trackColor = surfaceColor
                    )
                }

                if (selectedTab == 0) {
                    UserListCyber(users, onBanClick = { viewModel.banUser(it) }, surfaceColor, activeColor, neonPink)
                } else {
                    TargetListCyber(targets, onDeleteClick = { viewModel.deleteTarget(it) }, surfaceColor, activeColor, neonPink)
                }
            }
        }
    }

    if (showAddTargetDialog) {
        AddTargetDialogCyber(
            onDismiss = { showAddTargetDialog = false },
            onConfirm = { name, lat, lon ->
                viewModel.createTarget(name, lat, lon)
                showAddTargetDialog = false
            },
            primaryColor = activeColor,
            surfaceColor = surfaceColor
        )
    }
}

@Composable
fun DashboardStats(userCount: Int, targetCount: Int, primaryColor: Color, secondaryColor: Color, surfaceColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCardCyber(
            title = "ACTIVE USERS",
            count = userCount.toString(),
            icon = Icons.Default.Person,
            iconColor = primaryColor,
            surfaceColor = surfaceColor,
            modifier = Modifier.weight(1f)
        )
        StatCardCyber(
            title = "GEO TARGETS",
            count = targetCount.toString(),
            icon = Icons.Default.Flag,
            iconColor = secondaryColor,
            surfaceColor = surfaceColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCardCyber(title: String, count: String, icon: ImageVector, iconColor: Color, surfaceColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = CutCornerShape(bottomStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = count, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun UserListCyber(users: List<UserResponse>, onBanClick: (Int) -> Unit, surfaceColor: Color, primaryColor: Color, errorColor: Color) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(users) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CutCornerShape(8.dp))
                            .background(if (user.admin) primaryColor else Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.take(1).uppercase(),
                            color = if (user.admin) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (user.admin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADMIN", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ID: ${user.id}  • ", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = user.team ?: "NO TEAM", 
                                fontSize = 12.sp, 
                                color = if (user.team != null) Color.Cyan else Color.Gray
                            )
                        }
                    }

                    if (!user.admin) {
                        IconButton(onClick = { onBanClick(user.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Ban", tint = errorColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetListCyber(targets: List<TargetResponse>, onDeleteClick: (Int) -> Unit, surfaceColor: Color, primaryColor: Color, errorColor: Color) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(targets) { target ->
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place, 
                        contentDescription = null, 
                        tint = when(target.owner) {
                            "RED" -> Color(0xFFFF4081)
                            "BLUE" -> Color(0xFF00E5FF)
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.name, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Lat: ${String.format("%.4f", target.lat)}, Lon: ${String.format("%.4f", target.lon)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    IconButton(onClick = { onDeleteClick(target.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Elimina", tint = errorColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTargetDialogCyber(onDismiss: () -> Unit, onConfirm: (String, Double, Double) -> Unit, primaryColor: Color, surfaceColor: Color) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = CutCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "DEPLOY NEW TARGET", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )
                
                CyberTextFieldSimple(value = name, onValueChange = { name = it }, label = "TARGET NAME", primaryColor)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        CyberTextFieldSimple(value = lat, onValueChange = { lat = it }, label = "LATITUDE", primaryColor, true)
                    }
                    Box(Modifier.weight(1f)) {
                        CyberTextFieldSimple(value = lon, onValueChange = { lon = it }, label = "LONGITUDE", primaryColor, true)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("ABORT", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val latVal = lat.toDoubleOrNull()
                            val lonVal = lon.toDoubleOrNull()
                            if (name.isNotEmpty() && latVal != null && lonVal != null) {
                                onConfirm(name, latVal, lonVal)
                            }
                        },
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) { 
                        Text("DEPLOY", color = Color.Black, fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun CyberTextFieldSimple(value: String, onValueChange: (String) -> Unit, label: String, activeColor: Color, numeric: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeColor,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = activeColor
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(bottomStart = 12.dp, topEnd = 12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text)
    )
}
