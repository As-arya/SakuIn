# Sakuin — Product Requirements Document

Aplikasi pencatatan keuangan pribadi Android (Jetpack Compose), fully offline, dengan scan resi online via Gemini.


### A1. Ringkasan produk
Sakuin membantu mahasiswa/pekerja muda mencatat uang masuk & keluar di beberapa dompet (cash, kartu, e-wallet), melihat total saldo, dan memahami pola pengeluaran lewat chart. Semua data tersimpan lokal di HP; internet hanya dibutuhkan untuk scan resi.

### A2. Target pengguna
- Mahasiswa dengan uang jajan + kerja part-time, banyak transaksi kecil (makan, angkot, ojol).
- Pekerja muda dengan gaji bulanan, kartu debit, dan beberapa e-wallet.

### A3. Tujuan & metrik sukses
- Mencatat 1 transaksi ≤ 10 detik (≤ 4 tap + input nominal).
- 100% fitur inti berjalan tanpa internet (mode pesawat).
- Scan resi mengisi otomatis nominal dengan akurasi tinggi; user selalu bisa mengoreksi sebelum menyimpan.

### A4. Fitur & kebutuhan fungsional

**F1 — Dompet & Saldo (Pencatatan uang)**
- CRUD dompet: nama (mis. "BCA", "GoPay", "Dompet"), jenis `CASH | CARD | EWALLET`, saldo awal, warna/ikon.
- Saldo dompet = saldo awal + pemasukan − pengeluaran − transfer keluar + transfer masuk (dihitung, bukan disimpan).
- Total uang = jumlah semua saldo dompet aktif; ditampilkan juga per jenis (Cash / Card / E-wallet).
- Di Home, saldo tampil sebagai **tumpukan kartu** (urutan Total → Cash → Card → E-wallet). Swipe pada tumpukan memindahkan kartu depan ke belakang dan menampilkan kartu berikutnya; indikator titik menunjukkan posisi.
- Warna identitas jenis dompet (solid): Total biru, Cash merah, Card kuning, E-wallet hijau.
- Transfer antar dompet (mis. tarik tunai Card → Cash) dengan nominal, tanggal, catatan opsional. Transfer tidak dihitung sebagai pemasukan/pengeluaran di chart.
- Dompet bisa diarsipkan (tidak dihapus jika sudah punya transaksi).

**F2 — Uang Keluar**
- Field: jumlah (wajib, > 0), jenis kebutuhan / kategori (wajib; default: Food, Transport, Education, Shopping, Fun, Health, Bills, Others), nama kebutuhan (wajib; mis. "Nasi Goreng", "Bayar Kuliah", "Angkot"), deskripsi (opsional), dompet sumber (wajib), tanggal & jam (default sekarang).
- Saran nama kebutuhan dari riwayat (autocomplete per kategori).
- Boleh menyebabkan saldo minus, dengan peringatan.

**F3 — Uang Masuk**
- Field: jumlah (wajib), asal / sumber (wajib; default: Salary, Allowance, THR, Bonus, Gift, Others), dompet tujuan — jenisnya cash/card/e-wallet mengikuti dompet (wajib), deskripsi (opsional), tanggal.

**F4 — Fully offline**
- Semua data di Room (SQLite) lokal; tidak ada akun/login/sinkronisasi.
- Aplikasi dapat dibuka dan dipakai penuh dalam mode pesawat. Izin `INTERNET` hanya dipakai fitur scan.
- Kategori & sumber dapat ditambah/diubah user (Pengaturan).

**F5 — Scan foto resi (online)**
- Ambil foto (kamera) atau pilih dari galeri.
- Jika offline: tombol scan nonaktif + pesan "Butuh koneksi internet"; pencatatan manual tetap jalan.
- Foto dikompres lalu dikirim ke Gemini; hasil JSON: `total`, `merchant`, `date`, `category` (dipetakan ke kategori yang ada), `items[]`, `suggestedName`.
- Hasil membuka form Uang Keluar yang sudah terisi → user meninjau/mengoreksi → simpan. Foto resi disimpan lokal & tautkan ke transaksi (opsional dilihat lagi).
- Error handling: timeout, resi tidak terbaca, kuota habis → pesan jelas + opsi isi manual.

