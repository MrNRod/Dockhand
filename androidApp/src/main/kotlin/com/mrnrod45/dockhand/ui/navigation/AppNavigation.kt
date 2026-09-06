package com.mrnrod45.dockhand.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrnrod45.dockhand.R
import com.mrnrod45.dockhand.domain.file.getFileSplitter
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker
import com.mrnrod45.dockhand.ui.components.AppLayout
import com.mrnrod45.dockhand.ui.screens.RcmScreen
import com.mrnrod45.dockhand.ui.screens.SettingsScreen
import com.mrnrod45.dockhand.ui.screens.SplitMergeScreen
import com.mrnrod45.dockhand.ui.screens.UploadScreen
import com.mrnrod45.dockhand.ui.viewmodels.RcmViewModel
import com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel

enum class Screen(val route: String, val label: String, @DrawableRes val icon: Int) {
    Upload("upload", "Upload", R.drawable.ic_upload),
    Rcm("rcm", "Payload", R.drawable.ic_payload),
    SplitMerge("split_merge", "Split & Merge", R.drawable.ic_split_merge),
    Settings("settings", "Settings", R.drawable.ic_settings)
}

@Composable
fun AppNavigation(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = Screen.entries.filter { 
        if (it == Screen.Rcm) usbController.isRcmSupported else true 
    }

    AppLayout(
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
                UploadScreen(usbController, filePicker, settingsViewModel)
            }
            composable(Screen.Rcm.route) {
                val viewModel = viewModel { 
                    RcmViewModel(usbController, filePicker) 
                }
                RcmScreen(viewModel)
            }
            composable(Screen.SplitMerge.route) {
                val fileSplitter = getFileSplitter()
                SplitMergeScreen(filePicker, fileSplitter, settingsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}

