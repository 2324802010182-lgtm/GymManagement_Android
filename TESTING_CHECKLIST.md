# Danh sách kiểm thử

Đánh dấu ✅ sau khi **bạn** đã thử trên thiết bị/emulator với Firebase thật. Dự án chưa được chạy trên thiết bị trong môi trường tạo mã, nên mọi mục đều cần xác nhận.

## A. Chuẩn bị
- [ ] `app/google-services.json` thật đã đặt; Gradle Sync thành công; `./gradlew assembleDebug` pass
- [ ] Auth Email/Password bật; rules + indexes đã deploy (Console hiển thị đúng)
- [ ] `./gradlew testDebugUnitTest` pass (Bmi, Validators, DateKeys)
- [ ] Seed chạy xong (hoặc tự tạo dữ liệu); có ít nhất 1 tài khoản quản trị (`grant-admin.js`)

## B. Cấu hình / trạng thái chung
- [ ] Bỏ `google-services.json` tạm thời → app hiện hướng dẫn cấu hình, không hiện dữ liệu giả
- [ ] Tắt mạng → màn hình hiện “Không có kết nối mạng” + nút Thử lại; bật mạng và bấm Thử lại hoạt động
- [ ] Mỗi danh sách có đủ: skeleton khi tải, trạng thái trống, trạng thái lỗi, kéo để làm mới
- [ ] Bật “Remove animations” trong Developer options → hiệu ứng giảm/tắt, không lỗi

## C. Xác thực (F01)
- [ ] Đăng ký hội viên (không chọn gym) → vào trang chủ hội viên
- [ ] Đăng ký trùng email → báo lỗi rõ ràng; mật khẩu < 6 ký tự → báo lỗi
- [ ] Quên mật khẩu → nhận email đặt lại
- [ ] Đăng ký Chủ Gym → màn “Đang chờ phê duyệt”; “Kiểm tra trạng thái” vẫn pending
- [ ] Đăng nhập tài khoản bị từ chối/khóa → bị chặn kèm thông báo
- [ ] Sửa hồ sơ (họ tên, SĐT); không sửa được email/vai trò
- [ ] Đăng xuất → về màn đăng nhập, không còn phiên

## D. Quản trị nền tảng
- [ ] Đăng nhập tài khoản quản trị → màn Quản trị
- [ ] Danh sách Chủ Gym chờ duyệt: Duyệt → chủ gym đăng nhập được; Từ chối kèm lý do → bị chặn
- [ ] Tạm khóa thương hiệu → biến mất khỏi trang chủ hội viên; Mở lại → hiện lại
- [ ] Tài khoản thường **không** vào được màn quản trị (kể cả khi cố gọi trực tiếp)

## E. Chủ Gym
- [ ] Chưa có thương hiệu: các tab hiện lời mời tạo thương hiệu
- [ ] Tạo thương hiệu; tạo tên trùng → báo trùng; sửa thông tin, ẩn/hiện
- [ ] Gói tập: thêm/sửa/bật-tắt/xóa; kiểm tra giới hạn giá/ngày/quyền lợi
- [ ] Yêu cầu: Duyệt → hội viên thấy gói trong “Gym của tôi”, membership có `endAt = startAt + số ngày`; Từ chối kèm lý do
- [ ] Hội viên: tìm kiếm, lọc còn hạn/hết hạn, chi tiết, hủy gói (kèm lý do)
- [ ] Lịch tập: tạo cho hội viên còn hạn, sửa, hoàn thành, hủy → hội viên nhận thông báo
- [ ] Điểm danh: điểm danh hội viên còn hạn; lần 2 trong ngày bị từ chối; hết hạn bị từ chối; xóa lượt ghi nhầm
- [ ] Thông báo chung: gửi cho tất cả/chỉ còn hạn; lịch sử hiển thị
- [ ] Dashboard: các con số khớp dữ liệu (đếm lại bằng Console); đếm số chạy mượt

## F. Hội viên
- [ ] Trang chủ: danh sách gym active; tìm theo tên và theo địa chỉ; trạng thái trống khi không khớp
- [ ] Chi tiết gym: chỉ gói thuộc gym đó; gói tạm ẩn không đăng ký được
- [ ] Gửi yêu cầu: thành công, hiện hiệu ứng xác nhận; gửi lần 2 cùng gym khi còn pending → bị chặn rõ ràng
- [ ] Hủy yêu cầu pending → gửi lại được
- [ ] Gym của tôi: membership và yêu cầu (pending/approved/rejected) đúng trạng thái
- [ ] Lịch tập: xem theo ngày/tuần, chi tiết buổi
- [ ] Thư viện bài tập: lọc nhóm cơ, tìm kiếm, chi tiết đủ phần hướng dẫn chữ
- [ ] Video: bài **có** `videoUrl` phát được, toàn màn hình, lỗi mạng có nút thử lại, không tự phát; bài không có video hiển thị thông báo
- [ ] Hoàn thành bài tập → tạo workoutLog **chỉ sau khi xác nhận**
- [ ] Chỉ số cơ thể: thêm/sửa/xóa; ngoài miền hợp lệ bị chặn; BMI đúng
- [ ] Tiến độ: bộ lọc 7/30/90/tất cả; biểu đồ cân nặng và số buổi tập; mục tiêu tuần
- [ ] Thông báo: lọc chưa đọc, đánh dấu đã đọc, đọc tất cả; nhắc gói sắp hết hạn tạo đúng 1 lần

## G. Bảo mật (thử thủ công / emulator)
- [ ] `cd firebase/rules-tests && npm test` – sửa test/rules nếu có sai khác
- [ ] Hội viên không đọc được `users/{người khác}`
- [ ] Hội viên không tự duyệt yêu cầu, không tự tạo membership
- [ ] Chủ Gym A không sửa được dữ liệu Gym B
- [ ] Không client nào ghi được `platformAdmins` hoặc `exercises`
