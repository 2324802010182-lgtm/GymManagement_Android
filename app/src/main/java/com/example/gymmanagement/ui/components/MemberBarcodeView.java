package com.example.gymmanagement.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Decorative membership barcode derived from the member's actual account ID. */
public class MemberBarcodeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String code = "";

    public MemberBarcodeView(Context context) {
        super(context);
    }

    public MemberBarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MemberBarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCode(String value) {
        code = value == null ? "" : value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        paint.setColor(0xFF111111);
        paint.setStyle(Paint.Style.FILL);
        String source = code.isEmpty() ? "GYM" : code;
        int modules = 18;
        for (int i = 0; i < source.length(); i++) {
            int value = source.charAt(i);
            for (int bit = 0; bit < 5; bit++) modules += 2 + ((value >> bit) & 1);
        }
        float unit = getWidth() / (float) modules;
        float x = 0f;
        float top = getHeight() * 0.05f;
        float bottom = getHeight() * 0.98f;
        x += 2 * unit;
        for (int i = 0; i < 3; i++) {
            canvas.drawRect(x, top, x + unit, bottom, paint);
            x += 2 * unit;
        }
        for (int i = 0; i < source.length(); i++) {
            int value = source.charAt(i);
            for (int bit = 0; bit < 5; bit++) {
                int width = 1 + ((value >> bit) & 1);
                canvas.drawRect(x, top, x + width * unit, bottom, paint);
                x += (width + 1) * unit;
            }
        }
        for (int i = 0; i < 3; i++) {
            canvas.drawRect(x, top, x + unit, bottom, paint);
            x += 2 * unit;
        }
    }
}
