# Konten portofolio: Sakuin (draf)

Draf untuk form admin portofolio. Semua isi dicek terhadap kode di repo per commit `24c2dad` (23 Okt 2026).

## Fakta terverifikasi (untuk cek ulang)

- Aplikasi Android native, Kotlin + Jetpack Compose (Material 3), minSdk 24, targetSdk 36. Versi 1.1 (versionCode 2).
- Proyek individu. Peran: **Android Developer & UI Designer**. 5 commit di repo, semuanya dari akun saya, 21–23 Oktober 2026.
- Data lokal: Room 3 (`androidx.room3`) dengan bundled SQLite, 3 tabel (wallets, categories, transactions), skema versi 2 (versi 2 menambah biaya admin transfer). Pengaturan di Preferences DataStore.
- Nominal disimpan sebagai `Long` rupiah. Saldo dompet dihitung dari transaksi, tidak disimpan.
- Dompet jenis Cash, Card, E-wallet. Dompet yang sudah punya transaksi hanya bisa diarsipkan, tidak bisa dihapus.
- Transfer antardompet dengan biaya admin opsional (pilihan cepat Rp 1.000, Rp 2.500, Rp 6.500). Biaya dihitung sebagai pengeluaran dompet asal; nominal transfer sendiri tidak masuk chart.
- Uang keluar punya kategori dan nama kebutuhan, dengan saran nama dari riwayat per kategori.
- Daftar transaksi per bulan, dikelompokkan per hari, dengan pencarian, chip tipe, filter dompet/kategori, dan hapus dengan Undo.
- Statistik: total masuk/keluar/net, bar chart pemasukan vs pengeluaran 6 bulan, donut pengeluaran per kategori. Periode minggu/bulan/tahun dan filter dompet. Chart digambar sendiri dengan Compose Canvas.
- Scan resi: kamera (CameraX) atau galeri (Photo Picker). Foto diputar sesuai EXIF, diperkecil ke sisi terpanjang 1600 px, JPEG kualitas 80, lalu dikirim ke Gemini API (`generateContent` dengan JSON response schema) lewat OkHttp. Hasil: total, merchant, tanggal/jam, kategori (dari daftar kategori yang ada), nama saran, dan item.
- Klien Gemini mencoba model cadangan saat model utama 404 atau error server, mengulang error server dua kali (jeda 1 dan 2 detik), dan membedakan error: offline, timeout, sibuk, server, bukan resi, tidak terbaca, belum dikonfigurasi. Semua error punya jalan ke input manual.
- Hasil scan ditinjau dan bisa dikoreksi di layar review sebelum disimpan sebagai uang keluar. Foto resi bisa disimpan lokal dan ditautkan ke transaksi (bisa dimatikan di Settings).
- Fitur inti jalan tanpa internet; layar scan menampilkan status offline dan kembali sendiri saat koneksi pulih.
- Onboarding: splash, 3 slide intro, nama, dompet pertama, layar selesai.
- UI berbahasa Inggris, format Rupiah, light dan dark mode (ikut sistem atau dipilih di Settings).
- API key Gemini dibaca dari `local.properties` (tidak di-commit) dan dikirim lewat header, bukan URL.
- Unit test (JUnit, coroutines-test, MockWebServer) untuk klien dan parser respons Gemini, draf hasil scan, agregasi, pengelompokan per hari, format uang, rentang waktu, dan ukuran gambar.
- Desain dibuat dulu di Figma sebelum coding.

## Konten Indonesia

**Judul:** Sakuin: Pencatat Keuangan Offline dengan Scan Resi AI

**Kategori:** Mobile App / AI

**Ringkasan:** Aplikasi Android untuk mencatat uang masuk, keluar, dan transfer di beberapa dompet sekaligus: cash, kartu, dan e-wallet. Semua data tersimpan di HP dan fitur inti jalan tanpa internet. Foto resi bisa dibaca Gemini untuk mengisi nominal, toko, dan item secara otomatis. Dibangun dengan Kotlin dan Jetpack Compose.

**Isi detail**

- **Judul:** Latar belakang
  **Paragraf:** Mahasiswa dan pekerja muda biasanya memegang uang di beberapa tempat: dompet tunai, kartu debit, dan beberapa e-wallet, dengan banyak transaksi kecil setiap hari. Sakuin dibuat agar semua itu tercatat di satu tempat dengan cepat, tanpa akun dan tanpa harus online. Ini proyek individu: saya merancang UI di Figma lalu membangun aplikasinya sendiri.

- **Judul:** Fitur utama
  **Paragraf:** Pengguna mengelola dompet Cash, Card, dan E-wallet. Saldo setiap dompet dihitung dari transaksinya dan tampil di Home sebagai tumpukan kartu yang bisa di-swipe. Uang keluar dicatat dengan kategori dan nama kebutuhan, dengan saran nama dari riwayat, dan transfer antardompet mendukung biaya admin. Riwayat transaksi dikelompokkan per hari dan bisa dicari serta difilter. Halaman Stats menampilkan pemasukan dan pengeluaran enam bulan terakhir serta pengeluaran per kategori, per minggu, bulan, atau tahun.
  **Paragraf:** Foto resi dari kamera atau galeri dibaca Gemini lewat skema JSON, dengan prompt yang disesuaikan untuk resi Indonesia. Total, toko, tanggal, kategori, dan daftar item ditinjau di layar review dan bisa dikoreksi sebelum disimpan. Kalau offline atau foto tidak terbaca, aplikasi menampilkan pesan yang jelas dan tetap menyediakan input manual. Seluruh tampilan tersedia dalam light dan dark mode.
  **Gambar:** semua layar utama, light mode
  **Gambar:** semua layar utama, dark mode

