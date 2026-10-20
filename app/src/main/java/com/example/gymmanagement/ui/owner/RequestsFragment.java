package com.example.gymmanagement.ui.owner;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemOwnerRequestBinding;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.BaseViewModel;
import com.example.gymmanagement.viewmodel.RequestsViewModel;
import com.google.android.material.chip.Chip;

import java.util.List;

/** Chủ Gym duyệt / từ chối yêu cầu đăng ký gói tập của hội viên. */
public class RequestsFragment extends OwnerListFragment<MembershipRequest> {
    private RequestsViewModel vm;

    @Override
    protected BaseViewModel initViewModel() {
        vm = new ViewModelProvider(this).get(RequestsViewModel.class);
        return vm;
    }

    @Override
    protected SimpleAdapter<MembershipRequest, ?> createAdapter() {
        return new SimpleAdapter<MembershipRequest, ItemOwnerRequestBinding>(ItemOwnerRequestBinding::inflate, (ib, r, pos) -> {
            ib.orName.setText(r.memberName);
            String contact = r.memberEmail + (r.memberPhone.isEmpty() ? "" : " · " + r.memberPhone);
            ib.orContact.setText(contact);
            ib.orPackage.setText(getString(R.string.ms_package_line, r.packageName, Fmt.money(r.packagePrice))
                    + " · " + Fmt.duration(r.packageDurationDays));
            ib.orNote.setVisibility(r.note.isEmpty() ? View.GONE : View.VISIBLE);
            ib.orNote.setText(getString(R.string.rq_member_note, r.note));
            boolean showReason = MembershipRequest.REJECTED.equals(r.status) && !r.rejectReason.isEmpty();
            ib.orReason.setVisibility(showReason ? View.VISIBLE : View.GONE);
            ib.orReason.setText(getString(R.string.rq_reject_reason, r.rejectReason));
            ib.orTime.setText(getString(R.string.rq_sent_at, Fmt.dateTime(r.createdAt)));
            int type;
            switch (r.status) {
                case MembershipRequest.APPROVED:
                    type = UiUtil.BADGE_SUCCESS;
                    break;
                case MembershipRequest.PENDING:
                    type = UiUtil.BADGE_WARNING;
                    break;
                case MembershipRequest.REJECTED:
                    type = UiUtil.BADGE_PRIMARY;
                    break;
                default:
                    type = UiUtil.BADGE_NEUTRAL;
            }
            UiUtil.badge(ib.orStatus, r.statusLabel(), type);
            ib.orActions.setVisibility(r.isPending() ? View.VISIBLE : View.GONE);
            ib.orApprove.setOnClickListener(v -> Dialogs.confirm(requireContext(), getString(R.string.action_approve),
                    getString(R.string.rq_approve_confirm, r.memberName, r.packageName),
                    getString(R.string.action_approve), () -> vm.approve(r)));
            ib.orReject.setOnClickListener(v -> Dialogs.input(requireContext(), getString(R.string.action_reject),
                    getString(R.string.rq_reject_reason_hint), true, false, getString(R.string.action_reject),
                    text -> vm.reject(r, text)));
        });
    }

    @Override
    protected LiveData<Resource<List<MembershipRequest>>> source() {
        return vm.list();
    }

    @Override
    protected void reload() {
        vm.load();
    }

    @Override
    protected int emptyTitle() {
        return R.string.rq_owner_empty_title;
    }

    @Override
    protected int emptyMessage() {
        return R.string.rq_owner_empty_msg;
    }

    @Override
    protected int headline() {
        return R.string.owner_requests_title;
    }

    @Override
    protected void onSetup() {
        b.lsChipsScroll.setVisibility(View.VISIBLE);
        if (b.lsChips.getChildCount() > 0) return;
        String[] values = {MembershipRequest.PENDING, MembershipRequest.APPROVED, MembershipRequest.REJECTED, ""};
        int[] labels = {R.string.rq_filter_pending, R.string.rq_filter_approved, R.string.rq_filter_rejected, R.string.filter_all};
        for (int i = 0; i < values.length; i++) {
            Chip c = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.lsChips, false);
            c.setId(View.generateViewId());
            c.setText(labels[i]);
            c.setTag(values[i]);
            c.setChecked(values[i].equals(vm.filter()));
            b.lsChips.addView(c);
        }
        b.lsChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            View c = g.findViewById(ids.get(0));
            if (c != null) vm.setFilter((String) c.getTag());
        });
    }
}
