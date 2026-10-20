package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.SheetGoalBinding;
import com.example.gymmanagement.util.Bmi;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.AuthViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Đặt mục tiêu cân nặng / số buổi tập mỗi tuần (lưu trong users/{uid}.goal). */
public class GoalSheet extends BottomSheetDialogFragment {
    public static final String RESULT = "goal_saved";

    private SheetGoalBinding b;
    private AuthViewModel vm;
    private boolean submitted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetGoalBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        if (Session.user != null) {
            if (Session.user.goalTargetWeightKg != null)
                b.goalWeight.setText(Fmt.number(Session.user.goalTargetWeightKg, 1));
            if (Session.user.goalWeeklyWorkouts != null)
                b.goalWeekly.setText(String.valueOf(Session.user.goalWeeklyWorkouts));
            b.goalNote.setText(Session.user.goalNote);
        }
        b.goalCancel.setOnClickListener(v -> dismiss());
        b.goalSave.setOnClickListener(v -> save());
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.goalProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.goalSave.setEnabled(!loading);
            if (loading) return;
            if (r.status == Resource.Status.SUCCESS) {
                getParentFragmentManager().setFragmentResult(RESULT, new Bundle());
                dismiss();
            } else {
                submitted = false;
                b.goalError.setText(r.error.message);
                b.goalError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void save() {
        b.goalError.setVisibility(View.GONE);
        b.goalWeightLayout.setError(null);
        b.goalWeeklyLayout.setError(null);
        String ws = String.valueOf(b.goalWeight.getText()).trim().replace(',', '.');
        String ks = String.valueOf(b.goalWeekly.getText()).trim();
        Double w = null;
        Integer k = null;
        boolean ok = true;
        if (!ws.isEmpty()) {
            try {
                w = Double.parseDouble(ws);
                if (!Bmi.validWeight(w)) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                b.goalWeightLayout.setError(getString(R.string.error_weight_range));
                ok = false;
            }
        }
        if (!ks.isEmpty()) {
            try {
                k = Integer.parseInt(ks);
                if (k < 1 || k > 14) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                b.goalWeeklyLayout.setError(getString(R.string.error_weekly_range));
                ok = false;
            }
        }
        if (String.valueOf(b.goalNote.getText()).trim().length() > 200) {
            b.goalError.setText(R.string.error_too_long);
            b.goalError.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (!ok) return;
        submitted = true;
        vm.updateGoal(w, k, String.valueOf(b.goalNote.getText()));
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
