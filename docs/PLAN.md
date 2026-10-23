# Sakuin — Rencana Implementasi

Lihat [PRD.md](PRD.md) untuk kebutuhan produk. **Aturan: tidak ada `git push` ke GitHub.**


### Arsitektur
Single module `:app`, MVVM + Unidirectional Data Flow:
`ui (Compose screens + ViewModel + UiState)` → `domain (use case ringan, model)` → `data (Room DAO, Repository, Gemini client, DataStore)`.
DI manual lewat `AppContainer` di `SakuinApplication` (menghindari kompleksitas Hilt dengan AGP 9). Coroutines + Flow.

Struktur paket (di bawah `Sakuin.Project`, package dipertahankan agar tidak merusak `applicationId`):
```
data/local (SakuinDatabase, dao/, entity/, Converters, seed/DefaultCategories)
data/repository (WalletRepository, TransactionRepository, CategoryRepository)
data/remote (GeminiReceiptClient, ReceiptDto)
data/prefs (SettingsDataStore: nama pengguna, onboardingDone, preferensi)
domain/model, domain/usecase (GetDashboardSummary, ScanReceipt, ...)
ui/theme (Color, Type, Shape, Theme — dari token Figma)
ui/components (AmountText, AmountInput, CategoryChip, WalletCard, TransactionRow, EmptyState, ...)
ui/navigation (SakuinNavHost, routes type-safe)
ui/screens/{home, transactions, addtransaction, transfer, wallets, stats, scan, settings, onboarding}
util (RupiahFormatter, DateUtils, NetworkMonitor, ImageCompressor)
```

### Library (versi final diverifikasi saat implementasi terhadap AGP 9.2.1)
- Compose BOM + Material 3, `activity-compose`, `lifecycle-viewmodel-compose`/`runtime-compose`
- Plugin `org.jetbrains.kotlin.plugin.compose` (versi = versi Kotlin bawaan AGP) & `kotlin.plugin.serialization`
- `navigation-compose` (type-safe routes) · Room (`room-runtime`, `room-ktx`, `room-compiler` via KSP) · DataStore Preferences
- Chart: bar chart & donut custom dengan Compose (`Box`/`Canvas`), tanpa Vico (lihat Fase 6)
- OkHttp + kotlinx-serialization-json untuk REST Gemini (`generateContent` dengan `inline_data` image + `responseSchema` JSON)
- CameraX 1.6 (`camera-compose` `CameraXViewfinder` + `ImageCapture`) untuk kamera di dalam app, `PickVisualMedia` untuk galeri; thumbnail resi tanpa Coil (lihat Fase 7)
- Test: JUnit, kotlinx-coroutines-test, Turbine, Room in-memory (androidTest), Compose UI test

### Fase 0 — Dokumen
- Tulis `docs/PRD.md` (isi Bagian A) dan `docs/PLAN.md` (isi Bagian B) di repo.

### Fase 1 — Desain Figma (file baru) ✅
File: https://www.figma.com/design/vd10brqPrNMDhH3wFNTKyM — halaman Cover, Design System, Components, Screens (16 layar + 2 dark, dengan prototype), Flow.
**Revisi v2 (sudah diterapkan):** palet solid (primary biru `#3366DD`, netral abu dingin, warna dompet biru/merah/kuning/hijau), komponen `BalanceStack` (4 varian, swipe via `ON_DRAG` → `CHANGE_TO`), bottom nav tanpa label, semua teks UI berbahasa Inggris. Rincian di bawah ini adalah rencana awal; layar final mengikuti file Figma.
Wajib load skill `/figma-use` sebelum `use_figma`, dan `/figma-generate-design` untuk layar. Buat file via `create_new_file`.
- **Page "Design System"**: variables warna (primary hijau-teal "saku", income hijau, expense merah, transfer biru, neutral, surface light/dark), tipografi (mis. Plus Jakarta Sans / Inter), spacing 4/8, radius, elevasi; komponen: Button, FAB, TextField nominal, Chip kategori, Wallet card (3 varian jenis), Transaction row (3 tipe), Bottom nav, Top bar, Segmented control, Empty state, Dialog.
- **Page "Screens"** (frame 360×800, light; dark untuk Home & Stats): Onboarding, Beranda/Dashboard, Daftar Transaksi (+filter sheet), Tambah Pengeluaran, Tambah Pemasukan, Transfer, Pilih kategori (bottom sheet), Dompet (list + detail + form), Statistik, Scan Resi (pilih sumber → loading → review hasil → error/offline), Pengaturan & Kelola Kategori.
- **Page "Flow"**: prototype link alur utama (tambah pengeluaran, scan resi, transfer).
- Ambil `get_screenshot` tiap layar untuk review user sebelum lanjut coding. Token Figma (`get_variable_defs`) menjadi sumber `ui/theme`.

