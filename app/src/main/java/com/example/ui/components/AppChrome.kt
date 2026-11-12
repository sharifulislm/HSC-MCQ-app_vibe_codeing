package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRole
import com.example.data.model.ThemeMode

/**
 * Mobile-first Top App Bar displaying the HSC Edge brand mark,
 * dark/light mode toggle, Sign In action in guest mode, and Admin Panel ONLY to ADMIN_EMAIL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HscTopNavbar(
    title: String,
    subtitle: String,
    role: AppRole,
    themeMode: ThemeMode,
    isGuest: Boolean = false,
    onToggleTheme: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: (() -> Unit)?,
    onOpenSignIn: (() -> Unit)? = null,
    onSignOut: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val isDarkActive = themeMode == ThemeMode.DARK
    val badgeColor = if (role.isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (role.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "ADMIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            // Admin icon button is rendered ONLY when authenticated user == ADMIN_EMAIL
            if (role.isAdmin && onOpenAdminPanel != null) {
                IconButton(
                    onClick = onOpenAdminPanel,
                    modifier = Modifier.testTag("topbar_admin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = "Open Admin Panel",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isGuest && onOpenSignIn != null) {
                TextButton(
                    onClick = onOpenSignIn,
                    modifier = Modifier.testTag("topbar_sign_in_button")
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("topbar_theme_toggle")
            ) {
                Icon(
                    imageVector = if (isDarkActive) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = "Toggle dark mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("topbar_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile & History") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenProfile()
                        }
                    )
                    if (role.isAdmin && onOpenAdminPanel != null) {
                        DropdownMenuItem(
                            text = { Text("Admin Panel") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenAdminPanel()
                            }
                        )
                    }
                    HorizontalDivider()
                    if (isGuest && onOpenSignIn != null) {
                        DropdownMenuItem(
                            text = { Text("Sign In with Google", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenSignIn()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSignOut()
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
