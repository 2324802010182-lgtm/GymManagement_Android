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
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentOwnerDashboardBinding;
import com.example.gymmanagement.databinding.ItemRecentRowBinding;
import com.example.gymmanagement.model.DashboardStats;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.adapters.ScheduleAdapter;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.OwnerDashboardViewModel;

/** Dashboard Chủ Gym: mọi con số lấy trực tiếp từ Firestore (count / truy vấn thật). */
public class OwnerDashboardFragment extends Fragment {
    private FragmentOwnerDashboardBinding b;
    private OwnerDashboardViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentOwnerDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(OwnerDashboardViewModel.class);
        String name = Session.user == null ? "" : Session.user.fullName;
        b.dashHello.setText(getString(R.string.dash_hello, name));
        b.dashTotal.statLabel.setText(R.string.dash_total_members);
        b.dashActive.statLabel.setText(R.string.dash_active_members);
        b.dashPackages.statLabel.setText(R.string.dash_active_packages);
        b.dashPending.statLabel.setText(R.string.dash_pending_requests);
        b.dashToday.statLabel.setText(R.string.dash_attendance_today);

        ScheduleAdapter upcoming = new ScheduleAdapter(true, s -> HostActivity.open(requireContext(), HostActivity.SCHEDULES, null));
        b.dashUpcoming.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.dashUpcoming.setAdapter(upcoming);
        SimpleAdapter<MembershipRequest, ItemRecentRowBinding> recent = new SimpleAdapter<>(ItemRecentRowBinding::inflate, (ib, r, pos) -> {
            ib.rrTitle.setText(getString(R.string.dash_recent_line, r.memberName, r.packageName));
            ib.rrSub.setText(Fmt.dateTime(r.updatedAt != null ? r.updatedAt : r.createdAt));
            int type = MembershipRequest.APPROVED.equals(r.status) ? UiUtil.BADGE_SUCCESS
                    : MembershipRequest.PENDING.equals(r.status) ? UiUtil.BADGE_WARNING
                    : MembershipRequest.REJECTED.equals(r.status) ? UiUtil.BADGE_PRIMARY : UiUtil.BADGE_NEUTRAL;
            UiUtil.badge(ib.rrBadge, r.statusLabel(), type);
        });
        b.dashRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.dashRecent.setAdapter(recent);

        b.dashRefresh.setColorSchemeResources(R.color.primary);
        b.dashRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.dashRefresh.setOnRefreshListener(this::refresh);
        b.dashBell.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.NOTIFICATIONS, null));
        b.dashGoSchedule.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.SCHEDULES, null));
        b.dashGoAttendance.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.ATTENDANCE, null));
        b.dashExerciseLibrary.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.EXERCISE_LIBRARY, null));
        b.dashCreateBrand.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.BRAND, null));

        vm.stats().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.dashRefresh.setRefreshing(false);
            if (Session.gymId() == null) return;
            b.dashState.render(r, getString(R.string.dash_empty_title), getString(R.string.dash_empty_msg), this::refresh);
            if (r.status == Resource.Status.SUCCESS) bind(r.data, upcoming, recent);
        });
        vm.unread().observe(getViewLifecycleOwner(), n -> {
            if (n == null || n <= 0) {
                b.dashBellBadge.setVisibility(View.GONE);
            } else {
                b.dashBellBadge.setVisibility(View.VISIBLE);
                b.dashBellBadge.setText(n > 99 ? "99+" : String.valueOf(n));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) refresh();
    }

    private void refresh() {
        Gym g = Session.ownerGym;
        if (g == null) {
            b.dashBrand.setText(R.string.dash_no_brand);
            b.dashState.showEmpty(getString(R.string.owner_need_brand_title), getString(R.string.owner_need_brand_msg));
            b.dashCreateBrand.setVisibility(View.VISIBLE);
            b.dashRefresh.setRefreshing(false);
            return;
        }
        b.dashCreateBrand.setVisibility(View.GONE);
        b.dashBrand.setText(g.name);
        UiUtil.badge(b.dashBrandStatus, g.statusLabel(), g.isActive() ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_WARNING);
        vm.load();
    }

    private void bind(DashboardStats s, ScheduleAdapter upcoming, SimpleAdapter<MembershipRequest, ItemRecentRowBinding> recent) {
        Motion.countUp(b.dashTotal.statValue, s.totalMembers);
        Motion.countUp(b.dashActive.statValue, s.activeMembers);
        Motion.countUp(b.dashPackages.statValue, s.activePackages);
        Motion.countUp(b.dashPending.statValue, s.pendingRequests);
        Motion.countUp(b.dashToday.statValue, s.attendanceToday);
        upcoming.setItems(s.upcomingSchedules);
        b.dashUpcomingEmpty.setVisibility(s.upcomingSchedules.isEmpty() ? View.VISIBLE : View.GONE);
        recent.setItems(s.recentRequests);
        b.dashRecentEmpty.setVisibility(s.recentRequests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