### Fase 2 — Fondasi proyek ✅
**Status:** `assembleDebug` + `testDebugUnitTest` lulus, app jalan di emulator (Home placeholder, 4 tab, tombol + ke Add Transaction, light & dark).
Versi terpasang (dicek lewat Context7): KGP/Compose compiler/serialization `2.2.10` (bawaan AGP 9.2), Compose BOM `2026.09.00`, Navigation `2.10.0`, Lifecycle `2.11.0`, Activity Compose `1.13.0`, kotlinx-serialization-json `1.9.0`. `compileSdk` dinaikkan ke **37** karena library di atas mewajibkannya (`targetSdk` tetap 36). Build butuh JDK 17+ — pakai JBR Android Studio (`JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`) bila menjalankan `gradlew` dari terminal.
Belum dikerjakan di fase ini: KSP + Room (dipindah ke Fase 3 — Room terbaru adalah Room 3 `androidx.room3`, versi KSP-nya dipilih saat itu) dan file font Plus Jakarta Sans (sementara pakai sans-serif sistem).
- `settings.gradle.kts`: `rootProject.name = "Sakuin"`.
- `gradle/libs.versions.toml` + `app/build.gradle.kts`: tambah Compose, KSP, Room, Navigation, serialization, dll; `buildFeatures { compose = true; buildConfig = true }`; `GEMINI_API_KEY` dari `local.properties` → `BuildConfig`.
- Ganti `MainActivity` jadi `ComponentActivity` + `setContent { SakuinTheme { SakuinNavHost() } }`; hapus `res/layout/activity_main.xml`; rename tema `Theme.MyApplication` → `Theme.Sakuin` (`res/values/themes.xml`, `values-night`, Manifest); `app_name` = "Sakuin".
- `SakuinApplication` + `AppContainer`; Manifest: `android:name`, izin `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` (diminta saat scan dibuka).
- Theme Compose dari token Figma; scaffold bottom nav ikon-saja (Home, Transactions, [+], Stats, Wallets).
- String resource berbahasa Inggris (`res/values/strings.xml`).

### Fase 3 — Data layer ✅
**Status:** selesai; 9 unit test JVM + 10 instrumented test (emulator) lulus.
- Room 3 `3.0.2` + KSP `2.3.4` + `sqlite-bundled` `2.7.0` (driver `BundledSQLiteDriver`), schema diekspor ke `app/schemas`. DataStore Preferences `1.2.1`, core library desugaring (`java.time` di minSdk 24).
- `data/local`: entity, DAO, `SakuinDatabase`, `DefaultCategories` (nama Inggris). Enum tidak butuh converter (Room menyimpannya sebagai teks).
- Seed kategori lewat `CategoryRepository.ensureDefaults()` yang dipanggil `AppContainer` saat start (idempoten), bukan `RoomDatabase.Callback`.
- `data/repository`: `WalletRepository` (saldo + `BalanceSummary` untuk tumpukan kartu), `TransactionRepository` (add expense/income/transfer dengan validasi, edit, hapus + undo, daftar & filter, total, per kategori, per bulan, saran nama), `CategoryRepository`; `data/prefs/SettingsRepository` (nama, onboardingDone).
- `util/TimeRange`: rentang minggu/bulan/tahun berbasis zona waktu.
- Layer `domain/` belum dibuat: ViewModel memakai repository langsung, ditambah bila logika bisnis bertambah.
- Test: `app/src/test` (TimeRange, bucketByMonth, BalanceSummary) dan `app/src/androidTest/.../DataLayerTest.kt` (saldo + transfer, arsip, total & kategori, bulanan, filter/pencarian, saran nama, hapus/undo, validasi). Jalankan: `gradlew testDebugUnitTest connectedDebugAndroidTest`.

