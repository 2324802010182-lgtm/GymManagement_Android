package com.example.gymmanagement.ui.owner;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityOwnerMainBinding;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.util.UiUtil;

/** Khung chính của Chủ Gym với thanh điều hướng dưới và chuyển tab mượt (fade, giữ trạng thái từng tab). */
public class OwnerMainActivity extends BaseActivity {
    private static final String[] TAGS = {"dashboard", "packages", "requests", "members", "more"};
    private static final String KEY_TAB = "tab";

    private ActivityOwnerMainBinding b;
    private String current = "dashboard";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        b = ActivityOwnerMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, false);
        if (savedInstanceState != null) current = savedInstanceState.getString(KEY_TAB, "dashboard");
        b.ownerNav.setOnItemSelectedListener(item -> {
            show(tagFor(item.getItemId()));
            return true;
        });
        b.ownerNav.setSelectedItemId(idFor(current));
        show(current);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(KEY_TAB, current);
    }

    private String tagFor(int itemId) {
        if (itemId == R.id.nav_packages) return TAGS[1];
        if (itemId == R.id.nav_requests) return TAGS[2];
        if (itemId == R.id.nav_members) return TAGS[3];
        if (itemId == R.id.nav_more) return TAGS[4];
        return TAGS[0];
    }

    private int idFor(String tag) {
        switch (tag) {
            case "packages":
                return R.id.nav_packages;
            case "requests":
                return R.id.nav_requests;
            case "members":
                return R.id.nav_members;
            case "more":
                return R.id.nav_more;
            default:
                return R.id.nav_dashboard;
        }
    }

    private Fragment create(String tag) {
        switch (tag) {
            case "packages":
                return new PackagesFragment();
            case "requests":
                return new RequestsFragment();
            case "members":
                return new MembersFragment();
            case "more":
                return new OwnerMoreFragment();
            default:
                return new OwnerDashboardFragment();
        }
    }

    private void show(String tag) {
        current = tag;
        b.ownerNav.setVisibility("more".equals(tag) ? android.view.View.GONE : android.view.View.VISIBLE);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        for (String s : TAGS) {
            Fragment f = fm.findFragmentByTag(s);
            if (f != null && !s.equals(tag)) t.hide(f);
        }
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) t.add(R.id.owner_container, create(tag), tag);
        else t.show(target);
        t.commit();
    }
}
