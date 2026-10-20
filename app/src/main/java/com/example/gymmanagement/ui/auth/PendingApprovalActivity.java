package com.example.gymmanagement.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.ActivityPendingApprovalBinding;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.ui.shared.Router;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.AuthViewModel;

/** Trạng thái tài khoản chờ duyệt (Chủ Gym chưa được quản trị nền tảng phê duyệt). */
public class PendingApprovalActivity extends BaseActivity {
    private ActivityPendingApprovalBinding b;
    private AuthViewModel vm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        b = ActivityPendingApprovalBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        if (Session.user != null) {
            b.pendingMessage.setText(getString(R.string.pending_message_named, Session.user.fullName));
        }
        b.pendingRefresh.setOnClickListener(v -> vm.restoreSession());
        b.pendingSignout.setOnClickListener(v -> {
            Repos.auth().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
        vm.result().observe(this, ev -> {
            if (ev == null) return;
            Resource<User> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.pendingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.pendingRefresh.setEnabled(!loading);
            if (loading || ev.getIfNotHandled() == null) return;
            b.pendingStatus.setVisibility(View.VISIBLE);
            if (r.status == Resource.Status.ERROR) {
                b.pendingStatus.setText(r.error.message);
                return;
            }
            Intent i = Router.destination(this, r.data);
            if (i != null && !User.STATUS_PENDING.equals(r.data.accountStatus)) {
                startActivity(i);
                finish();
            } else if (i == null) {
                b.pendingStatus.setText(Router.deniedMessage(r.data));
            } else {
                b.pendingStatus.setText(R.string.pending_still);
            }
        });
    }
}
