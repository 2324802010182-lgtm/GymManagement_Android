package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** gyms/{gymId}/membershipRequests/{requestId}. */
public class MembershipRequest {
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";
    public static final String CANCELLED = "cancelled";

    public String id = "";
    public String gymId = "";
    public String gymName = "";
    public String memberUid = "";
    public String memberName = "";
    public String memberEmail = "";
    public String memberPhone = "";
    public String packageId = "";
    public String packageName = "";
    public long packagePrice;
    public int packageDurationDays;
    public String note = "";
    public String status = PENDING;
    public String reviewedBy = "";
    public String rejectReason = "";
    public String membershipId = "";
    public Date createdAt;
    public Date updatedAt;
    public Date reviewedAt;

    public static MembershipRequest from(DocumentSnapshot s) {
        MembershipRequest r = new MembershipRequest();
        r.id = s.getId();
        r.gymId = Docs.str(s, "gymId");
        r.gymName = Docs.str(s, "gymName");
        r.memberUid = Docs.str(s, "memberUid");
        r.memberName = Docs.str(s, "memberName");
        r.memberEmail = Docs.str(s, "memberEmail");
        r.memberPhone = Docs.str(s, "memberPhone");
        r.packageId = Docs.str(s, "packageId");
        r.packageName = Docs.str(s, "packageName");
        r.packagePrice = Docs.lng(s, "packagePrice");
        r.packageDurationDays = (int) Docs.lng(s, "packageDurationDays");
        r.note = Docs.str(s, "note");
        r.status = Docs.str(s, "status");
        r.reviewedBy = Docs.str(s, "reviewedBy");
        r.rejectReason = Docs.str(s, "rejectReason");
        r.membershipId = Docs.str(s, "membershipId");
        r.createdAt = Docs.date(s, "createdAt");
        r.updatedAt = Docs.date(s, "updatedAt");
        r.reviewedAt = Docs.date(s, "reviewedAt");
        return r;
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public String statusLabel() {
        switch (status) {
            case PENDING:
                return "Chờ duyệt";
            case APPROVED:
                return "Đã duyệt";
            case REJECTED:
                return "Bị từ chối";
            case CANCELLED:
                return "Đã hủy";
            default:
                return status;
        }
    }
}
