# Changelog

Semua perubahan penting pada proyek Nuna TV dicatat di dokumen ini mengikuti format Semantic Versioning (`MAJOR.MINOR.PATCH`).

## [1.1.0] - 2026-11-12

Pembaruan besar untuk pengalaman ponsel, pemutaran latar belakang, jendela popup (PiP), YouTube Shorts, siaran Live, optimasi kecepatan stream, dan deteksi kategori otomatis (`versionCode = 2`).

### Ditambahkan
- **Pemutaran Latar Belakang (Background Audio)** melalui `MediaPlaybackService` (`MediaSessionCompat` + `NotificationCompat.MediaStyle`) dengan kontrol Sebelumnya, Putar/Jeda, Berikutnya, dan Tutup di panel notifikasi maupun layar kunci (bawaan OFF, diaktifkan dari Settings).
- **Jendela Popup / Picture-in-Picture (PiP)** khusus perangkat non-TV dengan `RemoteAction` kontrol pemutaran, tombol PiP di player, serta aturan penutupan jendela PiP (berhenti total bila pemutaran latar belakang mati, atau lanjut memutar audio di notifikasi bila pemutaran latar belakang aktif).
- **Mini Player Dalam Aplikasi (`MiniPlayerBar`)** di atas bilah navigasi bawah saat player diperkecil agar pengguna dapat tetap menjelajahi tab lain sambil menonton atau mendengarkan.
- **Dukungan YouTube Shorts**: parsing renderer Shorts (`reelItemRenderer`, `shortsLockupViewModel`), rak carousel Shorts di Beranda, grid vertikal 9:16 pada tab Shorts, serta gestur swipe atas/bawah di player untuk berpindah antar Shorts.
- **Dukungan Siaran Live (HLS/DASH)** dengan menambahkan `media3-exoplayer-hls` dan `media3-exoplayer-dash`, penanganan manifest `m3u8`/`mpd`, serta badge `LIVE` di player dan media session.
- **Deteksi Kategori Otomatis (`CategoryDetector`)** saat mengunduh video berdasarkan kategori YouTube resmi, tag, judul, dan channel, plus tombol **Deteksi Otomatis Video 'Umum'** di dialog kelola kategori Home.
- **Kartu Unduhan Instan di Home**: video yang mulai diunduh langsung muncul di Home dengan status `0%` dan skeleton loading sebelum metadata selesai diambil, dilengkapi overlay progres setengah gelap pada thumbnail.
- **Panduan Awal (`OnboardingDialog`)** 3 langkah yang tampil otomatis saat pertama kali membuka aplikasi dan dapat dibuka ulang dari Settings.

### Diperbaiki & Dioptimalkan
- **Percepatan Muat Stream YouTube**: jalur cepat langsung ke `/youtubei/v1/player` tanpa proses Python/`yt-dlp` (dengan fallback otomatis ke `yt-dlp`), sinkronisasi `User-Agent`, dan prefetch video berikutnya di latar belakang.
- **Persistensi Tab YouTube (`YoutubeViewModel`)**: hasil pencarian, kategori aktif, filter, dan posisi scroll tetap terjaga saat berpindah tab atau kembali dari player.
- **Pemutaran Video Offline**: menggunakan `DefaultDataSource.Factory` agar file lokal (`file://`) dapat diputar lancar bersama stream HTTP.
- **Notifikasi Download**: membersihkan notifikasi progres foreground secara otomatis saat unduhan selesai atau gagal.
- **Desain UI & Responsivitas Mobile**: header tab YouTube 1 baris dengan bottom sheet Filter & Riwayat serta `PullToRefreshBox`, bilah navigasi bawah ramping dengan aksen atas `#E50914`, kontrol player mobile yang lebih bersih, dan pemindahan tombol Download ke menu bar atas player.

## [1.0.0] - 2026-11-12

Rilis awal aplikasi hybrid Android TV dan Android Mobile (`versionCode = 1`).

### Ditambahkan
- Beranda video offline berbasis Room database dengan kategori kustom, filter cepat, spotlight banner acak, serta indikator ruang penyimpanan di Beranda dan Settings.
- Menu aksi kartu video di mode TV melalui tahan tombol `OK` (650 ms) atau tombol `Menu` / `Info` / `Delete` / `Merah`, serta pelindung agar pelepasan tahan `OK` tidak memicu tombol pertama secara tidak sengaja.
- Tab YouTube Native tanpa WebView dengan feed `Untuk Anda`, riwayat tontonan hingga 20 video, riwayat pencarian, filter tipe dan durasi (`Semua`, `Video >= 4 mnt`, `Shorts / Pendek`, `Live`), serta pengurutan durasi.
- Dialog keyboard QWERTY dengan autocomplete di TV dan tombol tempel tautan YouTube beserta dialog responsif di ponsel.
- Menu titik tiga pada kartu YouTube (serta long-press `OK` / `Menu` di TV) untuk putar langsung, antrean putar berikutnya, unduh, halaman utama channel, dan salin/bagikan tautan.
- Pemutar Media3 ExoPlayer dengan penguat suara `LoudnessEnhancer` (`Normal`, `+6 dB`, `+12 dB`, `+18 dB`), menu atas (`DPAD_UP`) untuk berpindah ke halaman utama channel dan mengganti resolusi stream online (`MergingMediaSource`), panel rekomendasi bawah (`DPAD_DOWN`), popup autoplay 5 detik, dan banner download non-blocking 7 detik.
- Tata letak adaptif antara Android TV, ponsel horizontal, dan ponsel portrait (termasuk kontrol player 5 tombol yang simetris dan aman terhadap status bar maupun bar navigasi).
