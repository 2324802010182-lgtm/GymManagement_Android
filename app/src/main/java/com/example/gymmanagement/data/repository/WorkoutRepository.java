package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.model.WorkoutLog;
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

/** Nhật ký buổi tập (users/{uid}/workoutLogs) – chỉ ghi khi người dùng chủ động xác nhận. */
public class WorkoutRepository {

    private CollectionReference col() {
        return Fb.db().collection("users").document(Fb.uid()).collection("workoutLogs");
    }

    public void list(Callback<List<WorkoutLog>> cb) {
        if (!Repo.guard(cb)) return;
        col().orderBy("completedAt", Query.Direction.DESCENDING).limit(1000).get()
                .addOnSuccessListener(qs -> cb.onSuccess(Repo.map(qs, WorkoutLog::from)))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void addFromExercise(Exercise ex, int sets, int reps, int minutes, String note, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        if (!Repo.limit(cb, note, 300, "Ghi chú")) return;
        Map<String, Object> m = new HashMap<>();
        m.put("exerciseId", ex.id);
        m.put("exerciseName", ex.name);
        m.put("category", ex.category);
        m.put("sets", (long) sets);
        m.put("reps", (long) reps);
        m.put("durationMinutes", (long) minutes);
        m.put("note", note == null ? "" : note.trim());
        m.put("completedAt", new Timestamp(new Date()));
        m.put("source", "exercise_library");
        m.put("isSampleData", false);
        m.put("createdAt", FieldValue.serverTimestamp());
        col().document().set(m).addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }

    public void delete(String id, Callback<Void> cb) {
        if (!Repo.guard(cb)) return;
        col().document(id).delete().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
