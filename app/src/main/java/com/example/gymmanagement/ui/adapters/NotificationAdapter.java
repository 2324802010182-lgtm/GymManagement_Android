package com.example.gymmanagement.ui.adapters;

import android.content.Context;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemNotificationBinding;
import com.example.gymmanagement.model.AppNotification;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Fmt;

import java.util.function.Consumer;

public class NotificationAdapter extends SimpleAdapter<AppNotification, ItemNotificationBinding> {
    public NotificationAdapter(Consumer<AppNotification> onClick) {
        super(ItemNotificationBinding::inflate, (b, n, pos) -> {
            Context c = b.getRoot().getContext();
            b.ntTitle.setText(n.title);
            b.ntBody.setText(n.body);
            String meta = (n.gymName == null || n.gymName.isEmpty() ? "" : n.gymName + " · ") + Fmt.dateTime(n.createdAt);
            b.ntMeta.setText(meta);
            b.ntDot.setVisibility(n.isRead ? View.INVISIBLE : View.VISIBLE);
            b.ntIcon.setImageResource(iconFor(n.type));
            b.getRoot().setAlpha(n.isRead ? 0.75f : 1f);
            b.getRoot().setOnClickListener(v -> onClick.accept(n));
        });
    }

    private static int iconFor(String type) {
        if (type == null) return R.drawable.ic_notifications;
        switch (type) {
            case "request_approved":
                return R.drawable.ic_check;
            case "request_rejected":
            case "membership_cancelled":
            case "schedule_cancelled":
                return R.drawable.ic_error;
            case "membership_expiring":
                return R.drawable.ic_clock;
            case "schedule_created":
            case "schedule_updated":
                return R.drawable.ic_calendar;
            case "membership_request":
                return R.drawable.ic_assignment;
            case "announcement":
                return R.drawable.ic_announcement;
            default:
                return R.drawable.ic_notifications;
        }
    }
}
