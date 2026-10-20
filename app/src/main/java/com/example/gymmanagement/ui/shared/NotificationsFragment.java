package com.example.gymmanagement.ui.shared;

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
import com.example.gymmanagement.databinding.FragmentNotificationsBinding;
import com.example.gymmanagement.ui.adapters.NotificationAdapter;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.NotificationsViewModel;

/** F08 – thông báo trong ứng dụng (đọc từ Firestore, không dùng FCM). */
public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding b;
    private NotificationsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentNotificationsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.notif_title);
        vm = new ViewModelProvider(this).get(NotificationsViewModel.class);
        NotificationAdapter adapter = new NotificationAdapter(n -> vm.markRead(n));
        b.ntList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.ntList.setAdapter(adapter);
        b.ntRefresh.setColorSchemeResources(R.color.primary);
        b.ntRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.ntRefresh.setOnRefreshListener(vm::load);
        b.ntFilterUnread.setChecked(vm.unreadOnly());
        b.ntFilterUnread.setOnCheckedChangeListener((v, checked) -> vm.setUnreadOnly(checked));
        b.ntMarkAll.setOnClickListener(v -> vm.markAllRead());
        vm.list().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.ntRefresh.setRefreshing(false);
            b.ntState.render(r,
                    getString(vm.unreadOnly() ? R.string.notif_empty_unread_title : R.string.notif_empty_title),
                    getString(vm.unreadOnly() ? R.string.notif_empty_unread_msg : R.string.notif_empty_msg), vm::load);
            adapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
        });
        if (vm.list().getValue() == null) vm.load();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
