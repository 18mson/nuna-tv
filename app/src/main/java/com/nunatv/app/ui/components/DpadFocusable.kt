package com.nunatv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gaya fokus D-Pad berbasis peran komponen agar hierarki visual TV tetap jelas
 * dan tidak memakai border merah yang sama di semua tempat.
 */
enum class FocusStyle {
    /** Kartu video & banner: ring putih tajam + elevasi bayangan + scale ringan. */
    CARD,

    /** Tombol aksi utama (Putar, Download, Cari): outer ring putih kontras tinggi. */
    CTA,

    /** Chip kategori, tombol keyboard, & opsi setting: sorotan latar terang + ring putih. */
    PILL,

    /** Item navigasi sidebar: latar abu terang transparan + ring putih tipis. */
    NAV,

    /** Ikon kecil (Hapus, Acak, Tutup): halo lingkaran lembut + ring putih tipis. */
    ICON
}

@Composable
fun Modifier.dpadFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    style: FocusStyle = FocusStyle.CARD,
    focusedBorderColor: Color? = null,
    focusedBorderWidth: Dp? = null,
    focusedScale: Float? = null,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }

    val targetScale = when {
        !isFocused -> 1f
        focusedScale != null -> focusedScale
        else -> when (style) {
            FocusStyle.CARD -> 1.04f
            FocusStyle.CTA -> 1.05f
            FocusStyle.PILL -> 1.02f
            FocusStyle.NAV -> 1.02f
            FocusStyle.ICON -> 1.08f
        }
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 150),
        label = "dpad_scale_anim"
    )

    val resolvedBorderColor = focusedBorderColor ?: when (style) {
        FocusStyle.CARD -> Color.White
        FocusStyle.CTA -> Color.White
        FocusStyle.PILL -> Color.White
        FocusStyle.NAV -> Color.White.copy(alpha = 0.85f)
        FocusStyle.ICON -> Color.White
    }

    val resolvedBorderWidth = focusedBorderWidth ?: when (style) {
        FocusStyle.CARD -> 2.5.dp
        FocusStyle.CTA -> 2.5.dp
        FocusStyle.PILL -> 2.dp
        FocusStyle.NAV -> 1.5.dp
        FocusStyle.ICON -> 1.5.dp
    }

    val focusBackground = when {
        !isFocused -> Color.Transparent
        style == FocusStyle.PILL -> Color.White.copy(alpha = 0.14f)
        style == FocusStyle.NAV -> Color.White.copy(alpha = 0.16f)
        style == FocusStyle.ICON -> Color.White.copy(alpha = 0.20f)
        else -> Color.Transparent
    }

    return this
        .scale(scale)
        .then(
            if (isFocused && style == FocusStyle.CARD) {
                Modifier.shadow(elevation = 10.dp, shape = shape, clip = false)
            } else {
                Modifier
            }
        )
        .then(
            if (focusBackground != Color.Transparent) {
                Modifier.background(focusBackground, shape)
            } else {
                Modifier
            }
        )
        .then(
            if (isFocused) {
                Modifier.border(resolvedBorderWidth, resolvedBorderColor, shape)
            } else {
                Modifier
            }
        )
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
        .then(
            if (onClick == null) {
                Modifier.focusable()
            } else {
                Modifier
            }
        )
}
