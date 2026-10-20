# GymManagement — Android Java + Firebase

Ứng dụng quản lý phòng Gym đa thương hiệu. Phiên bản này đã **tắt hoàn toàn chế độ demo và lưu dữ liệu cục bộ giả lập**: đăng nhập, đăng ký và mọi thao tác dữ liệu chỉ hoạt động sau khi cấu hình Firebase thật.

> `google-services.json` chưa được đóng gói vì file này phải được tạo từ Firebase Project thuộc tài khoản của nhóm. Khi chưa có file cấu hình, ứng dụng sẽ hiển thị màn hình hướng dẫn kết nối Firebase thay vì tạo tài khoản hoặc dữ liệu mẫu.

## 1. Chức năng trong project

| Mã | Chức năng | Các luồng chính |
|---|---|---|
| F01 | Tài khoản/hồ sơ | Đăng ký, đăng nhập, đặt lại mật khẩu qua email, chỉnh sửa hồ sơ, đăng xuất |
| F02 | Dashboard | Dashboard quản trị/hội viên đọc dữ liệu từ Firestore |
| F03 | Gói tập | Quản trị thêm/sửa/xóa/ẩn gói; hội viên gửi yêu cầu; quản trị duyệt/từ chối |
| F04 | Lịch tập | Quản trị tạo/xóa lịch; hội viên xem lịch và ghi nhận đã tập |
| F05 | Bài tập | Danh sách, tìm kiếm/xem hướng dẫn, quản trị thêm/sửa/xóa nội dung riêng Gym |
| F06 | Chỉ số cơ thể | Lưu cân nặng, chiều cao, tỷ lệ mỡ tùy chọn; tính BMI và xem lịch sử |
| F07 | Tiến độ | Ghi nhận buổi tập, tổng số buổi/thời lượng, lịch sử cân nặng |
| F08 | Thông báo | Lưu thông báo trong Firestore, xem theo Gym/người nhận, đánh dấu đã đọc |
| F09 | Quản trị Gym | Danh sách hội viên, khóa/mở khóa tài khoản, duyệt yêu cầu gói, điểm danh, cập nhật thương hiệu |

Phạm vi hiện tại dùng thông báo **bên trong ứng dụng**, không có thanh toán trực tuyến và không có push notification khi ứng dụng đóng.

## 2. Mở và chạy project

1. Giải nén ZIP vào đường dẫn đơn giản, ví dụ `D:\AndroidProjects\GymManagement_Android`.
2. Mở Android Studio → **Open** → chọn thư mục `GymManagement_Android` (thư mục chứa `settings.gradle.kts`).
3. Chọn JDK 17 và cài Android SDK Platform 36.
4. Thực hiện các bước trong file `FIREBASE_SETUP_VI.md` trước khi sử dụng chức năng đăng nhập/đăng ký.
5. Sau khi đặt `app/google-services.json`, chọn **File → Sync Project with Gradle Files**, rồi chạy cấu hình `app`.

## 3. Tích hợp Firebase

Project đã khai báo:

- Firebase Authentication (`firebase-auth`).
- Cloud Firestore (`firebase-firestore`).
- Firebase Android BoM.
- Google Services Gradle plugin, chỉ áp dụng khi file `app/google-services.json` tồn tại.

**Không cần thêm SDK Firebase lần nữa.** Tạo Firebase Project của nhóm, đăng ký Android app với đúng package `com.example.gymmanagement`, tải file JSON và đặt vào thư mục `app/`.

## 4. Quy trình tài khoản chủ Gym

Ứng dụng không cho phép người dùng tự cấp vai trò quản trị cho chính họ.

1. Người đăng ký chọn **Chủ phòng gym**. Firebase Authentication tạo tài khoản và Firestore tạo hồ sơ có `vai_tro = chu_gym_cho_duyet`.
2. Tài khoản sẽ đăng xuất và hiển thị thông báo chờ phê duyệt; tài khoản chờ duyệt không thể vào dashboard hoặc tạo Gym.
3. Người quản lý dự án kiểm tra tài khoản trong Firebase Console rồi cấp vai trò `quan_tri_gym` cho document `nguoi_dung/{UID}` theo phần “Phê duyệt chủ Gym đầu tiên” trong `FIREBASE_SETUP_VI.md`.
4. Chủ Gym đăng nhập lại, tạo thương hiệu; ứng dụng lưu document thương hiệu và liên kết `gym_id` vào hồ sơ.

