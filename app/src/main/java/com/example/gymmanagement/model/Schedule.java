package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** gyms/{gymId}/schedules/{scheduleId} – lịch tập gán cho một hội viên. */
public class Schedule {
    public static final String SCHEDULED = "scheduled";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";

    public String id = "";
    public String gymId = "";
    public String gymName = "";
    public String memberUid = "";
    public String memberName = "";
    public String title = "";
    public String description = "";
    public String location = "";
    public Date startAt;
    public Date endAt;
    public String status = SCHEDULED;

    public static Schedule from(DocumentSnapshot s) {
        Schedule x = new Schedule();
        x.id = s.getId();
        x.gymId = Docs.str(s, "gymId");
        x.gymName = Docs.str(s, "gymName");
        x.memberUid = Docs.str(s, "memberUid");
        x.memberName = Docs.str(s, "memberName");
        x.title = Docs.str(s, "title");
        x.description = Docs.str(s, "description");
        x.location = Docs.str(s, "location");
        x.startAt = Docs.date(s, "startAt");
        x.endAt = Docs.date(s, "endAt");
        x.status = Docs.str(s, "status");
        return x;
    }

    public String statusLabel() {
        switch (status) {
            case SCHEDULED:
                return startAt != null && startAt.before(new Date()) && endAt != null && endAt.before(new Date())
                        ? "Đã qua" : "Sắp diễn ra";
            case COMPLETED:
                return "Hoàn thành";
            case CANCELLED:
                return "Đã hủy";
            default:
                return status;
        }
    }
}
