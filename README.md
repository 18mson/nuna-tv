# Nuna TV

Nuna TV adalah aplikasi pemutar YouTube dan pengelola video offline untuk Android TV maupun ponsel Android dalam satu basis kode Kotlin dan Jetpack Compose. Tampilan dan navigasi menyesuaikan perangkat secara otomatis, mendukung remote D-Pad di TV serta layar sentuh di ponsel tanpa login akun Google.

## Unduh APK (v1.2.0)

APK release terbaru (`v1.2.0`, `versionCode = 3`) tersimpan di direktori [`releases/v1.2.0/`](releases/v1.2.0) beserta berkas checksum [`SHA256SUMS.txt`](releases/v1.2.0/SHA256SUMS.txt). Riwayat perubahan lengkap tersedia di [`CHANGELOG.md`](CHANGELOG.md).

| Target Perangkat | Arsitektur ABI | Ukuran | Berkas APK |
| :--- | :--- | :--- | :--- |
| Ponsel Android 64-bit dan emulator ARM64 | `arm64-v8a` | 35 MB | [nuna-tv-v1.2.0-arm64-v8a.apk](releases/v1.2.0/nuna-tv-v1.2.0-arm64-v8a.apk) |
| Android TV dan proyektor 32-bit | `armeabi-v7a` | 33 MB | [nuna-tv-v1.2.0-armeabi-v7a.apk](releases/v1.2.0/nuna-tv-v1.2.0-armeabi-v7a.apk) |

Contoh pemasangan melalui ADB:

```bash
# Untuk ponsel Android (arm64-v8a)
adb install -r releases/v1.2.0/nuna-tv-v1.2.0-arm64-v8a.apk

# Untuk Android TV / proyektor (armeabi-v7a)
adb -s <IP_TV>:5555 install -r releases/v1.2.0/nuna-tv-v1.2.0-armeabi-v7a.apk
```

Catatan build: APK pada rilis ini ditandatangani dengan `signingConfigs.getByName("debug")` sesuai konfigurasi di [`app/build.gradle.kts`](app/build.gradle.kts).

## Fitur Utama

### Beranda Video Offline (Pustaka Offline) & Multi-Kategori
- **Dukungan Multi-Kategori per Video**: Satu video dapat dimasukkan ke dalam beberapa kategori sekaligus dengan dialog pilihan checklist multi-select yang praktis.
- **Pemutaran Kategori Instan (Play & Shuffle)**: Di samping nama setiap kategori, tersedia tombol **Putar (Play A-Z)** untuk memutar seluruh video urut abjad, serta tombol **Acak (Shuffle)** untuk langsung memutar video secara acak.
- **Deteksi Kategori Otomatis**: Mendeteksi kategori secara cerdas saat video diunduh berdasarkan kategori YouTube resmi, tag, judul, dan nama channel, dilengkapi tombol otomatisasi untuk merapikan kategori lama.
- **Kartu Unduhan Realtime**: Video langsung tampil di Beranda sejak `0%` dengan skeleton loading dan overlay progres setengah transparan sesuai kemajuan unduhan.
- **Navigasi TV & Ponsel**: Tahan tombol `OK` (650 ms) atau tekan tombol `Menu`, `Info`, `Delete`, atau `Merah` pada remote TV untuk membuka dialog aksi kartu; pada versi ponsel tersedia tombol menu di setiap kartu.

### Tab YouTube, Halaman Channel & Shorts
- **Jelajah Channel YouTube Lengkap**: Halaman channel menampilkan banner resmi, avatar foto profil, nama channel, handle (`@...`), deskripsi, serta kolom pencarian khusus di dalam channel (*"Cari di channel ini..."*) dan tab filter (*"Semua"*, *"Populer"*).
- **Beranda & Feed Adaptif**: State dan posisi scroll tersimpan persisten saat berpindah tab maupun kembali dari pemutar video.
- **YouTube Shorts**: Rak khusus Shorts di Beranda, tab/grid vertikal 9:16, serta gestur swipe atas/bawah di pemutar untuk berpindah video secara mulus.
- **Header Mobile & Input TV**: Pencarian mobile satu baris ringkas dengan bottom sheet Filter & Riwayat, tombol tempel tautan instan, dan gestur tarik untuk menyegarkan (*Pull-to-Refresh*). Di TV tersedia popup keyboard virtual QWERTY dengan sudut melengkung yang konsisten.

