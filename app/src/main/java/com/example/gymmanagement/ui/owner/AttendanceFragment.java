package com.example.gymmanagement.ui.owner;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemAttendanceBinding;
import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.AttendanceViewModel;
import com.example.gymmanagement.viewmodel.BaseViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Điểm danh: xem lượt hôm nay / gần đây, điểm danh hội viên còn hạn (mỗi ngày một lượt). */
public class AttendanceFragment extends OwnerListFragment<Attendance> {
    private AttendanceViewModel vm;
    private boolean showToday = true;
    private final MediatorLiveData<Resource<List<Attendance>>> view = new MediatorLiveData<>();

    @Override
    protected BaseViewModel initViewModel() {
        vm = new ViewModelProvider(this).get(AttendanceViewModel.class);
        view.addSource(vm.today(), r -> publish());
        view.addSource(vm.recent(), r -> publish());
        return vm;
    }

    private void publish() {
        view.setValue(showToday ? vm.today().getValue() : vm.recent().getValue());
    }

    @Override
    protected SimpleAdapter<Attendance, ?> createAdapter() {
        return new SimpleAdapter<Attendance, ItemAttendanceBinding>(ItemAttendanceBinding::inflate, (ib, a, pos) -> {
            ib.atName.setText(a.memberName);
            ib.atPackage.setText(a.packageName);
            ib.atTime.setText(getString(R.string.attendance_time_by, Fmt.dateTime(a.checkInAt), a.recordedByName));
            ib.atDelete.setVisibility(View.VISIBLE);
            ib.atDelete.setOnClickListener(v -> Dialogs.confirm(requireContext(), getString(R.string.attendance_delete_title),
                    getString(R.string.attendance_delete_msg, a.memberName), getString(R.string.action_delete),
                    () -> vm.delete(a)));
        });
    }

    @Override
    protected LiveData<Resource<List<Attendance>>> source() {
        return view;
    }

    @Override
    protected void reload() {
        vm.load();
    }

    @Override
    protected int emptyTitle() {
        return showToday ? R.string.attendance_empty_today_title : R.string.attendance_empty_title;
    }

    @Override
    protected int emptyMessage() {
        return showToday ? R.string.attendance_empty_today_msg : R.string.attendance_empty_msg;
    }

    @Override
    protected int hostTitle() {
        return R.string.owner_attendance_title;
    }

    @Override
    protected boolean hasFab() {
        return true;
    }

    @Override
    protected void onSetup() {
        b.lsChipsScroll.setVisibility(View.VISIBLE);
        if (b.lsChips.getChildCount() == 0) {
            int[] labels = {R.string.attendance_today, R.string.attendance_recent};
            for (int i = 0; i < labels.length; i++) {
                Chip c = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.lsChips, false);
                c.setId(View.generateViewId());
                c.setText(labels[i]);
                c.setTag(i == 0);
                c.setChecked((i == 0) == showToday);
                b.lsChips.addView(c);
            }
            b.lsChips.setOnCheckedStateChangeListener((g, ids) -> {
                if (ids.isEmpty()) return;
                View c = g.findViewById(ids.get(0));
                if (c != null) {
                    showToday = (Boolean) c.getTag();
                    publish();
                }
            });
        }
        b.lsFab.setText(R.string.attendance_check_in);
        b.lsFab.setOnClickListener(v -> pickMember());
    }

    private void pickMember() {
        Resource<List<MemberSummary>> r = vm.members().getValue();
        if (r == null || r.status == Resource.Status.LOADING) {
            UiUtil.snack(b.getRoot(), getString(R.string.loading_wait));
            return;
        }
        if (r.status != Resource.Status.SUCCESS) {
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.ERROR ? r.error.message : getString(R.string.members_empty_msg));
            return;
        }
        List<MemberSummary> eligible = new ArrayList<>();
        for (MemberSummary m : r.data) if (m.isActiveNow()) eligible.add(m);
        if (eligible.isEmpty()) {
            UiUtil.snack(b.getRoot(), getString(R.string.attendance_no_eligible));
            return;
        }
        String[] names = new String[eligible.size()];
        for (int i = 0; i < names.length; i++) {
            MemberSummary m = eligible.get(i);
            names[i] = m.fullName + " — " + m.latestPackageName;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.attendance_pick_member)
                .setItems(names, (d, which) -> vm.checkIn(eligible.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
