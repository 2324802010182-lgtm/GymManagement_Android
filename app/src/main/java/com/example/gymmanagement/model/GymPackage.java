package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.List;

/** gyms/{gymId}/packages/{packageId} – gói tập. */
public class GymPackage {
    public String id = "";
    public String gymId = "";
    public String name = "";
    public long price;
    public int durationDays = 30;
    public String description = "";
    public List<String> benefits = new java.util.ArrayList<>();
    public boolean isActive = true;
    public boolean isSampleData;
    public Date createdAt;
    public Date updatedAt;

    public static GymPackage from(DocumentSnapshot s) {
        GymPackage p = new GymPackage();
        p.id = s.getId();
        p.gymId = Docs.str(s, "gymId");
        p.name = Docs.str(s, "name");
        p.price = Docs.lng(s, "price");
        p.durationDays = (int) Docs.lng(s, "durationDays");
        p.description = Docs.str(s, "description");
        p.benefits = Docs.strList(s, "benefits");
        p.isActive = Docs.bool(s, "isActive");
        p.isSampleData = Docs.bool(s, "isSampleData");
        p.createdAt = Docs.date(s, "createdAt");
        p.updatedAt = Docs.date(s, "updatedAt");
        return p;
    }
}
