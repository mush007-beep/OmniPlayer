package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MxBlue
import com.example.ui.theme.MxRedProgress

@Composable
fun NavDrawerContent(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = if (isDarkMode) Color(0xFF14141E) else Color(0xFFFFFFFF),
        drawerContentColor = if (isDarkMode) Color.White else Color(0xFF1A1C24),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Header Logo (Screenshot 4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MxBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "MX PLAYER",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Drawer Items (Screenshot 4)
            DrawerMenuItem(
                icon = Icons.Default.Translate,
                title = "App Language",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "App Theme",
                subtitle = if (isDarkMode) "Dark" else "Light",
                onClick = {
                    onToggleTheme()
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.MusicNote,
                title = "Local Music",
                badgeText = "NEW",
                onClick = {
                    onNavigate("Music")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.CompareArrows,
                title = "File Transfer",
                badgeText = "NEW",
                onClick = {
                    onNavigate("Share")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Lock,
                title = "Private Folder",
                onClick = {
                    onNavigate("Private")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Equalizer,
                title = "Equalizer",
                onClick = {
                    onNavigate("Music")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.PlaylistPlay,
                title = "Video Playlists",
                badgeText = "NEW",
                onClick = {
                    onNavigate("Video")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.Podcasts,
                title = "Network Stream",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = Icons.Default.FolderShared,
                title = "Local Network (SMBv2)",
                onClick = { onCloseDrawer() }
            )

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Local Player Settings",
                onClick = {
                    onNavigate("Me")
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                icon = Icons.Default.HelpOutline,
                title = "Help",
                onClick = { onCloseDrawer() }
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("drawer_item_$title"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MxBlue
            )
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .background(MxRedProgress, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
