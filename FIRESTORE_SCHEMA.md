# Lược đồ Firestore

Quy ước: thời gian là `Timestamp` (UTC); tiền là số nguyên VND (`int`); `isSampleData = true` đánh dấu dữ liệu mẫu do seed tạo.

## users/{uid}
| Trường | Kiểu | Ghi chú |
|---|---|---|
| uid, fullName, email, phone | string | email khớp `request.auth.token.email` |
| role | string | `member` \| `gym_owner` (quản trị nền tảng **không** lưu ở đây) |
| accountStatus | string | `active` \| `pending` \| `rejected` \| `suspended` |
| applicationNote, proposedGymName | string? | Chỉ Chủ Gym khi đăng ký |
| goal | map? | `{targetWeightKg?, weeklyWorkoutTarget?, note?}` |
| reviewedBy, reviewedAt, reviewNote | — | Do Quản trị ghi khi duyệt |
| createdAt, updatedAt | timestamp | |

Subcollections:
- `notifications/{id}`: recipientUid, gymId, gymName, title, body, type, relatedId, isRead, senderUid, createdAt, readAt?
  `type` ∈ membership_request, request_approved, request_rejected, membership_cancelled, membership_expiring (`id = expiring_{membershipId}`), schedule_created/updated/cancelled, announcement, owner_approved, owner_rejected.
- `measurements/{id}`: weightKg, heightCm, bodyFatPercent?, bmi, measuredAt, note, isSampleData, createdAt, updatedAt
- `workoutLogs/{id}`: exerciseId, exerciseName, category, sets, reps, durationMinutes, note, completedAt, source, isSampleData, createdAt

## platformAdmins/{uid}
`uid`, `email`, `createdAt`. **Chỉ Admin SDK ghi.** Client chỉ đọc được tài liệu của chính mình.

## gymNameReservations/{normalizedName}
`gymId`, `ownerUid`, `name`, `createdAt`. Khóa = tên đã làm sạch & viết thường ⇒ đảm bảo tên thương hiệu duy nhất (tạo cùng batch với gym).

## gyms/{gymId}
name, normalizedName, ownerUid, ownerName, address, area, phone, description, openingHours, logoUrl, coverUrl, galleryUrls[], imageCredit, status (`active|hidden|suspended`), isSampleData, createdAt, updatedAt.

Subcollections:
| Đường dẫn | Trường chính |
|---|---|
| `packages/{id}` | gymId, name, price, durationDays, description, benefits[], isActive, isSampleData, createdAt, updatedAt |
| `membershipRequests/{id}` | gymId, gymName, memberUid, memberName, memberEmail, memberPhone, packageId, packageName, packagePrice, packageDurationDays, note, status (`pending|approved|rejected|cancelled`), reviewedBy?, reviewedAt?, rejectReason?, membershipId?, createdAt, updatedAt |
| `pendingRequests/{memberUid}` | requestId, memberUid, createdAt — marker: mỗi hội viên chỉ có 1 yêu cầu *pending*/Gym |
| `memberships/{requestId}` | gymId, gymName, memberUid, memberName, packageId, packageName, packagePrice, durationDays, requestId, status (`active|cancelled`), cancelReason?, startAt, endAt (= startAt + durationDays), approvedBy, createdAt, updatedAt. **ID = ID yêu cầu** |
| `members/{memberUid}` | memberUid, gymId, fullName, email, phone, latestMembershipId, latestPackageName, latestEndAt, joinedAt, updatedAt, isSampleData |
| `schedules/{id}` | gymId, gymName, memberUid, memberName, title, description, location, startAt, endAt, status (`scheduled|completed|cancelled`), createdBy, isSampleData, createdAt, updatedAt |
| `attendance/{memberUid_yyyyMMdd}` | gymId, gymName, memberUid, memberName, membershipId, packageName, dateKey, checkInAt, recordedBy, recordedByName — `dateKey` theo **giờ Việt Nam**, khớp `vnDateKey()` trong rules |
| `notifications/{id}` | Lịch sử thông báo chung của Gym: gymId, gymName, title, body, audience (`all_members|active_members`), recipientCount, createdBy, createdAt |

## exercises/{exerciseId}
name, description, category (Ngực/Lưng/Chân/Vai/Tay/Bụng/Cardio), difficulty, targetMuscles[], secondaryMuscles[], equipment, imageUrl, videoUrl, videoType (`mp4|hls|dash`), videoThumbnailUrl, videoDurationSeconds, setupPosition, bodyPlacement, instructions[], breathingGuide, rangeOfMotion, commonMistakes[], safetyNotes[], recommendedSets, recommendedReps, recommendedRestSeconds, isActive, isSampleData. Chỉ Admin SDK ghi.

## Chỉ mục
Mọi truy vấn trong app dùng lọc bằng bằng-nhau hoặc một `orderBy`, nên không cần chỉ mục tổng hợp. Truy vấn **collection group** theo `memberUid` (hội viên xem yêu cầu/membership/lịch/điểm danh của chính mình) cần *single-field override* ở phạm vi `COLLECTION_GROUP` — xem `firebase/firestore.indexes.json`.
