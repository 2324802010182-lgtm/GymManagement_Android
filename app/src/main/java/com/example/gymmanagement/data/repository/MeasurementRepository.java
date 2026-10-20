package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Bmi;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** F06 – chỉ số cơ thể (users/{uid}/measurements). */
public class MeasurementRepository {

    private CollectionReference col() {
        return Fb.db().collection("users").document(Fb.uid()).collection("measurements");
    }

    public void list(Callback<List<Measurement>> cb) {
        if (!Repo.guard(cb)) return;
        col().orderBy("measuredAt", Query.Direction.DESCENDING).limit(500).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, Measurement::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    /** id rỗng = tạo mới, ngược lại cập nhật. */
    public void save(String id, double weightKg, double heightCm, Double bodyFat, Date measuredAt, String note,
                     Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, note, 300, "Ghi chú")) return;
        if (!Bmi.validWeight(weightKg)) {
            cb.onError(AppError.invalid("Cân nặng phải trong khoảng 20–350 kg."));
            return;
        }
        if (!Bmi.validHeight(heightCm)) {
            cb.onError(AppError.invalid("Chiều cao phải trong khoảng 80–250 cm."));
            return;
        }
        if (bodyFat != null && !Bmi.validBodyFat(bodyFat)) {
            cb.onError(AppError.invalid("Tỷ lệ mỡ phải trong khoảng 2–70 %."));
            return;
        }
        if (measuredAt.after(new Date())) {
            cb.onError(AppError.invalid("Ngày đo không được ở tương lai."));
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("weightKg", weightKg);
        m.put("heightCm", heightCm);
        m.put("bodyFatPercent", bodyFat);
        m.put("bmi", Bmi.calc(weightKg, heightCm));
        m.put("measuredAt", new Timestamp(measuredAt));
        m.put("note", note == null ? "" : note.trim());
        m.put("updatedAt", FieldValue.serverTimestamp());
        if (id == null || id.isEmpty()) {
            m.put("isSampleData", false);
            m.put("createdAt", FieldValue.serverTimestamp());
            col().document().set(m).addOnSuccessListener(v -> cb.onSuccess(null))
                    .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
        } else {
            col().document(id).update(m).addOnSuccessListener(v -> cb.onSuccess(null))
                    .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
        }
    }

    public void delete(String id, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        col().document(id).delete().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
