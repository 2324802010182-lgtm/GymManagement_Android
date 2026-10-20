package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.AppNotification;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** F08 – thông báo trong ứng dụng (lưu Firestore, không dùng FCM). */
public class NotificationRepository {

    private CollectionReference mine() {
        return Fb.db().collection("users").document(Fb.uid()).collection("notifications");
    }

    public void listMine(Callback<List<AppNotification>> cb) {
        if (!Repo.guard(cb)) return;
        mine().orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, AppNotification::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void unreadCount(Callback<Integer> cb) {
        if (!Repo.guard(cb)) return;
        mine().whereEqualTo("isRead", false).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(s -> cb.onSuccess((int) s.getCount()))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void markRead(AppNotification n, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Map<String, Object> m = new HashMap<>();
        m.put("isRead", true);
        m.put("readAt", FieldValue.serverTimestamp());
        mine().document(n.id).update(m)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void markAllRead(List<AppNotification> list, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        WriteBatch b = Fb.db().batch();
        int n = 0;
        for (AppNotification x : list) {
            if (x.isRead || n >= 400) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("isRead", true);
            m.put("readAt", FieldValue.serverTimestamp());
            b.update(mine().document(x.id), m);
            n++;
        }
        if (n == 0) {
            cb.onSuccess(null);
            return;
        }
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /**
     * Chủ Gym gửi thông báo chung. Mỗi người nhận một bản ghi trong users/{uid}/notifications và một bản ghi lịch sử
     * trong gyms/{gymId}/notifications. Chia batch ≤ 400 thao tác; lô đầu có kèm bản ghi lịch sử.
     */
    public void sendAnnouncement(Gym gym, User owner, String title, String body, boolean activeOnly,
                                 List<MemberSummary> members, Callback<Integer> cb) {
        if (!Repo.guard(cb)) return;
        String tt = title == null ? "" : title.trim();
        String bb = body == null ? "" : body.trim();
        if (tt.length() < 2 || tt.length() > 80 || bb.length() < 2 || bb.length() > 500) {
            cb.onError(AppError.invalid("Tiêu đề phải từ 2 đến 80 ký tự và nội dung từ 2 đến 500 ký tự."));
            return;
        }
        List<MemberSummary> targets = new ArrayList<>();
        for (MemberSummary m : members) {
            if (!activeOnly || m.isActiveNow()) targets.add(m);
        }
        if (targets.isEmpty()) {
            cb.onError(AppError.invalid("Không có hội viên nào phù hợp để nhận thông báo."));
            return;
        }
        FirebaseFirestore db = Fb.db();
        DocumentReference hist = db.collection("gyms").document(gym.id).collection("notifications").document();
        Map<String, Object> h = new HashMap<>();
        h.put("gymId", gym.id);
        h.put("gymName", gym.name);
        h.put("title", title.trim());
        h.put("body", body.trim());
        h.put("audience", activeOnly ? "active_members" : "all_members");
        h.put("recipientCount", (long) targets.size());
        h.put("createdBy", owner.uid);
        h.put("createdAt", FieldValue.serverTimestamp());

        List<WriteBatch> batches = new ArrayList<>();
        WriteBatch cur = db.batch();
        cur.set(hist, h);
        int count = 1;
        for (MemberSummary m : targets) {
            if (count >= 400) {
                batches.add(cur);
                cur = db.batch();
                count = 0;
            }
            cur.set(db.collection("users").document(m.memberUid).collection("notifications").document(),
                    Repo.notification(m.memberUid, gym.id, gym.name, title, body, "announcement", hist.getId(), owner.uid));
            count++;
        }
        batches.add(cur);
        commitAll(batches, 0, targets.size(), cb);
    }

    private void commitAll(List<WriteBatch> batches, int i, int total, Callback<Integer> cb) {
        if (i >= batches.size()) {
            cb.onSuccess(total);
            return;
        }
        batches.get(i).commit()
                .addOnSuccessListener(v -> commitAll(batches, i + 1, total, cb))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void listGymAnnouncements(String gymId, Callback<List<AppNotification>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").document(gymId).collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener(qs -> {
                    List<AppNotification> l = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                        AppNotification n = AppNotification.from(d);
                        n.gymName = com.example.gymmanagement.util.Docs.str(d, "gymName");
                        n.type = "announcement";
                        n.isRead = true;
                        l.add(n);
                    }
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /**
     * Nhắc "gói sắp hết hạn" (≤ 7 ngày): hội viên tự tạo cho chính mình dựa trên membership thật;
     * ID cố định expiring_{membershipId} nên mỗi gói chỉ nhắc một lần (Rules chặn ghi đè).
     */
    public void ensureExpiringReminders(User me, List<Membership> memberships) {
        if (Fb.precheck() != null || me == null) return;
        for (Membership m : memberships) {
            if (!m.isValidNow() || m.daysLeft() > 7) continue;
            DocumentReference ref = mine().document("expiring_" + m.id);
            ref.get().addOnSuccessListener(d -> {
                if (d.exists()) return;
                Map<String, Object> n = Repo.notification(me.uid, m.gymId, m.gymName, "Gói tập sắp hết hạn",
                        "Gói \"" + m.packageName + "\" tại " + m.gymName + " còn " + m.daysLeft()
                                + " ngày (hết hạn " + com.example.gymmanagement.util.Fmt.date(m.endAt) + ").",
                        "membership_expiring", m.id, me.uid);
                ref.set(n);
            });
        }
    }
}