**F6 — Chart**
- Bar chart Pemasukan vs Pengeluaran per bulan (6 bulan terakhir).
- Donut chart pengeluaran per kategori untuk periode terpilih + daftar persentase.
- Filter periode: minggu / bulan / tahun, filter dompet.
- Ringkasan: total masuk, total keluar, selisih (net) periode.

**Pendukung**
- Daftar transaksi: dikelompokkan per tanggal, filter tipe/dompet/kategori/bulan, cari teks, edit & hapus (dengan konfirmasi + undo snackbar).
- Onboarding bergambar (ilustrasi vektor), muncul sekali saat aplikasi pertama dibuka:
  1. Splash dengan logo.
  2. Tiga slide intro (Track every rupiah · Works fully offline · Scan your receipts), bisa di-swipe atau dilewati dengan Skip.
  3. Data diri: nama pengguna (dipakai untuk sapaan "Hello, {name}"), langkah 1/2.
  4. Dompet pertama: pilih jenis (tile Cash/Card/E-wallet), nama dompet, saldo awal, langkah 2/2.
  5. Layar "You're all set" dengan ringkasan dompet, lalu masuk ke Home.
- Nama pengguna dan status "onboarding selesai" disimpan lokal (DataStore); nama bisa diubah di Settings.
- Logo sementara: `docs/assets/sakuin-logo-temp.webp` (versi vektornya ada di Figma, komponen `Logo/App Icon`).
- Format Rupiah (`Rp 25.000`), **seluruh UI berbahasa Inggris**, light & dark mode.

**Prinsip visual (revisi desain v2)**
- Warna solid saja, tanpa pastel. Hierarki warna: netral untuk ±90% UI; biru primary hanya untuk aksi utama, state terpilih, tab aktif, dan link; warna dompet hanya untuk identitas dompet; hijau untuk nominal pemasukan, merah untuk nominal pengeluaran (dengan tanda −); merah juga dipakai untuk error dan aksi hapus; transfer tetap netral.
- Teks seminimal mungkin. Sapaan satu baris "Hello, {name}" tanpa emoji.
- Bottom nav hanya ikon (tanpa label); tab aktif = ikon warna primary + titik indikator.
- Tombol scan hanya ada di quick action "Scan" di Home dan di app bar form Add Transaction.

### A5. Kebutuhan non-fungsional
- Android 7.0+ (minSdk 24), target SDK 36.
- Cold start < 2 detik; daftar transaksi tetap lancar di ribuan baris (Paging tidak wajib di awal, LazyColumn + query terbatas per bulan).
- Nominal disimpan sebagai `Long` rupiah (tanpa floating point).
- API key Gemini tidak di-commit (dibaca dari `local.properties`, sudah di-.gitignore).

### A6. Di luar cakupan (v1)
Login/cloud sync, multi mata uang, budget/target, pengingat, export CSV, widget, recurring transaction.

### A7. Model data (Room)
Diimplementasikan dengan Room 3 (`androidx.room3`) + `BundledSQLiteDriver`, enum disimpan sebagai teks.
- `WalletEntity(id, name, type: CASH|CARD|EWALLET, initialBalance: Long, isArchived, createdAt)` — warna dompet ditentukan oleh `type` (desain v2), jadi tidak ada kolom warna/ikon.
- `CategoryEntity(id, name, kind: EXPENSE|INCOME, iconKey, isDefault, isArchived, sortOrder)` — kategori pengeluaran dan "asal" pemasukan berbagi tabel, dibedakan `kind`.
- `TransactionEntity(id, type: EXPENSE|INCOME|TRANSFER, amount: Long, walletId, toWalletId?, categoryId?, title (nama kebutuhan, kosong untuk income/transfer), note?, occurredAt, receiptImagePath?, createdAt, updatedAt)` — FK ke wallet (RESTRICT) & kategori (SET NULL); index pada `walletId`, `toWalletId`, `categoryId`, `occurredAt`.
- Query di DAO: saldo per dompet (dihitung), daftar transaksi + filter/pencarian, total masuk/keluar per periode, total pengeluaran per kategori, autocomplete nama kebutuhan. Pengelompokan per bulan dilakukan di Kotlin (`bucketByMonth`) agar mengikuti zona waktu perangkat.
- Pengaturan kecil (nama pengguna, status onboarding) disimpan di Preferences DataStore.

