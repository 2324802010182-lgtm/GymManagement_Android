package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** gyms/{gymId}/memberships/{membershipId} – chỉ được tạo sau khi Chủ Gym duyệt yêu cầu. */
public class Membership {
    public static final String ACTIVE = "active";
    public static final String CANCELLED = "cancelled";

    public String id = "";
    public String gymId = "";
    public String gymName = "";
    public String memberUid = "";
    public String memberName = "";
    public String packageId = "";
    public String packageName = "";
    public long packagePrice;
    public int durationDays;
    public String requestId = "";
    public String status = ACTIVE;
    public String cancelReason = "";
    public Date startAt;
    public Date endAt;

    public static Membership from(DocumentSnapshot s) {
        Membership m = new Membership();
        m.id = s.getId();
        m.gymId = Docs.str(s, "gymId");
        m.gymName = Docs.str(s, "gymName");
        m.memberUid = Docs.str(s, "memberUid");
        m.memberName = Docs.str(s, "memberName");
        m.packageId = Docs.str(s, "packageId");
        m.packageName = Docs.str(s, "packageName");
        m.packagePrice = Docs.lng(s, "packagePrice");
        m.durationDays = (int) Docs.lng(s, "durationDays");
        m.requestId = Docs.str(s, "requestId");
        m.status = Docs.str(s, "status");
        m.cancelReason = Docs.str(s, "cancelReason");
        m.startAt = Docs.date(s, "startAt");
        m.endAt = Docs.date(s, "endAt");
        return m;
    }

    /** Đang hiệu lực: trạng thái active và trong khoảng [startAt, endAt). */
    public boolean isValidNow() {
        Date now = new Date();
        return ACTIVE.equals(status) && startAt != null && endAt != null
                && !startAt.after(now) && endAt.after(now);
    }

    public boolean isExpired() {
        return ACTIVE.equals(status) && endAt != null && !endAt.after(new Date());
    }

    public long daysLeft() {
        if (endAt == null) return 0;
        long ms = endAt.getTime() - System.currentTimeMillis();
        return ms <= 0 ? 0 : (ms + 86_399_999L) / 86_400_000L;
    }

    public String statusLabel() {
        if (CANCELLED.equals(status)) return "Đã hủy";
        if (isExpired()) return "Hết hạn";
        return "Đang hoạt động";
    }
}