### Pemutar Video, Background Audio, PiP & Mini Player
- **ExoPlayer Berkecepatan Tinggi**: Jalur cepat Innertube `/youtubei/v1/player` tanpa overhead Python untuk memulai video instan, prefetch video berikutnya di latar belakang, dan dukungan siaran langsung **Live (HLS/DASH)**.
- **Penyimpanan Posisi Tonton Terakhir (`PlaybackPositionStore`)**: Melanjutkan pemutaran video secara otomatis dari posisi terakhir saat video dibuka kembali.
- **Pemutaran Latar Belakang (Background Audio)**: Berjalan melalui `MediaPlaybackService` dan notifikasi `MediaStyle` (opsional, dapat diaktifkan di Settings).
- **Jendela Popup / Picture-in-Picture (PiP)**: Mendukung mode PiP di ponsel dengan integrasi kontrol pemutaran audio di latar belakang.
- **Mini Player**: `MiniPlayerBar` interaktif di atas bilah navigasi bawah saat pemutar diperkecil.
- **Menu Pengaturan Pemutar Bersih**: Menu atas (`DPAD_UP` di TV atau ikon Pengaturan di HP) menyediakan akses ke channel utama, audio boost (`LoudnessEnhancer` hingga +18 dB), autoplay, resolusi online, dan tombol unduh. Pada mode ponsel, menu otomatis tertutup saat layar di luar menu disentuh.

### Pengaturan Resolusi Unduhan & Personalisasi Tema
- **Tingkatan Resolusi Unduhan Adaptif**:
  - **Tinggi**: 1080p - 2K (memilih kualitas terbaik yang tersedia).
  - **Sedang**: 720p - 480p (keseimbangan kualitas dan ukuran).
  - **Rendah**: 360p - 240p (hemat kuota dan memori).
  - **Audio Saja**: mengekstrak format audio terbaik (M4A/MP3).
- **Hidden Setting (Easter Egg)**: Ketuk 5x pada opsi resolusi unduhan untuk beralih antara tingkatan adaptif dan rincian angka resolusi manual (1080p, 720p, 480p, 360p, 240p, Audio), dan ketuk 5x lagi untuk kembali.
- **Material 3 Expressive & Tema Warna**:
  - Pilihan tema warna dinamis (**Ungu**, **Merah**, **Biru**, **Coklat**) yang diterapkan seragam ke seluruh komponen aplikasi.
  - Mode Gelap (*Dark Mode*) dan Terang (*Light Mode*).
  - Floating pill navigation bar modern untuk ponsel portrait.
  - Bebas dari border line kasar untuk tampilan yang lebih halus dan modern.

## Teknologi yang Digunakan

- Bahasa & UI: Kotlin `2.0.21`, Jetpack Compose BOM `2024.10.01`, Material 3 Expressive, Compose for TV `1.0.0`, minSdk `24`, targetSdk `35`
- Pemutar media: AndroidX Media3 ExoPlayer, HLS, DASH & UI `1.5.0`, serta AndroidX Media Compat `1.7.0`
- Penyimpanan & preferensi: Room `2.6.1` (KSP) dan DataStore Preferences `1.1.1`
- Ekstraktor & pengunduh video: `youtubedl-android` & FFmpeg `0.17.4`
- Pemuatan gambar: Coil Compose `2.7.0`

## Cara Build dari Kode Sumber

1. Pastikan Android SDK 35 dan JDK 17+ tersedia (misalnya JBR bawaan Android Studio).
2. Sesuaikan `sdk.dir` pada file `local.properties` bila tidak memakai Android Studio secara langsung.
3. Jalankan perintah berikut dari root proyek:

```bash
./gradlew assembleRelease -x lintVitalRelease
```

Hasil build akan dibuat terpisah di `app/build/outputs/apk/release/` untuk `arm64-v8a` dan `armeabi-v7a`.

## Kredit

Nuna TV berjalan dengan dukungan berbagai proyek sumber terbuka berikut:

- [youtubedl-android](https://github.com/junkfood02/youtubedl-android) (`io.github.junkfood02.youtubedl-android` oleh JunkFood02 serta basis awal dari `yausername`) yang menyediakan integrasi Python dan runtime ekstraksi di Android.
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) untuk mengambil metadata video, daftar resolusi stream, dan pengunduhan media.
- [FFmpeg](https://ffmpeg.org/) untuk menggabungkan stream video dan audio serta memproses berkas media hasil unduhan.
- AndroidX Media3 (ExoPlayer), Jetpack Compose, Compose for TV, Room, WorkManager, dan DataStore dari tim Android Open Source Project (Google) untuk pemutar media, antarmuka, penyimpanan lokal, dan tugas latar belakang.
- [Coil](https://github.com/coil-kt/coil) untuk pemuatan gambar thumbnail di Jetpack Compose.
- Kotlin dan Kotlinx Coroutines dari JetBrains.

## Lisensi

Proyek ini dirilis di bawah lisensi MIT. Lihat berkas [`LICENSE`](LICENSE) untuk ketentuan lengkap.
