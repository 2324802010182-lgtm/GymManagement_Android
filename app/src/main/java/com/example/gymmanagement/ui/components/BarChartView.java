package com.example.gymmanagement.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.UiUtil;

import java.util.ArrayList;
import java.util.List;

/** Biểu đồ cột tự vẽ (số buổi tập theo khoảng thời gian). */
public class BarChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<ChartViews.Point> bars = new ArrayList<>();
    private float progress = 1f;

    public BarChartView(Context c) {
        this(c, null);
    }

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        bar.setColor(c.getColor(R.color.primary));
        text.setColor(c.getColor(R.color.text_secondary));
        text.setTextSize(UiUtil.dp(c, 11));
        text.setTextAlign(Paint.Align.CENTER);
        grid.setColor(c.getColor(R.color.divider));
        grid.setStrokeWidth(UiUtil.dp(c, 1));
    }

    /** x không dùng; y = giá trị cột; label = nhãn dưới cột. */
    public void setBars(List<ChartViews.Point> b) {
        bars = b == null ? new ArrayList<>() : b;
        if (Motion.reduced(getContext())) {
            progress = 1f;
            invalidate();
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(600);
        a.addUpdateListener(x -> {
            progress = (float) x.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = bars.size();
        if (n == 0) return;
        float padB = UiUtil.dp(getContext(), 22);
        float padT = UiUtil.dp(getContext(), 18);
        float h = getHeight() - padB - padT;
        float max = 1f;
        for (ChartViews.Point p : bars) max = Math.max(max, p.y);
        canvas.drawLine(0, padT + h, getWidth(), padT + h, grid);
        float slot = getWidth() / (float) n;
        float bw = Math.min(slot * 0.6f, UiUtil.dp(getContext(), 34));
        for (int i = 0; i < n; i++) {
            ChartViews.Point p = bars.get(i);
            float cx = slot * i + slot / 2f;
            float bh = h * (p.y / max) * progress;
            RectF r = new RectF(cx - bw / 2f, padT + h - bh, cx + bw / 2f, padT + h);
            canvas.drawRoundRect(r, UiUtil.dp(getContext(), 6), UiUtil.dp(getContext(), 6), bar);
            if (p.y > 0) canvas.drawText(String.valueOf((int) p.y), cx, padT + h - bh - UiUtil.dp(getContext(), 4), text);
            canvas.drawText(p.label, cx, getHeight() - UiUtil.dp(getContext(), 6), text);
        }
    }
}
