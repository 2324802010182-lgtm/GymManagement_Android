package com.example.gymmanagement.ui.owner;

import com.example.gymmanagement.R;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.ui.shared.ProfileFragment;

/** Tab "Thêm" của Chủ Gym: thương hiệu, lịch tập, điểm danh, thông báo + hồ sơ cá nhân. */
public class OwnerMoreFragment extends ProfileFragment {
    @Override
    protected void addExtraRows() {
        addRow(R.drawable.ic_store, R.string.owner_brand_title, 0,
                () -> HostActivity.open(requireContext(), HostActivity.BRAND, null));
        addRow(R.drawable.ic_calendar, R.string.owner_schedules_title, 0,
                () -> HostActivity.open(requireContext(), HostActivity.SCHEDULES, null));
        addRow(R.drawable.ic_event_available, R.string.owner_attendance_title, 0,
                () -> HostActivity.open(requireContext(), HostActivity.ATTENDANCE, null));
        addRow(R.drawable.ic_announcement, R.string.owner_announce_title, 0,
                () -> HostActivity.open(requireContext(), HostActivity.OWNER_NOTIFICATIONS, null));
    }
}
