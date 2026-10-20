package com.example.gymmanagement.ui.components;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.gymmanagement.R;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Khung trạng thái dùng cho mọi màn hình danh sách: loading (skeleton nhấp nháy), content, empty, error,
 * không có quyền, mất mạng. Các view con khai báo trong XML là "content".
 */
public class StateLayout extends FrameLayout {
    private final List<View> content = new ArrayList<>();
    private View skeleton;
    private View empty;
    private View error;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private TextView errorTitle;
    private TextView errorMessage;
    private ImageView errorIcon;
    private View retryButton;
    private ObjectAnimator pulse;
    private Runnable retry;

    public StateLayout(Context c) {
        super(c);
    }

    public StateLayout(Context c, AttributeSet a) {
        super(c, a);
    }

    public StateLayout(Context c, AttributeSet a, int s) {
        super(c, a, s);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) content.add(getChildAt(i));
        View overlay = inflate(getContext(), R.layout.view_state, null);
        addView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        skeleton = overlay.findViewById(R.id.state_skeleton);
        empty = overlay.findViewById(R.id.state_empty);
        error = overlay.findViewById(R.id.state_error);
        emptyTitle = overlay.findViewById(R.id.state_empty_title);
        emptyMessage = overlay.findViewById(R.id.state_empty_message);
        errorTitle = overlay.findViewById(R.id.state_error_title);
        errorMessage = overlay.findViewById(R.id.state_error_message);
        errorIcon = overlay.findViewById(R.id.state_error_icon);
        retryButton = overlay.findViewById(R.id.state_retry);
        retryButton.setOnClickListener(v -> {
            if (retry != null) retry.run();
        });
        showContent();
    }

    private void only(View shown) {
        for (View v : content) v.setVisibility(shown == null ? VISIBLE : GONE);
        skeleton.setVisibility(shown == skeleton ? VISIBLE : GONE);
        empty.setVisibility(shown == empty ? VISIBLE : GONE);
        error.setVisibility(shown == error ? VISIBLE : GONE);
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
    }

    public void showLoading() {
        only(skeleton);
        pulse = Motion.pulse(skeleton);
    }

    public void showContent() {
        only(null);
    }

    public void showEmpty(String title, String message) {
        only(empty);
        emptyTitle.setText(title);
        emptyMessage.setText(message);
        emptyMessage.setVisibility(message == null || message.isEmpty() ? GONE : VISIBLE);
        Motion.fadeIn(empty);
    }

    public void showError(AppError e, Runnable onRetry) {
        only(error);
        retry = onRetry;
        retryButton.setVisibility(onRetry == null ? GONE : VISIBLE);
        switch (e.kind) {
            case NETWORK:
                errorIcon.setImageResource(R.drawable.ic_cloud_off);
                errorTitle.setText(R.string.state_network_title);
                break;
            case PERMISSION:
                errorIcon.setImageResource(R.drawable.ic_lock);
                errorTitle.setText(R.string.state_permission_title);
                break;
            case NOT_CONFIGURED:
                errorIcon.setImageResource(R.drawable.ic_info);
                errorTitle.setText(R.string.state_not_configured_title);
                break;
            default:
                errorIcon.setImageResource(R.drawable.ic_error);
                errorTitle.setText(R.string.state_error_title);
        }
        errorMessage.setText(e.message);
        Motion.fadeIn(error);
    }

    /** Hiển thị theo Resource: loading/empty/error/success. */
    public <T> void render(Resource<T> r, String emptyTitleText, String emptyMessageText, Runnable onRetry) {
        if (r == null) return;
        switch (r.status) {
            case LOADING:
                showLoading();
                break;
            case EMPTY:
                showEmpty(emptyTitleText, emptyMessageText);
                break;
            case ERROR:
                showError(r.error, onRetry);
                break;
            default:
                showContent();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulse != null) pulse.cancel();
        super.onDetachedFromWindow();
    }
}
