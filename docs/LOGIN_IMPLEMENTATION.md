# OlahragaKuy — tahap pertama

Referensi: https://www.figma.com/design/zqSCG7yL09UcBLeYlPOR7b?node-id=5-2002

## Yang diimplementasikan
- Tema navy, warna aksen dan tombol sesuai node Login.
- Poppins lokal: Light, Regular, SemiBold, Bold; lisensi di docs/licenses.
- Wordmark dan empat ikon diekspor langsung dari node Figma sebagai PNG resolusi tinggi.
- Input nama pengguna dan kata sandi, masking password, validasi kolom kosong.
- Keyboard Next/Done, scroll saat ruang terbatas, penyesuaian system insets Android.
- Informasi eksplisit untuk login/provider/pendaftaran yang belum tersedia; tidak ada autentikasi palsu.
- Kembali menutup Activity sementara; hubungkan ke Opening ketika navigasi dibuat.

## Sumber aset
| Resource | Figma node | Ukuran tampilan |
|---|---|---|
| login_wordmark | 5:2034 | painted bounds 126.33 x 19 dp dalam area tinggi 23 dp |
| login_google | 5:2017 | 37 x 37 dp |
| login_facebook | 5:2024 | 36 x 36 dp |
| login_instagram | 5:2028 | 37 x 37 dp |
| login_back | 5:2039 | 18 x 18 dp |

PNG disimpan di drawable-nodpi agar density otomatis tidak mengubah ukuran.
Font berasal dari google/fonts, ofl/poppins. Tidak ada URL Figma sementara di kode runtime.

## Menjalankan
Buka project OlahragaKuy di Android Studio, tunggu Gradle Sync, pilih perangkat lalu Run.
Entry point: MainActivity; layar: ui/login/LoginScreen.kt.
Preview tersedia pada 440 x 906, 360 x 740, serta font scale 1.5.

## Verifikasi
- Build: ./gradlew :app:assembleDebug
- Analisis: ./gradlew :app:lintDebug
- Render referensi visual: ./gradlew :app:updateDebugScreenshotTest
- Regresi terhadap baseline yang sudah ditinjau: ./gradlew :app:validateDebugScreenshotTest

Screenshot test menggunakan plugin eksperimental resmi 0.0.1-alpha16, sesuai AGP 9.4.1 project ini.
Jangan menyamakan keberhasilan screenshot regression dengan kemiripan Figma 95%.
Area status bar iPhone tidak digambar di aplikasi Android. Baseline konten desain: 440 x 906.
Pada ukuran lain, font/kontrol tetap terbaca, whitespace menyesuaikan, dan konten bisa digulir.

## Berikutnya
Opening dan Daftar dari node masing-masing, navigasi, kemudian integrasi autentikasi sungguhan.
Maps dan fitur komunitas belum diimplementasikan pada tahap ini.

## Hasil pemeriksaan tahap pertama
- assembleDebug: berhasil.
- lintDebug: 0 error; peringatan terutama berasal dari template/dependency bawaan.
- Tiga preview (referensi, compact, font besar): berhasil dirender dan ditinjau.
- validateDebugScreenshotTest: berhasil terhadap baseline implementasi yang sudah ditinjau.
- Perbandingan Figma: SSIM RGB uniform 7x7 = 0.954171 pada kondisi form kosong,
  area konten 440x906 tanpa status bar. Screenshot Android dinormalisasi ke resolusi referensi.
- Pengujian interaksi pada HP/emulator belum dijalankan: emulator lokal gagal memulai.
  Validasi runtime berikutnya: input kosong, password tersembunyi, IME Next/Done,
  scrolling saat keyboard terbuka, tombol kembali, serta dialog fitur yang belum tersedia.
