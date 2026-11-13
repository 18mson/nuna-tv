package com.nunatv.app.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunatv.app.ui.navigation.Screen

@Composable
fun AdaptiveScaffold(
    isTv: Boolean,
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    miniPlayerContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val navItems = listOf(Screen.Home, Screen.YoutubeTv, Screen.Settings)
    val isPlayerActive = currentRoute == "player" || currentRoute?.startsWith("player") == true

    if (isPlayerActive) {
        // Mode Fullscreen tanpa navigation bar
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Navigasi di sisi kiri jika TV atau saat layar horizontal (landscape) di HP
    val useSideNav = isTv || isLandscape

    if (useSideNav) {
        var isSidebarFocused by remember { mutableStateOf(false) }

        val sidebarWidth by animateDpAsState(
            targetValue = if (isSidebarFocused) 196.dp else 72.dp,
            animationSpec = tween(durationMillis = 200),
            label = "tv_sidebar_width"
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sidebar TV Adaptif: Membesar saat difokuskan remote, mengecil saat masuk ke konten menu
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .onFocusChanged { focusState ->
                        isSidebarFocused = focusState.hasFocus
                    }
                    .focusGroup()
                    .padding(vertical = 18.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Brand Nuna TV
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Nuna TV",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isSidebarFocused,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(100))
                    ) {
                        Text(
                            text = "NUNA TV",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item Navigasi Sidebar
                navItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    SidebarNavItem(
                        screen = screen,
                        selected = selected,
                        expanded = isSidebarFocused,
                        onClick = { onNavigate(screen) }
                    )
                }
            }

            // Garis pemisah tipis antara sidebar dan konten
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                miniPlayerContent()
            }
        }
    } else {
        // UI Layout HP / Touchscreen: Mini Player + M3 Expressive Floating Capsule Toolbar
        // Pill navbar melayang di atas konten tanpa background (transparan)
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val pillContainerColor = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        val pillBorderColor = if (isDark) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        }

        // Tinggi pill navbar: 48dp item + 12dp padding atas-bawah + 8dp gap atas
        val pillNavHeightDp = 80.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Konten utama mengambil seluruh layar, diberi padding bawah agar tidak tertutup navbar
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                // Mini player (di atas navbar)
                miniPlayerContent()
                // Spacer sebesar tinggi pill agar konten tidak tersembunyi di bawah navbar
                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(pillNavHeightDp))
            }

            // Pill navbar floating overlay — transparan di sekitarnya
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = pillContainerColor,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, pillBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val targetBgColor = if (selected) {
                                if (isDark) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerLowest
                            } else {
                                Color.Transparent
                            }
                            val targetIconColor = if (selected) {
                                if (isDark) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            }

                            val animBgColor by animateColorAsState(
                                targetValue = targetBgColor,
                                animationSpec = tween(durationMillis = 200),
                                label = "tab_bg_${screen.route}"
                            )
                            val animIconColor by animateColorAsState(
                                targetValue = targetIconColor,
                                animationSpec = tween(durationMillis = 200),
                                label = "tab_icon_${screen.route}"
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(animBgColor)
                                    .clickable { onNavigate(screen) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = animIconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    screen: Screen,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)

    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        isFocused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        isFocused -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (isFocused) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), shape)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = screen.icon,
            contentDescription = screen.title,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100))
        ) {
            Text(
                text = screen.title,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
