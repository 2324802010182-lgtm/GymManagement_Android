package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Quản trị nền tảng: duyệt tài khoản Chủ Gym (quyền platformAdmins/{uid} do Admin SDK cấp). */
public class AdminRepository {

    public void listPendingOwners(Callback<List<User>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("users").whereEqualTo("role", User.ROLE_OWNER)
                .whereEqualTo("accountStatus", User.STATUS_PENDING).get()
                .addOnSuccessListener(qs -> {
                    List<User> l = Repo.map(qs, User::from);
                    Collections.sort(l, (a, b) -> Repo.cmpAsc(a.createdAt, b.createdAt));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void reviewOwner(User owner, boolean approve, String note, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, note, 300, "Ghi chú")) return;
        FirebaseFirestore db = Fb.db();
        String me = Fb.uid();
        Map<String, Object> m = new HashMap<>();
        m.put("accountStatus", approve ? User.STATUS_ACTIVE : User.STATUS_REJECTED);
        m.put("reviewedBy", me);
        m.put("reviewedAt", FieldValue.serverTimestamp());
        m.put("reviewNote", note == null ? "" : note.trim());
        m.put("updatedAt", FieldValue.serverTimestamp());
        DocumentReference uref = db.collection("users").document(owner.uid);
        WriteBatch b = db.batch();
        b.update(uref, m);
        b.set(uref.collection("notifications").document(),
                Repo.notification(owner.uid, "", "",
                        approve ? "Tài khoản Chủ Gym đã được duyệt" : "Tài khoản Chủ Gym chưa được duyệt",
                        approve ? "Bạn có thể đăng nhập và tạo thương hiệu Gym của mình."
                                : "Yêu cầu làm Chủ Gym chưa được chấp nhận." + (note == null || note.trim().isEmpty() ? "" : " Ghi chú: " + note.trim()),
                        approve ? "owner_approved" : "owner_rejected", owner.uid, me));
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
