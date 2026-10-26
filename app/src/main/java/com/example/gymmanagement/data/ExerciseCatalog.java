package com.example.gymmanagement.data;

import android.content.Context;

import com.example.gymmanagement.model.Exercise;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Danh mục bài tập đi kèm ứng dụng để thư viện vẫn dùng được khi Firestore chưa có dữ liệu. */
public final class ExerciseCatalog {
    private static List<Exercise> cached;

    private ExerciseCatalog() {
    }

    public static synchronized List<Exercise> load(Context context) throws IOException {
        if (cached == null) {
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("exercises.json"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            try {
                JSONArray rows = new JSONArray(json.toString());
                List<Exercise> exercises = new ArrayList<>(rows.length());
                for (int i = 0; i < rows.length(); i++) {
                    Exercise exercise = Exercise.fromJson(rows.getJSONObject(i));
                    if (exercise.isActive && !exercise.id.isEmpty()) exercises.add(exercise);
                }
                cached = exercises;
            } catch (org.json.JSONException e) {
                throw new IOException("Không đọc được danh mục bài tập đi kèm ứng dụng.", e);
            }
        }
        return new ArrayList<>(cached);
    }
}
