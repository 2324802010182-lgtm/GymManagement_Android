package com.example.gymmanagement.ui.shared;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentProfileBinding;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.auth.ForgotPasswordActivity;
import com.example.gymmanagement.ui.auth.LoginActivity;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.UiUtil;

import java.util.List;

/** Hồ sơ hội viên và chủ phòng tập. */
public class ProfileFragment extends Fragment {
    private static final int MENU_NOTIFICATIONS = 1;
    private static final int MENU_MY_GYMS = 2;
    private static final int MENU_MEASUREMENTS = 3;
    private static final int MENU_OWNER_BRAND = 4;
    private static final int MENU_OWNER_SCHEDULES = 5;
    private static final int MENU_OWNER_ATTENDANCE = 6;
    private static final int MENU_OWNER_ANNOUNCEMENTS = 7;

    protected FragmentProfileBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtil.applyInsets(b.pfScroll, false, true);
        bindHeader();
        b.pfVersion.setText(getString(R.string.profile_brand_version, appVersion()));
        b.pfBack.setOnClickListener(v -> returnToMainTab());
        b.pfMenu.setOnClickListener(this::showMoreActions);
        b.pfEdit.setOnClickListener(v -> openEditProfile());
        b.pfAvatarEdit.setOnClickListener(v -> openEditProfile());
        b.pfPhoneRow.setOnClickListener(v -> openEditProfile());
        b.pfPasswordRow.setOnClickListener(v -> openPasswordReset());
        b.pfLogoutRow.setOnClickListener(v -> confirmLogout());
        b.pfMembershipCard.setOnClickListener(v ->
                HostActivity.open(requireContext(), HostActivity.MY_GYMS, null));
    }

    private String appVersion() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b == null) return;
        bindHeader();
        bindMembership();
    }

    private void bindHeader() {
        User u = Session.user;
        if (u == null || b == null) return;
        b.pfAvatar.setText(UiUtil.initials(u.fullName));
        b.pfName.setText(u.fullName);
        b.pfEmail.setText(u.email);
        b.pfEmailValue.setText(u.email);
        b.pfRole.setText(u.roleLabel());
        b.pfPhone.setText(u.phone == null || u.phone.trim().isEmpty()
                ? getString(R.string.profile_no_phone_value) : u.phone);
        b.pfMembershipCard.setVisibility(u.isMember() ? View.VISIBLE : View.GONE);
    }

    private void bindMembership() {
        User u = Session.user;
        if (b == null || u == null || !u.isMember()) return;
        b.pfGymName.setText(R.string.profile_no_registered_gym);
        b.pfPackageName.setText(R.string.profile_no_current_package);
        b.pfMembershipDetails.setVisibility(View.GONE);
        b.pfPackageLabel.setVisibility(View.GONE);
        Repos.memberships().listMyMemberships(new Callback<List<Membership>>() {
            @Override
            public void onSuccess(List<Membership> memberships) {
                if (b == null || !isAdded()) return;
                Membership current = selectMembership(memberships);
                if (current == null) return;
                b.pfGymName.setText(nonEmpty(current.gymName, getString(R.string.profile_no_registered_gym)));
                b.pfPackageName.setText(nonEmpty(current.packageName,
                        getString(R.string.profile_no_current_package)));
                b.pfPackageLabel.setVisibility(View.VISIBLE);
                b.pfMembershipDetails.setVisibility(View.VISIBLE);
                b.pfMembershipUntil.setText(getString(R.string.profile_until_date, Fmt.date(current.endAt)));
                bindMembershipStatus(current);
            }

            @Override
            public void onError(AppError error) {
                // Keep the empty state when membership information cannot be loaded.
            }
        });
    }

    @Nullable
    private Membership selectMembership(List<Membership> memberships) {
        if (memberships == null || memberships.isEmpty()) return null;
        for (Membership membership : memberships) {
            if (membership.isValidNow()) return membership;
        }
        return memberships.get(0);
    }

    private void bindMembershipStatus(Membership membership) {
        boolean active = membership.isValidNow();
        int color = ContextCompat.getColor(requireContext(), active ? R.color.success
                : Membership.CANCELLED.equals(membership.status) ? R.color.danger : R.color.warning);
        b.pfMembershipStatus.setText(membership.statusLabel());
        b.pfMembershipStatus.setTextColor(color);
        b.pfStatusDot.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void showMoreActions(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, MENU_NOTIFICATIONS, 0, R.string.profile_notifications);
        User user = Session.user;
        if (user != null && user.isOwner()) {
            menu.getMenu().add(0, MENU_OWNER_BRAND, 1, R.string.owner_brand_title);
            menu.getMenu().add(0, MENU_OWNER_SCHEDULES, 2, R.string.owner_schedules_title);
            menu.getMenu().add(0, MENU_OWNER_ATTENDANCE, 3, R.string.owner_attendance_title);
            menu.getMenu().add(0, MENU_OWNER_ANNOUNCEMENTS, 4, R.string.owner_announce_title);
        } else {
            menu.getMenu().add(0, MENU_MY_GYMS, 1, R.string.profile_my_gyms);
            menu.getMenu().add(0, MENU_MEASUREMENTS, 2, R.string.profile_measurements);
        }
        menu.setOnMenuItemClickListener(this::onMoreActionSelected);
        menu.show();
    }

    private boolean onMoreActionSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NOTIFICATIONS:
                HostActivity.open(requireContext(), HostActivity.NOTIFICATIONS, null);
                return true;
            case MENU_MY_GYMS:
                HostActivity.open(requireContext(), HostActivity.MY_GYMS, null);
                return true;
            case MENU_MEASUREMENTS:
                HostActivity.open(requireContext(), HostActivity.MEASUREMENTS, null);
                return true;
            case MENU_OWNER_BRAND:
                HostActivity.open(requireContext(), HostActivity.BRAND, null);
                return true;
            case MENU_OWNER_SCHEDULES:
                HostActivity.open(requireContext(), HostActivity.SCHEDULES, null);
                return true;
            case MENU_OWNER_ATTENDANCE:
                HostActivity.open(requireContext(), HostActivity.ATTENDANCE, null);
                return true;
            case MENU_OWNER_ANNOUNCEMENTS:
                HostActivity.open(requireContext(), HostActivity.OWNER_NOTIFICATIONS, null);
                return true;
            default:
                return false;
        }
    }

    private void returnToMainTab() {
        View memberNav = requireActivity().findViewById(R.id.member_nav);
        if (memberNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            ((com.google.android.material.bottomnavigation.BottomNavigationView) memberNav)
                    .setSelectedItemId(R.id.nav_home);
            return;
        }
        View ownerNav = requireActivity().findViewById(R.id.owner_nav);
        if (ownerNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            ((com.google.android.material.bottomnavigation.BottomNavigationView) ownerNav)
                    .setSelectedItemId(R.id.nav_dashboard);
            return;
        }
        requireActivity().finish();
    }

    private void openEditProfile() {
        HostActivity.open(requireContext(), HostActivity.EDIT_PROFILE, null);
    }

    private void openPasswordReset() {
        Intent intent = new Intent(requireContext(), ForgotPasswordActivity.class);
        if (Session.user != null) intent.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, Session.user.email);
        startActivity(intent);
    }

    private void confirmLogout() {
        Dialogs.confirm(requireContext(), getString(R.string.action_logout), getString(R.string.logout_confirm),
                getString(R.string.action_logout), () -> {
                    Repos.auth().signOut();
                    Intent i = new Intent(requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                });
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
