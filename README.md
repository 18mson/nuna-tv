# Nuna TV

Nuna TV adalah aplikasi pemutar YouTube dan pengelola video offline untuk Android TV maupun ponsel Android dalam satu basis kode Kotlin dan Jetpack Compose. Tampilan dan navigasi menyesuaikan perangkat secara otomatis, mendukung remote D-Pad di TV serta layar sentuh di ponsel tanpa login akun Google.

## Unduh APK (v1.0.0)

APK release terbaru (`v1.0.0`, `versionCode = 1`) tersimpan di direktori [`releases/v1.0.0/`](releases/v1.0.0) beserta berkas checksum [`SHA256SUMS.txt`](releases/v1.0.0/SHA256SUMS.txt):

| Target Perangkat | Arsitektur ABI | Ukuran | Berkas APK |
| :--- | :--- | :--- | :--- |
| Ponsel Android 64-bit dan emulator ARM64 | `arm64-v8a` | 35 MB | [nuna-tv-v1.0.0-arm64-v8a.apk](releases/v1.0.0/nuna-tv-v1.0.0-arm64-v8a.apk) |
| Android TV dan proyektor 32-bit | `armeabi-v7a` | 33 MB | [nuna-tv-v1.0.0-armeabi-v7a.apk](releases/v1.0.0/nuna-tv-v1.0.0-armeabi-v7a.apk) |

Contoh pemasangan melalui ADB:

```bash
# Untuk ponsel Android (arm64-v8a)
adb install -r releases/v1.0.0/nuna-tv-v1.0.0-arm64-v8a.apk

# Untuk Android TV / proyektor (armeabi-v7a)
adb -s <IP_TV>:5555 install -r releases/v1.0.0/nuna-tv-v1.0.0-armeabi-v7a.apk
```

Catatan build: APK pada rilis ini ditandatangani dengan `signingConfigs.getByName("debug")` sesuai konfigurasi di [`app/build.gradle.kts`](app/build.gradle.kts).

## Fitur Utama

### Beranda Video Offline
- Menyimpan daftar video yang sudah diunduh di database Room lengkap dengan kategori kustom, pencarian cepat, banner acak, serta informasi sisa ruang penyimpanan.
- Pada mode TV, tahan tombol `OK` selama 650 ms atau tekan tombol `Menu`, `Info`, `Delete`, atau `Merah` di kartu video untuk membuka dialog aksi (putar, ubah kategori, atau hapus). Ikon hapus kecil di kartu disembunyikan pada mode TV agar fokus D-Pad tetap rapi.

### Tab YouTube Tanpa WebView
- Menampilkan feed rekomendasi `Untuk Anda` yang diperbarui dari video terakhir yang ditonton dan riwayat pencarian.
- Menyimpan riwayat tontonan hingga 20 video terakhir pada chip `Riwayat` dan menyertakan chip `Hapus Riwayat`.
- Menyediakan filter tipe konten dan durasi (`Semua`, `Video (>= 4 mnt)`, `Shorts / Pendek`, `Live`) serta pengurutan durasi.
- Pencarian di TV memakai dialog keyboard QWERTY di layar beserta saran ketikan otomatis, sedangkan di ponsel tersedia kolom ketik langsung dan tombol tempel tautan YouTube (`watch`, `youtu.be`, `shorts`, `live`, `embed`) untuk memutar atau mengunduh dari URL.
- Kartu video memiliki menu titik tiga di versi ponsel (serta tahan `OK` atau tombol `Menu` di versi TV) untuk memutar langsung, menambah ke antrean putar berikutnya, mengunduh sesuai resolusi default, membuka halaman utama channel, atau menyalin/membagikan tautan.

### Pemutar Video & Audio Boost
- Dibangun dengan Media3 ExoPlayer untuk memutar file lokal maupun stream YouTube online (termasuk video dan audio terpisah via `MergingMediaSource`).
- Kontrol remote D-Pad di TV: `OK` untuk jeda atau lanjut, kiri/kanan singkat untuk lompat 10 detik, tahan kiri/kanan untuk pindah video sebelumnya atau berikutnya, bawah untuk membuka panel rekomendasi, dan atas untuk membuka menu atas.
- Menu atas di pemutar berisi pintasan menuju halaman utama channel pemilik video (berlaku untuk video online maupun offline) serta opsi penggantian resolusi stream yang sedang aktif (khusus video online, dengan penanda resolusi yang sedang diputar).
- Menampilkan popup putar otomatis video berikutnya 5 detik sebelum video berakhir, serta banner non-blocking selama 7 detik di awal pemutaran stream YouTube jika video belum diunduh.
- Penguat suara (`Audio Boost`) memanfaatkan `LoudnessEnhancer` Android dengan empat pilihan tingkat (`Normal`, `+6 dB`, `+12 dB`, `+18 dB`) yang dapat diatur permanen lewat Settings atau langsung saat memutar di ponsel.

### Pengunduhan & Tata Letak Adaptif
- Unduhan berjalan di latar belakang melalui Foreground Service (`DownloadService`) menggunakan `youtubedl-android` dan FFmpeg, dengan pemeriksaan sisa penyimpanan sebelum unduhan dimulai serta pembersihan otomatis bila unduhan gagal.
- Navigasi menggunakan sidebar di Android TV atau mode layar horizontal (melebar saat fokus remote masuk ke sidebar dan mengecil saat kembali ke konten) serta bilah navigasi bawah pada ponsel mode portrait.

## Teknologi yang Digunakan

- Bahasa & UI: Kotlin `2.0.21`, Jetpack Compose BOM `2024.10.01`, Material 3, Compose for TV `1.0.0`, minSdk `24`, targetSdk `35`
- Pemutar media: AndroidX Media3 ExoPlayer & UI `1.5.0`
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

