package com.mrnrod45.nstk.ui.navigation

import com.mrnrod45.nstk.ui.components.PlatformDraggableArea

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
import com.mrnrod45.nstk.ui.screens.RcmScreen
import com.mrnrod45.nstk.ui.screens.SettingsScreen
import com.mrnrod45.nstk.ui.viewmodels.RcmViewModel
import com.mrnrod45.nstk.ui.screens.UploadScreen
import com.mrnrod45.nstk.ui.screens.SplitMergeScreen
import getFileSplitter
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker

import nstk.shared.generated.resources.Res
import nstk.shared.generated.resources.ic_payload
import nstk.shared.generated.resources.ic_settings
import nstk.shared.generated.resources.ic_split_merge
import nstk.shared.generated.resources.ic_upload
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
    settingsViewModel: com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Simple adaptive layout check using BoxWithConstraints would be better, 
    // but for simplicity in KMP without extra libs, lets assume Desktop usually wants Rail 
    // and Android usually wants BottomBar. 
    // However, to be "True KMP", we should check window size.
    // Let's use a BoxWithConstraints to be responsive.
    
    androidx.compose.foundation.layout.BoxWithConstraints {
        val useNavRail = maxWidth >= 600.dp
        
        val screens = Screen.entries.filter { 
            if (it == Screen.Rcm) usbController.isRcmSupported else true 
        }

        Scaffold(
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(painterResource(screen.icon), contentDescription = screen.label) },
                                label = null, // User requested icon-only style for mobile/bottom bar too
                                selected = currentDestination?.route == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Gradient Removed for solid color style

                // Remove padding from Row so NavigationRail can extend to the top (behind traffic lights)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start
                ) {
                    if (useNavRail) {
                         // Native-like Translucent Sidebar
                        NavigationRail(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent, // Transparent to show gradient
                            modifier = Modifier
                                .background(
                                    // Soft translucent background
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
                                )
                        ) {
                            // Draggable area for Traffic Lights (macOS)
                            // traffic lights are approx ~60-70dp wide. Rail is naturally ~80dp.
                            // Standard unified bar height is ~50-54dp. 32dp is too tight for modern macOS.
                            // We set fixed width to prevent Rail from expanding to fill screen if Row is loose.
                           PlatformDraggableArea(
                               modifier = Modifier
                                   .width(80.dp) 
                                   .height(54.dp)
                           ) {
                               // Empty content, just a drag handle
                           }

                             screens.forEach { screen ->
                                 NavigationRailItem(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    icon = { 
                                        Icon(
                                            painterResource(screen.icon), 
                                            contentDescription = screen.label
                                        ) 
                                    },
                                    label = null, // REMOVED Labels for sidebar
                                    selected = currentDestination?.route == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Upload.route,
                        // Apply padding here instead.
                        // If using NavRail (Desktop), we don't want Scaffold padding (which might try to account for bars we don't have).
                        // If not (Mobile), we need innerPadding for BottomBar.
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                            top = if (useNavRail) 0.dp else innerPadding.calculateTopPadding(),
                            bottom = if (useNavRail) 0.dp else innerPadding.calculateBottomPadding()
                        )
                    ) {
                        composable(Screen.Upload.route) {
                            UploadScreen(usbController, filePicker, settingsViewModel)
                        }
                        composable(Screen.Rcm.route) {
                            // Instantiate ViewModel here (state survives configuration change if using proper DI/ViewModelStore, 
                            // but for KMP simple setup without koin/voyager, remembering it is "okay" specifically for desktop, 
                            // but Android needs proper VM scope.
                            // For now, let's use a simple remember/factory approach compatible with KMP PoC.
                            // Ideally we'd use androidx.lifecycle.viewmodel.compose.viewModel()
                            
                            val viewModel = viewModel { 
                                RcmViewModel(usbController, filePicker) 
                            }
                            RcmScreen(viewModel)
                        }
                        composable(Screen.SplitMerge.route) {
                            val fileSplitter = getFileSplitter()
                            SplitMergeScreen(filePicker, fileSplitter)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}

