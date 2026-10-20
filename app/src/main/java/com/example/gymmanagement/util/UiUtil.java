package com.example.gymmanagement.util;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gymmanagement.R;
import com.google.android.material.snackbar.Snackbar;

public final class UiUtil {
    public static final int BADGE_PRIMARY = 0;
    public static final int BADGE_SUCCESS = 1;
    public static final int BADGE_WARNING = 2;
    public static final int BADGE_INFO = 3;
    public static final int BADGE_NEUTRAL = 4;

    private UiUtil() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int dp(float v) {
        return Math.round(v * Resources.getSystem().getDisplayMetrics().density);
    }

    /** Áp dụng insets (status bar, thanh điều hướng, bàn phím) làm padding cho view gốc – bắt buộc khi edge-to-edge. */
    public static void applyInsets(View root, boolean top, boolean bottom) {
        final int pl = root.getPaddingLeft();
        final int pt = root.getPaddingTop();
        final int pr = root.getPaddingRight();
        final int pb = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(pl + bars.left, pt + (top ? bars.top : 0), pr + bars.right,
                    pb + (bottom ? Math.max(bars.bottom, ime.bottom) : 0));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static void hideKeyboard(View v) {
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void snack(View anchor, String msg) {
        if (anchor == null) return;
        Snackbar.make(anchor, msg, Snackbar.LENGTH_LONG).show();
    }

    /** Tô màu nhãn trạng thái dạng viên thuốc. */
    public static void badge(TextView tv, String text, int type) {
        tv.setText(text);
        Context c = tv.getContext();
        int bg;
        int fg;
        switch (type) {
            case BADGE_SUCCESS:
                bg = R.drawable.bg_badge_success;
                fg = R.color.success;
                break;
            case BADGE_WARNING:
                bg = R.drawable.bg_badge_warning;
                fg = R.color.warning;
                break;
            case BADGE_INFO:
                bg = R.drawable.bg_badge_info;
                fg = R.color.info;
                break;
            case BADGE_NEUTRAL:
                bg = R.drawable.bg_badge_neutral;
                fg = R.color.text_secondary;
                break;
            default:
                bg = R.drawable.bg_badge_primary;
                fg = R.color.primary;
        }
        tv.setBackgroundResource(bg);
        tv.setTextColor(c.getColor(fg));
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }
}
