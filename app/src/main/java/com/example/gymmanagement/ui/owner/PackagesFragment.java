package com.example.gymmanagement.ui.owner;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemOwnerPackageBinding;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.BaseViewModel;
import com.example.gymmanagement.viewmodel.PackagesViewModel;

import java.util.List;

/** F03 – Chủ Gym quản lý gói tập: thêm / sửa / bật-tắt / xóa. */
public class PackagesFragment extends OwnerListFragment<GymPackage> {
    private PackagesViewModel vm;

    @Override
    protected BaseViewModel initViewModel() {
        vm = new ViewModelProvider(this).get(PackagesViewModel.class);
        return vm;
    }

    public PackagesViewModel viewModel() {
        return vm;
    }

    @Override
    protected SimpleAdapter<GymPackage, ?> createAdapter() {
        return new SimpleAdapter<GymPackage, ItemOwnerPackageBinding>(ItemOwnerPackageBinding::inflate, (ib, p, pos) -> {
            ib.pkName.setText(p.name);
            ib.pkPrice.setText(getString(R.string.pkg_price_duration, Fmt.money(p.price), Fmt.duration(p.durationDays)));
            ib.pkDesc.setText(p.description);
            ib.pkDesc.setVisibility(p.description.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
            UiUtil.badge(ib.pkStatus, getString(p.isActive ? R.string.pkg_active : R.string.pkg_inactive),
                    p.isActive ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_NEUTRAL);
            ib.pkSwitch.setOnCheckedChangeListener(null);
            ib.pkSwitch.setChecked(p.isActive);
            ib.pkSwitch.setOnCheckedChangeListener((v, checked) -> {
                if (checked != p.isActive) vm.toggle(p);
            });
            ib.pkEdit.setOnClickListener(v -> PackageEditSheet.newInstance(p).show(getChildFragmentManager(), "pkg"));
            ib.pkDelete.setOnClickListener(v -> Dialogs.confirm(requireContext(), getString(R.string.pkg_delete_title),
                    getString(R.string.pkg_delete_msg, p.name), getString(R.string.action_delete), () -> vm.delete(p)));
        });
    }

    @Override
    protected LiveData<Resource<List<GymPackage>>> source() {
        return vm.list();
    }

    @Override
    protected void reload() {
        vm.load();
    }

    @Override
    protected int emptyTitle() {
        return R.string.pkg_owner_empty_title;
    }

    @Override
    protected int emptyMessage() {
        return R.string.pkg_owner_empty_msg;
    }

    @Override
    protected int headline() {
        return R.string.owner_packages_title;
    }

    @Override
    protected boolean hasFab() {
        return true;
    }

    @Override
    protected void onSetup() {
        b.lsSearchLayout.setVisibility(android.view.View.VISIBLE);
        b.lsSearchLayout.setHint(R.string.pkg_search_hint);
        b.lsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int bf, int c) { }
            public void afterTextChanged(Editable e) { vm.search(e.toString()); }
        });
        b.lsFab.setText(R.string.pkg_add);
        b.lsFab.setOnClickListener(v -> PackageEditSheet.newInstance(null).show(getChildFragmentManager(), "pkg"));
    }
}
