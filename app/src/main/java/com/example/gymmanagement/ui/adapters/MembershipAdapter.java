package com.example.gymmanagement.ui.adapters;

import android.content.Context;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemMembershipBinding;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

/** Membership thật (chỉ có sau khi Chủ Gym duyệt). */
public class MembershipAdapter extends SimpleAdapter<Membership, ItemMembershipBinding> {
    public MembershipAdapter(Consumer<Membership> onClick) {
        super(ItemMembershipBinding::inflate, (b, m, pos) -> {
            Context c = b.getRoot().getContext();
            b.msGym.setText(m.gymName);
            b.msPackage.setText(c.getString(R.string.ms_package_line, m.packageName, Fmt.money(m.packagePrice)));
            b.msDates.setText(c.getString(R.string.ms_dates_line, Fmt.date(m.startAt), Fmt.date(m.endAt)));
            int type = Membership.CANCELLED.equals(m.status) ? UiUtil.BADGE_NEUTRAL
                    : m.isExpired() ? UiUtil.BADGE_WARNING : UiUtil.BADGE_SUCCESS;
            UiUtil.badge(b.msStatus, m.statusLabel(), type);
            if (m.isValidNow()) {
                b.msLeft.setVisibility(android.view.View.VISIBLE);
                b.msLeft.setText(c.getString(R.string.ms_days_left, m.daysLeft()));
            } else {
                b.msLeft.setVisibility(android.view.View.GONE);
            }
            b.getRoot().setOnClickListener(v -> onClick.accept(m));
        });
    }
}
