package com.example.gymmanagement.ui.shared;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentEditProfileBinding;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.util.Validators;
import com.example.gymmanagement.viewmodel.AuthViewModel;

/** Chỉnh sửa họ tên và số điện thoại (email/vai trò/trạng thái không thể tự sửa). */
public class EditProfileFragment extends Fragment {
    private FragmentEditProfileBinding b;
    private AuthViewModel vm;
    private boolean submitted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentEditProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.profile_edit);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        if (savedInstanceState == null && Session.user != null) {
            b.epName.setText(Session.user.fullName);
            b.epPhone.setText(Session.user.phone);
        }
        if (Session.user != null) b.epEmail.setText(Session.user.email);
        b.epSave.setOnClickListener(v -> save());
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.epProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.epSave.setEnabled(!loading);
            if (loading) return;
            submitted = false;
            if (r.status == Resource.Status.SUCCESS) {
                UiUtil.toast(requireContext(), getString(R.string.profile_saved));
                requireActivity().finish();
            } else {
                b.epError.setText(r.error.message);
                b.epError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void save() {
        UiUtil.hideKeyboard(b.getRoot());
        b.epError.setVisibility(View.GONE);
        b.epNameLayout.setError(null);
        b.epPhoneLayout.setError(null);
        String name = Validators.cleanName(String.valueOf(b.epName.getText()));
        String phone = String.valueOf(b.epPhone.getText()).trim();
        boolean ok = true;
        if (!Validators.name(name)) {
            b.epNameLayout.setError(getString(R.string.error_name));
            ok = false;
        }
        if (!Validators.phone(phone)) {
            b.epPhoneLayout.setError(getString(R.string.error_phone));
            ok = false;
        }
        if (!ok) return;
        submitted = true;
        vm.updateProfile(name, phone);
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
