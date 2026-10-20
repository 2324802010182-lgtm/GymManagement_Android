package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** users/{uid}/measurements/{id} – chỉ số cơ thể. */
public class Measurement {
    public String id = "";
    public double weightKg;
    public double heightCm;
    public Double bodyFatPercent;
    public double bmi;
    public Date measuredAt;
    public String note = "";

    public static Measurement from(DocumentSnapshot s) {
        Measurement m = new Measurement();
        m.id = s.getId();
        m.weightKg = Docs.dbl(s, "weightKg");
        m.heightCm = Docs.dbl(s, "heightCm");
        m.bodyFatPercent = Docs.dblOrNull(s, "bodyFatPercent");
        m.bmi = Docs.dbl(s, "bmi");
        m.measuredAt = Docs.date(s, "measuredAt");
        m.note = Docs.str(s, "note");
        return m;
    }
}
