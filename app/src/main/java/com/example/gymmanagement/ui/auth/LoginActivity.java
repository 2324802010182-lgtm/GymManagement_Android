package com.example.gymmanagement.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityLoginBinding;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.ui.shared.Router;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.util.Validators;
import com.example.gymmanagement.viewmodel.AuthViewModel;

public class LoginActivity extends BaseActivity {
    public static final String EXTRA_MESSAGE = "message";

    private ActivityLoginBinding b;
    private AuthViewModel vm;

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        String msg = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (msg != null) {
            b.loginBanner.setText(msg);
            b.loginBanner.setVisibility(View.VISIBLE);
        }
        Motion.press(b.loginButton);
        b.loginButton.setOnClickListener(v -> submit());
        b.loginPassword.setOnEditorActionListener((tv, action, e) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        b.loginForgot.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        b.loginRegisterMember.setOnClickListener(v -> startActivity(new Intent(this, RegisterMemberActivity.class)));
        b.loginRegisterOwner.setOnClickListener(v -> startActivity(new Intent(this, RegisterOwnerActivity.class)));

        vm.result().observe(this, ev -> {
            if (ev == null) return;
            Resource<User> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.loginButton.setEnabled(!loading);
            if (loading || ev.getIfNotHandled() == null) return;
            if (r.status == Resource.Status.ERROR) {
                b.loginError.setText(r.error.message);
                b.loginError.setVisibility(View.VISIBLE);
                return;
            }
            Intent i = Router.destination(this, r.data);
            if (i == null) {
                String denied = Router.deniedMessage(r.data);
                Repos.auth().signOut();
                b.loginError.setText(denied);
                b.loginError.setVisibility(View.VISIBLE);
                return;
            }
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }

    private void submit() {
        b.loginEmailLayout.setError(null);
        b.loginPasswordLayout.setError(null);
        b.loginError.setVisibility(View.GONE);
        String email = String.valueOf(b.loginEmail.getText()).trim();
        String pass = String.valueOf(b.loginPassword.getText());
        boolean ok = true;
        if (!Validators.email(email)) {
            b.loginEmailLayout.setError(getString(R.string.error_email_invalid));
            ok = false;
        }
        if (pass.isEmpty()) {
            b.loginPasswordLayout.setError(getString(R.string.error_required));
            ok = false;
        }
        if (!ok) return;
        UiUtil.hideKeyboard(b.getRoot());
        vm.login(email, pass);
    }
}
