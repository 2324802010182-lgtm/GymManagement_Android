package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** users/{uid}/notifications/{id} – thông báo trong ứng dụng. */
public class AppNotification {
    public String id = "";
    public String recipientUid = "";
    public String gymId = "";
    public String gymName = "";
    public String title = "";
    public String body = "";
    public String type = "";
    public String relatedId = "";
    public boolean isRead;
    public String senderUid = "";
    public Date createdAt;

    public static AppNotification from(DocumentSnapshot s) {
        AppNotification n = new AppNotification();
        n.id = s.getId();
        n.recipientUid = Docs.str(s, "recipientUid");
        n.gymId = Docs.str(s, "gymId");
        n.gymName = Docs.str(s, "gymName");
        n.title = Docs.str(s, "title");
        n.body = Docs.str(s, "body");
        n.type = Docs.str(s, "type");
        n.relatedId = Docs.str(s, "relatedId");
        n.isRead = Docs.bool(s, "isRead");
        n.senderUid = Docs.str(s, "senderUid");
        n.createdAt = Docs.date(s, "createdAt");
        return n;
    }
}
