package com.example.gymmanagement.ui.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.SheetPackageEditBinding;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.PackagesViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/** Tạo / sửa gói tập; kiểm tra theo đúng giới hạn của Security Rules. */
public class PackageEditSheet extends BottomSheetDialogFragment {
    public static PackageEditSheet newInstance(@Nullable GymPackage p) {
        PackageEditSheet s = new PackageEditSheet();
        Bundle a = new Bundle();
        if (p != null) {
            a.putString("id", p.id);
            a.putString("name", p.name);
            a.putLong("price", p.price);
            a.putInt("days", p.durationDays);
            a.putString("desc", p.description);
            a.putString("benefits", android.text.TextUtils.join("\n", p.benefits));
            a.putBoolean("active", p.isActive);
        }
        s.setArguments(a);
        return s;
    }

    private SheetPackageEditBinding b;
    private PackagesViewModel vm;
    private boolean submitted;
    private String id = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetPackageEditBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireParentFragment()).get(PackagesViewModel.class);
        Bundle a = requireArguments();
        id = a.getString("id", "");
        boolean edit = !id.isEmpty();
        b.peTitle.setText(edit ? R.string.pkg_edit : R.string.pkg_add);
        if (edit && savedInstanceState == null) {
            b.peName.setText(a.getString("name"));
            b.pePrice.setText(String.valueOf(a.getLong("price")));
            b.peDays.setText(String.valueOf(a.getInt("days")));
            b.peDesc.setText(a.getString("desc"));
            b.peBenefits.setText(a.getString("benefits"));
            b.peActive.setChecked(a.getBoolean("active", true));
        } else if (!edit && savedInstanceState == null) {
            b.peDays.setText("30");
        }
        b.peCancel.setOnClickListener(v -> dismiss());
        b.peSave.setOnClickListener(v -> save());
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.peProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.peSave.setEnabled(!loading);
            if (loading) return;
            if (r.status == Resource.Status.SUCCESS) {
                dismiss();
            } else {
                submitted = false;
                b.peError.setText(r.error.message);
                b.peError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void save() {
        b.peError.setVisibility(View.GONE);
        b.peNameLayout.setError(null);
        b.pePriceLayout.setError(null);
        b.peDaysLayout.setError(null);
        b.peDescLayout.setError(null);
        b.peBenefitsLayout.setError(null);
        String name = String.valueOf(b.peName.getText()).trim().replaceAll("\\s+", " ");
        String desc = String.valueOf(b.peDesc.getText()).trim();
        boolean ok = true;
        if (name.length() < 2 || name.length() > 80) {
            b.peNameLayout.setError(getString(R.string.error_pkg_name));
            ok = false;
        }
        long price = parse(b.pePrice.getText());
        if (price < 0 || price > 1_000_000_000L) {
            b.pePriceLayout.setError(getString(R.string.error_pkg_price));
            ok = false;
        }
        long days = parse(b.peDays.getText());
        if (days < 1 || days > 3650) {
            b.peDaysLayout.setError(getString(R.string.error_pkg_days));
            ok = false;
        }
        if (desc.length() > 1000) {
            b.peDescLayout.setError(getString(R.string.error_too_long));
            ok = false;
        }
        List<String> benefits = new ArrayList<>();
        for (String line : String.valueOf(b.peBenefits.getText()).split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) benefits.add(t.length() > 120 ? t.substring(0, 120) : t);
        }
        if (benefits.size() > 20) {
            b.peBenefitsLayout.setError(getString(R.string.error_pkg_benefits));
            ok = false;
        }
        if (!ok) return;
        GymPackage p = new GymPackage();
        p.id = id;
        p.name = name;
        p.price = price;
        p.durationDays = (int) days;
        p.description = desc;
        p.benefits = benefits;
        p.isActive = b.peActive.isChecked();
        submitted = true;
        vm.save(p, id.isEmpty());
    }

    private long parse(CharSequence s) {
        try {
            return Long.parseLong(String.valueOf(s).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
