package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** gyms/{gymId}/members/{memberUid} – hồ sơ hội viên trong phạm vi một Gym (tạo khi duyệt yêu cầu). */
public class MemberSummary implements java.io.Serializable {
    public String memberUid = "";
    public String gymId = "";
    public String fullName = "";
    public String email = "";
    public String phone = "";
    public String latestMembershipId = "";
    public String latestPackageName = "";
    public Date latestEndAt;
    public Date joinedAt;

    public static MemberSummary from(DocumentSnapshot s) {
        MemberSummary m = new MemberSummary();
        m.memberUid = s.getId();
        m.gymId = Docs.str(s, "gymId");
        m.fullName = Docs.str(s, "fullName");
        m.email = Docs.str(s, "email");
        m.phone = Docs.str(s, "phone");
        m.latestMembershipId = Docs.str(s, "latestMembershipId");
        m.latestPackageName = Docs.str(s, "latestPackageName");
        m.latestEndAt = Docs.date(s, "latestEndAt");
        m.joinedAt = Docs.date(s, "joinedAt");
        return m;
    }

    public boolean isActiveNow() {
        return latestEndAt != null && latestEndAt.after(new Date());
    }
}
