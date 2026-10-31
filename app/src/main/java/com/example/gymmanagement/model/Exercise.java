package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;

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
    public String videoAttribution = "";
    public String videoLicense = "";
    public String videoLicenseUrl = "";
    public String videoSourceUrl = "";
    public String videoSourceName = "";
    public String imageAttribution = "";
    public String imageLicense = "";
    public String imageLicenseUrl = "";
    public String imageSourceUrl = "";
    public String imageSourceName = "";
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
        e.videoAttribution = Docs.str(s, "videoAttribution");
        e.videoLicense = Docs.str(s, "videoLicense");
        e.videoLicenseUrl = Docs.str(s, "videoLicenseUrl");
        e.videoSourceUrl = Docs.str(s, "videoSourceUrl");
        e.videoSourceName = Docs.str(s, "videoSourceName");
        e.imageAttribution = Docs.str(s, "imageAttribution");
        e.imageLicense = Docs.str(s, "imageLicense");
        e.imageLicenseUrl = Docs.str(s, "imageLicenseUrl");
        e.imageSourceUrl = Docs.str(s, "imageSourceUrl");
        e.imageSourceName = Docs.str(s, "imageSourceName");
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
        if (e.videoUrl == null || e.videoUrl.trim().isEmpty() || !e.videoUrl.startsWith("https://")) {
            e.videoUrl = getRelevantVideoUrl(e.category);
            e.videoType = "mp4";
        }
        return e;
    }

    public static Exercise fromJson(JSONObject s) {
        Exercise e = new Exercise();
        e.id = s.optString("id", "");
        e.name = s.optString("name", "");
        e.description = s.optString("description", "");
        e.category = s.optString("category", "");
        e.difficulty = s.optString("difficulty", "");
        e.targetMuscles = jsonStrings(s.optJSONArray("targetMuscles"));
        e.secondaryMuscles = jsonStrings(s.optJSONArray("secondaryMuscles"));
        e.equipment = s.optString("equipment", "");
        e.imageUrl = s.optString("imageUrl", "");
        e.videoUrl = s.optString("videoUrl", "");
        e.videoType = s.optString("videoType", "");
        e.videoThumbnailUrl = s.optString("videoThumbnailUrl", "");
        e.videoDurationSeconds = s.optLong("videoDurationSeconds", 0);
        e.videoAttribution = s.optString("videoAttribution", "");
        e.videoLicense = s.optString("videoLicense", "");
        e.videoLicenseUrl = s.optString("videoLicenseUrl", "");
        e.videoSourceUrl = s.optString("videoSourceUrl", "");
        e.videoSourceName = s.optString("videoSourceName", "");
        e.imageAttribution = s.optString("imageAttribution", "");
        e.imageLicense = s.optString("imageLicense", "");
        e.imageLicenseUrl = s.optString("imageLicenseUrl", "");
        e.imageSourceUrl = s.optString("imageSourceUrl", "");
        e.imageSourceName = s.optString("imageSourceName", "");
        e.setupPosition = s.optString("setupPosition", "");
        e.bodyPlacement = s.optString("bodyPlacement", "");
        e.instructions = jsonStrings(s.optJSONArray("instructions"));
        e.breathingGuide = s.optString("breathingGuide", "");
        e.rangeOfMotion = s.optString("rangeOfMotion", "");
        e.commonMistakes = jsonStrings(s.optJSONArray("commonMistakes"));
        e.safetyNotes = jsonStrings(s.optJSONArray("safetyNotes"));
        e.recommendedSets = s.optInt("recommendedSets", 0);
        e.recommendedReps = s.optString("recommendedReps", "");
        e.recommendedRestSeconds = s.optInt("recommendedRestSeconds", 0);
        e.isActive = s.optBoolean("isActive", true);
        e.isSampleData = s.optBoolean("isSampleData", true);
        if (e.videoUrl == null || e.videoUrl.trim().isEmpty() || !e.videoUrl.startsWith("https://")) {
            e.videoUrl = getRelevantVideoUrl(e.category);
            e.videoType = "mp4";
        }
        return e;
    }

    public static String getRelevantVideoUrl(String category) {
        if (category == null) return "https://www.w3schools.com/html/mov_bbb.mp4";
        String c = category.toLowerCase(java.util.Locale.ROOT);
        if (c.contains("ngực")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
        if (c.contains("lưng")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
        if (c.contains("chân")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
        if (c.contains("vai")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";
        if (c.contains("tay")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4";
        if (c.contains("bụng")) return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4";
        return "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
    }

    private static List<String> jsonStrings(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) return values;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    /** Luôn có video phát hướng dẫn (tự động gán link mẫu nếu bài tập chưa có link). */
    public boolean hasVideo() {
        return true;
    }

    public String searchText() {
        return (name + " " + category + " " + String.join(" ", targetMuscles) + " "
                + String.join(" ", secondaryMuscles) + " " + equipment)
                .toLowerCase(java.util.Locale.ROOT);
    }
}
