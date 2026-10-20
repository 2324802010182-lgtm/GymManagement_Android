package com.example.gymmanagement.ui.member;

import android.content.Intent;
import android.net.Uri;
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
import com.example.gymmanagement.databinding.FragmentGymDetailBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.adapters.GalleryAdapter;
import com.example.gymmanagement.ui.adapters.PackageMemberAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.ImageLoader;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.GymDetailViewModel;

/** Chi tiết thương hiệu Gym + danh sách gói tập thuộc đúng Gym đang xem + đăng ký gói. */
public class GymDetailFragment extends Fragment {
    public static final String ARG_GYM_ID = "gymId";
    public static final String ARG_GYM_NAME = "gymName";

    private FragmentGymDetailBinding b;
    private GymDetailViewModel vm;
    private PackageMemberAdapter packageAdapter;
    private GalleryAdapter galleryAdapter;
    private String gymId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentGymDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        gymId = args.getString(ARG_GYM_ID, "");
        requireActivity().setTitle(args.getString(ARG_GYM_NAME, ""));
        vm = new ViewModelProvider(this).get(GymDetailViewModel.class);

        packageAdapter = new PackageMemberAdapter(Session.user != null && Session.user.isMember(), this::onRegister);
        b.detailPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.detailPackages.setAdapter(packageAdapter);
        galleryAdapter = new GalleryAdapter();
        b.detailGallery.setAdapter(galleryAdapter);

        b.detailPendingCancel.setOnClickListener(v -> Dialogs.confirm(requireContext(),
                getString(R.string.action_cancel_request), getString(R.string.cancel_request_confirm),
                getString(R.string.action_cancel_request), vm::cancelPending));

        vm.gym().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.detailState.render(r, getString(R.string.state_error_title), "", this::load);
            if (r.status == Resource.Status.SUCCESS) bindGym(r.data);
        });
        vm.packages().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.detailPackagesState.render(r, getString(R.string.gym_no_packages_title),
                    getString(R.string.gym_no_packages_msg), this::load);
            packageAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.myRequests().observe(getViewLifecycleOwner(), r -> updatePending());
        vm.submitted().observe(getViewLifecycleOwner(), ev -> {
            // Sheet tự xử lý kết quả; đây chỉ làm mới trạng thái yêu cầu đang chờ
            updatePending();
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
        });
        if (vm.gym().getValue() == null) load();
    }

    private void load() {
        vm.load(gymId);
    }

    private void bindGym(Gym g) {
        requireActivity().setTitle(g.name);
        ImageLoader.load(b.detailCover, g.coverUrl, R.drawable.bg_gym_placeholder);
        ImageLoader.load(b.detailLogo, g.logoUrl, R.drawable.ic_placeholder_dumbbell);
        b.detailName.setText(g.name);
        UiUtil.badge(b.detailStatus, g.statusLabel(), g.isActive() ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_NEUTRAL);
        b.detailSample.setVisibility(g.isSampleData ? View.VISIBLE : View.GONE);
        b.detailAddress.setText(g.address.isEmpty() ? getString(R.string.no_address) : g.address);
        b.detailPhoneRow.setVisibility(g.phone.isEmpty() ? View.GONE : View.VISIBLE);
        b.detailPhone.setText(g.phone);
        b.detailPhoneRow.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + g.phone)));
            } catch (Exception e) {
                UiUtil.snack(b.getRoot(), getString(R.string.error_no_dialer));
            }
        });
        b.detailHoursRow.setVisibility(g.openingHours.isEmpty() ? View.GONE : View.VISIBLE);
        b.detailHours.setText(g.openingHours);
        b.detailDesc.setText(g.description);
        b.detailDesc.setVisibility(g.description.isEmpty() ? View.GONE : View.VISIBLE);
        boolean hasGallery = !g.galleryUrls.isEmpty();
        b.detailGallery.setVisibility(hasGallery ? View.VISIBLE : View.GONE);
        b.detailGalleryTitle.setVisibility(hasGallery ? View.VISIBLE : View.GONE);
        galleryAdapter.setItems(g.galleryUrls);
        if (!g.isActive()) {
            // Gym không hoạt động: không cho đăng ký mới
            packageAdapter = new PackageMemberAdapter(false, this::onRegister);
            b.detailPackages.setAdapter(packageAdapter);
            Resource<java.util.List<GymPackage>> pr = vm.packages().getValue();
            if (pr != null && pr.data != null) packageAdapter.setItems(pr.data);
        }
        updatePending();
    }

    private void updatePending() {
        if (b == null) return;
        MembershipRequest p = vm.pendingHere();
        if (p == null) {
            b.detailPendingBox.setVisibility(View.GONE);
            return;
        }
        b.detailPendingBox.setVisibility(View.VISIBLE);
        b.detailPendingText.setText(getString(R.string.detail_pending_text, p.packageName));
    }

    private void onRegister(GymPackage p) {
        Resource<Gym> g = vm.gym().getValue();
        if (g == null || g.data == null || !g.data.isActive()) {
            UiUtil.snack(b.getRoot(), getString(R.string.gym_inactive_cannot_register));
            return;
        }
        if (vm.pendingHere() != null) {
            UiUtil.snack(b.getRoot(), getString(R.string.detail_pending_block));
            return;
        }
        PackageRequestSheet.newInstance(p.id).show(getChildFragmentManager(), "request");
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
