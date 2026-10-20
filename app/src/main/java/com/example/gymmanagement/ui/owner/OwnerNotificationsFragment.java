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
import com.example.gymmanagement.databinding.FragmentOwnerNotificationsBinding;
import com.example.gymmanagement.ui.adapters.NotificationAdapter;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.OwnerNotificationsViewModel;

/** Chủ Gym gửi thông báo chung cho hội viên (lưu trong Firestore) và xem lịch sử đã gửi. */
public class OwnerNotificationsFragment extends Fragment {
    private FragmentOwnerNotificationsBinding b;
    private OwnerNotificationsViewModel vm;
    private boolean sending;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentOwnerNotificationsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.owner_announce_title);
        vm = new ViewModelProvider(this).get(OwnerNotificationsViewModel.class);
        NotificationAdapter adapter = new NotificationAdapter(n -> {
        });
        b.onList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.onList.setAdapter(adapter);
        if (Session.gymId() == null) {
            b.onSend.setEnabled(false);
            b.onState.showEmpty(getString(R.string.owner_need_brand_title), getString(R.string.owner_need_brand_msg));
            return;
        }
        b.onSend.setOnClickListener(v -> send());
        vm.history().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.onState.render(r, getString(R.string.onotif_empty_title), getString(R.string.onotif_empty_msg), vm::load);
            if (r.status == Resource.Status.SUCCESS) {
                for (com.example.gymmanagement.model.AppNotification n : r.data) {
                    n.isRead = true;
                    n.type = "announcement";
                }
            }
            adapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !sending) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.onProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.onSend.setEnabled(!loading);
            if (loading) return;
            sending = false;
            if (r.status == Resource.Status.SUCCESS) {
                b.onTitle.setText("");
                b.onBody.setText("");
                UiUtil.snack(b.getRoot(), r.data);
                vm.load();
            } else {
                b.onError.setText(r.error.message);
                b.onError.setVisibility(View.VISIBLE);
            }
        });
        if (vm.history().getValue() == null) vm.load();
    }

    private void send() {
        UiUtil.hideKeyboard(b.getRoot());
        b.onError.setVisibility(View.GONE);
        b.onTitleLayout.setError(null);
        b.onBodyLayout.setError(null);
        String t = String.valueOf(b.onTitle.getText()).trim();
        String body = String.valueOf(b.onBody.getText()).trim();
        boolean ok = true;
        if (t.length() < 2 || t.length() > 80) {
            b.onTitleLayout.setError(getString(R.string.error_notif_title));
            ok = false;
        }
        if (body.length() < 2 || body.length() > 500) {
            b.onBodyLayout.setError(getString(R.string.error_notif_body));
            ok = false;
        }
        if (!ok) return;
        sending = true;
        vm.send(t, body, b.onActiveOnly.isChecked());
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
