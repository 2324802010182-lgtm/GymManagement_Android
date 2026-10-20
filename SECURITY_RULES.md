# Security Rules

File: `firebase/firestore.rules` (mặc định **từ chối**; chỉ mở đúng quyền).

## Nguyên tắc
1. **Không tin giao diện.** Mọi ràng buộc vai trò/trạng thái được kiểm tra ở rules.
2. Hồ sơ mới chỉ được là `member/active` hoặc `gym_owner/pending`. Không thể tự đổi `role`, `accountStatus`, `email`.
3. Chủ Gym chỉ thao tác trong Gym mình sở hữu (`ownsGym(gymId)` = chủ đã `active` **và** `gym.ownerUid == uid`).
4. Quản trị nền tảng = tồn tại `platformAdmins/{uid}` (chỉ Admin SDK ghi). Quản trị chỉ đổi `accountStatus` người dùng (không đổi role) và `status` Gym giữa `active/suspended`.
5. Thao tác nhiều tài liệu dùng `WriteBatch` + `getAfter/existsAfter` để bảo đảm tính nguyên tử và chống gian lận.

## Bảo đảm nghiệp vụ quan trọng
| Yêu cầu | Cơ chế trong rules |
|---|---|
| Hội viên không tự duyệt yêu cầu | `ownerReview()` bắt buộc `ownsGym` |
| Không gửi trùng yêu cầu *pending* | `pendingRequests/{uid}` phải *chưa tồn tại* trước và tồn tại sau, trỏ về yêu cầu mới |
| Chỉ tạo membership khi yêu cầu chuyển *pending → approved* trong cùng batch | `validMembershipCreate()` đọc `get(request)` (trước) và `getAfter(request)` (sau); ID membership = ID yêu cầu |
| `endAt = startAt + durationDays` | kiểm tra trực tiếp |
| Tên thương hiệu duy nhất | `gymNameReservations/{normalized}` chưa tồn tại + tạo cùng batch với Gym |
| Mỗi hội viên một lượt điểm danh/ngày | ID `uid_yyyyMMdd` (giờ VN từ `request.time`) + membership còn hiệu lực |
| Thông báo không giả mạo | `validNotificationShape()` + chỉ 4 kịch bản hợp lệ (hội viên→chủ khi gửi yêu cầu; chủ→hội viên của mình; quản trị; hội viên tự nhắc hết hạn) |
| Số đo hợp lệ | cân nặng 20–350 kg, chiều cao 80–250 cm, mỡ 2–70 %, BMI khớp công thức (sai số < 0.06) |
| Bài tập chỉ đọc | `allow write: if false` (seed bằng Admin SDK) |

## Truy vấn collection group
Hội viên chỉ đọc được bản ghi có `memberUid == uid` ở `membershipRequests`, `memberships`, `schedules`, `attendance`; mọi truy vấn của app đều kèm điều kiện này.

## Giới hạn đã biết
- Rules đã được **viết nhưng chưa được kiểm thử bằng emulator** trong môi trường tạo dự án (không tải được emulator). Hãy chạy `firebase/rules-tests` (xem file) và thử thủ công theo `TESTING_CHECKLIST.md` trước khi dùng thật.
- `normalizedName == name.lower()`: ứng dụng dùng `toLowerCase(Locale.ROOT)`. Với ký tự có dấu tiếng Việt, hãy kiểm tra thực tế rằng `lower()` của rules và Java cho cùng kết quả (nếu khác, tạo thương hiệu sẽ bị từ chối với thông báo “tên có thể đã tồn tại / dữ liệu không hợp lệ”).
- Rules dùng nhiều `get()/exists()`; mỗi lần gọi tính phí đọc và Firestore giới hạn số truy cập tài liệu trong rules cho mỗi yêu cầu/batch (10 với yêu cầu đơn, 20 với batch/giao dịch). Các batch của app được thiết kế để nằm trong giới hạn nhưng **chưa được kiểm chứng thực tế** — hãy thử luồng duyệt yêu cầu và gửi thông báo trên emulator/dự án thật.
- Không có Cloud Functions: việc “gói sắp hết hạn” được app của hội viên tự tạo thông báo cho chính mình (mỗi gói một lần), không phải đẩy FCM.

## Triển khai
```bash
cd firebase && firebase deploy --only firestore:rules,firestore:indexes
```
