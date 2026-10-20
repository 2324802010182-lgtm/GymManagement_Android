# Hướng dẫn phát triển theo nhóm

## Quy trình Git
- Nhánh: `main` (ổn định) ← `develop` ← `feature/<tên>` / `fix/<tên>`.
- Commit nhỏ, thông điệp tiếng Việt hoặc Anh nhất quán, ví dụ: `feat(owner): thêm sửa gói tập`.
- Pull Request cần ít nhất 1 người review; chạy `./gradlew testDebugUnitTest` và `./gradlew lint` trước khi mở PR.
- **Không commit**: `app/google-services.json`, khóa service account, `seed/.env`, mật khẩu (đã có trong `.gitignore`). Mỗi thành viên tự tải `google-services.json` từ Firebase Console (hoặc dùng chung một dự án Firebase *phát triển*, không phải dự án thật).

## Môi trường
- Android Studio mới nhất, JDK 17, Android SDK 36.
- Mỗi thành viên nên có **dự án Firebase riêng để phát triển** (hoặc dùng Emulator) để không làm bẩn dữ liệu của nhau.
- Khi sửa `firebase/firestore.rules` hoặc `firestore.indexes.json`: chạy `firebase/rules-tests`, cập nhật `FIRESTORE_SCHEMA.md` / `SECURITY_RULES.md`, và ghi rõ trong PR.

## Cấu trúc & quy ước mã
- `ui/*` chỉ lo hiển thị; logic trạng thái nằm trong `viewmodel/*`; truy cập Firebase chỉ trong `data/repository/*`.
- Repository trả kết quả bằng `Callback<T>` và lỗi chuẩn hóa qua `ErrorMapper` → `AppError` (NETWORK, PERMISSION, NOT_CONFIGURED, ...). Màn hình hiển thị bằng `Resource<T>` + `StateLayout` để luôn có đủ loading/empty/error.
- Thao tác ghi nhiều tài liệu: dùng `WriteBatch`, và **khóa/giá trị phải khớp rules** (xem các hàm `validXxx()` trong `firestore.rules`). Khi đổi lược đồ phải sửa đồng thời: model, repository, rules, seed, `FIRESTORE_SCHEMA.md`.
- Kết quả thao tác ghi đi qua `BaseViewModel.action` (`Event<Resource<String>>`); chỉ báo “thành công” sau khi Firebase xác nhận.
- Chuỗi giao diện đặt ở `res/values/strings.xml` (tiếng Việt); màu/kích thước dùng token trong `colors.xml`/`dimens.xml`; không hard-code màu.
- Không thêm dữ liệu giả hay “fallback” để che lỗi Firebase.

## Thêm một màn hình mới (ví dụ)
1. Tạo layout `fragment_xxx.xml` (dùng `StateLayout` nếu có dữ liệu bất đồng bộ).
2. Tạo `XxxViewModel` (kế thừa `BaseViewModel`), `XxxRepository` nếu cần (đăng ký trong `Repos`).
3. Tạo `XxxFragment`; màn hình chi tiết mở qua `HostActivity.open(...)` (thêm hằng số + `case` trong `HostActivity`).
4. Thêm chuỗi vào `strings.xml`; cập nhật `TESTING_CHECKLIST.md`.
5. Nếu có thao tác ghi mới: cập nhật rules + test.

## Kiểm tra nhanh khi không có Android SDK
`python3 tools/resgen.py app/src/main/res <thư mục đầu ra>` kiểm tra mọi tham chiếu `@type/name` trong XML tồn tại và sinh `R`/ViewBinding giả để `javac` bắt lỗi tham chiếu nội bộ. Đây chỉ là kiểm tra bổ trợ — **không thay thế** việc build bằng Gradle.

## Phát hành
- Tăng `versionCode/versionName` trong `app/build.gradle.kts`.
- Bật `isMinifyEnabled` cho release chỉ sau khi kiểm thử đầy đủ (đã có `proguard-rules.pro` cơ bản).
- Ký APK bằng keystore riêng, **không** đưa keystore vào repo.
