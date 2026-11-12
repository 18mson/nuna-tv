package com.nunatv.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Banner konfirmasi download non-blocking yang muncul di sisi bawah layar
 * agar tidak menghalangi video yang sedang diputar.
 * Otomatis menutup (memilih "Tidak") setelah 7 detik.
 */
@Composable
fun DownloadPromptBanner(
    visible: Boolean,
    videoTitle: String,
    resolutionLabel: String,
    requestFocusOnShow: Boolean = false,
    timeoutSeconds: Int = 7,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsLeft by remember(visible, videoTitle) { mutableIntStateOf(timeoutSeconds) }
    val downloadFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible, videoTitle) {
        if (visible) {
            secondsLeft = timeoutSeconds
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        LaunchedEffect(Unit) {
            if (requestFocusOnShow) {
                delay(60)
                try {
                    downloadFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xEE181818),
            border = BorderStroke(1.dp, Color(0xFF3A3A3A)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 460.dp
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Download video ini? ($resolutionLabel)",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = videoTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DownloadBannerActionsRow(
                                secondsLeft = secondsLeft,
                                fillButtonsEvenly = true,
                                downloadFocusRequester = downloadFocusRequester,
                                onDownload = onDownload,
                                onDismiss = onDismiss
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Download video ini? ($resolutionLabel)",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = videoTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            DownloadBannerActionsRow(
                                secondsLeft = secondsLeft,
                                fillButtonsEvenly = false,
                                downloadFocusRequester = downloadFocusRequester,
                                onDownload = onDownload,
                                onDismiss = onDismiss
                            )
                        }
                    }

                    // Indikator hitung mundur
                    LinearProgressIndicator(
                        progress = { (secondsLeft / timeoutSeconds.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)),
                        color = Color(0xFFE50914),
                        trackColor = Color(0xFF2A2A2A)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadBannerActionsRow(
    secondsLeft: Int,
    fillButtonsEvenly: Boolean,
    downloadFocusRequester: FocusRequester,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (fillButtonsEvenly) Modifier.fillMaxWidth() else Modifier
    ) {
        Button(
            onClick = onDownload,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
                .then(if (fillButtonsEvenly) Modifier.weight(1f) else Modifier)
                .heightIn(min = 42.dp)
                .focusRequester(downloadFocusRequester)
                .dpadFocusable(
                    shape = RoundedCornerShape(8.dp),
                    style = FocusStyle.CTA,
                    onClick = onDownload
                )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Download",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }

        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF555555)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier
                .then(if (fillButtonsEvenly) Modifier.weight(1f) else Modifier)
                .heightIn(min = 42.dp)
                .dpadFocusable(
                    shape = RoundedCornerShape(8.dp),
                    style = FocusStyle.CTA,
                    onClick = onDismiss
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Tidak (${secondsLeft}s)",
                color = Color.LightGray,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
