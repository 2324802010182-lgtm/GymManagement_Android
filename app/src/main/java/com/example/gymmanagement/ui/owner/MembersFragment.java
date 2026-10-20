package com.example.gymmanagement.ui.owner;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemOwnerMemberBinding;
import com.example.gymmanagement.model.MemberSummary;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.BaseViewModel;
import com.example.gymmanagement.viewmodel.MembersViewModel;
import com.google.android.material.chip.Chip;

import java.util.List;

/** F09 – danh sách hội viên của Gym: tìm kiếm + lọc còn hạn / hết hạn. */
public class MembersFragment extends OwnerListFragment<MemberSummary> {
    private MembersViewModel vm;

    @Override
    protected BaseViewModel initViewModel() {
        vm = new ViewModelProvider(this).get(MembersViewModel.class);
        return vm;
    }

    @Override
    protected SimpleAdapter<MemberSummary, ?> createAdapter() {
        return new SimpleAdapter<MemberSummary, ItemOwnerMemberBinding>(ItemOwnerMemberBinding::inflate, (ib, m, pos) -> {
            ib.omAvatar.setText(UiUtil.initials(m.fullName));
            ib.omName.setText(m.fullName);
            ib.omContact.setText(m.email + (m.phone.isEmpty() ? "" : " · " + m.phone));
            ib.omPackage.setText(m.latestPackageName.isEmpty() ? getString(R.string.member_no_package)
                    : getString(R.string.member_package_until, m.latestPackageName, Fmt.date(m.latestEndAt)));
            UiUtil.badge(ib.omStatus, getString(m.isActiveNow() ? R.string.member_active : R.string.member_expired),
                    m.isActiveNow() ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_WARNING);
            ib.getRoot().setOnClickListener(v -> {
                Bundle a = new Bundle();
                a.putSerializable(MemberDetailFragment.ARG_MEMBER, m);
                HostActivity.open(requireContext(), HostActivity.MEMBER_DETAIL, a);
            });
        });
    }

    @Override
    protected LiveData<Resource<List<MemberSummary>>> source() {
        return vm.list();
    }

    @Override
    protected void reload() {
        vm.load();
    }

    @Override
    protected int emptyTitle() {
        return R.string.members_empty_title;
    }

    @Override
    protected int emptyMessage() {
        return R.string.members_empty_msg;
    }

    @Override
    protected int headline() {
        return R.string.owner_members_title;
    }

    @Override
    protected void onSetup() {
        b.lsSearchLayout.setVisibility(View.VISIBLE);
        b.lsSearchLayout.setHint(R.string.members_search_hint);
        b.lsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int bf, int c) { }
            public void afterTextChanged(Editable e) { vm.search(e.toString()); }
        });
        b.lsChipsScroll.setVisibility(View.VISIBLE);
        if (b.lsChips.getChildCount() > 0) return;
        int[] values = {MembersViewModel.FILTER_ALL, MembersViewModel.FILTER_ACTIVE, MembersViewModel.FILTER_EXPIRED};
        int[] labels = {R.string.filter_all, R.string.member_active, R.string.member_expired};
        for (int i = 0; i < values.length; i++) {
            Chip c = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.lsChips, false);
            c.setId(View.generateViewId());
            c.setText(labels[i]);
            c.setTag(values[i]);
            c.setChecked(i == 0);
            b.lsChips.addView(c);
        }
        b.lsChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            View c = g.findViewById(ids.get(0));
            if (c != null) vm.setFilter((Integer) c.getTag());
        });
    }
}
