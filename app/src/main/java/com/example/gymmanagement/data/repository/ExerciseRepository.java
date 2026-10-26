package com.example.gymmanagement.data.repository;

import android.content.Context;

import com.example.gymmanagement.data.ExerciseCatalog;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Fb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** F05 – thư viện bài tập (đọc từ collection exercises). */
public class ExerciseRepository {

    public void listActive(Context context, Callback<List<Exercise>> cb) {
        final List<Exercise> bundled;
        try {
            bundled = ExerciseCatalog.load(context.getApplicationContext());
        } catch (Exception e) {
            cb.onError(AppError.unknown("Không tải được thư viện bài tập có sẵn trong ứng dụng."));
            return;
        }
        if (Fb.precheck() != null) {
            cb.onSuccess(bundled);
            return;
        }
        Fb.db().collection("exercises").whereEqualTo("isActive", true).get()
                .addOnSuccessListener(qs -> {
                    Map<String, Exercise> byId = new LinkedHashMap<>();
                    for (Exercise exercise : bundled) byId.put(exercise.id, exercise);
                    for (Exercise remote : Repo.map(qs, Exercise::from)) {
                        Exercise local = byId.get(remote.id);
                        if (local == null) byId.put(remote.id, remote);
                        else byId.put(remote.id, preferRemoteAndKeepLocalGaps(remote, local));
                    }
                    List<Exercise> l = new java.util.ArrayList<>(byId.values());
                    Collections.sort(l, (a, b) -> a.name.compareToIgnoreCase(b.name));
                    cb.onSuccess(l);
                })
                .addOnFailureListener(e -> cb.onSuccess(bundled));
    }

    private Exercise preferRemoteAndKeepLocalGaps(Exercise remote, Exercise local) {
        if (blank(remote.name)) remote.name = local.name;
        if (blank(remote.description)) remote.description = local.description;
        if (blank(remote.category)) remote.category = local.category;
        if (blank(remote.difficulty)) remote.difficulty = local.difficulty;
        if (remote.targetMuscles.isEmpty()) remote.targetMuscles = local.targetMuscles;
        if (remote.secondaryMuscles.isEmpty()) remote.secondaryMuscles = local.secondaryMuscles;
        if (blank(remote.equipment)) remote.equipment = local.equipment;
        if (blank(remote.imageUrl)) remote.imageUrl = local.imageUrl;
        if (blank(remote.videoUrl)) remote.videoUrl = local.videoUrl;
        if (blank(remote.videoType)) remote.videoType = local.videoType;
        if (blank(remote.videoThumbnailUrl)) remote.videoThumbnailUrl = local.videoThumbnailUrl;
        if (remote.videoDurationSeconds == 0) remote.videoDurationSeconds = local.videoDurationSeconds;
        if (blank(remote.videoAttribution)) remote.videoAttribution = local.videoAttribution;
        if (blank(remote.videoLicense)) remote.videoLicense = local.videoLicense;
        if (blank(remote.videoLicenseUrl)) remote.videoLicenseUrl = local.videoLicenseUrl;
        if (blank(remote.videoSourceUrl)) remote.videoSourceUrl = local.videoSourceUrl;
        if (blank(remote.videoSourceName)) remote.videoSourceName = local.videoSourceName;
        if (blank(remote.imageAttribution)) remote.imageAttribution = local.imageAttribution;
        if (blank(remote.imageLicense)) remote.imageLicense = local.imageLicense;
        if (blank(remote.imageLicenseUrl)) remote.imageLicenseUrl = local.imageLicenseUrl;
        if (blank(remote.imageSourceUrl)) remote.imageSourceUrl = local.imageSourceUrl;
        if (blank(remote.imageSourceName)) remote.imageSourceName = local.imageSourceName;
        if (blank(remote.setupPosition)) remote.setupPosition = local.setupPosition;
        if (blank(remote.bodyPlacement)) remote.bodyPlacement = local.bodyPlacement;
        if (remote.instructions.isEmpty()) remote.instructions = local.instructions;
        if (blank(remote.breathingGuide)) remote.breathingGuide = local.breathingGuide;
        if (blank(remote.rangeOfMotion)) remote.rangeOfMotion = local.rangeOfMotion;
        if (remote.commonMistakes.isEmpty()) remote.commonMistakes = local.commonMistakes;
        if (remote.safetyNotes.isEmpty()) remote.safetyNotes = local.safetyNotes;
        if (remote.recommendedSets == 0) remote.recommendedSets = local.recommendedSets;
        if (blank(remote.recommendedReps)) remote.recommendedReps = local.recommendedReps;
        if (remote.recommendedRestSeconds == 0) remote.recommendedRestSeconds = local.recommendedRestSeconds;
        return remote;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
