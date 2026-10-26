package com.example.gymmanagement.ui.auth;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityForgotPasswordBinding;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.util.Validators;
import com.example.gymmanagement.viewmodel.AuthViewModel;

public class ForgotPasswordActivity extends BaseActivity {
    public static final String EXTRA_EMAIL = "email";

    private ActivityForgotPasswordBinding b;
    private AuthViewModel vm;

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        String initialEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (initialEmail != null) b.forgotEmail.setText(initialEmail);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        b.forgotButton.setOnClickListener(v -> {
            b.forgotEmailLayout.setError(null);
            String email = String.valueOf(b.forgotEmail.getText()).trim();
            if (!Validators.email(email)) {
                b.forgotEmailLayout.setError(getString(R.string.error_email_invalid));
                return;
            }
            UiUtil.hideKeyboard(b.getRoot());
            vm.sendReset(email);
        });
        vm.reset().observe(this, ev -> {
            if (ev == null) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.forgotProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.forgotButton.setEnabled(!loading);
            if (loading) return;
            b.forgotMessage.setVisibility(View.VISIBLE);
            if (r.status == Resource.Status.SUCCESS) {
                b.forgotMessage.setTextColor(getColor(R.color.success));
                b.forgotMessage.setText(getString(R.string.forgot_sent, r.data));
            } else {
                b.forgotMessage.setTextColor(getColor(R.color.danger));
                b.forgotMessage.setText(r.error.message);
            }
        });
    }
}
