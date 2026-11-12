# Changelog

Semua perubahan penting pada proyek Nuna TV dicatat di dokumen ini mengikuti format Semantic Versioning (`MAJOR.MINOR.PATCH`).

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
