package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.SheetCompleteExerciseBinding;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.ExerciseViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Xác nhận hoàn thành bài tập – chỉ ghi workout log sau khi người dùng chủ động bấm xác nhận. */
public class CompleteExerciseSheet extends BottomSheetDialogFragment {
    public static CompleteExerciseSheet newInstance() {
        return new CompleteExerciseSheet();
    }

    private SheetCompleteExerciseBinding b;
    private ExerciseViewModel vm;
    private boolean submitted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetCompleteExerciseBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ExerciseDetailFragment parent = (ExerciseDetailFragment) requireParentFragment();
        Exercise ex = parent.exercise();
        vm = new ViewModelProvider(parent).get(ExerciseViewModel.class);
        b.doneExercise.setText(ex.name);
        b.doneSets.setText(String.valueOf(Math.max(1, ex.recommendedSets)));
        b.doneReps.setText(String.valueOf(firstNumber(ex.recommendedReps)));
        b.doneMin.setText("10");
        b.doneCancel.setOnClickListener(v -> dismiss());
        b.doneConfirm.setOnClickListener(v -> {
            b.doneError.setVisibility(View.GONE);
            b.doneSetsLayout.setError(null);
            b.doneRepsLayout.setError(null);
            b.doneMinLayout.setError(null);
            int sets = parse(b.doneSets.getText());
            int reps = parse(b.doneReps.getText());
            int min = parse(b.doneMin.getText());
            boolean ok = true;
            if (sets < 1 || sets > 50) {
                b.doneSetsLayout.setError(getString(R.string.error_range_1_50));
                ok = false;
            }
            if (reps < 0 || reps > 500) {
                b.doneRepsLayout.setError(getString(R.string.error_range_0_500));
                ok = false;
            }
            if (min < 1 || min > 600) {
                b.doneMinLayout.setError(getString(R.string.error_range_1_600));
                ok = false;
            }
            if (!ok) return;
            submitted = true;
            vm.completeExercise(ex, sets, reps, min, String.valueOf(b.doneNote.getText()));
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.doneProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.doneConfirm.setEnabled(!loading);
            if (loading) return;
            if (r.status == Resource.Status.SUCCESS) {
                dismiss();
            } else {
                b.doneError.setText(r.error.message);
                b.doneError.setVisibility(View.VISIBLE);
                submitted = false;
            }
        });
    }

    private int parse(CharSequence s) {
        try {
            return Integer.parseInt(String.valueOf(s).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int firstNumber(String reps) {
        StringBuilder sb = new StringBuilder();
        for (char c : reps.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else if (sb.length() > 0) break;
        }
        return sb.length() == 0 ? 10 : Integer.parseInt(sb.toString());
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
