# Thay đổi trong bản Firebase-only

1. Bỏ SharedPreferences/demo accounts/dữ liệu mẫu khỏi `KhoDuLieu.java`.
2. Các phương thức đọc/ghi dữ liệu trả lỗi nếu Firebase chưa khởi tạo; không ghi dữ liệu giả vào thiết bị.
3. Khi thiếu Firebase config, app hiển thị màn hình hướng dẫn cấu hình thay vì dashboard demo.
4. Tài khoản chủ Gym đăng ký với vai trò `chu_gym_cho_duyet`; app đăng xuất và chờ phê duyệt trước khi cho tạo thương hiệu.
5. Tăng ràng buộc trong `firestore.rules`: khóa quyền quản trị tự cấp, kiểm tra Gym tồn tại khi hội viên đăng ký, khóa `gym_id`/chủ sở hữu ở thao tác cập nhật và phân quyền đọc/ghi theo Gym/UID.
6. `capNhat()` sử dụng Firestore `update()` để tránh vô tình tạo document mới khi ID không tồn tại.
7. Bổ sung `FIREBASE_SETUP_VI.md` mô tả tạo Firebase Project, bật Authentication, tạo Firestore, publish Rules và phê duyệt chủ Gym.

## Những gì chưa thể xác nhận từ môi trường đóng gói

- Chưa có `google-services.json` vì project Firebase chưa được tạo bởi người dùng.
- Chưa thể kiểm thử đăng nhập/đọc/ghi trên Firebase thật.
- Không thể chạy Gradle build trong môi trường đóng gói này vì máy không phân giải được `services.gradle.org`; cần chạy Sync/Build trên máy có kết nối mạng.
- Trước khi bảo vệ, cần kiểm thử Rules và đầy đủ các luồng nghiệp vụ trên Firebase Project thật.