- **Judul:** Arsitektur
  **Paragraf:** Aplikasi native Kotlin dengan Jetpack Compose, Material 3, dan Navigation Compose, mengikuti pola ViewModel dan repository. Data disimpan lokal dengan Room dan SQLite, nominal disimpan sebagai bilangan bulat rupiah, dan pengaturan disimpan di DataStore. Kamera memakai CameraX, dan permintaan ke Gemini memakai OkHttp dan Kotlinx Serialization, dengan model cadangan dan percobaan ulang saat server error. Logika parsing hasil scan dan perhitungan statistik diuji dengan unit test.

## English content

**Title:** Sakuin: An Offline Money Tracker with AI Receipt Scanning

**Category:** Mobile App / AI

**Summary:** An Android app for recording money in, money out and transfers across several wallets at once: cash, cards and e-wallets. All data stays on the phone, and the core features work without internet. Gemini can read a receipt photo to fill in the amount, store and items automatically. Built with Kotlin and Jetpack Compose.

**Detail blocks**

- **Heading:** Background
  **Paragraph:** Students and young workers usually keep money in several places: a cash wallet, a debit card and a few e-wallets, with many small transactions every day. Sakuin puts all of it in one place and makes recording quick, with no account and no need to be online. It is a solo project: I designed the UI in Figma and then built the app myself.

- **Heading:** Key features
  **Paragraph:** Users manage Cash, Card and E-wallet wallets. Each balance is calculated from its transactions and shown on Home as a swipeable card stack. Expenses are recorded with a category and a name, with name suggestions from history, and transfers between wallets support an admin fee. Transaction history is grouped by day and can be searched and filtered. The Stats page shows income and expenses over the last six months and spending per category, by week, month or year.
  **Paragraph:** Gemini reads a receipt photo from the camera or gallery through a JSON schema, with a prompt tuned for Indonesian receipts. The total, store, date, category and line items are shown on a review screen where they can be corrected before saving. When the phone is offline or the photo cannot be read, the app shows a clear message and still offers manual entry. Every screen comes in light and dark mode.
  **Image:** all main screens, light mode
  **Image:** all main screens, dark mode

- **Heading:** Architecture
  **Paragraph:** A native Kotlin app built with Jetpack Compose, Material 3 and Navigation Compose, following a ViewModel and repository pattern. Data is stored locally with Room and SQLite, amounts are stored as whole rupiah integers, and settings live in DataStore. The camera uses CameraX, and Gemini requests use OkHttp and Kotlinx Serialization, with a fallback model and retries on server errors. Scan parsing and statistics logic are covered by unit tests.

## Stack Sakuin

- **Stack proyek (centang):** Kotlin, Android Studio, Figma, Git, Claude Code (opsional; centang kalau Anda memang ingin menampilkannya)
- **Teknologi lainnya:** Jetpack Compose, Material 3, Navigation Compose, Room, SQLite, DataStore, CameraX, OkHttp, Kotlinx Serialization, Google Gemini API, JUnit

## Foto sampul dan gambar detail

- Sampul: 3 frame cover di page "Portfolio" di Figma, diekspor sebagai PNG 1920×1080. Taruh cover hero di urutan pertama.
- Isi detail punya 3 judul: Latar belakang, Fitur utama, Arsitektur. Dua `+ Gambar` (page semua layar light, lalu dark) diletakkan di blok Fitur utama, setelah kedua paragrafnya. Urutan blok di form: Judul, Paragraf, Paragraf, Gambar, Gambar.

## Jangan diklaim

- Jumlah pengguna, unduhan, rating, atau rilis di Play Store. Repo tidak menunjukkan aplikasi sudah dirilis.
- Akurasi scan dalam angka, atau target dari PRD seperti "mencatat ≤ 10 detik" dan "cold start < 2 detik". Itu target, belum pernah diukur.
- Login, sinkronisasi cloud, atau backup. Data hanya ada di HP dan hilang kalau aplikasi di-uninstall.
- Scan resi berjalan offline. Scan butuh internet dan API key Gemini saat build; tanpa key, scan menampilkan pesan "belum dikonfigurasi".
- API key aman di dalam aplikasi. Key tidak di-commit, tetapi tetap tertanam di APK lewat BuildConfig.
- Multi mata uang. Baris Currency di Settings hanya menampilkan IDR.
- Nama pengguna bisa diubah di Settings. Nama hanya diisi saat onboarding.
- Budget, pengingat, export CSV, widget, transaksi berulang. Semua di luar cakupan v1.
- Logo final. Logo yang dipakai masih sementara.
- CI atau instrumented test yang rutin dijalankan. Yang ada adalah unit test lokal.

## Perlu Anda lengkapi

- Mata kuliah (dan semester, kalau mau). Setelah ada, tambahkan ke blok Latar belakang di ID dan EN.
- Link repo, kalau mau ditampilkan.
