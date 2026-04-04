# MoneyNote

Ứng dụng Android quản lý thu chi cá nhân, lưu dữ liệu offline bằng SQLite.

## Tính năng chính
- Nhập giao dịch theo 2 nhóm: `Chi tiêu` và `Thu nhập`
- Quản lý ví tiền (tiền mặt, tài khoản, ví tự tạo)
- Xem giao dịch theo lịch tháng/ngày
- Sửa, xóa giao dịch trực tiếp từ tab Lịch
- Xem tổng quan thu/chi theo tuần và tháng
- Sao lưu/khôi phục dữ liệu bằng JSON
- Hỗ trợ ngôn ngữ: Tiếng Việt / English

## Công nghệ
- Kotlin
- Android ViewBinding
- SQLite (`SQLiteOpenHelper`)
- Material Components

## Cấu trúc chính
- `MainActivity`
- `EntryHostFragment`, `EntryFormFragment`
- `CalendarFragment`
- `ReportFragment`
- `WalletFragment`
- `SettingsFragment`
- `MoneyNoteDatabase`, `TransactionRepository`

## Build bằng AndroidIDE
```bash
cd /storage/emulated/0/Documents/MoneyNote
./gradlew assembleDebug
```

APK debug:
`/storage/emulated/0/Documents/MoneyNote/app/build/outputs/apk/debug/app-debug.apk`

Build release đã ký:
```bash
cd /storage/emulated/0/Documents/MoneyNote
./gradlew :app:assembleRelease
```

APK release:
`/storage/emulated/0/Documents/MoneyNote/app/build/outputs/apk/release/app-release.apk`

## Tác giả
- GitHub: [@TorryTran](https://github.com/TorryTran)
