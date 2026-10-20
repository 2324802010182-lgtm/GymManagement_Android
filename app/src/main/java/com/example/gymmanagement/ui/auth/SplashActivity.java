package com.example.gymmanagement.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.databinding.ActivitySplashBinding;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.shared.BaseActivity;
import com.example.gymmanagement.ui.shared.Router;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Fb;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.AuthViewModel;

/** Splash: hiệu ứng logo, kiểm tra cấu hình Firebase, khôi phục phiên và điều hướng theo vai trò thực tế. */
public class SplashActivity extends BaseActivity {
    private static final long MIN_SPLASH_MS = 1100;

    private ActivitySplashBinding b;
    private AuthViewModel vm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startedAt;
    private User pendingUser;
    private boolean navigated;

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtil.applyInsets(b.getRoot(), true, true);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);
        startedAt = System.currentTimeMillis();

        playIntro();
        b.splashRetry.setOnClickListener(v -> start());
        b.splashSignout.setOnClickListener(v -> {
            Repos.auth().signOut();
            goLogin(null);
        });

        vm.result().observe(this, ev -> {
            if (ev == null) return;
            Resource<User> r = ev.peek();
            if (r.status == Resource.Status.LOADING) return;
            if (ev.getIfNotHandled() == null) return;
            if (r.status == Resource.Status.SUCCESS) {
                pendingUser = r.data;
                navigateWhenReady();
            } else {
                showError(r.error);
            }
        });
        start();
    }

    private void playIntro() {
        if (Motion.reduced(this)) return;
        b.splashLogo.setScaleX(0.4f);
        b.splashLogo.setScaleY(0.4f);
        b.splashLogo.setAlpha(0f);
        b.splashLogo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.6f)).start();
        for (View v : new View[]{b.splashTitle, b.splashTagline}) {
            v.setAlpha(0f);
            v.setTranslationY(UiUtil.dp(this, 14));
            v.animate().alpha(1f).translationY(0f).setStartDelay(250).setDuration(450).start();
        }
    }

    private void start() {
        b.splashError.setVisibility(View.GONE);
        b.splashProgress.setVisibility(View.VISIBLE);
        if (!Fb.isConfigured(this)) {
            showError(AppError.notConfigured());
            b.splashRetry.setVisibility(View.GONE);
            return;
        }
        b.splashRetry.setVisibility(View.VISIBLE);
        if (!Repos.auth().isSignedIn()) {
            handler.postDelayed(() -> goLogin(null), remaining());
            return;
        }
        vm.restoreSession();
    }

    private long remaining() {
        return Math.max(0, MIN_SPLASH_MS - (System.currentTimeMillis() - startedAt));
    }

    private void navigateWhenReady() {
        handler.postDelayed(() -> {
            if (navigated || isFinishing() || pendingUser == null) return;
            Intent i = Router.destination(this, pendingUser);
            if (i == null) {
                String msg = Router.deniedMessage(pendingUser);
                Repos.auth().signOut();
                goLogin(msg);
                return;
            }
            navigated = true;
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, remaining());
    }

    private void goLogin(String message) {
        if (navigated || isFinishing()) return;
        navigated = true;
        Intent i = new Intent(this, LoginActivity.class);
        if (message != null) i.putExtra(LoginActivity.EXTRA_MESSAGE, message);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showError(AppError e) {
        b.splashProgress.setVisibility(View.GONE);
        b.splashError.setVisibility(View.VISIBLE);
        b.splashErrorTitle.setText(e.kind == AppError.Kind.NOT_CONFIGURED
                ? R.string.state_not_configured_title
                : e.kind == AppError.Kind.NETWORK ? R.string.state_network_title : R.string.state_error_title);
        b.splashErrorMessage.setText(e.message);
        b.splashSignout.setVisibility(e.kind == AppError.Kind.NOT_CONFIGURED ? View.GONE : View.VISIBLE);
        Motion.fadeIn(b.splashError);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
