package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MxBlue
import com.example.ui.theme.OmniPlayerTheme
import com.example.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val playerViewModel: PlayerViewModel = viewModel()
            val isDarkMode by playerViewModel.isDarkMode.collectAsState()
            val currentVideo by playerViewModel.currentVideo.collectAsState()
            val activeTab by playerViewModel.activeTab.collectAsState()

            LaunchedEffect(intent) {
                intent?.data?.let { uri ->
                    playerViewModel.openMediaUri(uri)
                }
            }

            OmniPlayerTheme(darkTheme = isDarkMode) {
                // If a video is playing, display the full gesture cinema player!
                if (currentVideo != null) {
                    BackHandler {
                        playerViewModel.closePlayer()
                    }
                    VideoPlayerScreen(
                        video = currentVideo!!,
                        viewModel = playerViewModel
                    )
                } else {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    var showShareScreen by remember { mutableStateOf(false) }

                    if (showShareScreen) {
                        BackHandler {
                            showShareScreen = false
                        }
                        MxShareScreen(
                            viewModel = playerViewModel,
                            onBack = { showShareScreen = false }
                        )
                    } else {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                NavDrawerContent(
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { playerViewModel.toggleTheme() },
                                    onNavigate = { route ->
                                        if (route == "Share") {
                                            showShareScreen = true
                                        } else {
                                            playerViewModel.setActiveTab(route)
                                        }
                                    },
                                    onCloseDrawer = {
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.testTag("bottom_nav_bar")
                                    ) {
                                        val tabs = listOf(
                                            "Local" to Icons.Default.Folder,
                                            "Video" to Icons.Default.PlayCircle,
                                            "Music" to Icons.Default.MusicNote,
                                            "Me" to Icons.Default.Person
                                        )

                                        tabs.forEach { (title, icon) ->
                                            NavigationBarItem(
                                                icon = { Icon(icon, contentDescription = title) },
                                                label = { Text(title) },
                                                selected = activeTab == title,
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MxBlue,
                                                    selectedTextColor = MxBlue,
                                                    indicatorColor = MxBlue.copy(alpha = 0.15f)
                                                ),
                                                onClick = { playerViewModel.setActiveTab(title) }
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (activeTab) {
                                        "Local" -> LocalFoldersScreen(
                                            viewModel = playerViewModel,
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                        "Video" -> AllVideosTabScreen(
                                            viewModel = playerViewModel,
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                        "Music" -> MusicEqScreen(
                                            viewModel = playerViewModel
                                        )
                                        "Me" -> MeProfileScreen(
                                            viewModel = playerViewModel,
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                        else -> LocalFoldersScreen(
                                            viewModel = playerViewModel,
                                            onOpenDrawer = { scope.launch { drawerState.open() } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
