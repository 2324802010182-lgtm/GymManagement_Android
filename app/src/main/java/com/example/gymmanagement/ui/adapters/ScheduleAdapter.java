package com.example.gymmanagement.ui.adapters;

import android.content.Context;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemScheduleBinding;
import com.example.gymmanagement.model.Schedule;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

/** Buổi tập; showMember=true khi Chủ Gym xem (hiện tên hội viên), false khi hội viên xem (hiện tên Gym). */
public class ScheduleAdapter extends SimpleAdapter<Schedule, ItemScheduleBinding> {
    public ScheduleAdapter(boolean showMember, Consumer<Schedule> onClick) {
        super(ItemScheduleBinding::inflate, (b, s, pos) -> {
            Context c = b.getRoot().getContext();
            b.scStart.setText(Fmt.time(s.startAt));
            b.scEnd.setText(c.getString(R.string.sched_until, Fmt.time(s.endAt)));
            b.scDate.setText(s.startAt == null ? "" : new java.text.SimpleDateFormat("dd/MM", java.util.Locale.US).format(s.startAt));
            b.scTitle.setText(s.title);
            String who = showMember ? s.memberName : s.gymName;
            b.scSub.setText(s.location.isEmpty() ? who : who + " • " + s.location);
            int type;
            switch (s.status) {
                case Schedule.COMPLETED:
                    type = UiUtil.BADGE_SUCCESS;
                    break;
                case Schedule.CANCELLED:
                    type = UiUtil.BADGE_NEUTRAL;
                    break;
                default:
                    type = UiUtil.BADGE_INFO;
            }
            UiUtil.badge(b.scStatus, s.statusLabel(), type);
            b.getRoot().setOnClickListener(v -> onClick.accept(s));
        });
    }
}
