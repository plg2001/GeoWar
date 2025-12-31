package com.example.geowar.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geowar.data.auth.UserResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    
    // COLORI CYBERPUNK
    val darkBg = Color(0xFF0A0E17)
    val neonBlue = Color(0xFF00E5FF)
    val neonPink = Color(0xFFFF4081)
    val activeColor = neonBlue
    val surfaceColor = Color(0xFF151A25)

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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(darkBg)
        ) {
            
            DashboardStats(
                userCount = users.size, 
                primaryColor = activeColor,
                surfaceColor = surfaceColor
            )

            Text(
                text = "USERS DATABASE",
                fontWeight = FontWeight.Bold,
                color = activeColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && users.isEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = activeColor,
                        trackColor = surfaceColor
                    )
                }
                UserListCyber(users, onBanClick = { viewModel.banUser(it) }, surfaceColor, activeColor, neonPink)
            }
        }
    }
}

@Composable
fun DashboardStats(userCount: Int, primaryColor: Color, surfaceColor: Color) {
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
        items(users, key = { it.id }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
