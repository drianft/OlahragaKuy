# 🏃‍♂️ OlahragaKuy

> **Anti-Hustle Fitness & Organic Social Network**
> 
> OlahragaKuy adalah aplikasi kebugaran dan jejaring sosial *mobile native* (Android) yang dirancang khusus untuk pemula. Kami berfokus pada privasi, komunitas organik, dan lingkungan yang suportif tanpa tekanan metrik kompetitif (*no leaderboards*).

---

## 👥 User Roles

Aplikasi ini dirancang sesederhana mungkin dengan hierarki pengguna yang *flat* untuk menjaga komunitas tetap organik:

- **Member (Regular User):** Pengguna utama aplikasi. Memiliki akses penuh ke pelacakan olahraga, panduan 2D *workout*, pencarian *venue*, serta fitur sosial (Post, Chat, Message Request).
- **System Admin:** Pengelola *database* dan ekosistem. Bertugas memoderasi konten toksik/spam, menyetujui atau memperbarui data *venue* di peta, dan mengelola keamanan sistem.
- **Community Moderator (*Future Update*):** Member terverifikasi yang diberikan akses ringan untuk membantu Admin menjaga grup obrolan dan *feed* agar tetap positif dan suportif.


## ✨ Fitur Utama

-  **Organic Social & Community:** Cari teman olahraga di dunia nyata menggunakan ID/Nickname. Posting aktivitas ke komunitas (100% *location-tag free* untuk privasi).
-  **Private Chat & Message Requests:** Ngobrol aman dengan teman atau grup komunitas. Pesan dari pengguna tak dikenal masuk ke *Message Requests* (anti-spam).
-  **Venue Finder Maps:** Temukan lokasi GOR, taman, atau *gym* terdekat menggunakan OpenStreetMap (OPM) API dan bagikan langsung ke ruang obrolan untuk janjian.
-  **Lightweight 2D Workout Guides:** Panduan *bodyweight workout* di rumah menggunakan animasi 2D *frame-by-frame* yang ringan, hemat kuota, dan mudah diikuti pemula.
-  **Step, Route & Health Tracker:** Terintegrasi dengan **Health Connect API**. Lacak rute, langkah, dan kalori. Fitur *Share Route* secara otomatis menghilangkan peta latar (hanya menampilkan garis rute) demi privasi lokasi rumah/rute harian.
-  **Hydration Reminder:** Pengingat minum air agar pengguna tetap terhidrasi.
-  **Personal Streak:** Gamifikasi berbasis konsistensi personal, bukan kompetisi dengan orang lain.


## 💻 Tech Stack

Dibangun sepenuhnya secara *Native* untuk Android demi mengejar performa yang mulus, responsif, dan hemat baterai saat melacak rute.

### Frontend (Android)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (UI deklaratif modern)
- **Architecture:** MVVM (Model-View-ViewModel) dengan Clean Architecture
- **Animations:** Lottie (Format `.json` yang sangat ringan untuk animasi 2D tanpa membebani ukuran APK)
- **Dependency Injection:** Dagger Hilt

### Backend & Data Management
- **BaaS:** Firebase (Authentication, Cloud Firestore untuk *chat & feed*, Cloud Messaging untuk notifikasi)
- **Local Storage:** Room Database (Untuk menyimpan data *Personal Streak* dan *Hydration Reminder* secara *offline*)

### Integrations & APIs
- **Maps:** OpenStreetMap (OSM) terintegrasi via OSMDroid (Open-source, ringan, dan ramah privasi).
- **Health Tracking:** Health Connect API (Standar terbaru Android) dan Location Services untuk *tracking* rute.
