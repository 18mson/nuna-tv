package com.nunatv.app.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
                .background(Color(0xFF0F0F0F))
        ) {
            // Sidebar TV Adaptif: Membesar saat difokuskan remote, mengecil saat masuk ke konten menu
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(Color(0xFF141414))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE50914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Nuna TV",
                            tint = Color.White,
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
                            color = Color.White,
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
                    .background(Color(0xFF242424))
            )

            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                miniPlayerContent()
            }
        }
    } else {
        // UI Layout HP / Touchscreen: Mini Player + Bottom Navigation Bar Ramping
        Scaffold(
            containerColor = Color(0xFF0F0F0F),
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    miniPlayerContent()
                    Surface(
                        color = Color(0xFF141414),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            HorizontalDivider(color = Color(0xFF242424), thickness = 1.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                navItems.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    val itemColor = if (selected) Color.White else Color(0xFF8C8C8C)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { onNavigate(screen) }
                                            .padding(bottom = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.5.dp)
                                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                                .background(if (selected) Color(0xFFE50914) else Color.Transparent)
                                        )
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            tint = itemColor,
                                            modifier = Modifier.size(21.dp)
                                        )
                                        Text(
                                            text = screen.title,
                                            color = itemColor,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content()
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
    val shape = RoundedCornerShape(10.dp)

    val containerColor = when {
        isFocused -> Color.White.copy(alpha = 0.16f)
        selected -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused || selected -> Color.White
        else -> Color(0xFF9E9E9E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (isFocused) {
                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), shape)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Penanda status aktif (garis aksen merah di tepi kiri) terpisah dari pointer fokus putih
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Color(0xFFE50914) else Color.Transparent)
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