### Fase 4 — Dompet & Transfer ✅
**Status:** selesai dan dicoba di emulator dari data kosong: onboarding → tambah dompet → detail → transfer (saldo & riwayat ter-update), light & dark.
- Splash: `core-splashscreen` 1.0.1 + `Theme.Sakuin.Starting`, ditahan sampai status onboarding terbaca (`MainViewModel`).
- Onboarding 00b–00g, Wallets (total + bar komposisi), Add/Edit Wallet (warna mengikuti jenis, tombol Archive), Wallet detail (kartu saldo, tombol Transfer, riwayat), Add Transaction dengan tab Expense/Income/Transfer — **baru tab Transfer yang berfungsi** (sheet pilih dompet, tukar dompet, tanggal+jam, catatan, peringatan saldo minus).
- Komponen bersama di `ui/components`: Button, TextField/PickerField, Chip, SegmentedControl, TopBar, AmountInput (pemisah ribuan), WalletCard/WalletIconBox, WalletPickerSheet, DateTimeField, TransactionRow, PageDots.
- Ikon, logo, dan ilustrasi dibuat dari sumber SVG Figma dengan `node tools/svg-to-vector-drawable.js app/src/main/res/drawable`.
- Setelah transfer tersimpan, app kembali ke layar asal (bukan selalu ke tab Wallets seperti di prototype Figma).
- Layar daftar dompet (total + per jenis), form tambah/edit, detail dompet (riwayat), arsip.
- Layar Transfer (dari/ke dompet, validasi dompet berbeda, nominal > 0).
- Onboarding (layar 00a–00g di Figma): splash → 3 slide intro (`HorizontalPager` + indikator titik, Skip) → nama (simpan ke DataStore) → dompet pertama (tile jenis, nama, saldo awal) → "You're all set" → Home. Start destination ditentukan dari flag `onboardingDone`.
- Ilustrasi onboarding diekspor dari Figma sebagai SVG lalu dikonversi ke VectorDrawable (`res/drawable/ill_*.xml`).

### Fase 5 — Uang Keluar, Uang Masuk, Daftar Transaksi ✅
**Status:** selesai; 13 unit test JVM + 10 instrumented test lulus, dicoba di emulator (tambah expense/income, saran nama, cari, filter, swipe-hapus + Undo, edit, kelola kategori).
- `AddTransactionScreen` + `AddTransactionViewModel`: tab Expense/Income/Transfer berbagi jumlah, tanggal, catatan. Expense: kategori (chip), nama kebutuhan + saran dari riwayat (debounce 200 ms), "Pay from" (sheet), peringatan saldo minus. Income: sumber (chip), dompet tujuan (chip). Default kategori/dompet pertama sudah terpilih.
- Mode edit lewat `AddTransactionRoute(transactionId)`: tab terkunci sesuai tipe, tombol hapus dengan dialog konfirmasi. Dibuka dari daftar Transactions dan riwayat dompet.
- `TransactionsScreen`: per bulan (bulan depan dikunci), chip tipe, pencarian (debounce 250 ms), sheet filter dompet/kategori, grup per hari dengan net harian, swipe kiri untuk hapus + snackbar Undo (10 detik).
- `SettingsScreen` (Data: Categories, Wallets) dan `CategoriesScreen` (tab Expense/Income, tambah/edit nama + ikon, arsip). Opsi Appearance & Receipt scan ditambahkan bersama fiturnya.
- Catatan teknis: key baris daftar memakai `id-updatedAt` dan `restoreTransaction` menyegarkan `updatedAt`, karena state swipe disimpan per key — tanpa itu baris yang di-Undo kembali dalam posisi "terhapus".
- Satu `AddEditTransactionScreen` dengan tab Keluar/Masuk (dan mode edit), keypad/`AmountInput` berformat Rupiah, bottom sheet kategori/sumber, pemilih dompet, date-time picker M3, autocomplete nama kebutuhan.
- Daftar transaksi: grouping tanggal, filter & pencarian, swipe/hapus + undo.
- Pengaturan: kelola kategori & sumber.

