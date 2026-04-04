# MoneyNote - So thu chi

Ung dung Android quan ly thu chi offline (SQLite), UI Material dark mode.

## Chuc nang
- Man hinh Nhap vao voi 2 tab: Chi tieu / Thu nhap
- Date picker, ghi chu, so tien, danh muc dang grid
- Luu giao dich offline vao SQLite (`transactions`)
- Man hinh Lich thang:
  - Tong thu (xanh) va tong chi (do) theo tung ngay
  - Bam vao ngay de xem danh sach giao dich
  - Tong ket thang: Thu nhap, Chi tieu, Tong con lai
- Sua / xoa giao dich bang nhan giu item trong danh sach ngay

## Cau truc chinh
- `MainActivity`
- Fragments:
  - `EntryHostFragment`
  - `EntryFormFragment` (dung chung cho Thu/Chi)
  - `CalendarFragment`
  - `ReportFragment`
- RecyclerView Adapters:
  - `CategoryAdapter`
  - `DayCellAdapter`
  - `TransactionAdapter`
- Database: `MoneyNoteDatabase` (SQLiteOpenHelper)

## Build APK (AndroidIDE tren dien thoai)
```bash
cd /storage/emulated/0/Documents/MoneyNote
./gradlew clean
./gradlew assembleDebug
```
APK debug:
`/storage/emulated/0/Documents/MoneyNote/app/build/outputs/apk/debug/app-debug.apk`

## Build APK (Android Studio)
1. Open project `MoneyNote`
2. Sync Gradle
3. Build > Build APK(s)
