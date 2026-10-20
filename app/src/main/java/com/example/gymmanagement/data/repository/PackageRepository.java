package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** F03 – gói tập. */
public class PackageRepository {

    private CollectionReference col(String gymId) {
        return Fb.db().collection("gyms").document(gymId).collection("packages");
    }

    /** Hội viên: chỉ các gói đang mở bán của Gym. */
    public void listActive(String gymId, Callback<List<GymPackage>> cb) {
        if (!Repo.guard(cb)) return;
        col(gymId).whereEqualTo("isActive", true).get()
                .addOnSuccessListener(qs -> {
                    List<GymPackage> l = Repo.map(qs, GymPackage::from);
                    Collections.sort(l, (a, b) -> Long.compare(a.price, b.price));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Chủ Gym: tất cả gói (kể cả đã tắt). */
    public void listAll(String gymId, Callback<List<GymPackage>> cb) {
        if (!Repo.guard(cb)) return;
        col(gymId).get()
                .addOnSuccessListener(qs -> {
                    List<GymPackage> l = Repo.map(qs, GymPackage::from);
                    Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void create(String gymId, GymPackage p, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        DocumentReference ref = col(gymId).document();
        Map<String, Object> m = new HashMap<>();
        m.put("gymId", gymId);
        m.put("name", p.name.trim());
        m.put("price", p.price);
        m.put("durationDays", (long) p.durationDays);
        m.put("description", p.description.trim());
        m.put("benefits", p.benefits);
        m.put("isActive", p.isActive);
        m.put("isSampleData", false);
        m.put("createdAt", FieldValue.serverTimestamp());
        m.put("updatedAt", FieldValue.serverTimestamp());
        ref.set(m).addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void update(String gymId, GymPackage p, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Map<String, Object> m = new HashMap<>();
        m.put("name", p.name.trim());
        m.put("price", p.price);
        m.put("durationDays", (long) p.durationDays);
        m.put("description", p.description.trim());
        m.put("benefits", p.benefits);
        m.put("isActive", p.isActive);
        m.put("updatedAt", FieldValue.serverTimestamp());
        col(gymId).document(p.id).update(m)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void setActive(String gymId, GymPackage p, boolean active, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Map<String, Object> m = new HashMap<>();
        m.put("isActive", active);
        m.put("updatedAt", FieldValue.serverTimestamp());
        col(gymId).document(p.id).update(m)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** Xóa chỉ khi không còn yêu cầu đang chờ dùng gói này. */
    public void delete(String gymId, GymPackage p, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("gyms").document(gymId).collection("membershipRequests")
                .whereEqualTo("packageId", p.id).whereEqualTo("status", "pending").limit(1).get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        cb.onError(com.example.gymmanagement.util.AppError.invalid(
                                "Gói này đang có yêu cầu chờ duyệt, hãy xử lý yêu cầu hoặc chỉ tắt gói."));
                        return;
                    }
                    col(gymId).document(p.id).delete()
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
