# Hướng dẫn seed dữ liệu mẫu

Seed chạy bằng **Admin SDK trên máy của bạn** (không nằm trong APK). Dữ liệu mẫu **chỉ mang tính minh họa**: tên “Quốc Anh Gym”, “FitLife Gym”, “TDMU Fitness”, địa chỉ và mô tả đều ghi rõ là dữ liệu mẫu, **không** phải cơ sở có thật.

## Chuẩn bị
1. Đã làm xong `FIREBASE_SETUP_VI.md` (Auth bật, Firestore tạo, rules + indexes đã deploy).
2. Khóa service account lưu **ngoài repo** (Project settings → Service accounts → Generate new private key).
3. Node.js ≥ 18.

## Chạy
```bash
cd seed
npm install
cp .env.example .env          # rồi điền SEED_PASSWORD (tự đặt, ≥ 8 ký tự) và GOOGLE_APPLICATION_CREDENTIALS
node seed.js --dry-run        # xem trước, không ghi gì (vẫn cần khóa để đọc)
node seed.js                  # ghi thật
```
Biến môi trường: `GCLOUD_PROJECT` (mặc định `gymmanagement-tdmu` – đổi theo ID thật), `GOOGLE_APPLICATION_CREDENTIALS`, `SEED_PASSWORD`, tùy chọn `SEED_ADMIN_EMAIL` (cấp quyền quản trị cho một tài khoản Auth đã có).
Mật khẩu **không có giá trị mặc định** và không bao giờ nằm trong repo.

## Seed tạo gì?
| Nhóm | Nội dung |
|---|---|
| Tài khoản | 3 Chủ Gym đã duyệt, 1 Chủ Gym **đang chờ duyệt**, 4 hội viên — email `seed.*@example.com` |
| Thương hiệu | 3 gym (kèm `gymNameReservations`), mỗi gym 3–4 gói (có 1 gói tạm ẩn) |
| Kịch bản hội viên | `m1`: gói còn hạn ở Quốc Anh Gym + 1 gói đã hết hạn ở FitLife, có lịch tập, điểm danh, chỉ số cơ thể, nhật ký tập, mục tiêu; `m2`: yêu cầu đang chờ ở FitLife; `m3`: yêu cầu bị từ chối ở TDMU; `m4`: gói sắp hết hạn ở TDMU |
| Thông báo | Vài thông báo cá nhân + 1 thông báo chung của Gym |
| Bài tập | 24 bài / 7 nhóm (Ngực, Lưng, Chân, Vai, Tay, Bụng, Cardio) kèm hướng dẫn chữ chi tiết (tư thế, vị trí, các bước, nhịp thở, biên độ, lỗi thường gặp, an toàn) |

## Idempotent
- Tài liệu có ID cố định. Chạy lại: dữ liệu *tham chiếu* (gym, gói, bài tập) được cập nhật (`merge`), dữ liệu *giao dịch* (yêu cầu, membership, lịch, điểm danh, nhật ký, thông báo) **chỉ tạo nếu chưa có** — không nhân đôi, không dịch chuyển thời gian.
- Tài khoản Auth tạo nếu chưa tồn tại (theo email).

## Video và hình ảnh
Dự án **không tự gắn video/ảnh**: `videoUrl`, `imageUrl` mặc định để trống và app hiển thị “Chưa có video hướng dẫn”. Lý do: không thể xác minh bản quyền/khả năng phát của URL từ môi trường tạo dự án.
Để thêm media thật:
1. Tự tìm video/ảnh bạn **có quyền sử dụng**, mở thử để chắc chắn phát được.
2. Copy `media.example.json` → `media.json`, điền `videoUrl` (https, định dạng `mp4`, `hls` hoặc `dash`), tùy chọn `videoThumbnailUrl`, `videoDurationSeconds`, `imageUrl`.
   (Trang YouTube không phải file video — ExoPlayer cần URL trực tiếp tới `.mp4`/`.m3u8`/`.mpd`.)
3. `node seed.js` lại — script kiểm tra URL phải bắt đầu bằng `https://`.

## Xóa dữ liệu mẫu
```bash
node seed.js --delete-samples     # xóa tài liệu mẫu theo ID cố định + tài khoản Auth seed.*@example.com
```
Lệnh chỉ nhắm vào các ID/email do seed tạo, nhưng hãy luôn thử `--dry-run` hoặc dùng dự án phát triển riêng, **không** chạy trên dự án có dữ liệu thật.

## Kiểm thử bằng Emulator (tùy chọn)
Đặt `FIRESTORE_EMULATOR_HOST=localhost:8080` và `FIREBASE_AUTH_EMULATOR_HOST=localhost:9099` để seed vào emulator thay vì dự án thật.
