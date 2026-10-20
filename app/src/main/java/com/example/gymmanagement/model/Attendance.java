package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** gyms/{gymId}/attendance/{memberUid_yyyyMMdd}. */
public class Attendance {
    public String id = "";
    public String gymId = "";
    public String gymName = "";
    public String memberUid = "";
    public String memberName = "";
    public String membershipId = "";
    public String packageName = "";
    public String dateKey = "";
    public Date checkInAt;
    public String recordedBy = "";
    public String recordedByName = "";

    public static Attendance from(DocumentSnapshot s) {
        Attendance a = new Attendance();
        a.id = s.getId();
        a.gymId = Docs.str(s, "gymId");
        a.gymName = Docs.str(s, "gymName");
        a.memberUid = Docs.str(s, "memberUid");
        a.memberName = Docs.str(s, "memberName");
        a.membershipId = Docs.str(s, "membershipId");
        a.packageName = Docs.str(s, "packageName");
        a.dateKey = Docs.str(s, "dateKey");
        a.checkInAt = Docs.date(s, "checkInAt");
        a.recordedBy = Docs.str(s, "recordedBy");
        a.recordedByName = Docs.str(s, "recordedByName");
        return a;
    }
}
