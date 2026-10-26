package com.example.gymmanagement.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** F05 – danh sách bài tập, tìm kiếm, lọc nhóm cơ & độ khó (lọc cục bộ trên dữ liệu Firestore). */
public class ExerciseViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Exercise>>> list = new MutableLiveData<>();
    private List<Exercise> all = new ArrayList<>();
    private String query = "";
    private String category = "";
    private String difficulty = "";

    public LiveData<Resource<List<Exercise>>> list() {
        return list;
    }

    public List<Exercise> all() {
        return all;
    }

    public Exercise byId(String id) {
        for (Exercise e : all) if (e.id.equals(id)) return e;
        return null;
    }

    public void load(Context context) {
        list.setValue(Resource.<List<Exercise>>loading());
        Repos.exercises().listActive(context, new Callback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> data) {
                all = data;
                apply();
            }

            @Override
            public void onError(AppError error) {
                list.setValue(Resource.<List<Exercise>>error(error));
            }
        });
    }

    public void setQuery(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        apply();
    }

    public void setCategory(String c) {
        category = c == null ? "" : c;
        apply();
    }

    public void setDifficulty(String d) {
        difficulty = d == null ? "" : d;
        apply();
    }

    public boolean isFiltering() {
        return !query.isEmpty() || !category.isEmpty() || !difficulty.isEmpty();
    }

    private void apply() {
        List<Exercise> out = new ArrayList<>();
        for (Exercise e : all) {
            if (!category.isEmpty() && !category.equals(e.category)) continue;
            if (!difficulty.isEmpty() && !difficulty.equals(e.difficulty)) continue;
            if (!query.isEmpty() && !e.searchText().contains(query)) continue;
            out.add(e);
        }
        list.setValue(Resource.ofList(out));
    }

    public void completeExercise(Exercise ex, int sets, int reps, int minutes, String note) {
        runAction("Đã ghi nhận buổi tập vào nhật ký.",
                c -> Repos.workouts().addFromExercise(ex, sets, reps, minutes, note, c));
    }
}
