package com.example.gymmanagement.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.ActivityAdminMainBinding;
import com.example.gymmanagement.databinding.ItemAdminGymBinding;
import com.example.gymmanagement.databinding.ItemAdminOwnerBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.auth.LoginActivity;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.AdminViewModel;
import com.google.android.material.chip.Chip;

/**
 * Quản trị nền tảng (chỉ vào được khi tồn tại platformAdmins/{uid}):
 * duyệt/từ chối Chủ Gym và tạm khóa / mở lại thương hiệu.
 */
public class AdminMainActivity extends BaseActivity {
    private ActivityAdminMainBinding b;
    private AdminViewModel vm;
    private boolean showOwners = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        if (!Session.platformAdmin) {
            finish();
            return;
        }
        b = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        vm = new ViewModelProvider(this).get(AdminViewModel.class);
        if (savedInstanceState != null) showOwners = savedInstanceState.getBoolean("owners", true);
        b.adminUser.setText(Session.user == null ? "" : Session.user.email);
        b.adminLogout.setOnClickListener(v -> Dialogs.confirm(this, getString(R.string.action_logout),
                getString(R.string.logout_confirm), getString(R.string.action_logout), () -> {
                    Repos.auth().signOut();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                }));

        SimpleAdapter<User, ItemAdminOwnerBinding> ownerAdapter = new SimpleAdapter<>(ItemAdminOwnerBinding::inflate, (ib, u, pos) -> {
            ib.aoName.setText(u.fullName);
            ib.aoContact.setText(u.email + (u.phone.isEmpty() ? "" : " · " + u.phone));
            ib.aoGym.setText(getString(R.string.admin_proposed_gym, u.proposedGymName));
            ib.aoNote.setVisibility(u.applicationNote.isEmpty() ? View.GONE : View.VISIBLE);
            ib.aoNote.setText(u.applicationNote);
            ib.aoApprove.setOnClickListener(v -> Dialogs.confirm(this, getString(R.string.action_approve),
                    getString(R.string.admin_approve_confirm, u.fullName), getString(R.string.action_approve),
                    () -> vm.review(u, true, "")));
            ib.aoReject.setOnClickListener(v -> Dialogs.input(this, getString(R.string.action_reject),
                    getString(R.string.rq_reject_reason_hint), true, false, getString(R.string.action_reject),
                    text -> vm.review(u, false, text)));
        });
        SimpleAdapter<Gym, ItemAdminGymBinding> gymAdapter = new SimpleAdapter<>(ItemAdminGymBinding::inflate, (ib, g, pos) -> {
            ib.agName.setText(g.name);
            ib.agOwner.setText(getString(R.string.admin_owner_line, g.ownerName));
            ib.agAddress.setText(g.address);
            UiUtil.badge(ib.agStatus, g.statusLabel(), Gym.STATUS_ACTIVE.equals(g.status) ? UiUtil.BADGE_SUCCESS
                    : Gym.STATUS_SUSPENDED.equals(g.status) ? UiUtil.BADGE_PRIMARY : UiUtil.BADGE_NEUTRAL);
            boolean suspended = Gym.STATUS_SUSPENDED.equals(g.status);
            ib.agToggle.setText(suspended ? R.string.admin_reopen : R.string.admin_suspend);
            ib.agToggle.setOnClickListener(v -> Dialogs.confirm(this,
                    getString(suspended ? R.string.admin_reopen : R.string.admin_suspend),
                    getString(suspended ? R.string.admin_reopen_msg : R.string.admin_suspend_msg, g.name),
                    getString(suspended ? R.string.admin_reopen : R.string.admin_suspend), () -> vm.toggleGym(g)));
        });
        b.adminList.setLayoutManager(new LinearLayoutManager(this));

        String[] labels = {getString(R.string.admin_tab_owners), getString(R.string.admin_tab_gyms)};
        for (int i = 0; i < 2; i++) {
            Chip c = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.adminChips, false);
            c.setId(View.generateViewId());
            c.setText(labels[i]);
            c.setTag(i == 0);
            c.setChecked((i == 0) == showOwners);
            b.adminChips.addView(c);
        }
        b.adminChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            View c = g.findViewById(ids.get(0));
            if (c != null) {
                showOwners = (Boolean) c.getTag();
                applyTab(ownerAdapter, gymAdapter);
            }
        });
        b.adminRefresh.setColorSchemeResources(R.color.primary);
        b.adminRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.adminRefresh.setOnRefreshListener(vm::load);

        vm.owners().observe(this, r -> {
            if (r == null || !showOwners) return;
            b.adminRefresh.setRefreshing(false);
            b.adminState.render(r, getString(R.string.admin_owners_empty_title), getString(R.string.admin_owners_empty_msg), vm::load);
            ownerAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.gyms().observe(this, r -> {
            if (r == null || showOwners) return;
            b.adminRefresh.setRefreshing(false);
            b.adminState.render(r, getString(R.string.admin_gyms_empty_title), getString(R.string.admin_gyms_empty_msg), vm::load);
            gymAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(this, ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
            if (r.status == Resource.Status.SUCCESS) vm.load();
        });
        applyTab(ownerAdapter, gymAdapter);
        vm.load();
    }

    private void applyTab(SimpleAdapter<User, ItemAdminOwnerBinding> o, SimpleAdapter<Gym, ItemAdminGymBinding> g) {
        if (showOwners) {
            b.adminList.setAdapter(o);
            Resource<java.util.List<User>> r = vm.owners().getValue();
            if (r != null) {
                b.adminState.render(r, getString(R.string.admin_owners_empty_title), getString(R.string.admin_owners_empty_msg), vm::load);
                o.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
            }
        } else {
            b.adminList.setAdapter(g);
            Resource<java.util.List<Gym>> r = vm.gyms().getValue();
            if (r != null) {
                b.adminState.render(r, getString(R.string.admin_gyms_empty_title), getString(R.string.admin_gyms_empty_msg), vm::load);
                g.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("owners", showOwners);
    }
}
