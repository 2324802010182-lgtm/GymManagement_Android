package com.example.gymmanagement.ui.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentListScreenBinding;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.BaseViewModel;

import java.util.List;

/**
 * Khung màn hình danh sách của Chủ Gym: tiêu đề, tìm kiếm, bộ lọc chip, danh sách có đủ
 * trạng thái loading/empty/error, nút thêm và phản hồi kết quả thao tác ghi.
 */
public abstract class OwnerListFragment<T> extends Fragment {
    protected FragmentListScreenBinding b;
    protected SimpleAdapter<T, ?> adapter;

    /** Tạo ViewModel (gọi một lần, trước các hàm còn lại). */
    protected abstract BaseViewModel initViewModel();

    protected abstract SimpleAdapter<T, ?> createAdapter();

    protected abstract LiveData<Resource<List<T>>> source();

    protected abstract void reload();

    protected abstract int emptyTitle();

    protected abstract int emptyMessage();

    /** Tiêu đề lớn trên màn hình (0 = ẩn; dùng khi là tab chính). */
    protected int headline() {
        return 0;
    }

    /** Tiêu đề thanh công cụ khi mở từ HostActivity (0 = không đặt). */
    protected int hostTitle() {
        return 0;
    }

    protected void onSetup() {
    }

    /** Màn hình có nút thêm (FAB) – cấu hình text/listener trong onSetup(). */
    protected boolean hasFab() {
        return false;
    }

    /** Yêu cầu phải có thương hiệu thì mới dùng được màn hình này. */
    protected boolean needsBrand() {
        return true;
    }

    /** Gọi sau khi thao tác ghi thành công. */
    protected void onActionSuccess() {
        reload();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentListScreenBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BaseViewModel vm = initViewModel();
        if (hostTitle() != 0 && requireActivity() instanceof HostActivity) requireActivity().setTitle(hostTitle());
        if (headline() != 0) {
            b.lsTitle.setText(headline());
            b.lsTitle.setVisibility(View.VISIBLE);
        }
        adapter = createAdapter();
        b.lsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lsList.setAdapter(adapter);
        b.lsRefresh.setColorSchemeResources(R.color.primary);
        b.lsRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.lsRefresh.setOnRefreshListener(() -> {
            if (hasAccess()) reload();
            else b.lsRefresh.setRefreshing(false);
        });
        onSetup();
        source().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.lsRefresh.setRefreshing(false);
            b.lsState.render(r, getString(emptyTitle()), getString(emptyMessage()), this::reload);
            @SuppressWarnings("unchecked") SimpleAdapter<T, ?> a = adapter;
            a.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            boolean sheetOpen = !getChildFragmentManager().getFragments().isEmpty();
            if (r.status == Resource.Status.SUCCESS) {
                UiUtil.snack(b.getRoot(), r.data);
                onActionSuccess();
            } else if (!sheetOpen) {
                UiUtil.snack(b.getRoot(), r.error.message);
            }
        });
        if (hasAccess()) {
            if (source().getValue() == null) reload();
        }
        showNoBrandIfNeeded();
    }

    protected boolean hasAccess() {
        return !needsBrand() || Session.gymId() != null;
    }

    private void showNoBrandIfNeeded() {
        if (hasAccess()) {
            b.lsCta.setVisibility(View.GONE);
            b.lsFab.setVisibility(hasFab() ? View.VISIBLE : View.GONE);
            return;
        }
        b.lsState.showEmpty(getString(R.string.owner_need_brand_title), getString(R.string.owner_need_brand_msg));
        b.lsCta.setText(R.string.owner_create_brand);
        b.lsCta.setVisibility(View.VISIBLE);
        b.lsCta.setOnClickListener(v -> HostActivity.open(requireContext(), HostActivity.BRAND, null));
        b.lsFab.setVisibility(View.GONE);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && b != null && hasAccess()) reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) {
            if (hasAccess()) {
                b.lsCta.setVisibility(View.GONE);
                b.lsFab.setVisibility(hasFab() ? View.VISIBLE : View.GONE);
                reload();
            } else {
                showNoBrandIfNeeded();
            }
        }
    }

    @Override
    public void onDestroyView() {
        b = null;
        adapter = null;
        super.onDestroyView();
    }
}
