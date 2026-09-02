package com.mrnrod45.dockhand.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrnrod45.dockhand.ui.screens.RcmScreen
import com.mrnrod45.dockhand.ui.screens.SettingsScreen
import com.mrnrod45.dockhand.ui.viewmodels.RcmViewModel
import com.mrnrod45.dockhand.ui.screens.UploadScreen
import com.mrnrod45.dockhand.ui.screens.SplitMergeScreen
import com.mrnrod45.dockhand.domain.file.getFileSplitter
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker

import dockhand.composeapp.generated.resources.Res
import dockhand.composeapp.generated.resources.ic_payload
import dockhand.composeapp.generated.resources.ic_settings
import dockhand.composeapp.generated.resources.ic_split_merge
import dockhand.composeapp.generated.resources.ic_upload
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class Screen(val route: String, val label: String, val icon: DrawableResource) {
    Upload("upload", "Upload", Res.drawable.ic_upload),
    Rcm("rcm", "Payload", Res.drawable.ic_payload),
    SplitMerge("split_merge", "Split & Merge", Res.drawable.ic_split_merge),
    Settings("settings", "Settings", Res.drawable.ic_settings)
}

@Composable
fun AppNavigation(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = Screen.entries.filter { 
        if (it == Screen.Rcm) usbController.isRcmSupported else true 
    }

    com.mrnrod45.dockhand.ui.components.PlatformAppLayout(
        screens = screens,
        currentDestination = currentDestination?.route,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Upload.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Upload.route) {
                com.mrnrod45.dockhand.ui.screens.platform.PlatformUploadScreen(usbController, filePicker, settingsViewModel)
            }
            composable(Screen.Rcm.route) {
                val viewModel = viewModel { 
                    RcmViewModel(usbController, filePicker) 
                }
                com.mrnrod45.dockhand.ui.screens.platform.PlatformRcmScreen(viewModel)
            }
            composable(Screen.SplitMerge.route) {
                val fileSplitter = getFileSplitter()
                com.mrnrod45.dockhand.ui.screens.platform.PlatformSplitMergeScreen(filePicker, fileSplitter, settingsViewModel)
            }
            composable(Screen.Settings.route) {
                com.mrnrod45.dockhand.ui.screens.platform.PlatformSettingsScreen(settingsViewModel)
            }
        }
    }
}

