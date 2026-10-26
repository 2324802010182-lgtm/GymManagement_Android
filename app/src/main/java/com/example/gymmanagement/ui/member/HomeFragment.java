package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentHomeBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.adapters.GymAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.HomeViewModel;

import java.util.List;

/** F02 – Trang chủ hội viên khám phá Gym: header, tìm kiếm (debounce), banner, thẻ Gym từ Firestore. */
public class HomeFragment extends Fragment {
    private static final long DEBOUNCE_MS = 300;

    private FragmentHomeBinding b;
    private HomeViewModel vm;
    private GymAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        String name = Session.user == null ? "" : Session.user.fullName;
        String[] parts = name.trim().split("\\s+");
        b.homeGreeting.setText(getString(R.string.home_greeting, parts.length == 0 || parts[0].isEmpty() ? "bạn" : parts[parts.length - 1]));
        b.homeAvatar.setText(UiUtil.initials(name));
        b.homeAvatar.setOnClickListener(v -> openProfileTab());

        adapter = new GymAdapter(this::openGym);
        b.homeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.homeList.setAdapter(adapter);

        b.homeRefresh.setColorSchemeResources(R.color.primary);
        b.homeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.homeRefresh.setOnRefreshListener(this::reload);

        b.homeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int bf, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                b.homeSearchClear.setVisibility(e.length() > 0 ? View.VISIBLE : View.GONE);
                scheduleSearch(e.toString());
            }
        });
        b.homeSearch.setOnEditorActionListener((tv, action, ev) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                UiUtil.hideKeyboard(tv);
                return true;
            }
            return false;
        });
        b.homeSearchClear.setOnClickListener(v -> b.homeSearch.setText(""));
        b.homeBannerButton.setOnClickListener(v -> b.homeScroll.smoothScrollTo(0, b.homeSectionTitle.getTop() - UiUtil.dp(16)));
        b.homeBell.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.NOTIFICATIONS, null));
        b.homeMyCard.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.MY_GYMS, null));
        b.homeExerciseLibrary.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.EXERCISE_LIBRARY, null));
        Motion.press(b.homeMyCard);
        Motion.press(b.homeExerciseLibrary);
        Motion.appear(b.homeBanner, 0);

        vm.gyms().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.homeRefresh.setRefreshing(false);
            boolean q = vm.hasQuery();
            b.homeState.render(r, getString(q ? R.string.home_empty_search_title : R.string.home_empty_title),
                    getString(q ? R.string.home_empty_search_msg : R.string.home_empty_msg), this::reload);
            if (r.status == Resource.Status.SUCCESS) {
                adapter.setItems(r.data);
                b.homeResultCount.setText(q ? getString(R.string.home_results, r.data.size(), vm.query())
                        : getString(R.string.home_total, r.data.size()));
            } else {
                adapter.setItems(null);
                b.homeResultCount.setText("");
            }
        });
        vm.memberships().observe(getViewLifecycleOwner(), r -> updateMyCard());
        vm.requests().observe(getViewLifecycleOwner(), r -> updateMyCard());
        vm.unread().observe(getViewLifecycleOwner(), n -> {
            b.homeBellBadge.setVisibility(n != null && n > 0 ? View.VISIBLE : View.GONE);
            b.homeBellBadge.setText(n != null && n > 99 ? "99+" : String.valueOf(n));
        });
        updateMyCard();
        if (vm.gyms().getValue() == null) vm.load();
    }

    private void scheduleSearch(String text) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        b.homeSearchProgress.setVisibility(View.VISIBLE);
        pendingSearch = () -> {
            if (b == null) return;
            vm.search(text);
            b.homeSearchProgress.setVisibility(View.GONE);
        };
        handler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void reload() {
        vm.load();
        vm.loadMine();
        vm.refreshUnread();
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refreshUnread();
        vm.loadMine();
    }

    private void openProfileTab() {
        if (getActivity() instanceof MemberMainActivity) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = requireActivity().findViewById(R.id.member_nav);
            if (nav != null) nav.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void openGym(Gym g) {
        Bundle a = new Bundle();
        a.putString(GymDetailFragment.ARG_GYM_ID, g.id);
        a.putString(GymDetailFragment.ARG_GYM_NAME, g.name);
        HostActivity.open(requireContext(), HostActivity.GYM_DETAIL, a);
    }

    /** Thẻ tóm tắt: membership thật nếu có; ngược lại yêu cầu đang chờ; ngược lại trạng thái "chưa đăng ký". */
    private void updateMyCard() {
        if (b == null) return;
        Resource<List<Membership>> ms = vm.memberships().getValue();
        Resource<List<MembershipRequest>> rq = vm.requests().getValue();
        if (ms != null && ms.status == Resource.Status.ERROR) {
            b.homeMyTitle.setText(R.string.home_my_error_title);
            b.homeMySub.setText(R.string.home_my_error_sub);
            return;
        }
        Membership best = null;
        Membership expired = null;
        if (ms != null && ms.data != null) {
            for (Membership m : ms.data) {
                if (m.isValidNow() && (best == null || m.endAt.after(best.endAt))) best = m;
                if (m.isExpired() && (expired == null || m.endAt.after(expired.endAt))) expired = m;
            }
        }
        MembershipRequest pending = null;
        if (rq != null && rq.data != null) {
            for (MembershipRequest r : rq.data) if (r.isPending()) pending = r;
        }
        if (best != null) {
            b.homeMyTitle.setText(getString(R.string.home_my_active, best.packageName));
            b.homeMySub.setText(getString(R.string.home_my_active_sub, best.gymName, best.daysLeft(), Fmt.date(best.endAt)));
        } else if (pending != null) {
            b.homeMyTitle.setText(R.string.home_my_pending);
            b.homeMySub.setText(getString(R.string.home_my_pending_sub, pending.packageName, pending.gymName));
        } else if (expired != null) {
            b.homeMyTitle.setText(R.string.home_my_expired);
            b.homeMySub.setText(getString(R.string.home_my_expired_sub, expired.gymName, Fmt.date(expired.endAt)));
        } else if (ms != null && ms.status != Resource.Status.LOADING) {
            b.homeMyTitle.setText(R.string.home_my_none);
            b.homeMySub.setText(R.string.home_my_none_sub);
        } else {
            b.homeMyTitle.setText(R.string.home_my_default);
            b.homeMySub.setText(R.string.home_my_default_sub);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        b = null;
        super.onDestroyView();
    }
}
