package com.nunatv.app.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nunatv.app.data.repository.YoutubeSearchRepository
import kotlinx.coroutines.delay

/**
 * Dialog Pencarian dengan Keyboard QWERTY Bawaan khusus Android TV & Emulator.
 * Mendukung navigasi remote D-Pad, klik mouse di emulator, sekaligus ketik langsung
 * dari keyboard fisik tanpa konflik fokus dengan Gboard TV.
 */
@Composable
fun TvSearchDialog(
    initialQuery: String,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val firstKeyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            suggestions = emptyList()
        } else {
            delay(250)
            suggestions = YoutubeSearchRepository.getSearchSuggestions(query)
        }
    }

    val rows = remember {
        listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )
    }

    LaunchedEffect(Unit) {
        try {
            firstKeyFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        val code = event.nativeKeyEvent.keyCode
                        when (code) {
                            KeyEvent.KEYCODE_DEL -> {
                                if (query.isNotEmpty()) query = query.dropLast(1)
                                true
                            }
                            KeyEvent.KEYCODE_SPACE -> {
                                query += " "
                                true
                            }
                            else -> {
                                val unicode = event.nativeKeyEvent.unicodeChar
                                if (unicode >= 32 && unicode != 127) {
                                    query += unicode.toChar()
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Kotak tampilan teks pencarian sama persis rounded & ukurannya dengan tab YouTube
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (query.isEmpty()) "Ketik kata kunci pencarian..." else "$query|",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Baris Hint Rekomendasi Pencarian (Autocomplete YouTube)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (suggestions.isEmpty()) {
                        Text(
                            text = if (query.trim().length < 2) {
                                "Ketik minimal 2 huruf untuk melihat saran pencarian..."
                            } else {
                                "Mencari saran..."
                            },
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(suggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFF282828),
                                    border = BorderStroke(1.dp, Color(0xFF444444)),
                                    modifier = Modifier
                                        .dpadFocusable(
                                            shape = RoundedCornerShape(18.dp),
                                            style = FocusStyle.PILL,
                                            focusedScale = 1.04f,
                                            onClick = { onSearch(suggestion) }
                                        )
                                        .clickable { onSearch(suggestion) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = suggestion,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Baris Tombol Angka & Huruf QWERTY
                rows.forEachIndexed { rowIndex, keys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        keys.forEachIndexed { keyIndex, ch ->
                            val isFirstKey = rowIndex == 1 && keyIndex == 0 // Fokus awal di huruf 'Q'
                            var keyFocused by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (keyFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .size(width = 48.dp, height = 42.dp)
                                    .scale(if (keyFocused) 1.08f else 1f)
                                    .then(
                                        if (isFirstKey) Modifier.focusRequester(firstKeyFocusRequester)
                                        else Modifier
                                    )
                                    .onFocusChanged { keyFocused = it.isFocused }
                                    .clickable { query += ch.lowercase() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ch,
                                        color = if (keyFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Baris Aksi: Spasi, Hapus, Clear, Cari, Tutup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { query += " " },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.dpadFocusable(
                            shape = RoundedCornerShape(50),
                            style = FocusStyle.CTA,
                            onClick = { query += " " }
                        )
                    ) {
                        Icon(Icons.Default.SpaceBar, contentDescription = "Spasi", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spasi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { if (query.isNotEmpty()) query = query.dropLast(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.dpadFocusable(
                            shape = RoundedCornerShape(50),
                            style = FocusStyle.CTA,
                            onClick = { if (query.isNotEmpty()) query = query.dropLast(1) }
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { query = "" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.dpadFocusable(
                            shape = RoundedCornerShape(50),
                            style = FocusStyle.CTA,
                            onClick = { query = "" }
                        )
                    ) {
                        Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                onSearch(query.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.dpadFocusable(
                            shape = RoundedCornerShape(8.dp),
                            style = FocusStyle.CTA,
                            onClick = {
                                if (query.isNotBlank()) {
                                    onSearch(query.trim())
                                }
                            }
                        )
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cari", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.dpadFocusable(
                            shape = RoundedCornerShape(8.dp),
                            style = FocusStyle.CTA,
                            onClick = onDismiss
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batal", color = Color.LightGray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
