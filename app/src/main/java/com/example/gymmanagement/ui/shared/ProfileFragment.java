package com.example.gymmanagement.ui.shared;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentProfileBinding;
import com.example.gymmanagement.databinding.ItemProfileRowBinding;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.auth.LoginActivity;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.UiUtil;

/**
 * Hồ sơ cá nhân + lối tắt. Lớp con (OwnerMoreFragment) bổ sung các mục của chủ gym.
 */
public class ProfileFragment extends Fragment {
    protected FragmentProfileBinding b;
    private ItemProfileRowBinding notifRow;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindHeader();
        buildRows();
        b.pfVersion.setText(getString(R.string.profile_version, appVersion()));
    }

    private String appVersion() {
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b == null) return;
        bindHeader();
        refreshUnread();
    }

    private void bindHeader() {
        User u = Session.user;
        if (u == null) return;
        b.pfAvatar.setText(UiUtil.initials(u.fullName));
        b.pfName.setText(u.fullName);
        b.pfEmail.setText(u.email);
        UiUtil.badge(b.pfRole, u.roleLabel(), UiUtil.BADGE_PRIMARY);
    }

    /** Các mục chung; lớp con gọi super rồi thêm mục riêng qua addRow. */
    protected void buildRows() {
        notifRow = addRow(R.drawable.ic_notifications, R.string.profile_notifications, 0,
                () -> HostActivity.open(requireContext(), HostActivity.NOTIFICATIONS, null));
        addExtraRows();
        addRow(R.drawable.ic_edit, R.string.profile_edit, 0,
                () -> HostActivity.open(requireContext(), HostActivity.EDIT_PROFILE, null));
        addRow(R.drawable.ic_logout, R.string.action_logout, 0, this::confirmLogout);
    }

    /** Mặc định: mục dành cho hội viên. */
    protected void addExtraRows() {
        addRow(R.drawable.ic_assignment, R.string.profile_my_gyms, 0,
                () -> HostActivity.open(requireContext(), HostActivity.MY_GYMS, null));
        addRow(R.drawable.ic_ruler, R.string.profile_measurements, 0,
                () -> HostActivity.open(requireContext(), HostActivity.MEASUREMENTS, null));
    }

    protected ItemProfileRowBinding addRow(@DrawableRes int icon, @StringRes int title, @StringRes int sub, Runnable onClick) {
        ItemProfileRowBinding r = ItemProfileRowBinding.inflate(getLayoutInflater(), b.pfRows, false);
        r.rowIcon.setImageResource(icon);
        r.rowTitle.setText(title);
        if (sub != 0) {
            r.rowSub.setText(sub);
            r.rowSub.setVisibility(View.VISIBLE);
        }
        r.getRoot().setOnClickListener(v -> onClick.run());
        b.pfRows.addView(r.getRoot());
        return r;
    }

    private void refreshUnread() {
        if (notifRow == null || !Repos.auth().isSignedIn()) return;
        Repos.notifications().unreadCount(new Callback<Integer>() {
            @Override
            public void onSuccess(Integer n) {
                if (b == null || notifRow == null) return;
                if (n > 0) {
                    notifRow.rowBadge.setVisibility(View.VISIBLE);
                    UiUtil.badge(notifRow.rowBadge, getString(R.string.profile_unread_count, n), UiUtil.BADGE_PRIMARY);
                } else {
                    notifRow.rowBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(AppError e) {
                if (notifRow != null) notifRow.rowBadge.setVisibility(View.GONE);
            }
        });
    }

    protected void confirmLogout() {
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
        notifRow = null;
        super.onDestroyView();
    }
}
