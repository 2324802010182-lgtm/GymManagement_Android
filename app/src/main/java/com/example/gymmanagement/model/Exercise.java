package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** exercises/{exerciseId} – thư viện bài tập (ghi bằng seed/Admin SDK, ứng dụng chỉ đọc). */
public class Exercise implements java.io.Serializable {
    public static final String[] CATEGORIES = {"Ngực", "Lưng", "Chân", "Vai", "Tay", "Bụng", "Cardio"};
    public static final String[] DIFFICULTIES = {"Cơ bản", "Trung bình", "Nâng cao"};

    public String id = "";
    public String name = "";
    public String description = "";
    public String category = "";
    public String difficulty = "";
    public List<String> targetMuscles = new ArrayList<>();
    public List<String> secondaryMuscles = new ArrayList<>();
    public String equipment = "";
    public String imageUrl = "";
    public String videoUrl = "";
    public String videoType = "";
    public String videoThumbnailUrl = "";
    public long videoDurationSeconds;
    public String setupPosition = "";
    public String bodyPlacement = "";
    public List<String> instructions = new ArrayList<>();
    public String breathingGuide = "";
    public String rangeOfMotion = "";
    public List<String> commonMistakes = new ArrayList<>();
    public List<String> safetyNotes = new ArrayList<>();
    public int recommendedSets;
    public String recommendedReps = "";
    public int recommendedRestSeconds;
    public boolean isActive = true;
    public boolean isSampleData;

    public static Exercise from(DocumentSnapshot s) {
        Exercise e = new Exercise();
        e.id = s.getId();
        e.name = Docs.str(s, "name");
        e.description = Docs.str(s, "description");
        e.category = Docs.str(s, "category");
        e.difficulty = Docs.str(s, "difficulty");
        e.targetMuscles = Docs.strList(s, "targetMuscles");
        e.secondaryMuscles = Docs.strList(s, "secondaryMuscles");
        e.equipment = Docs.str(s, "equipment");
        e.imageUrl = Docs.str(s, "imageUrl");
        e.videoUrl = Docs.str(s, "videoUrl");
        e.videoType = Docs.str(s, "videoType");
        e.videoThumbnailUrl = Docs.str(s, "videoThumbnailUrl");
        e.videoDurationSeconds = Docs.lng(s, "videoDurationSeconds");
        e.setupPosition = Docs.str(s, "setupPosition");
        e.bodyPlacement = Docs.str(s, "bodyPlacement");
        e.instructions = Docs.strList(s, "instructions");
        e.breathingGuide = Docs.str(s, "breathingGuide");
        e.rangeOfMotion = Docs.str(s, "rangeOfMotion");
        e.commonMistakes = Docs.strList(s, "commonMistakes");
        e.safetyNotes = Docs.strList(s, "safetyNotes");
        e.recommendedSets = (int) Docs.lng(s, "recommendedSets");
        e.recommendedReps = Docs.str(s, "recommendedReps");
        e.recommendedRestSeconds = (int) Docs.lng(s, "recommendedRestSeconds");
        e.isActive = Docs.bool(s, "isActive");
        e.isSampleData = Docs.bool(s, "isSampleData");
        return e;
    }

    /** Chỉ phát video khi có URL HTTPS hợp lệ (không giả định URL trang YouTube là file video). */
    public boolean hasVideo() {
        return videoUrl != null && videoUrl.startsWith("https://") && videoUrl.length() > 12;
    }

    public String searchText() {
        return (name + " " + category + " " + String.join(" ", targetMuscles) + " " + equipment)
                .toLowerCase(java.util.Locale.ROOT);
    }
}
