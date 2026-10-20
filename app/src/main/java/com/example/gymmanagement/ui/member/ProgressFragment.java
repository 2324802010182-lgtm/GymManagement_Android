package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentProgressBinding;
import com.example.gymmanagement.databinding.ViewStatCardBinding;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.model.WorkoutLog;
import com.example.gymmanagement.ui.components.ChartViews;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.DateKeys;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.ProgressViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** F07 – tiến độ tập luyện: số liệu thật từ workoutLogs, measurements, attendance. */
public class ProgressFragment extends Fragment {
    private FragmentProgressBinding b;
    private ProgressViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentProgressBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(ProgressViewModel.class);

        int[] ranges = {ProgressViewModel.RANGE_7, ProgressViewModel.RANGE_30, ProgressViewModel.RANGE_90, ProgressViewModel.RANGE_ALL};
        int[] labels = {R.string.prog_range_7, R.string.prog_range_30, R.string.prog_range_90, R.string.prog_range_all};
        for (int i = 0; i < ranges.length; i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.progRanges, false);
            chip.setText(labels[i]);
            chip.setId(View.generateViewId());
            chip.setTag(ranges[i]);
            chip.setChecked(ranges[i] == vm.range());
            b.progRanges.addView(chip);
        }
        b.progRanges.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            View c = group.findViewById(ids.get(0));
            if (c != null) vm.setRange((Integer) c.getTag());
        });

        label(b.progCardSessions, R.string.prog_sessions);
        label(b.progCardMinutes, R.string.prog_minutes);
        label(b.progCardAttendance, R.string.prog_attendance);
        label(b.progCardWeight, R.string.prog_weight);

        b.progRefresh.setColorSchemeResources(R.color.primary);
        b.progRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.progRefresh.setOnRefreshListener(vm::load);
        b.progMeasure.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.MEASUREMENTS, null));
        b.progGoalEdit.setOnClickListener(v -> new GoalSheet().show(getChildFragmentManager(), "goal"));
        getChildFragmentManager().setFragmentResultListener(GoalSheet.RESULT, this, (k, r) -> renderGoal(null));

        vm.summary().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.progRefresh.setRefreshing(false);
            b.progState.render(r, getString(R.string.prog_empty_title), getString(R.string.prog_empty_msg), vm::load);
            if (r.status == Resource.Status.EMPTY) {
                // Không có dữ liệu nhưng vẫn cho phép đặt mục tiêu/nhập chỉ số
                b.progState.showEmpty(getString(R.string.prog_empty_title), getString(R.string.prog_empty_msg));
            }
            if (r.status == Resource.Status.SUCCESS) bind(r.data);
        });
        vm.load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null && vm.summary().getValue() != null
                && vm.summary().getValue().status != Resource.Status.LOADING) vm.load();
    }

    private void label(ViewStatCardBinding c, int res) {
        c.statLabel.setText(res);
    }

    private void bind(ProgressViewModel.Summary s) {
        Motion.countUp(b.progCardSessions.statValue, s.sessions);
        Motion.countUp(b.progCardMinutes.statValue, s.minutes);
        Motion.countUp(b.progCardAttendance.statValue, s.attendanceCount);
        b.progCardWeight.statValue.setText(s.latestWeight == null ? "—" : Fmt.number(s.latestWeight, 1));

        // Biểu đồ cân nặng
        List<ChartViews.Point> pts = new ArrayList<>();
        for (Measurement m : s.weightSeries) {
            if (m.measuredAt == null) continue;
            pts.add(new ChartViews.Point(m.measuredAt.getTime(), (float) m.weightKg, Fmt.date(m.measuredAt)));
        }
        boolean enoughWeight = pts.size() >= 2;
        b.progWeightChart.setVisibility(enoughWeight ? View.VISIBLE : View.GONE);
        b.progWeightEmpty.setVisibility(enoughWeight ? View.GONE : View.VISIBLE);
        b.progWeightEmpty.setText(pts.isEmpty() ? R.string.prog_weight_empty : R.string.prog_weight_need_more);
        if (enoughWeight) b.progWeightChart.setPoints(pts);

        // Biểu đồ số buổi tập: theo ngày (7 ngày) hoặc theo tuần
        List<ChartViews.Point> bars = buckets(s);
        b.progBarChart.setVisibility(bars.isEmpty() ? View.GONE : View.VISIBLE);
        b.progBarEmpty.setVisibility(bars.isEmpty() ? View.VISIBLE : View.GONE);
        b.progBarTitle.setText(vm.range() == ProgressViewModel.RANGE_7 ? R.string.prog_bar_by_day : R.string.prog_bar_by_week);
        if (!bars.isEmpty()) b.progBarChart.setBars(bars);
        renderGoal(s);
    }

    private List<ChartViews.Point> buckets(ProgressViewModel.Summary s) {
        List<ChartViews.Point> out = new ArrayList<>();
        if (s.logsInRange.isEmpty()) return out;
        boolean daily = vm.range() == ProgressViewModel.RANGE_7;
        int n = daily ? 7 : vm.range() == ProgressViewModel.RANGE_30 ? 5 : vm.range() == ProgressViewModel.RANGE_90 ? 13 : 12;
        int step = daily ? 1 : 7;
        Date today = DateKeys.startOfDay(new Date());
        int[] counts = new int[n];
        for (WorkoutLog w : s.logsInRange) {
            if (w.completedAt == null) continue;
            long diffDays = (today.getTime() - DateKeys.startOfDay(w.completedAt).getTime()) / 86400000L;
            int idx = (int) (diffDays / step);
            if (idx >= 0 && idx < n) counts[n - 1 - idx]++;
        }
        for (int i = 0; i < n; i++) {
            Date d = DateKeys.addDays(today, -(n - 1 - i) * step);
            String lbl = new java.text.SimpleDateFormat(daily ? "dd/MM" : "dd/MM", java.util.Locale.US).format(d);
            out.add(new ChartViews.Point(i, counts[i], lbl));
        }
        return out;
    }

    private void renderGoal(@Nullable ProgressViewModel.Summary s) {
        if (b == null) return;
        User u = Session.user;
        Integer weekly = u == null ? null : u.goalWeeklyWorkouts;
        Double target = u == null ? null : u.goalTargetWeightKg;
        ProgressViewModel.Summary sum = s != null ? s : null;
        if (weekly == null && target == null) {
            b.progGoalText.setText(R.string.prog_goal_none);
            b.progGoalBar.setVisibility(View.GONE);
            b.progGoalWeight.setVisibility(View.GONE);
            return;
        }
        if (weekly != null && weekly > 0) {
            int done = sum == null ? 0 : sum.thisWeekSessions;
            b.progGoalText.setText(getString(R.string.prog_goal_weekly, done, weekly));
            b.progGoalBar.setVisibility(View.VISIBLE);
            b.progGoalBar.setMax(weekly);
            b.progGoalBar.setProgressCompat(Math.min(done, weekly), !Motion.reduced(requireContext()));
        } else {
            b.progGoalText.setText(R.string.prog_goal_no_weekly);
            b.progGoalBar.setVisibility(View.GONE);
        }
        if (target != null) {
            b.progGoalWeight.setVisibility(View.VISIBLE);
            Double cur = sum == null ? null : sum.latestWeight;
            b.progGoalWeight.setText(cur == null
                    ? getString(R.string.prog_goal_target_only, Fmt.number(target, 1))
                    : getString(R.string.prog_goal_target, Fmt.number(cur, 1), Fmt.number(target, 1),
                    Fmt.number(Math.abs(cur - target), 1)));
        } else {
            b.progGoalWeight.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