### Fase 6 — Beranda & Chart ✅
**Status:** selesai; 14 unit test JVM lulus (termasuk `formatRupiahCompact`), dicoba di emulator light & dark.
- `BalanceStack` (`ui/components`): 4 kartu dengan bentuk gelombang dari Figma (`GenericShape`, sudut tetap bulat di lebar layar berapa pun). Swipe ke kiri/kanan atau tap → kartu depan "terlempar" lalu pindah ke belakang; posisi naik/turun dianimasikan. Tombol mata menyembunyikan saldo (Home: kartu + ringkasan). TalkBack: kartu belakang diabaikan, ada aksi "Show next balance".
- Home: sapaan, stack, Income/Expense bulan ini, quick action Expense/Income/Transfer/Scan, 5 transaksi terakhir + "See all".
- Stats: filter dompet (dropdown), periode Week/Month/Year dengan panah sebelum/sesudah (periode depan dikunci), ringkasan Income/Expense/Net format ringkas ("Rp 1,87jt"), bar chart 6 bulan sampai bulan terpilih (bulan aktif tebal, lainnya transparan), donut pengeluaran per kategori (maks. 5 irisan + "Others") dengan legend persentase.
- **Perubahan dari rencana:** chart dibuat dengan Compose biasa (`Box` untuk batang, `Canvas.drawArc` untuk donut) tanpa Vico — lebih sedikit dependensi dan sama persis dengan desain Figma.
- Home: sapaan "Hello, {name}", `BalanceStack` (tumpukan kartu Total → Cash → Card → E-wallet; swipe horizontal memutar kartu dengan animasi, dibuat dengan `Modifier.pointerInput` + `animateFloatAsState`/`Animatable`), ringkasan bulan ini (Income/Expense), quick action Expense/Income/Transfer/Scan, transaksi terakhir.
- Statistik: bar chart masuk vs keluar 6 bulan, donut kategori (Canvas), filter periode & dompet, legend + persentase.

