package com.example.gymmanagement.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

/** Hiệu ứng chuyển động; tự tắt khi người dùng đặt "Xóa hiệu ứng động" / tốc độ animation = 0. */
public final class Motion {
    private Motion() {
    }

    public static boolean reduced(Context c) {
        try {
            return Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    /** Thẻ xuất hiện nhẹ: trượt lên + mờ dần, so le theo vị trí. */
    public static void appear(View v, int index) {
        if (reduced(v.getContext())) return;
        v.setAlpha(0f);
        v.setTranslationY(UiUtil.dp(v.getContext(), 18));
        v.animate().alpha(1f).translationY(0f).setDuration(320)
                .setStartDelay(Math.min(index, 8) * 45L)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    public static void fadeIn(View v) {
        if (reduced(v.getContext())) {
            v.setAlpha(1f);
            return;
        }
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(260).start();
    }

    /** Hiệu ứng nhấn: thu nhỏ nhẹ khi chạm. Không chặn sự kiện click. */
    public static void press(View v) {
        v.setOnTouchListener((view, e) -> {
            if (reduced(view.getContext())) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    /** Số liệu dashboard chạy từ 0 tới giá trị thật. */
    public static void countUp(TextView tv, int to) {
        if (reduced(tv.getContext()) || to <= 0) {
            tv.setText(String.valueOf(to));
            return;
        }
        ValueAnimator a = ValueAnimator.ofInt(0, to);
        a.setDuration(700);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(x -> tv.setText(String.valueOf((int) x.getAnimatedValue())));
        a.start();
    }

    /** Hiệu ứng xác nhận: biểu tượng bật lên với overshoot. */
    public static void pop(View v) {
        if (reduced(v.getContext())) {
            v.setScaleX(1f);
            v.setScaleY(1f);
            return;
        }
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(new OvershootInterpolator(2f)).start();
    }

    /** Nhấp nháy mờ cho skeleton. */
    public static ObjectAnimator pulse(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0.35f);
        a.setDuration(750);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        if (!reduced(v.getContext())) a.start();
        return a;
    }
}
