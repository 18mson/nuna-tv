package com.nunatv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val points: List<String>
)

@Composable
fun OnboardingDialog(
    isTv: Boolean,
    onFinish: () -> Unit
) {
    val steps = remember(isTv) {
        listOf(
            OnboardingStep(
                icon = Icons.Default.DownloadDone,
                title = "Home: Pustaka Video Offline",
                subtitle = "Semua video yang sudah Anda unduh tersimpan di tab Home dan dapat ditonton kapan saja tanpa koneksi internet.",
                points = if (isTv) {
                    listOf(
                        "Tekan OK singkat pada kartu video untuk langsung memutar.",
                        "Tahan tombol OK atau tekan Menu untuk mengubah kategori atau menghapus video.",
                        "Gunakan filter kategori dan lihat sisa ruang penyimpanan di bagian atas."
                    )
                } else {
                    listOf(
                        "Ketuk kartu video untuk memutar secara offline tanpa kuota.",
                        "Gunakan ikon kategori atau hapus pada kartu untuk merapikan koleksi.",
                        "Pantau sisa ruang bebas perangkat lewat bar penyimpanan di bagian atas."
                    )
                }
            ),
            OnboardingStep(
                icon = Icons.Default.SmartDisplay,
                title = "Tab YouTube Tanpa Login",
                subtitle = "Cari, tonton langsung, atau unduh video YouTube tanpa perlu masuk akun Google.",
                points = if (isTv) {
                    listOf(
                        "Feed Untuk Anda menyesuaikan topik dari tontonan dan pencarian terakhir.",
                        "Gunakan chip Riwayat untuk membuka hingga 20 video terakhir yang ditonton.",
                        "Tahan OK atau tekan Menu pada kartu untuk mengunduh, menambah antrean, atau membuka channel."
                    )
                } else {
                    listOf(
                        "Tempel tautan YouTube (watch, Shorts, youtu.be) lewat tombol tempel di sebelah cari.",
                        "Ketuk ikon titik tiga di kartu untuk mengunduh langsung, antrean putar berikutnya, atau buka channel.",
                        "Filter hasil berdasarkan Video Panjang, Shorts, Live, atau urutkan durasinya."
                    )
                }
            ),
            OnboardingStep(
                icon = Icons.Default.Settings,
                title = "Pemutar Video & Settings",
                subtitle = "Sesuaikan kenyaringan suara, resolusi unduhan, dan mode pemutaran di menu Settings.",
                points = if (isTv) {
                    listOf(
                        "Saat menonton: tekan OK untuk jeda/lanjut, kiri/kanan lompat 10 detik, tahan kiri/kanan ganti video.",
                        "Tekan tombol Atas untuk membuka halaman Channel dan mengganti resolusi video online.",
                        "Tekan tombol Bawah untuk membuka panel rekomendasi video berikutnya."
                    )
                } else {
                    listOf(
                        "Aktifkan Putar di Belakang Layar di Settings bila ingin audio lanjut saat layar mati seperti YouTube Music.",
                        "Aktifkan Popup Jendela (PiP) di Settings untuk menampilkan jendela mengambang saat keluar aplikasi.",
                        "Gunakan Penguat Suara (Audio Boost) di Settings atau pemutar bila volume asli video terasa pelan."
                    )
                }
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val primaryFocusRequester = remember { FocusRequester() }
    val active = steps[currentStep]

    LaunchedEffect(Unit) {
        try {
            primaryFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackButtons = maxWidth < 360.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = active.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Selamat Datang di Nuna TV (${currentStep + 1}/${steps.size})",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onFinish,
                            modifier = Modifier.dpadFocusable(
                                shape = RoundedCornerShape(8.dp),
                                style = FocusStyle.PILL,
                                onClick = onFinish
                            )
                        ) {
                            Text("Lewati", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = active.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = active.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(14.dp)
                    ) {
                        active.points.forEachIndexed { idx, line ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = line,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            if (idx < active.points.lastIndex) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }

                    // Indikator step
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(steps.size) { idx ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(if (idx == currentStep) 22.dp else 8.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (idx == currentStep) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3A))
                            )
                        }
                    }

                    val isLast = currentStep == steps.lastIndex
                    if (stackButtons) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (isLast) onFinish() else currentStep++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .focusRequester(primaryFocusRequester)
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        style = FocusStyle.CTA,
                                        onClick = { if (isLast) onFinish() else currentStep++ }
                                    )
                            ) {
                                if (isLast) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isLast) "Mulai Nonton" else "Lanjut",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep-- },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF444444)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .dpadFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            style = FocusStyle.PILL,
                                            onClick = { currentStep-- }
                                        )
                                ) {
                                    Text("Sebelumnya", color = Color.LightGray, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep-- },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF444444)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .dpadFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            style = FocusStyle.PILL,
                                            onClick = { currentStep-- }
                                        )
                                ) {
                                    Text("Sebelumnya", color = Color.LightGray, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Button(
                                onClick = {
                                    if (isLast) onFinish() else currentStep++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                                    .focusRequester(primaryFocusRequester)
                                    .dpadFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        style = FocusStyle.CTA,
                                        onClick = { if (isLast) onFinish() else currentStep++ }
                                    )
                            ) {
                                if (isLast) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isLast) "Mulai Nonton" else "Lanjut",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
