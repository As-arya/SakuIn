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
- Vico (`compose-m3`) untuk bar chart; donut chart custom dengan `Canvas`
- OkHttp + kotlinx-serialization-json untuk REST Gemini (`generateContent` dengan `inline_data` image + `responseSchema` JSON)
- Coil (thumbnail resi), `ActivityResultContracts.TakePicture` + `PickVisualMedia` (tanpa CameraX untuk v1)
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
- `SakuinApplication` + `AppContainer`; Manifest: `android:name`, izin `INTERNET`, `ACCESS_NETWORK_STATE`, `FileProvider` untuk foto kamera.
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

### Fase 4 — Dompet & Transfer
- Layar daftar dompet (total + per jenis), form tambah/edit, detail dompet (riwayat), arsip.
- Layar Transfer (dari/ke dompet, validasi dompet berbeda, nominal > 0).
- Onboarding (layar 00a–00g di Figma): splash → 3 slide intro (`HorizontalPager` + indikator titik, Skip) → nama (simpan ke DataStore) → dompet pertama (tile jenis, nama, saldo awal) → "You're all set" → Home. Start destination ditentukan dari flag `onboardingDone`.
- Ilustrasi onboarding diekspor dari Figma sebagai SVG lalu dikonversi ke VectorDrawable (`res/drawable/ill_*.xml`).

### Fase 5 — Uang Keluar, Uang Masuk, Daftar Transaksi
- Satu `AddEditTransactionScreen` dengan tab Keluar/Masuk (dan mode edit), keypad/`AmountInput` berformat Rupiah, bottom sheet kategori/sumber, pemilih dompet, date-time picker M3, autocomplete nama kebutuhan.
- Daftar transaksi: grouping tanggal, filter & pencarian, swipe/hapus + undo.
- Pengaturan: kelola kategori & sumber.

### Fase 6 — Beranda & Chart
- Home: sapaan "Hello, {name}", `BalanceStack` (tumpukan kartu Total → Cash → Card → E-wallet; swipe horizontal memutar kartu dengan animasi, dibuat dengan `Modifier.pointerInput` + `animateFloatAsState`/`Animatable`), ringkasan bulan ini (Income/Expense), quick action Expense/Income/Transfer/Scan, transaksi terakhir.
- Statistik: Vico bar chart masuk vs keluar 6 bulan, donut kategori (Canvas), filter periode & dompet, legend + persentase.

### Fase 7 — Scan Resi (Gemini)
- `NetworkMonitor` (ConnectivityManager callback → `StateFlow<Boolean>`).
- Ambil/pilih foto → `ImageCompressor` (maks sisi 1600px, JPEG 80) → `GeminiReceiptClient.scan()` dengan prompt Bahasa Indonesia + daftar kategori yang ada + `responseSchema` → parse `ReceiptDto` → map ke kategori (fallback "Others").
- Navigasi ke form Uang Keluar terisi; simpan salinan foto di `filesDir/receipts`.
- State UI: idle / offline / loading / success / error. Model default `gemini-2.5-flash` (cek model terbaru yang tersedia saat implementasi).
- Catatan keamanan: key di BuildConfig cukup untuk proyek kuliah; untuk produksi pindah ke Firebase AI Logic + App Check.

### Fase 8 — Polish & QA
- Dark mode, empty states, aksesibilitas (contentDescription, ukuran sentuh 48dp), string resource Inggris, ikon launcher Sakuin (adaptive icon dari komponen `Logo/App Icon`: background biru `#1F5BDD`, foreground dompet), splash screen API dengan logo.
- Bandingkan layar aplikasi dengan screenshot Figma.

### Verifikasi
- `./gradlew assembleDebug` sukses; `./gradlew testDebugUnitTest` (saldo, agregasi, parser JSON resi, ViewModel dengan Turbine).
- `./gradlew connectedDebugAndroidTest` di emulator (Room DAO in-memory, Compose UI test alur tambah pengeluaran) — jika emulator/perangkat tersedia.
- Uji manual di emulator: buat 2 dompet → transfer → catat keluar/masuk → cek saldo & total → chart berubah → aktifkan mode pesawat dan ulangi pencatatan (harus jalan; tombol scan nonaktif) → online, scan foto resi contoh → hasil terisi & bisa dikoreksi.
- Cek `git status`/`git log` di akhir: tidak ada push; `local.properties` (API key) tidak ter-track.

### File kritis
`settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/Sakuin/Project/MainActivity.kt`, `app/src/main/res/values{,-night}/themes.xml`, `app/src/main/res/values/strings.xml`, serta paket baru di `app/src/main/java/Sakuin/Project/{data,domain,ui,util}`.