### Fase 7 — Scan Resi (Gemini) ✅
**Status:** selesai; 36 unit test JVM lulus (22 baru: pemetaan hasil scan, parsing respons/error Gemini, request body, client HTTP dengan MockWebServer, ukuran gambar). Dicoba di emulator light & dark: galeri dan kamera → kompres → reading → review → simpan, offline, error, foto resi di form edit. Setelah key diisi (awal Fase 8), scan asli dengan Gemini berhasil dalam ±7–10 detik: nama toko, total (bukan uang tunai), tanggal & jam, item, dan kategori terbaca benar.
- Model default `gemini-3.6-flash` (GA), cadangan `gemini-3.8-flash`, lewat REST `generateContent`: foto sebagai `inlineData`, `responseSchema` JSON dengan `enum` nama kategori aktif, `thinkingLevel: LOW` agar cepat. Model berikutnya dipakai bila model sudah dipensiunkan (404) atau tetap error 5xx setelah retry. Catatan Okt 2026: `gemini-3.8-flash` (dan `gemini-flash-latest`) tercantum untuk key proyek tetapi request-nya menggantung, sedangkan 3.6 menjawab dalam beberapa detik; `gemini-2.5-flash` sudah 404. Key dikirim di header `x-goog-api-key`, tidak pernah di URL; build menerima key di `local.properties` dengan atau tanpa tanda kutip. OkHttp; timeout total 60 detik; error 5xx dicoba ulang 2× (1 dtk, 2 dtk), 429 tidak.
- Prompt meminta nama toko & item dalam title case dan harga per baris apa adanya (tidak dikali jumlah) — dua hal ini sempat salah pada uji pertama dengan resi contoh.
- `data/remote`: `GeminiReceiptClient`, `ReceiptDto` → `ReceiptDraft` (`toDraft`: kategori dicocokkan per nama, fallback "Others"; total kosong → jumlah item; tanggal di masa depan → sekarang; tanggal hari ini tanpa jam → sekarang). `ScanError` membedakan NotConfigured, Image, Network, Timeout, Busy, Server, NotReceipt, Unreadable — masing-masing punya pesan sendiri.
- `data/local/ReceiptStore`: foto dari kamera di dalam app (CameraX, ke `cache/camera/capture.jpg`) atau galeri (`PickVisualMedia`) diputar sesuai EXIF, diperkecil ke sisi panjang maks. 1600 px, JPEG 80 di `cache/scan`; saat disimpan disalin ke `files/receipts/` dan path relatif disimpan di `receiptImagePath`. Saat app start, salinan scan lama dan foto yang tidak lagi dipakai transaksi (mis. sudah dihapus) dibersihkan.
- `util/NetworkMonitor`: callback default network → `StateFlow<Boolean>`; layar scan langsung pindah ke/dari layar offline.
- `ui/screens/scan`: Capture (gelap seperti kamera, ikon status bar putih), Reading (garis scan beranimasi, Cancel), Review (thumbnail → foto layar penuh, ubah tanggal, Total bisa diedit, daftar item, chip kategori dengan tanda AI, nama item, Pay from), Problem (offline/error: Add Manually + Try Again). Back dari Reading/Review/Error kembali ke Capture.
- Pintu masuk: quick action Home dan ikon scan di app bar Add Transaction (tab Expense). Dari form: "Add Manually" kembali ke form yang sama, Save menutup scan dan form.
- Form edit expense menampilkan baris "Receipt photo" bila transaksinya dari scan. Nama toko disimpan sebagai catatan bila berbeda dari nama item.
- **Perubahan dari rencana:** hasil scan ditinjau di layar Review sendiri (sesuai Figma), bukan langsung di form Uang Keluar; offline ditampilkan sebagai layar di alur scan (bukan tombol nonaktif) agar ada pilihan Add Manually; thumbnail tanpa Coil (decode `BitmapFactory` dengan subsampling); prompt dalam bahasa Inggris dengan kata kunci resi Indonesia.
- `NetworkMonitor` (ConnectivityManager callback → `StateFlow<Boolean>`).
- Ambil/pilih foto → `ImageCompressor` (maks sisi 1600px, JPEG 80) → `GeminiReceiptClient.scan()` dengan prompt Bahasa Indonesia + daftar kategori yang ada + `responseSchema` → parse `ReceiptDto` → map ke kategori (fallback "Others").
- Navigasi ke form Uang Keluar terisi; simpan salinan foto di `filesDir/receipts`.
- State UI: idle / offline / loading / success / error. Model default `gemini-2.5-flash` (cek model terbaru yang tersedia saat implementasi).
- Catatan keamanan: key di BuildConfig cukup untuk proyek kuliah; untuk produksi pindah ke Firebase AI Logic + App Check.

