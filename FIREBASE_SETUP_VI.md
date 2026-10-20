# HƯỚNG DẪN KẾT NỐI FIREBASE THẬT — GYMMANAGEMENT

Thực hiện theo đúng thứ tự. Không bật chế độ Test/cho phép mọi người đọc ghi toàn bộ database.

## A. Tạo Firebase Project

1. Mở https://console.firebase.google.com/ và đăng nhập bằng tài khoản Google của nhóm.
2. Chọn **Create a project / Tạo dự án**.
3. Đặt tên, ví dụ `GymManagement-TDMU`. Analytics không bắt buộc đối với các chức năng hiện tại.
4. Sau khi tạo xong, vào trang tổng quan của project.

## B. Đăng ký ứng dụng Android

1. Tại Project Overview, chọn **Add app → Android**.
2. Nhập chính xác Android package name:

   `com.example.gymmanagement`

3. Tên ứng dụng có thể đặt `GymManagement`; SHA-1 chưa cần cho đăng nhập Email/Password.
4. Tải file `google-services.json`.
5. Đặt file vào đúng đường dẫn:

   `GymManagement_Android/app/google-services.json`

   Không đổi tên thành `google-services (1).json`, không đặt trong `app/src/main`.

## C. Bật đăng nhập Email/Password

1. Firebase Console → **Authentication**.
2. Chọn **Get started** nếu Firebase yêu cầu.
3. Mở tab **Sign-in method**.
4. Chọn **Email/Password**, bật Enable và Save.

## D. Tạo Cloud Firestore

1. Firebase Console → **Firestore Database** → **Create database**.
2. Chọn **Production mode**.
3. Chọn khu vực database phù hợp với người dùng của nhóm. Hãy xem kỹ trước khi xác nhận vì vị trí database là quyết định hạ tầng quan trọng.
4. Tạo database và chờ hoàn tất.
5. Chọn tab **Rules**.
6. Mở file `firestore.rules` trong project, sao chép toàn bộ nội dung và thay thế nội dung Rules trong Console.
7. Nhấn **Publish**.

Không cần tạo collection hoặc document thủ công trước; ứng dụng tạo các document đầu tiên khi tài khoản và Gym được đăng ký hợp lệ.

## E. Sync và chạy ứng dụng

1. Mở project `GymManagement_Android` bằng Android Studio.
2. Xác nhận JSON nằm ở `app/google-services.json`.
3. Chọn **File → Sync Project with Gradle Files**.
4. Nếu Sync thành công, chạy cấu hình `app` trên emulator/điện thoại có Internet.
5. Nếu app vẫn hiện màn hình “CẦN KẾT NỐI FIREBASE”, kiểm tra lại tên file, thư mục, package name, sau đó Sync và chạy lại app.

## F. Tạo tài khoản chủ Gym đầu tiên

Để tránh người dùng tự cấp quyền quản trị cho chính mình, tài khoản chủ Gym sẽ ở trạng thái chờ duyệt.

1. Trong app, chọn **Tạo tài khoản mới → Chủ phòng gym** và đăng ký bằng email thật mà bạn kiểm soát.
2. App báo tài khoản đang chờ phê duyệt rồi đăng xuất. Đây là hành vi mong đợi.
3. Firebase Console → Authentication → Users: tìm email vừa đăng ký và sao chép UID.
4. Firestore Database → Data → collection `nguoi_dung` → mở document có ID trùng với UID.
5. Chỉ sau khi xác minh đây là tài khoản chủ Gym hợp lệ, sửa trường:

   - `vai_tro`: `quan_tri_gym` (kiểu String)
   - `gym_id`: chuỗi rỗng `""` (kiểu String)
   - `trang_thai`: `true` (kiểu Boolean)

   Không sửa/xóa UID hoặc email. Chờ lưu thay đổi xong.

6. Đăng nhập lại trong app. Chủ Gym được vào màn hình tạo thương hiệu; sau khi lưu, app tạo document trong `phong_gym` và liên kết `gym_id` trong hồ sơ.
7. Lặp lại quy trình phê duyệt cho mỗi chủ Gym mới trong bản đồ án này.

**Quan trọng:** Đây là cách phê duyệt thủ công để phù hợp đồ án. Không cấp vai trò `quan_tri_gym` cho tài khoản không được xác minh. Với hệ thống triển khai rộng, cần backend tin cậy để phê duyệt/cấp vai trò và cần kiểm tra bảo mật kỹ hơn.

## G. Tạo hội viên và kiểm tra dữ liệu

1. Sau khi có Gym đang hoạt động, đăng xuất khỏi tài khoản chủ Gym.
2. Chọn **Tạo tài khoản mới → Hội viên**.
3. Danh sách thương hiệu lấy từ collection `phong_gym`; chọn một Gym và tạo tài khoản.
4. Trong Firestore, kiểm tra `nguoi_dung/{UID}`: vai trò phải là `hoi_vien` và `gym_id` phải trùng ID document của Gym đã chọn.
5. Đăng nhập hội viên rồi thử xem gói tập, lịch tập và thông báo của Gym đã chọn.

## H. Quy trình kiểm tra hai Gym

Để xác nhận phân tách dữ liệu, tạo hai Gym thử nghiệm bằng hai tài khoản chủ Gym đã được phê duyệt thủ công. Mỗi Gym tạo gói riêng. Đăng nhập hội viên thuộc Gym A và kiểm tra họ chỉ nhìn thấy dữ liệu thuộc Gym A. Lặp lại với Gym B.

Dùng Firestore Rules Playground/Rules Simulator và thử các trường hợp trái quyền. Nếu nhận `PERMISSION_DENIED`, không nới rules thành quyền đọc/ghi công khai; hãy kiểm tra vai trò, `gym_id`, truy vấn và quy tắc tương ứng.

## I. Khắc phục lỗi hay gặp

- **`No matching client found for package name`**: package name Firebase khác `com.example.gymmanagement`; đăng ký đúng package và tải lại JSON.
- **`Default FirebaseApp is not initialized` / màn hình yêu cầu Firebase**: kiểm tra JSON có đúng chỗ và tên file, Sync Gradle, Build/Run lại.
- **`PERMISSION_DENIED`**: xác nhận đã Publish `firestore.rules`, tài khoản có document hồ sơ, vai trò được phê duyệt và `gym_id` chính xác.
- **Không tạo được hội viên**: bảo đảm đã có ít nhất một Gym ở trạng thái `hoat_dong`; hội viên không thể chọn Gym không tồn tại hoặc đã ngừng hoạt động.
- **Không gửi được email đặt lại mật khẩu**: kiểm tra Email/Password đã bật; dùng email có trong Authentication Users.
- **Đăng ký chủ Gym vẫn chờ duyệt**: đó là trạng thái đúng cho đến khi trường `vai_tro` của `nguoi_dung/{UID}` được phê duyệt.
