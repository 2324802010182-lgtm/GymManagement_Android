package com.example.gymmanagement.ui.owner;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemScheduleBinding;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.ui.adapters.ScheduleAdapter;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.BaseViewModel;
import com.example.gymmanagement.viewmodel.OwnerScheduleViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** F04 – Chủ Gym tạo lịch tập cho hội viên, sửa, hoàn thành hoặc hủy. */
public class OwnerScheduleFragment extends OwnerListFragment<Schedule> {
    private OwnerScheduleViewModel vm;

    public OwnerScheduleViewModel viewModel() {
        return vm;
    }

    @Override
    protected BaseViewModel initViewModel() {
        vm = new ViewModelProvider(this).get(OwnerScheduleViewModel.class);
        return vm;
    }

    @Override
    protected SimpleAdapter<Schedule, ?> createAdapter() {
        return new ScheduleAdapter(true, s -> {
            if (!Schedule.SCHEDULED.equals(s.status)) return;
            String[] items = {getString(R.string.action_edit), getString(R.string.sched_mark_done),
                    getString(R.string.sched_cancel)};
            new MaterialAlertDialogBuilder(requireContext()).setTitle(s.title)
                    .setItems(items, (d, which) -> {
                        if (which == 0) ScheduleEditSheet.newInstance(s).show(getChildFragmentManager(), "sched");
                        else if (which == 1) vm.complete(s);
                        else com.example.gymmanagement.util.Dialogs.confirm(requireContext(),
                                    getString(R.string.sched_cancel), getString(R.string.sched_cancel_msg, s.title),
                                    getString(R.string.sched_cancel), () -> vm.cancel(s));
                    }).show();
        });
    }

    @Override
    protected LiveData<Resource<List<Schedule>>> source() {
        return vm.list();
    }

    @Override
    protected void reload() {
        vm.load();
    }

    @Override
    protected int emptyTitle() {
        return R.string.sched_owner_empty_title;
    }

    @Override
    protected int emptyMessage() {
        return R.string.sched_owner_empty_msg;
    }

    @Override
    protected int hostTitle() {
        return R.string.owner_schedules_title;
    }

    @Override
    protected boolean hasFab() {
        return true;
    }

    @Override
    protected void onSetup() {
        b.lsFab.setText(R.string.sched_add);
        b.lsFab.setOnClickListener(v -> ScheduleEditSheet.newInstance(null).show(getChildFragmentManager(), "sched"));
    }
}
