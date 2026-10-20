package com.example.gymmanagement.ui.adapters;

import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemGymBinding;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.ImageLoader;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

/** Thẻ thương hiệu Gym trên trang chủ khám phá. */
public class GymAdapter extends SimpleAdapter<Gym, ItemGymBinding> {
    public GymAdapter(Consumer<Gym> onClick) {
        super(ItemGymBinding::inflate, (b, g, pos) -> {
            ImageLoader.load(b.gymCover, g.coverUrl, R.drawable.bg_gym_placeholder);
            b.gymName.setText(g.name);
            b.gymAddress.setText(g.address.isEmpty() ? b.getRoot().getContext().getString(R.string.no_address) : g.address);
            boolean hasPhone = !g.phone.isEmpty();
            b.gymPhoneRow.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
            b.gymPhone.setText(g.phone);
            b.gymDesc.setText(g.description);
            b.gymDesc.setVisibility(g.description.isEmpty() ? View.GONE : View.VISIBLE);
            UiUtil.badge(b.gymStatus, g.statusLabel(), g.isActive() ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_NEUTRAL);
            b.gymSampleBadge.setVisibility(g.isSampleData ? View.VISIBLE : View.GONE);
            if (g.activePackageCount != null) {
                b.gymPackages.setVisibility(View.VISIBLE);
                b.gymPackages.setText(g.activePackageCount > 0
                        ? b.getRoot().getContext().getString(R.string.gym_packages_count, g.activePackageCount)
                        : b.getRoot().getContext().getString(R.string.gym_no_packages_short));
            } else {
                b.gymPackages.setVisibility(View.GONE);
            }
            Motion.press(b.getRoot());
            b.getRoot().setOnClickListener(v -> onClick.accept(g));
            b.gymDetailButton.setOnClickListener(v -> onClick.accept(g));
        });
    }
}