### Fase 8 — Polish & QA ✅
**Status:** selesai; 39 unit test JVM + 11 instrumented test lulus, lint 0 error (sisa 14 peringatan yang memang dibiarkan, lihat di bawah). Dicoba di emulator light & dark, ukuran huruf 100%, 130% dan 200%.
- **Ikon launcher:** adaptive icon dari logo Figma (background `#1F5BDD`, foreground art logo ±44dp di kanvas 108dp) + ikon monokrom untuk themed icons Android 13+ (gesper dompet dilubangi dengan `fillType="evenOdd"`), dibuat oleh `tools/svg-to-vector-drawable.js`. Ikon lama (API 24–25) berupa PNG mdpi–xxxhdpi, kotak & bulat, dirender dari SVG yang sama dengan Edge headless lalu diperkecil.
- **Settings (Figma 11) lengkap:** Appearance → Dark mode (switch; sebelum disentuh app mengikuti tema HP; Android 12+ juga memakai `UiModeManager.setApplicationNightMode` agar splash & window ikut), Currency IDR. Receipt scan → "AI receipt scan" (off menyembunyikan Scan di Home dan ikon scan di form) dan "Keep receipt photos" (off: expense hasil scan disimpan tanpa foto). Disimpan di DataStore (`ThemeMode`, dua boolean). Ikon status/navigation bar mengikuti tema app.
- **Empty states:** komponen `EmptyState` (ikon dalam kotak, judul, petunjuk, aksi opsional) untuk Home, Transactions (bulan kosong / "No matches" + tombol Clear filters), Wallets (+ Add wallet), riwayat dompet. Figma tidak punya frame empty state; gayanya mengikuti layar offline scan.
- **Aksesibilitas:** judul layar & seksi ditandai heading (`Modifier.asHeading()`) untuk navigasi TalkBack; semua tombol ikon punya label; panah di Stats berlabel "Previous/Next period". Teks nominal besar (kartu saldo, total dompet, kartu dompet, donut, ringkasan Home) memakai `autoSize` agar mengecil, bukan terpotong, pada saldo besar atau huruf besar; label quick action satu baris.
- **Lint & dependensi:** ExifInterface AndroidX `1.4.2`, plural untuk "Show all N items", `ic_logo` yang tidak terpakai dihapus. Dinaikkan (patch/minor): core-ktx 1.19.1, core-splashscreen 1.2.0, Navigation 2.10.2, Room 3.0.3, sqlite-bundled 2.7.1, DataStore 1.3.0, OkHttp/MockWebServer 5.5.1, androidx.test junit 1.3.0, espresso 3.7.0. **Sengaja dibiarkan:** plugin Kotlin 2.4.x (harus sama dengan Kotlin bawaan AGP 9.2 = 2.2.10), kotlinx-serialization 1.12 & coroutines-test 1.11 (butuh Kotlin lebih baru), AGP 9.4 (upgrade besar), `targetSdk` 37 (perlu uji perubahan perilaku), ukuran ilustrasi vector > 200dp (hanya digambar sekali per layar).
- **Figma diperbarui:** komponen baru `Switch` (On/Off) dan `EmptyState` (properti Title, Body, Icon, Show action + Button bersarang) di halaman Components; set `Launcher Icon` (Adaptive, Legacy square, Legacy round, Themed) + pratinjau mask & keyline di seksi Brand & Illustrations, dibuat dari SVG yang sama dengan app. Layar `11 · Settings` memakai instance Switch, baris 56dp, dan footer versi. Layar baru: `01c · Home — Empty`, `02b · Transactions — Empty month`, `02c · Transactions — No matches`, `07b · Wallets — Empty` (di bawah kolom layar aslinya).
- Belum: tema hanya bisa diubah Light/Dark lewat switch (kembali ke "ikuti sistem" butuh hapus data app); Figma belum punya frame Wallet detail, jadi empty state riwayat dompet hanya ada di app.
- Rencana awal: Dark mode, empty states, aksesibilitas (contentDescription, ukuran sentuh 48dp), string resource Inggris, ikon launcher Sakuin (adaptive icon dari komponen `Logo/App Icon`: background biru `#1F5BDD`, foreground dompet), splash screen API dengan logo.
- Bandingkan layar aplikasi dengan screenshot Figma.

### Verifikasi
- `./gradlew assembleDebug` sukses; `./gradlew testDebugUnitTest` (saldo, agregasi, parser JSON resi, ViewModel dengan Turbine).
- `./gradlew connectedDebugAndroidTest` di emulator (Room DAO in-memory, Compose UI test alur tambah pengeluaran) — jika emulator/perangkat tersedia.
- Uji manual di emulator: buat 2 dompet → transfer → catat keluar/masuk → cek saldo & total → chart berubah → aktifkan mode pesawat dan ulangi pencatatan (harus jalan; scan menampilkan layar offline) → online, scan foto resi contoh → hasil terisi & bisa dikoreksi.
- Cek `git status`/`git log` di akhir: tidak ada push; `local.properties` (API key) tidak ter-track.

### File kritis
`settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/Sakuin/Project/MainActivity.kt`, `app/src/main/res/values{,-night}/themes.xml`, `app/src/main/res/values/strings.xml`, serta paket baru di `app/src/main/java/Sakuin/Project/{data,domain,ui,util}`.