Đây là quy trình phê duyệt thủ công phù hợp đồ án. Nếu triển khai công khai/thương mại, nên thay bằng backend tin cậy (ví dụ Cloud Functions/Admin SDK) và không để người dùng cuối tự thay đổi vai trò trong Firestore.

## 5. Quy tắc dữ liệu và bảo mật

- `uid`: ID tài khoản do Firebase Authentication cấp.
- `gym_id`: ID document của thương hiệu tại collection `phong_gym`.
- `nguoi_dung`: hồ sơ tài khoản/role/gym.
- `phong_gym`: danh mục thương hiệu để người đăng ký hội viên có thể chọn.
- `goi_tap`: gói tập theo Gym.
- `dang_ky_goi`: yêu cầu đăng ký và trạng thái duyệt.
- `lich_tap`, `bai_tap`, `diem_danh`: dữ liệu nghiệp vụ theo Gym.
- `chi_so_co_the`, `tien_do`: dữ liệu cá nhân theo UID và Gym.
- `thong_bao`: thông báo theo Gym, có thể gửi toàn Gym hoặc một hội viên.

File `firestore.rules` được chuẩn bị theo mô hình trên. Sau khi tạo Firestore, hãy sao chép nội dung file vào **Firestore Database → Rules → Publish**. Không dùng rules mở toàn bộ quyền đọc/ghi. Queries trong app đã lọc theo `gym_id`/`uid_nguoi_dung` ở các luồng nghiệp vụ; nếu tự thêm truy vấn mới, phải thiết kế truy vấn phù hợp với Security Rules vì Firestore Rules không tự lọc kết quả truy vấn.

## 6. Cấu trúc code

- `hoat_dong/MainActivity.java`: điều hướng và giao diện hiện tại.
- `du_lieu/KhoDuLieu.java`: lớp dùng chung cho Firebase Authentication và Cloud Firestore.
- `mo_hinh`, `giao_dien`, `bo_chuyen_doi`, `thong_bao`, `tien_ich`: các package có sẵn để tiếp tục tách model/giao diện/tiện ích khi nhóm phát triển song song.

Để nhóm 3 người làm việc ít xung đột, không nên để cả ba cùng sửa `MainActivity.java`. Hãy tách từng màn hình sang Activity/Fragment hoặc lớp giao diện riêng, B và C phụ trách phần nghiệp vụ của mình; lớp dữ liệu chung chỉ thay đổi khi cả nhóm thống nhất.

## 7. Kiểm thử tối thiểu trước khi nộp

- Đăng ký hội viên bằng email chưa tồn tại; xác minh hồ sơ xuất hiện tại `nguoi_dung/{UID}`.
- Kiểm tra đăng nhập và email đặt lại mật khẩu.
- Đăng ký chủ Gym: xác nhận vai trò ban đầu là `chu_gym_cho_duyet` và tài khoản không thể truy cập dashboard.
- Phê duyệt thủ công chủ Gym, đăng nhập lại, tạo thương hiệu và kiểm tra `gym_id` trong hồ sơ.
- Tạo ít nhất hai thương hiệu thử nghiệm; kiểm tra dữ liệu gói tập/lịch tập/thông báo không bị trộn giữa hai Gym.
- Thử truy cập trái quyền bằng tài khoản hội viên/quản trị khác; thao tác trái quyền phải bị Firestore từ chối.
- Kiểm tra tạo/sửa/xóa gói, gửi và duyệt yêu cầu đăng ký, lịch tập, bài tập, chỉ số, tiến độ, thông báo và điểm danh.

## 8. Giới hạn còn lại

- File `google-services.json` cần được tạo từ Firebase Project của nhóm; chưa thể nhúng trước khi nhóm tạo project.
- Chủ Gym được phê duyệt thủ công qua Firebase Console; chưa có màn hình super-admin/backend phê duyệt.
- Không tích hợp cổng thanh toán và thông báo đẩy FCM.
- Cần kiểm thử trên Firebase Project thật để xác nhận tất cả Security Rules/queries phù hợp trước khi bảo vệ hoặc phát hành.
