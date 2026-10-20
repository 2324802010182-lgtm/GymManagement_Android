package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** users/{uid}/workoutLogs/{id} – chỉ tạo khi người dùng chủ động xác nhận hoàn thành. */
public class WorkoutLog {
    public String id = "";
    public String exerciseId = "";
    public String exerciseName = "";
    public String category = "";
    public int sets;
    public int reps;
    public int durationMinutes;
    public String note = "";
    public Date completedAt;
    public String source = "exercise_library";

    public static WorkoutLog from(DocumentSnapshot s) {
        WorkoutLog w = new WorkoutLog();
        w.id = s.getId();
        w.exerciseId = Docs.str(s, "exerciseId");
        w.exerciseName = Docs.str(s, "exerciseName");
        w.category = Docs.str(s, "category");
        w.sets = (int) Docs.lng(s, "sets");
        w.reps = (int) Docs.lng(s, "reps");
        w.durationMinutes = (int) Docs.lng(s, "durationMinutes");
        w.note = Docs.str(s, "note");
        w.completedAt = Docs.date(s, "completedAt");
        w.source = Docs.str(s, "source");
        return w;
    }
}
