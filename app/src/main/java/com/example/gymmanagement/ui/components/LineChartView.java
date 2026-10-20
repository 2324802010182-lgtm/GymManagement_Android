package com.example.gymmanagement.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Motion;
import com.example.gymmanagement.util.UiUtil;

import java.util.ArrayList;
import java.util.List;

/** Biểu đồ đường tự vẽ (không dùng thư viện ngoài), có hiệu ứng vẽ dần. Chỉ vẽ khi có dữ liệu thật. */
public class LineChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<ChartViews.Point> points = new ArrayList<>();
    private float progress = 1f;

    public LineChartView(Context c) {
        this(c, null);
    }

    public LineChartView(Context c, AttributeSet a) {
        super(c, a);
        int primary = c.getColor(R.color.primary);
        line.setColor(primary);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(UiUtil.dp(c, 3));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        fill.setColor(c.getColor(R.color.primary_soft));
        dot.setColor(primary);
        grid.setColor(c.getColor(R.color.divider));
        grid.setStrokeWidth(UiUtil.dp(c, 1));
        text.setColor(c.getColor(R.color.text_secondary));
        text.setTextSize(UiUtil.dp(c, 11));
    }

    public void setPoints(List<ChartViews.Point> p) {
        points = p == null ? new ArrayList<>() : p;
        if (Motion.reduced(getContext())) {
            progress = 1f;
            invalidate();
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(700);
        a.addUpdateListener(x -> {
            progress = (float) x.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;
        float padL = UiUtil.dp(getContext(), 40);
        float padR = UiUtil.dp(getContext(), 12);
        float padT = UiUtil.dp(getContext(), 12);
        float padB = UiUtil.dp(getContext(), 26);
        float w = getWidth() - padL - padR;
        float h = getHeight() - padT - padB;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (ChartViews.Point p : points) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
        }
        if (maxY - minY < 1f) {
            maxY += 1f;
            minY -= 1f;
        }
        float span = (maxY - minY) * 0.15f;
        minY -= span;
        maxY += span;
        for (int i = 0; i <= 3; i++) {
            float y = padT + h * i / 3f;
            canvas.drawLine(padL, y, padL + w, y, grid);
            float val = maxY - (maxY - minY) * i / 3f;
            canvas.drawText(Fmt.number(val, 1), UiUtil.dp(getContext(), 2), y + UiUtil.dp(getContext(), 4), text);
        }
        Path path = new Path();
        Path area = new Path();
        int n = points.size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            ChartViews.Point p = points.get(i);
            xs[i] = n == 1 || maxX == minX ? padL + w / 2f : padL + w * (p.x - minX) / (maxX - minX);
            ys[i] = padT + h * (1f - (p.y - minY) / (maxY - minY));
        }
        float limit = padL + w * progress;
        boolean started = false;
        for (int i = 0; i < n; i++) {
            if (xs[i] > limit && i > 0) break;
            if (!started) {
                path.moveTo(xs[i], ys[i]);
                area.moveTo(xs[i], padT + h);
                area.lineTo(xs[i], ys[i]);
                started = true;
            } else {
                path.lineTo(xs[i], ys[i]);
                area.lineTo(xs[i], ys[i]);
            }
        }
        if (started) {
            float lastX = xs[0];
            for (int i = 0; i < n; i++) if (xs[i] <= limit || i == 0) lastX = xs[i];
            area.lineTo(lastX, padT + h);
            area.close();
            canvas.drawPath(area, fill);
            canvas.drawPath(path, line);
            for (int i = 0; i < n; i++) {
                if (xs[i] <= limit || i == 0) canvas.drawCircle(xs[i], ys[i], UiUtil.dp(getContext(), 4), dot);
            }
        }
        canvas.drawText(points.get(0).label, padL, getHeight() - UiUtil.dp(getContext(), 6), text);
        String last = points.get(n - 1).label;
        if (n > 1) {
            canvas.drawText(last, padL + w - text.measureText(last), getHeight() - UiUtil.dp(getContext(), 6), text);
        }
    }
}
