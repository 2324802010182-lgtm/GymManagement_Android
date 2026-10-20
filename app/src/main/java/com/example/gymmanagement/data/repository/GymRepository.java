package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.example.gymmanagement.util.Validators;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Thương hiệu Gym: khám phá (hội viên), quản lý thương hiệu (Chủ Gym), duyệt (quản trị). */
public class GymRepository {

    public void listActiveGyms(Callback<List<Gym>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").whereEqualTo("status", Gym.STATUS_ACTIVE).get()
                .addOnSuccessListener(qs -> {
                    List<Gym> l = Repo.map(qs, Gym::from);
                    Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void getGym(String gymId, Callback<Gym> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").document(gymId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) cb.onError(AppError.notFound("Không tìm thấy Gym này."));
                    else cb.onSuccess(Gym.from(d));
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Đếm số gói đang mở bán bằng aggregation (không tải toàn bộ gói). */
    public void countActivePackages(String gymId, Callback<Integer> cb) {
        Fb.db().collection("gyms").document(gymId).collection("packages")
                .whereEqualTo("isActive", true).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(s -> cb.onSuccess((int) s.getCount()))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void isNameTaken(String normalizedName, Callback<Boolean> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gymNameReservations").document(normalizedName).get()
                .addOnSuccessListener(d -> cb.onSuccess(d.exists()))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public static String normalize(String name) {
        return Validators.cleanName(name).toLowerCase(Locale.ROOT);
    }

    /** Tạo thương hiệu + giữ chỗ tên duy nhất trong CÙNG một batch (Rules kiểm tra bằng getAfter). */
    public void createGym(Gym g, User owner, Callback<Gym> cb) {
        if (!Repo.guard(cb)) return;
        String name = Validators.cleanName(g.name);
        String normalized = normalize(name);
        FirebaseFirestore db = Fb.db();
        DocumentReference gref = db.collection("gyms").document();
        DocumentReference rref = db.collection("gymNameReservations").document(normalized);

        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("normalizedName", normalized);
        m.put("ownerUid", owner.uid);
        m.put("ownerName", owner.fullName);
        m.put("address", g.address.trim());
        m.put("area", g.area.trim());
        m.put("phone", g.phone.trim());
        m.put("description", g.description.trim());
        m.put("openingHours", g.openingHours.trim());
        m.put("logoUrl", g.logoUrl.trim());
        m.put("coverUrl", g.coverUrl.trim());
        m.put("galleryUrls", g.galleryUrls == null ? new ArrayList<String>() : g.galleryUrls);
        m.put("imageCredit", "");
        m.put("status", g.status == null || g.status.isEmpty() ? Gym.STATUS_ACTIVE : g.status);
        m.put("isSampleData", false);
        m.put("createdAt", FieldValue.serverTimestamp());
        m.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> r = new HashMap<>();
        r.put("gymId", gref.getId());
        r.put("ownerUid", owner.uid);
        r.put("name", name);
        r.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch b = db.batch();
        b.set(gref, m);
        b.set(rref, r);
        b.commit()
                .addOnSuccessListener(v -> getGym(gref.getId(), cb))
                .addOnFailureListener(e -> cb.onError(nameConflictOr(e)));
    }

    /** Cập nhật thương hiệu; đổi tên thì trả tên cũ và giữ chỗ tên mới trong cùng batch. */
    public void updateGym(Gym old, Gym edited, Callback<Gym> cb) {
        if (!Repo.guard(cb)) return;
        String name = Validators.cleanName(edited.name);
        String normalized = normalize(name);
        FirebaseFirestore db = Fb.db();
        DocumentReference gref = db.collection("gyms").document(old.id);

        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("normalizedName", normalized);
        m.put("address", edited.address.trim());
        m.put("area", edited.area.trim());
        m.put("phone", edited.phone.trim());
        m.put("description", edited.description.trim());
        m.put("openingHours", edited.openingHours.trim());
        m.put("logoUrl", edited.logoUrl.trim());
        m.put("coverUrl", edited.coverUrl.trim());
        m.put("galleryUrls", edited.galleryUrls == null ? new ArrayList<String>() : edited.galleryUrls);
        m.put("status", edited.status);
        m.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch b = db.batch();
        b.update(gref, m);
        if (!normalized.equals(old.normalizedName)) {
            Map<String, Object> r = new HashMap<>();
            r.put("gymId", old.id);
            r.put("ownerUid", old.ownerUid);
            r.put("name", name);
            r.put("createdAt", FieldValue.serverTimestamp());
            b.set(db.collection("gymNameReservations").document(normalized), r);
            b.delete(db.collection("gymNameReservations").document(old.normalizedName));
        }
        b.commit()
                .addOnSuccessListener(v -> getGym(old.id, cb))
                .addOnFailureListener(e -> cb.onError(nameConflictOr(e)));
    }

    private AppError nameConflictOr(Exception e) {
        AppError err = ErrorMapper.from(e);
        if (err.kind == AppError.Kind.PERMISSION) {
            return new AppError(AppError.Kind.CONFLICT,
                    "Không lưu được: tên thương hiệu có thể đã tồn tại, dữ liệu không hợp lệ hoặc tài khoản chưa đủ quyền.");
        }
        return err;
    }

    // ---- Quản trị nền tảng ----

    public void listAllGyms(Callback<List<Gym>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").get()
                .addOnSuccessListener(qs -> {
                    List<Gym> l = Repo.map(qs, Gym::from);
                    Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void setStatusAsAdmin(Gym g, String status, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("updatedAt", FieldValue.serverTimestamp());
        Fb.db().collection("gyms").document(g.id).update(m)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
