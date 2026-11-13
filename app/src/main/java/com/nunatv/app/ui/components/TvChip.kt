package com.nunatv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Chip kategori kustom tanpa minimum-touch-target padding 48dp bawaan Material3,
 * sehingga border fokus D-Pad membungkus simetris di keempat sisi (atas, bawah, kiri, kanan).
 */
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color? = null,
    isOutlined: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isOutlined -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isOutlined -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (isOutlined && !selected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }

    Surface(
        shape = shape,
        color = containerColor,
        border = border,
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .dpadFocusable(
                shape = shape,
                style = FocusStyle.PILL,
                onClick = onClick
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else (leadingIconTint ?: contentColor),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
