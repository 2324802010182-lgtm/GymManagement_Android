package com.example.gymmanagement.ui.adapters;

import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemPackageMemberBinding;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

/** Gói tập hiển thị cho hội viên (chỉ gói đang hoạt động mới đăng ký được). */
public class PackageMemberAdapter extends SimpleAdapter<GymPackage, ItemPackageMemberBinding> {
    public PackageMemberAdapter(boolean canRegister, Consumer<GymPackage> onRegister) {
        super(ItemPackageMemberBinding::inflate, (b, p, pos) -> {
            b.pkgName.setText(p.name);
            b.pkgPrice.setText(Fmt.money(p.price));
            b.pkgDuration.setText(Fmt.duration(p.durationDays));
            b.pkgDesc.setText(p.description);
            b.pkgDesc.setVisibility(p.description.isEmpty() ? View.GONE : View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (String s : p.benefits) sb.append("• ").append(s).append('\n');
            b.pkgBenefits.setText(sb.toString().trim());
            b.pkgBenefits.setVisibility(p.benefits.isEmpty() ? View.GONE : View.VISIBLE);
            UiUtil.badge(b.pkgStatus, p.isActive
                            ? b.getRoot().getContext().getString(R.string.pkg_active)
                            : b.getRoot().getContext().getString(R.string.pkg_inactive),
                    p.isActive ? UiUtil.BADGE_SUCCESS : UiUtil.BADGE_NEUTRAL);
            b.pkgRegister.setEnabled(p.isActive && canRegister);
            b.pkgRegister.setOnClickListener(v -> onRegister.accept(p));
        });
    }
}
