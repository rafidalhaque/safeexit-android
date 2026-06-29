package com.teamsabily.safeexit.ui.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamsabily.safeexit.ui.theme.CardBackground
import com.teamsabily.safeexit.ui.theme.DarkNavy
import com.teamsabily.safeexit.ui.theme.PrimaryRed
import com.teamsabily.safeexit.ui.theme.SecondaryBlue
import com.teamsabily.safeexit.ui.theme.SurfaceLight
import com.teamsabily.safeexit.ui.theme.TextPrimary
import com.teamsabily.safeexit.ui.theme.TextSecondary
import com.teamsabily.safeexit.viewmodel.ManageViewModel

@Composable
fun ManageScreen(
    manageViewModel: ManageViewModel = viewModel(),
) {
    val filteredApps by manageViewModel.filteredApps.collectAsState()
    val searchQuery by manageViewModel.searchQuery.collectAsState()
    val includeSystemApps by manageViewModel.includeSystemApps.collectAsState()
    val selectedPackages by manageViewModel.selectedPackages.collectAsState()
    val selectedCount by manageViewModel.selectedCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // Header with count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Manage Apps",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            if (selectedCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryRed.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { manageViewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search apps...",
                    color = TextSecondary,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SecondaryBlue,
                unfocusedBorderColor = SurfaceLight,
                focusedContainerColor = SurfaceLight.copy(alpha = 0.5f),
                unfocusedContainerColor = SurfaceLight.copy(alpha = 0.3f),
                cursorColor = SecondaryBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // System apps toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { manageViewModel.toggleSystemApps() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Show system apps",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )

            Switch(
                checked = includeSystemApps,
                onCheckedChange = { manageViewModel.toggleSystemApps() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryBlue,
                    checkedTrackColor = SecondaryBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceLight,
                    uncheckedBorderColor = SurfaceLight,
                ),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // App count info
        Text(
            text = "${filteredApps.size} apps found",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // App list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                items = filteredApps,
                key = { it.packageName },
            ) { app ->
                val isSelected = selectedPackages.contains(app.packageName)
                AppRow(
                    appName = app.appName,
                    packageName = app.packageName,
                    icon = app.icon,
                    isSelected = isSelected,
                    onToggle = { manageViewModel.toggleAppSelection(app.packageName) },
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    appName: String,
    packageName: String,
    icon: Drawable?,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                PrimaryRed.copy(alpha = 0.08f)
            } else {
                CardBackground
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    PrimaryRed.copy(alpha = 0.15f),
                                    PrimaryRed.copy(alpha = 0.02f),
                                ),
                                startX = 0f,
                                endX = 200f,
                            ),
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
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

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryRed,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = TextPrimary,
                ),
            )
        }
    }
}
