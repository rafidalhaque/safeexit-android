package com.teamsabily.safeexit.ui.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamsabily.safeexit.ui.component.PanicSlider
import com.teamsabily.safeexit.ui.theme.CardBackground
import com.teamsabily.safeexit.ui.theme.DarkNavy
import com.teamsabily.safeexit.ui.theme.PrimaryRed
import com.teamsabily.safeexit.ui.theme.SecondaryBlue
import com.teamsabily.safeexit.ui.theme.SurfaceLight
import com.teamsabily.safeexit.ui.theme.TextPrimary
import com.teamsabily.safeexit.ui.theme.TextSecondary
import com.teamsabily.safeexit.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToManage: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val selectedApps by homeViewModel.selectedApps.collectAsState()
    val isDeviceOwner by homeViewModel.isDeviceOwner.collectAsState()
    val showResults by homeViewModel.showResults.collectAsState()
    val uninstallResults by homeViewModel.uninstallResults.collectAsState()
    val isUninstalling by homeViewModel.isUninstalling.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.refreshSelectedApps()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavy)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            // Main content column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                // Header
                Text(
                    text = "SafeExit",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Text(
                    text = "Emergency App Removal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Device Owner warning
                if (!isDeviceOwner) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryRed.copy(alpha = 0.15f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = PrimaryRed,
                                modifier = Modifier.size(20.dp),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Not enrolled as Device Owner",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryRed,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Silent uninstall requires Device Owner. Run:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "adb shell dpm set-device-owner com.teamsabily.safeexit/.receiver.SafeExitDeviceAdminReceiver",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                    ),
                                    color = SecondaryBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            SurfaceLight.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp),
                                        )
                                        .padding(8.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Uninstalling indicator
                if (isUninstalling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryRed,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Uninstalling...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryRed,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // App list or empty state
                if (selectedApps.isEmpty() && !isUninstalling) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No apps selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onNavigateToManage,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SecondaryBlue,
                                    contentColor = TextPrimary,
                                ),
                            ) {
                                Text(text = "Go to Manage")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = selectedApps,
                            key = { it.packageName },
                        ) { app ->
                            SelectedAppCard(app.appName, app.packageName, app.icon)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Panic slider on the right
            PanicSlider(
                enabled = selectedApps.isNotEmpty(),
                isDeviceOwner = isDeviceOwner,
                onTrigger = { homeViewModel.executePanicUninstall() },
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
            )
        }

        // Results overlay
        if (showResults) {
            ResultScreen(
                results = uninstallResults,
                onDismiss = { homeViewModel.dismissResults() },
            )
        }
    }
}

@Composable
private fun SelectedAppCard(
    appName: String,
    packageName: String,
    icon: Drawable?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceLight),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(
                        painter = BitmapPainter(
                            icon.toBitmap(40, 40).asImageBitmap()
                        ),
                        contentDescription = appName,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = appName,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
