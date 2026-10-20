package com.example.gymmanagement.ui.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentBrandBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.data.repository.GymRepository;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.util.Validators;
import com.example.gymmanagement.viewmodel.BrandViewModel;

import java.util.ArrayList;
import java.util.List;

/** F09 – tạo hoặc cập nhật thương hiệu Gym (tên thương hiệu là duy nhất). */
public class BrandFragment extends Fragment {
    private FragmentBrandBinding b;
    private BrandViewModel vm;
    private boolean saving;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentBrandBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(BrandViewModel.class);
        Gym g = Session.ownerGym;
        boolean edit = g != null;
        requireActivity().setTitle(edit ? R.string.brand_edit_title : R.string.brand_create_title);
        b.brIntro.setText(edit ? R.string.brand_edit_intro : R.string.brand_create_intro);
        if (edit && savedInstanceState == null) {
            b.brName.setText(g.name);
            b.brAddress.setText(g.address);
            b.brArea.setText(g.area);
            b.brPhone.setText(g.phone);
            b.brHours.setText(g.openingHours);
            b.brDesc.setText(g.description);
            b.brLogo.setText(g.logoUrl);
            b.brCover.setText(g.coverUrl);
            b.brGallery.setText(android.text.TextUtils.join("\n", g.galleryUrls));
        }
        boolean suspended = edit && Gym.STATUS_SUSPENDED.equals(g.status);
        b.brVisible.setVisibility(edit && !suspended ? View.VISIBLE : View.GONE);
        if (edit) b.brVisible.setChecked(Gym.STATUS_ACTIVE.equals(g.status));
        if (suspended) {
            b.brError.setText(R.string.brand_suspended_notice);
            b.brError.setVisibility(View.VISIBLE);
        }
        b.brSave.setOnClickListener(v -> save());
        vm.saved().observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !saving) return;
            Resource<Gym> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.brProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.brSave.setEnabled(!loading);
            if (loading) return;
            saving = false;
            if (r.status == Resource.Status.SUCCESS) {
                UiUtil.toast(requireContext(), getString(edit ? R.string.brand_saved : R.string.brand_created));
                requireActivity().finish();
            } else {
                b.brError.setText(r.error.message);
                b.brError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void save() {
        UiUtil.hideKeyboard(b.getRoot());
        b.brError.setVisibility(View.GONE);
        b.brNameLayout.setError(null);
        b.brAddressLayout.setError(null);
        b.brPhoneLayout.setError(null);
        b.brLogoLayout.setError(null);
        b.brCoverLayout.setError(null);
        b.brGalleryLayout.setError(null);
        b.brDescLayout.setError(null);
        Gym d = new Gym();
        d.name = Validators.cleanName(String.valueOf(b.brName.getText()));
        d.address = String.valueOf(b.brAddress.getText()).trim();
        d.area = String.valueOf(b.brArea.getText()).trim();
        d.phone = String.valueOf(b.brPhone.getText()).trim();
        d.openingHours = String.valueOf(b.brHours.getText()).trim();
        d.description = String.valueOf(b.brDesc.getText()).trim();
        d.logoUrl = String.valueOf(b.brLogo.getText()).trim();
        d.coverUrl = String.valueOf(b.brCover.getText()).trim();
        List<String> gallery = new ArrayList<>();
        for (String line : String.valueOf(b.brGallery.getText()).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) gallery.add(t);
        }
        d.galleryUrls = gallery;
        Gym cur = Session.ownerGym;
        d.status = cur == null ? Gym.STATUS_ACTIVE
                : Gym.STATUS_SUSPENDED.equals(cur.status) ? Gym.STATUS_SUSPENDED
                : (b.brVisible.isChecked() ? Gym.STATUS_ACTIVE : Gym.STATUS_HIDDEN);

        boolean ok = true;
        if (d.name.length() < 2 || d.name.length() > 80 || d.name.contains("/")) {
            b.brNameLayout.setError(getString(R.string.error_brand_name));
            ok = false;
        }
        if (d.address.length() < 5 || d.address.length() > 200) {
            b.brAddressLayout.setError(getString(R.string.error_brand_address));
            ok = false;
        }
        if (!d.phone.isEmpty() && !Validators.phone(d.phone)) {
            b.brPhoneLayout.setError(getString(R.string.error_phone));
            ok = false;
        }
        if (d.description.length() > 1000 || d.area.length() > 100 || d.openingHours.length() > 120) {
            b.brDescLayout.setError(getString(R.string.error_too_long));
            ok = false;
        }
        if (!Validators.httpsOrEmpty(d.logoUrl)) {
            b.brLogoLayout.setError(getString(R.string.error_https_url));
            ok = false;
        }
        if (!Validators.httpsOrEmpty(d.coverUrl)) {
            b.brCoverLayout.setError(getString(R.string.error_https_url));
            ok = false;
        }
        if (gallery.size() > 10) {
            b.brGalleryLayout.setError(getString(R.string.error_gallery_max));
            ok = false;
        }
        for (String u : gallery) {
            if (!Validators.httpsOrEmpty(u)) {
                b.brGalleryLayout.setError(getString(R.string.error_https_url));
                ok = false;
                break;
            }
        }
        if (!ok) return;

        String normalized = GymRepository.normalize(d.name);
        boolean nameChanged = cur == null || !normalized.equals(cur.normalizedName);
        saving = true;
        b.brProgress.setVisibility(View.VISIBLE);
        b.brSave.setEnabled(false);
        if (!nameChanged) {
            vm.save(d);
            return;
        }
        // Kiểm tra trước để báo lỗi trùng tên rõ ràng (Rules vẫn là lớp bảo đảm cuối cùng).
        Repos.gyms().isNameTaken(normalized, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean taken) {
                if (b == null) return;
                if (taken) {
                    saving = false;
                    b.brProgress.setVisibility(View.GONE);
                    b.brSave.setEnabled(true);
                    b.brNameLayout.setError(getString(R.string.error_brand_name_taken));
                } else {
                    vm.save(d);
                }
            }

            @Override
            public void onError(AppError e) {
                if (b == null) return;
                saving = false;
                b.brProgress.setVisibility(View.GONE);
                b.brSave.setEnabled(true);
                b.brError.setText(e.message);
                b.brError.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
