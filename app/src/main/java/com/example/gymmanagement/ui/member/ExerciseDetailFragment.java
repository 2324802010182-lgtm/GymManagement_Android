package com.example.gymmanagement.ui.member;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.databinding.FragmentExerciseDetailBinding;
import com.example.gymmanagement.databinding.ItemGuideSectionBinding;
import com.example.gymmanagement.databinding.ItemGuideLineBinding;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.ui.adapters.ExerciseAdapter;
import com.example.gymmanagement.util.ImageLoader;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.ExerciseViewModel;

import java.util.List;

/** F05 – chi tiết bài tập: ảnh, thông số, video thật (ExoPlayer) và hướng dẫn thực hiện riêng từng bài. */
public class ExerciseDetailFragment extends Fragment {
    public static final String ARG_EXERCISE = "exercise";

    private FragmentExerciseDetailBinding b;
    private ExerciseViewModel vm;
    private Exercise ex;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentExerciseDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ex = (Exercise) requireArguments().getSerializable(ARG_EXERCISE);
        if (ex == null) {
            requireActivity().finish();
            return;
        }
        requireActivity().setTitle(ex.name);
        vm = new ViewModelProvider(this).get(ExerciseViewModel.class);

        ImageLoader.load(b.exdImage, ex.imageUrl, R.drawable.bg_gym_placeholder);
        b.exdName.setText(ex.name);
        UiUtil.badge(b.exdCategory, ex.category, UiUtil.BADGE_INFO);
        UiUtil.badge(b.exdDifficulty, ex.difficulty, ExerciseAdapter.difficultyBadge(ex.difficulty));
        UiUtil.badge(b.exdEquipment, ex.equipment, UiUtil.BADGE_NEUTRAL);
        b.exdSets.setText(String.valueOf(ex.recommendedSets));
        b.exdReps.setText(ex.recommendedReps);
        b.exdRest.setText(getString(R.string.ex_rest_value, ex.recommendedRestSeconds));
        b.exdMuscles.setText(getString(R.string.ex_muscles_line, join(ex.targetMuscles),
                ex.secondaryMuscles.isEmpty() ? getString(R.string.none) : join(ex.secondaryMuscles)));

        boolean hasVideo = ex.hasVideo();
        b.exdVideoTitle.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
        b.exdImage.setVisibility(hasVideo ? View.GONE : View.VISIBLE);
        if (hasVideo) {
            b.exdVideo.setVisibility(View.VISIBLE);
            b.exdNoVideo.setVisibility(View.GONE);
            String poster = ex.videoThumbnailUrl.isEmpty() ? ex.imageUrl : ex.videoThumbnailUrl;
            b.exdVideo.setSource(ex.videoUrl, ex.videoType, poster);
        } else {
            b.exdVideo.setVisibility(View.GONE);
            b.exdNoVideo.setVisibility(View.VISIBLE);
        }
        bindMediaCredits();

        // HƯỚNG DẪN THỰC HIỆN – nội dung riêng của từng bài, lấy từ Firestore
        addSection(getString(R.string.guide_setup), ex.setupPosition, null, false);
        addSection(getString(R.string.guide_placement), ex.bodyPlacement, null, false);
        addSection(getString(R.string.guide_steps), null, ex.instructions, true);
        addSection(getString(R.string.guide_breathing), ex.breathingGuide, null, false);
        addSection(getString(R.string.guide_range), ex.rangeOfMotion, null, false);
        addSection(getString(R.string.guide_mistakes), null, ex.commonMistakes, false);
        addSection(getString(R.string.guide_safety), null, ex.safetyNotes, false);

        StringBuilder info = new StringBuilder(ex.description);
        if (ex.isSampleData) info.append("\n\n").append(getString(R.string.ex_sample_note));
        b.exdInfo.setText(info.toString().trim());

        boolean canLogWorkout = Session.user != null && Session.user.isMember();
        b.exdComplete.setVisibility(canLogWorkout ? View.VISIBLE : View.GONE);
        if (canLogWorkout) {
            b.exdComplete.setOnClickListener(v -> CompleteExerciseSheet.newInstance().show(getChildFragmentManager(), "done"));
            vm.action.observe(getViewLifecycleOwner(), ev -> {
                if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
                Resource<String> r = ev.peek();
                UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
            });
        }
    }

    public Exercise exercise() {
        return ex;
    }

    private String join(List<String> l) {
        return String.join(", ", l);
    }

    private void bindMediaCredits() {
        SpannableStringBuilder credits = new SpannableStringBuilder();
        appendCredit(credits, "Ảnh", ex.imageAttribution, ex.imageLicense,
                ex.imageSourceUrl, ex.imageLicenseUrl);
        appendCredit(credits, "Video", ex.videoAttribution, ex.videoLicense,
                ex.videoSourceUrl, ex.videoLicenseUrl);
        b.exdMediaCredit.setVisibility(credits.length() == 0 ? View.GONE : View.VISIBLE);
        if (credits.length() > 0) {
            b.exdMediaCredit.setText(credits);
            b.exdMediaCredit.setMovementMethod(LinkMovementMethod.getInstance());
            b.exdMediaCredit.setLinkTextColor(requireContext().getColor(R.color.primary));
        }
    }

    private void appendCredit(SpannableStringBuilder out, String type, String author, String license,
                              String sourceUrl, String licenseUrl) {
        if (author == null || author.trim().isEmpty() || license == null || license.trim().isEmpty()) return;
        if (out.length() > 0) out.append('\n');
        out.append(type).append(": ");
        int authorStart = out.length();
        out.append(author);
        if (isHttps(sourceUrl)) addLink(out, authorStart, out.length(), sourceUrl);
        out.append(" · ");
        int licenseStart = out.length();
        out.append(license);
        if (isHttps(licenseUrl)) addLink(out, licenseStart, out.length(), licenseUrl);
        if (isHttps(sourceUrl)) {
            out.append(" · ");
            int sourceStart = out.length();
            out.append("wger.de");
            addLink(out, sourceStart, out.length(), sourceUrl);
        }
    }

    private void addLink(SpannableStringBuilder text, int start, int end, String url) {
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                    UiUtil.snack(b.getRoot(), getString(R.string.video_error));
                }
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }

    private void addSection(String title, String text, List<String> lines, boolean numbered) {
        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasLines = lines != null && !lines.isEmpty();
        if (!hasText && !hasLines) return;
        LayoutInflater inf = LayoutInflater.from(requireContext());
        ItemGuideSectionBinding s = ItemGuideSectionBinding.inflate(inf, b.exdGuide, false);
        s.guideTitle.setText(title);
        if (hasText) s.guideText.setText(text); else s.guideText.setVisibility(View.GONE);
        if (hasLines) {
            for (int i = 0; i < lines.size(); i++) {
                ItemGuideLineBinding l = ItemGuideLineBinding.inflate(inf, s.guideLines, false);
                l.lineMarker.setText(numbered ? (i + 1) + "." : "•");
                l.lineText.setText(lines.get(i));
                s.guideLines.addView(l.getRoot());
            }
        }
        b.exdGuide.addView(s.getRoot());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (b != null && ex != null && ex.hasVideo()) b.exdVideo.start();
    }

    @Override
    public void onStop() {
        if (b != null) b.exdVideo.stop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (b != null) b.exdVideo.stop();
        b = null;
        super.onDestroyView();
    }
}
