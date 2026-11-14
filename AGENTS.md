# Panduan kerja OlahragaKuy

## Komunikasi
- Gunakan bahasa Indonesia yang ringkas dan mudah dipahami.
- Jika diminta saran atau diskusi, jelaskan dahulu tanpa mengedit.
- Jika diminta implementasi, kerjakan sampai selesai dan verifikasi.
- Laporkan perubahan, hasil pemeriksaan, dan kendala yang tersisa.

## Konteks produk
- Aplikasi Android kebugaran dan sosial untuk pemula.
- Utamakan privasi dan komunitas yang suportif.
- Pertahankan prinsip tanpa leaderboard dan tanpa tag lokasi pada post.
- Baca README.md untuk arah produk; cek kode untuk status implementasi.
- MVVM, Clean Architecture, Hilt, dan Room yang disebut di README adalah
  arah arsitektur; pastikan penerapannya dari kode dan dependensi sebelum
  mengandalkannya. Jangan melakukan migrasi arsitektur di luar lingkup tugas.

## Peta kode
Path berikut relatif terhadap app/src/main/java/com/example/olahragakuy/:
- pages/: layar aplikasi.
- pages/posts/: pembuatan post, unggah foto, dan komentar.
- ui/components/: komponen UI bersama.
- ui/theme/: warna, tema, dan tipografi.
- AuthNavigation.kt: navigasi autentikasi.

Konfigurasi dependensi ada di app/build.gradle.kts dan gradle/libs.versions.toml.

## Cara bekerja
- Ikuti pola Kotlin dan Jetpack Compose yang digunakan di fitur terkait.
- Gunakan kembali komponen dan tema yang tersedia.
- Fokuskan perubahan pada kebutuhan tugas.
- Pertahankan perubahan pengguna yang sudah ada.
- Gunakan tools Android Studio untuk membaca dan mengedit file,
  agar perubahan yang belum disimpan tetap terbaca.
- Jalankan perintah terminal melalui studio_run_shell_command.
- Sesuaikan dependensi dengan konfigurasi Gradle dan version catalog
  yang sudah ada.

## Verifikasi
- Untuk perubahan kode, jalankan build atau kompilasi yang relevan.
- Jalankan unit test terkait ketika mengubah perilaku atau logika.
- Untuk perubahan UI, periksa preview atau perangkat bila tersedia.
- Perubahan komentar perlu memperhatikan keyboard, tombol kembali,
  scrolling, serta kondisi loading, kosong, dan gagal.
- Sebutkan pemeriksaan yang belum bisa dijalankan beserta alasannya.
- Perubahan dokumentasi saja cukup diperiksa isi dan diff-nya;
  tidak perlu menjalankan build aplikasi.
