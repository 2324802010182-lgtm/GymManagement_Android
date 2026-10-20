package com.example.gymmanagement.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityRegisterOwnerBinding;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.ui.shared.Router;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.util.Validators;
import com.example.gymmanagement.viewmodel.AuthViewModel;

/** Đăng ký Chủ Gym: tài khoản ở trạng thái "pending" cho tới khi quản trị nền tảng phê duyệt. */
public class RegisterOwnerActivity extends BaseActivity {
    private ActivityRegisterOwnerBinding b;
    private AuthViewModel vm;

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterOwnerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        b.regButton.setOnClickListener(v -> submit());
        vm.result().observe(this, ev -> {
            if (ev == null) return;
            Resource<User> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.regProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.regButton.setEnabled(!loading);
            if (loading || ev.getIfNotHandled() == null) return;
            if (r.status == Resource.Status.ERROR) {
                b.regError.setText(r.error.message);
                b.regError.setVisibility(View.VISIBLE);
                return;
            }
            Intent i = Router.destination(this, r.data);
            if (i != null) {
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
            finish();
        });
    }

    private void submit() {
        b.regError.setVisibility(View.GONE);
        b.regNameLayout.setError(null);
        b.regEmailLayout.setError(null);
        b.regPhoneLayout.setError(null);
        b.regPasswordLayout.setError(null);
        b.regConfirmLayout.setError(null);
        b.regGymLayout.setError(null);
        String name = String.valueOf(b.regName.getText()).trim();
        String email = String.valueOf(b.regEmail.getText()).trim();
        String phone = String.valueOf(b.regPhone.getText()).trim();
        String pass = String.valueOf(b.regPassword.getText());
        String confirm = String.valueOf(b.regConfirm.getText());
        String gym = Validators.cleanName(String.valueOf(b.regGym.getText()));
        String note = String.valueOf(b.regNote.getText()).trim();
        boolean ok = true;
        if (!Validators.name(name)) {
            b.regNameLayout.setError(getString(R.string.error_name_invalid));
            ok = false;
        }
        if (!Validators.email(email)) {
            b.regEmailLayout.setError(getString(R.string.error_email_invalid));
            ok = false;
        }
        if (!Validators.phone(phone)) {
            b.regPhoneLayout.setError(getString(R.string.error_phone_invalid));
            ok = false;
        }
        if (!Validators.password(pass)) {
            b.regPasswordLayout.setError(getString(R.string.error_password_short));
            ok = false;
        }
        if (!pass.equals(confirm)) {
            b.regConfirmLayout.setError(getString(R.string.error_password_mismatch));
            ok = false;
        }
        if (gym.length() > 80) {
            b.regGymLayout.setError(getString(R.string.error_too_long));
            ok = false;
        }
        if (note.length() > 300) {
            b.regNoteLayout.setError(getString(R.string.error_too_long));
            ok = false;
        }
        if (!ok) return;
        UiUtil.hideKeyboard(b.getRoot());
        vm.registerOwner(name, email, phone, pass, note, gym);
    }
}
