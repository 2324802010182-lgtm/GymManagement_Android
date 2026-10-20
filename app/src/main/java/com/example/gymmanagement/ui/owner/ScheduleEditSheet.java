package com.example.gymmanagement.ui.owner;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.SheetScheduleEditBinding;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.OwnerScheduleViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Tạo / sửa buổi tập; khi tạo mới phải chọn hội viên còn hạn. */
public class ScheduleEditSheet extends BottomSheetDialogFragment {
    public static ScheduleEditSheet newInstance(@Nullable Schedule s) {
        ScheduleEditSheet f = new ScheduleEditSheet();
        Bundle a = new Bundle();
        if (s != null) {
            a.putString("id", s.id);
            a.putString("title", s.title);
            a.putString("desc", s.description);
            a.putString("loc", s.location);
            a.putString("memberName", s.memberName);
            a.putString("memberUid", s.memberUid);
            a.putString("gymId", s.gymId);
            a.putString("gymName", s.gymName);
            a.putLong("start", s.startAt == null ? 0 : s.startAt.getTime());
            a.putLong("end", s.endAt == null ? 0 : s.endAt.getTime());
        }
        f.setArguments(a);
        return f;
    }

    private SheetScheduleEditBinding b;
    private OwnerScheduleViewModel vm;
    private boolean submitted;
    private MemberSummary member;
    private long startMs, endMs;
    private String id = "";
    private Schedule editing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetScheduleEditBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireParentFragment()).get(OwnerScheduleViewModel.class);
        Bundle a = requireArguments();
        id = a.getString("id", "");
        boolean edit = !id.isEmpty();
        b.seTitle.setText(edit ? R.string.sched_edit : R.string.sched_add);
        if (savedInstanceState != null) {
            startMs = savedInstanceState.getLong("start");
            endMs = savedInstanceState.getLong("end");
            String uid = savedInstanceState.getString("memberUid");
            if (uid != null) member = findMember(uid);
        } else if (edit) {
            startMs = a.getLong("start");
            endMs = a.getLong("end");
            b.seWhat.setText(a.getString("title"));
            b.seDesc.setText(a.getString("desc"));
            b.seLocation.setText(a.getString("loc"));
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 1);
            c.set(Calendar.HOUR_OF_DAY, 18);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            startMs = c.getTimeInMillis();
            endMs = startMs + 60 * 60 * 1000L;
        }
        if (edit) {
            b.seMember.setText(a.getString("memberName"));
            b.seMember.setEnabled(false);
        } else {
            refreshMemberButton();
            b.seMember.setOnClickListener(v -> pickMember());
        }
        refreshTimes();
        b.seDate.setOnClickListener(v -> pickDate());
        b.seStart.setOnClickListener(v -> pickTime(true));
        b.seEnd.setOnClickListener(v -> pickTime(false));
        b.seCancel.setOnClickListener(v -> dismiss());
        b.seSave.setOnClickListener(v -> save());
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.seProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.seSave.setEnabled(!loading);
            if (loading) return;
            if (r.status == Resource.Status.SUCCESS) {
                dismiss();
            } else {
                submitted = false;
                b.seError.setText(r.error.message);
                b.seError.setVisibility(View.VISIBLE);
            }
        });
    }

    private MemberSummary findMember(String uid) {
        Resource<List<MemberSummary>> r = vm.members().getValue();
        if (r != null && r.data != null) for (MemberSummary m : r.data) if (m.memberUid.equals(uid)) return m;
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("start", startMs);
        out.putLong("end", endMs);
        if (member != null) out.putString("memberUid", member.memberUid);
    }

    private void refreshMemberButton() {
        b.seMember.setText(member == null ? getString(R.string.sched_pick_member) : member.fullName);
    }

    private void refreshTimes() {
        b.seDate.setText(Fmt.dayLabel(new Date(startMs)));
        b.seStart.setText(getString(R.string.sched_from, Fmt.time(new Date(startMs))));
        b.seEnd.setText(getString(R.string.sched_to, Fmt.time(new Date(endMs))));
    }

    private void pickMember() {
        Resource<List<MemberSummary>> r = vm.members().getValue();
        List<MemberSummary> eligible = new ArrayList<>();
        if (r != null && r.data != null) for (MemberSummary m : r.data) if (m.isActiveNow()) eligible.add(m);
        if (eligible.isEmpty()) {
            b.seError.setText(R.string.sched_no_eligible);
            b.seError.setVisibility(View.VISIBLE);
            return;
        }
        String[] names = new String[eligible.size()];
        for (int i = 0; i < names.length; i++) names[i] = eligible.get(i).fullName + " — " + eligible.get(i).latestPackageName;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.sched_pick_member)
                .setItems(names, (d, which) -> {
                    member = eligible.get(which);
                    refreshMemberButton();
                }).show();
    }

    private void pickDate() {
        MaterialDatePicker<Long> p = MaterialDatePicker.Builder.datePicker().setSelection(startMs).build();
        p.addOnPositiveButtonClickListener(sel -> {
            Calendar picked = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(sel);
            long dur = endMs - startMs;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(startMs);
            c.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH));
            startMs = c.getTimeInMillis();
            endMs = startMs + dur;
            refreshTimes();
        });
        p.show(getChildFragmentManager(), "sdate");
    }

    private void pickTime(boolean isStart) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(isStart ? startMs : endMs);
        new TimePickerDialog(requireContext(), (tp, h, m) -> {
            Calendar base = Calendar.getInstance();
            base.setTimeInMillis(startMs);
            base.set(Calendar.HOUR_OF_DAY, h);
            base.set(Calendar.MINUTE, m);
            base.set(Calendar.SECOND, 0);
            base.set(Calendar.MILLISECOND, 0);
            if (isStart) {
                long dur = endMs - startMs;
                startMs = base.getTimeInMillis();
                endMs = startMs + Math.max(dur, 15 * 60 * 1000L);
            } else {
                endMs = base.getTimeInMillis();
            }
            refreshTimes();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void save() {
        b.seError.setVisibility(View.GONE);
        b.seWhatLayout.setError(null);
        String title = String.valueOf(b.seWhat.getText()).trim();
        String loc = String.valueOf(b.seLocation.getText()).trim();
        String desc = String.valueOf(b.seDesc.getText()).trim();
        boolean edit = !id.isEmpty();
        if (!edit && member == null) {
            b.seError.setText(R.string.sched_pick_member_first);
            b.seError.setVisibility(View.VISIBLE);
            return;
        }
        if (title.length() < 2 || title.length() > 80) {
            b.seWhatLayout.setError(getString(R.string.error_sched_title));
            return;
        }
        if (desc.length() > 500 || loc.length() > 80) {
            b.seError.setText(R.string.error_sched_fields_long);
            b.seError.setVisibility(View.VISIBLE);
            return;
        }
        if (endMs <= startMs) {
            b.seError.setText(R.string.error_sched_time);
            b.seError.setVisibility(View.VISIBLE);
            return;
        }
        if (!edit && startMs < System.currentTimeMillis() - 60_000L) {
            b.seError.setText(R.string.error_sched_past);
            b.seError.setVisibility(View.VISIBLE);
            return;
        }
        submitted = true;
        if (edit) {
            Schedule s = new Schedule();
            Bundle a = requireArguments();
            s.id = id;
            s.gymId = a.getString("gymId", "");
            s.gymName = a.getString("gymName", "");
            s.memberUid = a.getString("memberUid", "");
            s.memberName = a.getString("memberName", "");
            vm.update(s, title, desc, loc, new Date(startMs), new Date(endMs));
        } else {
            vm.create(member, title, desc, loc, new Date(startMs), new Date(endMs));
        }
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
