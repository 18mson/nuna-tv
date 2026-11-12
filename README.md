# Nuna TV

Nuna TV adalah aplikasi pemutar YouTube dan pengelola video offline untuk Android TV maupun ponsel Android dalam satu basis kode Kotlin dan Jetpack Compose. Tampilan dan navigasi menyesuaikan perangkat secara otomatis, mendukung remote D-Pad di TV serta layar sentuh di ponsel tanpa login akun Google.

## Unduh APK (v1.1.0)

APK release terbaru (`v1.1.0`, `versionCode = 2`) tersimpan di direktori [`releases/v1.1.0/`](releases/v1.1.0) beserta berkas checksum [`SHA256SUMS.txt`](releases/v1.1.0/SHA256SUMS.txt). Riwayat perubahan lengkap tersedia di [`CHANGELOG.md`](CHANGELOG.md).

| Target Perangkat | Arsitektur ABI | Ukuran | Berkas APK |
| :--- | :--- | :--- | :--- |
| Ponsel Android 64-bit dan emulator ARM64 | `arm64-v8a` | 35 MB | [nuna-tv-v1.1.0-arm64-v8a.apk](releases/v1.1.0/nuna-tv-v1.1.0-arm64-v8a.apk) |
| Android TV dan proyektor 32-bit | `armeabi-v7a` | 33 MB | [nuna-tv-v1.1.0-armeabi-v7a.apk](releases/v1.1.0/nuna-tv-v1.1.0-armeabi-v7a.apk) |

Contoh pemasangan melalui ADB:

```bash
# Untuk ponsel Android (arm64-v8a)
adb install -r releases/v1.1.0/nuna-tv-v1.1.0-arm64-v8a.apk

# Untuk Android TV / proyektor (armeabi-v7a)
adb -s <IP_TV>:5555 install -r releases/v1.1.0/nuna-tv-v1.1.0-armeabi-v7a.apk
```

Catatan build: APK pada rilis ini ditandatangani dengan `signingConfigs.getByName("debug")` sesuai konfigurasi di [`app/build.gradle.kts`](app/build.gradle.kts).

## Fitur Utama

### Beranda Video Offline (Pustaka Offline)
- Menyimpan daftar video yang sudah diunduh di database Room lengkap dengan kategori kustom, pencarian cepat, banner acak, serta informasi sisa ruang penyimpanan.
- Deteksi kategori otomatis saat video diunduh berdasarkan kategori YouTube, tag, judul, dan nama channel, ditambah tombol **Deteksi Otomatis Video 'Umum'** untuk merapikan koleksi lama.
- Kartu unduhan langsung tampil instan di Home sejak progres `0%` (dengan skeleton loading sebelum metadata selesai diambil) serta menampilkan overlay progres setengah gelap sesuai persentase unduhan.
- Pada mode TV, tahan tombol `OK` selama 650 ms atau tekan tombol `Menu`, `Info`, `Delete`, atau `Merah` di kartu video untuk membuka dialog aksi. Pada versi ponsel tersedia tombol menu di setiap kartu.

### Tab YouTube Tanpa WebView & Shorts
- Menampilkan beranda feed adaptif dengan persistensi state dan posisi scroll saat berpindah tab maupun kembali dari pemutar video.
- Mendukung **YouTube Shorts** lewat rak khusus di Beranda, tab/grid vertikal 9:16, serta gestur swipe atas/bawah di pemutar untuk berpindah antar Shorts.
- Header mobile dirancang ringkas dengan satu baris pencarian, tombol Filter & Riwayat (bottom sheet), tombol tempel tautan (`watch`, `youtu.be`, `shorts`, `live`, `embed`), serta gestur tarik untuk menyegarkan (`PullToRefresh`).
- Pencarian di TV memakai dialog keyboard QWERTY di layar beserta saran ketikan otomatis.

### Pemutar Video, Background Audio, PiP & Mini Player
- Dibangun dengan Media3 ExoPlayer (termasuk dukungan HLS/DASH untuk siaran **Live** dan `MergingMediaSource`), dengan jalur cepat Innertube `/youtubei/v1/player` serta prefetch video berikutnya agar pembukaan video lebih cepat.
- **Pemutaran Latar Belakang** melalui `MediaPlaybackService` dan notifikasi `MediaStyle` (bawaan nonaktif, dapat diaktifkan lewat menu **Settings**).
- **Jendela Popup / Picture-in-Picture (PiP)** untuk ponsel (bawaan nonaktif, dapat diaktifkan lewat menu **Settings**; pemutaran berhenti total ketika jendela PiP ditutup).
- **Mini Player** dalam aplikasi (`MiniPlayerBar`) di atas bilah navigasi bawah saat player diperkecil.
- Kontrol remote D-Pad di TV (`OK`, kiri/kanan singkat +/-10 detik, tahan kiri/kanan pindah video, bawah rekomendasi, atas menu channel & resolusi) serta tombol **Download** langsung di bar atas pemutar.
- Penguat suara (`Audio Boost`) memanfaatkan `LoudnessEnhancer` Android (`Normal`, `+6 dB`, `+12 dB`, `+18 dB`) dan panduan pengguna 3 langkah yang dapat dibuka ulang dari **Settings**.

### Pengunduhan & Tata Letak Adaptif
- Unduhan berjalan di latar belakang melalui Foreground Service (`DownloadService`) menggunakan `youtubedl-android` dan FFmpeg, dengan pemeriksaan sisa penyimpanan sebelum unduhan dimulai, pembersihan otomatis bila gagal, dan pembersihan notifikasi progres saat selesai.
- Navigasi menggunakan sidebar di Android TV atau mode layar horizontal serta bilah navigasi bawah ramping pada ponsel mode portrait.

## Teknologi yang Digunakan

- Bahasa & UI: Kotlin `2.0.21`, Jetpack Compose BOM `2024.10.01`, Material 3, Compose for TV `1.0.0`, minSdk `24`, targetSdk `35`
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

