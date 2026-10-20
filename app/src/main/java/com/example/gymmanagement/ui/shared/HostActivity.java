package com.example.gymmanagement.ui.shared;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ActivityHostBinding;
import com.example.gymmanagement.ui.member.ExerciseDetailFragment;
import com.example.gymmanagement.ui.member.GymDetailFragment;
import com.example.gymmanagement.ui.member.MeasurementsFragment;
import com.example.gymmanagement.ui.member.MyGymsFragment;
import com.example.gymmanagement.ui.owner.AttendanceFragment;
import com.example.gymmanagement.ui.owner.BrandFragment;
import com.example.gymmanagement.ui.owner.MemberDetailFragment;
import com.example.gymmanagement.ui.owner.OwnerNotificationsFragment;
import com.example.gymmanagement.ui.owner.OwnerScheduleFragment;
import com.example.gymmanagement.util.UiUtil;

/** Khung chứa các màn hình chi tiết (một Fragment mỗi Activity) với hiệu ứng trượt/mờ khi mở. */
public class HostActivity extends BaseActivity {
    public static final String GYM_DETAIL = "gym_detail";
    public static final String EXERCISE_DETAIL = "exercise_detail";
    public static final String NOTIFICATIONS = "notifications";
    public static final String MEASUREMENTS = "measurements";
    public static final String MY_GYMS = "my_gyms";
    public static final String EDIT_PROFILE = "edit_profile";
    public static final String SCHEDULES = "schedules";
    public static final String ATTENDANCE = "attendance";
    public static final String OWNER_NOTIFICATIONS = "owner_notifications";
    public static final String BRAND = "brand";
    public static final String MEMBER_DETAIL = "member_detail";

    private static final String EXTRA_SCREEN = "screen";
    private static final String EXTRA_ARGS = "args";

    public static void open(Context c, String screen, @Nullable Bundle args) {
        Intent i = new Intent(c, HostActivity.class);
        i.putExtra(EXTRA_SCREEN, screen);
        if (args != null) i.putExtra(EXTRA_ARGS, args);
        ActivityOptionsCompat o = ActivityOptionsCompat.makeCustomAnimation(c, R.anim.slide_in_right, R.anim.fade_out);
        c.startActivity(i, o.toBundle());
    }

    private ActivityHostBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) return;
        binding = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtil.applyInsets(binding.getRoot(), true, true);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (savedInstanceState == null) {
            Fragment f = create(getIntent().getStringExtra(EXTRA_SCREEN), getIntent().getBundleExtra(EXTRA_ARGS));
            if (f == null) {
                finish();
                return;
            }
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.host_container, f)
                    .commit();
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (binding != null) binding.toolbar.setTitle(title);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    private Fragment create(String screen, Bundle args) {
        if (screen == null) return null;
        Fragment f;
        switch (screen) {
            case GYM_DETAIL:
                f = new GymDetailFragment();
                break;
            case EXERCISE_DETAIL:
                f = new ExerciseDetailFragment();
                break;
            case NOTIFICATIONS:
                f = new NotificationsFragment();
                break;
            case MEASUREMENTS:
                f = new MeasurementsFragment();
                break;
            case MY_GYMS:
                f = new MyGymsFragment();
                break;
            case EDIT_PROFILE:
                f = new EditProfileFragment();
                break;
            case SCHEDULES:
                f = new OwnerScheduleFragment();
                break;
            case ATTENDANCE:
                f = new AttendanceFragment();
                break;
            case OWNER_NOTIFICATIONS:
                f = new OwnerNotificationsFragment();
                break;
            case BRAND:
                f = new BrandFragment();
                break;
            case MEMBER_DETAIL:
                f = new MemberDetailFragment();
                break;
            default:
                return null;
        }
        if (args != null) f.setArguments(args);
        return f;
    }

    public void hideToolbar(boolean hide) {
        binding.appbar.setVisibility(hide ? View.GONE : View.VISIBLE);
    }
}
