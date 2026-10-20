package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.ErrorMapper;
import com.example.gymmanagement.util.Fb;

import java.util.Collections;
import java.util.List;

/** F05 – thư viện bài tập (đọc từ collection exercises). */
public class ExerciseRepository {

    public void listActive(Callback<List<Exercise>> cb) {
        if (!Repo.guard(cb)) return;
        Fb.db().collection("exercises").whereEqualTo("isActive", true).get()
                .addOnSuccessListener(qs -> {
                    List<Exercise> l = Repo.map(qs, Exercise::from);
                    Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onError(ErrorMapper.from(e)));
    }
}
