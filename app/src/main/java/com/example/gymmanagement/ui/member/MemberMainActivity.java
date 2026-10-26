package com.example.gymmanagement.ui.member;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityMemberMainBinding;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.ui.shared.ProfileFragment;
import com.example.gymmanagement.util.UiUtil;

/** Khung chính của hội viên với thanh điều hướng dưới và chuyển tab mượt (fade, giữ trạng thái từng tab). */
public class MemberMainActivity extends BaseActivity {
    private static final String[] TAGS = {"home", "schedule", "exercises", "progress", "profile"};
    private static final String KEY_TAB = "tab";

    private ActivityMemberMainBinding b;
    private String current = "home";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        b = ActivityMemberMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, false);
        if (savedInstanceState != null) current = savedInstanceState.getString(KEY_TAB, "home");
        b.memberNav.setOnItemSelectedListener(item -> {
            show(tagFor(item.getItemId()));
            return true;
        });
        b.memberNav.setSelectedItemId(idFor(current));
        show(current);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(KEY_TAB, current);
    }

    private String tagFor(int itemId) {
        if (itemId == R.id.nav_schedule) return TAGS[1];
        if (itemId == R.id.nav_exercises) return TAGS[2];
        if (itemId == R.id.nav_progress) return TAGS[3];
        if (itemId == R.id.nav_profile) return TAGS[4];
        return TAGS[0];
    }

    private int idFor(String tag) {
        switch (tag) {
            case "schedule":
                return R.id.nav_schedule;
            case "exercises":
                return R.id.nav_exercises;
            case "progress":
                return R.id.nav_progress;
            case "profile":
                return R.id.nav_profile;
            default:
                return R.id.nav_home;
        }
    }

    private Fragment create(String tag) {
        switch (tag) {
            case "schedule":
                return new MemberScheduleFragment();
            case "exercises":
                return new ExerciseListFragment();
            case "progress":
                return new ProgressFragment();
            case "profile":
                return new ProfileFragment();
            default:
                return new HomeFragment();
        }
    }

    private void show(String tag) {
        current = tag;
        b.memberNav.setVisibility("profile".equals(tag) ? android.view.View.GONE : android.view.View.VISIBLE);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        for (String s : TAGS) {
            Fragment f = fm.findFragmentByTag(s);
            if (f != null && !s.equals(tag)) t.hide(f);
        }
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) t.add(R.id.member_container, create(tag), tag);
        else t.show(target);
        t.commit();
    }
}
