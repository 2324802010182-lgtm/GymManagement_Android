package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.FragmentMemberScheduleBinding;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.ui.adapters.ScheduleAdapter;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.MemberScheduleViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** F04 – lịch tập cá nhân của hội viên, xem theo ngày hoặc tuần. */
public class MemberScheduleFragment extends Fragment {
    private FragmentMemberScheduleBinding b;
    private MemberScheduleViewModel vm;
    private ScheduleAdapter adapter;
    private boolean weekMode;
    private final Calendar anchor = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentMemberScheduleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(MemberScheduleViewModel.class);
        anchor.setFirstDayOfWeek(Calendar.MONDAY);
        adapter = new ScheduleAdapter(false, this::showDetail);
        b.schedList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.schedList.setAdapter(adapter);
        b.schedMode.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            weekMode = id == R.id.sched_mode_week;
            render();
        });
        b.schedPrev.setOnClickListener(v -> shift(-1));
        b.schedNext.setOnClickListener(v -> shift(1));
        vm.list().observe(getViewLifecycleOwner(), r -> render());
        if (vm.list().getValue() == null) vm.load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm.list().getValue() != null) vm.load();
    }

    private void shift(int dir) {
        anchor.add(Calendar.DAY_OF_YEAR, weekMode ? 7 * dir : dir);
        render();
    }

    private Date[] range() {
        Calendar c = (Calendar) anchor.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (weekMode) {
            int dow = c.get(Calendar.DAY_OF_WEEK);
            int back = dow == Calendar.SUNDAY ? 6 : dow - Calendar.MONDAY;
            c.add(Calendar.DAY_OF_YEAR, -back);
        }
        Date from = c.getTime();
        c.add(Calendar.DAY_OF_YEAR, weekMode ? 7 : 1);
        return new Date[]{from, c.getTime()};
    }

    private void render() {
        if (b == null) return;
        Date[] r = range();
        SimpleDateFormat f = new SimpleDateFormat("dd/MM", Locale.US);
        if (weekMode) {
            Date last = new Date(r[1].getTime() - 1);
            b.schedLabel.setText(getString(R.string.sched_week_label, f.format(r[0]), f.format(last)));
        } else {
            b.schedLabel.setText(Fmt.dayLabel(r[0]));
        }
        Resource<List<Schedule>> res = vm.list().getValue();
        if (res == null) return;
        if (res.status != Resource.Status.SUCCESS) {
            b.schedState.render(res, getString(R.string.sched_empty_all_title), getString(R.string.sched_empty_all_msg), vm::load);
            adapter.setItems(null);
            return;
        }
        List<Schedule> inRange = new ArrayList<>();
        for (Schedule s : res.data) {
            if (s.startAt != null && !s.startAt.before(r[0]) && s.startAt.before(r[1])) inRange.add(s);
        }
        if (inRange.isEmpty()) {
            b.schedState.showEmpty(getString(R.string.sched_empty_range_title), getString(R.string.sched_empty_range_msg));
        } else {
            b.schedState.showContent();
        }
        adapter.setItems(inRange);
    }

    private void showDetail(Schedule s) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.sched_detail_time, Fmt.dateTime(s.startAt), Fmt.time(s.endAt))).append('\n');
        sb.append(getString(R.string.sched_detail_gym, s.gymName)).append('\n');
        if (!s.location.isEmpty()) sb.append(getString(R.string.sched_detail_location, s.location)).append('\n');
        sb.append(getString(R.string.sched_detail_status, s.statusLabel()));
        if (!s.description.isEmpty()) sb.append("\n\n").append(s.description);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(s.title)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.action_close, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
