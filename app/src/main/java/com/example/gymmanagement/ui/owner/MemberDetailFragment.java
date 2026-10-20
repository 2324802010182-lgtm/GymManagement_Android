package com.example.gymmanagement.ui.owner;

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
import com.example.gymmanagement.databinding.FragmentMemberDetailBinding;
import com.example.gymmanagement.databinding.ItemAttendanceBinding;
import com.example.gymmanagement.model.Attendance;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.ui.adapters.MembershipAdapter;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.MembersViewModel;

/** Chi tiết hội viên của Gym: gói tập, lịch sử điểm danh, hủy gói. */
public class MemberDetailFragment extends Fragment {
    public static final String ARG_MEMBER = "member";

    private FragmentMemberDetailBinding b;
    private MembersViewModel vm;
    private MemberSummary member;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentMemberDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.member_detail_title);
        member = (MemberSummary) requireArguments().getSerializable(ARG_MEMBER);
        if (member == null) {
            requireActivity().finish();
            return;
        }
        vm = new ViewModelProvider(this).get(MembersViewModel.class);
        b.mdAvatar.setText(UiUtil.initials(member.fullName));
        b.mdName.setText(member.fullName);
        b.mdEmail.setText(member.email);
        b.mdPhone.setText(member.phone.isEmpty() ? getString(R.string.no_phone) : member.phone);
        b.mdJoined.setText(getString(R.string.member_joined, Fmt.date(member.joinedAt)));

        MembershipAdapter msAdapter = new MembershipAdapter(m -> {
            if (!m.isValidNow()) return;
            Dialogs.input(requireContext(), getString(R.string.member_cancel_title),
                    getString(R.string.member_cancel_reason_hint), true, false,
                    getString(R.string.member_cancel_action), reason -> vm.cancelMembership(m, reason));
        });
        b.mdMsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mdMsList.setAdapter(msAdapter);
        SimpleAdapter<Attendance, ItemAttendanceBinding> atAdapter = new SimpleAdapter<>(ItemAttendanceBinding::inflate, (ib, a, pos) -> {
            ib.atName.setText(Fmt.dayLabel(a.checkInAt));
            ib.atPackage.setText(a.packageName);
            ib.atTime.setText(getString(R.string.attendance_time_by, Fmt.time(a.checkInAt), a.recordedByName));
            ib.atDelete.setVisibility(View.GONE);
        });
        b.mdAtList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mdAtList.setAdapter(atAdapter);

        vm.memberships().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.mdMsState.render(r, getString(R.string.member_ms_empty), "", () -> vm.loadDetail(member.memberUid));
            msAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.attendance().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.mdAtState.render(r, getString(R.string.member_at_empty), "", () -> vm.loadDetail(member.memberUid));
            atAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
            if (r.status == Resource.Status.SUCCESS) vm.loadDetail(member.memberUid);
        });
        if (vm.memberships().getValue() == null) vm.loadDetail(member.memberUid);
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
