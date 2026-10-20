package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.databinding.SheetPackageRequestBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.GymDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Bottom sheet xem lại Gym / gói / giá / thời hạn rồi xác nhận gửi yêu cầu (trạng thái pending). */
public class PackageRequestSheet extends BottomSheetDialogFragment {
    private static final String ARG_PKG = "packageId";

    public static PackageRequestSheet newInstance(String packageId) {
        PackageRequestSheet s = new PackageRequestSheet();
        Bundle a = new Bundle();
        a.putString(ARG_PKG, packageId);
        s.setArguments(a);
        return s;
    }

    private SheetPackageRequestBinding b;
    private GymDetailViewModel vm;
    private GymPackage pkg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetPackageRequestBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireParentFragment()).get(GymDetailViewModel.class);
        String id = requireArguments().getString(ARG_PKG, "");
        Resource<java.util.List<GymPackage>> pr = vm.packages().getValue();
        Resource<Gym> gr = vm.gym().getValue();
        if (pr != null && pr.data != null) {
            for (GymPackage p : pr.data) if (p.id.equals(id)) pkg = p;
        }
        if (pkg == null || gr == null || gr.data == null) {
            dismiss();
            return;
        }
        b.reqGym.setText(gr.data.name);
        b.reqPackage.setText(pkg.name);
        b.reqPrice.setText(Fmt.money(pkg.price));
        b.reqDuration.setText(Fmt.duration(pkg.durationDays));
        b.reqCancel.setOnClickListener(v -> dismiss());
        b.reqDone.setOnClickListener(v -> dismiss());
        b.reqSubmit.setOnClickListener(v -> {
            b.reqError.setVisibility(View.GONE);
            vm.submit(pkg, String.valueOf(b.reqNote.getText()));
        });
        vm.submitted().observe(getViewLifecycleOwner(), ev -> {
            if (ev == null) return;
            Resource<MembershipRequest> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.reqProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.reqSubmit.setEnabled(!loading);
            if (loading || ev.getIfNotHandled() == null) return;
            if (r.status == Resource.Status.ERROR) {
                b.reqError.setText(r.error.message);
                b.reqError.setVisibility(View.VISIBLE);
            } else {
                b.reqForm.setVisibility(View.GONE);
                b.reqSuccess.setVisibility(View.VISIBLE);
                Motion.pop(b.reqSuccessIcon);
            }
        });
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
