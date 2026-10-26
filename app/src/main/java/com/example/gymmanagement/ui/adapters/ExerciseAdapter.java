package com.example.gymmanagement.ui.adapters;

import android.content.Context;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemExerciseBinding;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.ImageLoader;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.UiUtil;

import java.util.function.Consumer;

public class ExerciseAdapter extends SimpleAdapter<Exercise, ItemExerciseBinding> {
    public static int difficultyBadge(String difficulty) {
        if ("Cơ bản".equals(difficulty)) return UiUtil.BADGE_SUCCESS;
        if ("Trung bình".equals(difficulty)) return UiUtil.BADGE_WARNING;
        return UiUtil.BADGE_PRIMARY;
    }

    public ExerciseAdapter(Consumer<Exercise> onClick) {
        super(ItemExerciseBinding::inflate, (b, e, pos) -> {
            Context c = b.getRoot().getContext();
            ImageLoader.load(b.exiImage, e.imageUrl, R.drawable.bg_gym_placeholder);
            b.exiName.setText(e.name);
            UiUtil.badge(b.exiCategory, e.category, UiUtil.BADGE_INFO);
            UiUtil.badge(b.exiDifficulty, e.difficulty, difficultyBadge(e.difficulty));
            b.exiEquipment.setText(c.getString(R.string.ex_equipment_line, e.equipment));
            b.exiSets.setText(c.getString(R.string.ex_sets_line, e.recommendedSets, e.recommendedReps));
            boolean video = e.hasVideo();
            b.exiVideoIcon.setVisibility(video ? View.VISIBLE : View.GONE);
            b.exiVideo.setText(video ? c.getString(R.string.ex_has_video) : "");
            StringBuilder credit = new StringBuilder();
            if (!e.imageAttribution.isEmpty() && !e.imageLicense.isEmpty()) {
                credit.append(c.getString(R.string.ex_image_credit, e.imageAttribution, e.imageLicense));
            }
            if (!e.videoAttribution.isEmpty() && !e.videoLicense.isEmpty()) {
                if (credit.length() > 0) credit.append('\n');
                credit.append(c.getString(R.string.ex_video_credit, e.videoAttribution, e.videoLicense));
            }
            b.exiMediaCredit.setText(credit);
            b.exiMediaCredit.setVisibility(credit.length() > 0 ? View.VISIBLE : View.GONE);
            Motion.press(b.getRoot());
            b.getRoot().setOnClickListener(v -> onClick.accept(e));
        });
    }
}
