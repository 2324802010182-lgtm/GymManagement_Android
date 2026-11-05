package com.example.gymmanagement.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/** Minh họa bản đồ nhóm cơ hai mặt trước/sau cho F06. */
public class BodyMapView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean back;
    private static final int BASE = 0xFFD8DEE7, RED = 0xFFFF7777, TEAL = 0xFF13B58A;
    public BodyMapView(Context c, @Nullable AttributeSet a) { super(c, a); }
    public void setBack(boolean value) { back = value; invalidate(); }

    private void fill(Canvas c, int color, Path path) { p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawPath(path, p); }
    private Path path(float... xy) {
        Path v = new Path(); v.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) v.lineTo(xy[i], xy[i + 1]);
        v.close(); return v;
    }
    private void oval(Canvas c, int color, float l, float t, float r, float b) {
        p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawOval(l,t,r,b,p);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.save(); c.scale(getWidth()/340f, getHeight()/470f);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF20242B); c.drawRoundRect(new RectF(0,0,340,470),22,22,p);
        // Hình dáng cơ thể nền
        oval(c, BASE, 147,18,193,75); c.drawRoundRect(153,66,187,106,12,12,p);
        fill(c,BASE,path(128,100,212,100,239,125,230,205,216,266,207,320,201,447,177,447,170,331,160,331,153,447,129,447,123,320,114,266,100,205,91,125));
        p.setColor(BASE); c.drawRoundRect(75,115,112,267,18,18,p); c.drawRoundRect(228,115,265,267,18,18,p);
        oval(c,BASE,63,249,101,286); oval(c,BASE,239,249,277,286);
        // Tô các nhóm cơ thay đổi theo mặt trước / sau
        int chest = back ? BASE : RED, backMuscle = back ? RED : BASE;
        int abs = back ? BASE : TEAL, lats = back ? TEAL : BASE;
        oval(c,chest,119,111,159,161); oval(c,chest,181,111,221,161);
        fill(c,abs,path(128,164,158,164,158,219,132,215)); fill(c,abs,path(182,164,212,164,208,215,182,219));
        fill(c,abs,path(134,224,158,224,157,272,138,267)); fill(c,abs,path(182,224,206,224,202,267,183,272));
        fill(c,lats,path(116,166,132,171,127,226,113,213)); fill(c,lats,path(208,171,224,166,227,213,213,226));
        // Vai, tay trước/sau và cẳng tay
        oval(c,backMuscle,94,112,127,151); oval(c,backMuscle,213,112,246,151);
        oval(c,back?TEAL:RED,78,143,111,218); oval(c,back?TEAL:RED,229,143,262,218);
        fill(c,back?RED:TEAL,path(81,216,105,220,91,267,72,259)); fill(c,back?RED:TEAL,path(235,220,259,216,268,259,249,267));
        // Đùi và bắp chân
        oval(c,back?RED:TEAL,124,277,157,374); oval(c,back?RED:TEAL,183,277,216,374);
        oval(c,back?TEAL:RED,127,365,155,423); oval(c,back?TEAL:RED,185,365,213,423);
        oval(c,BASE,133,420,151,451); oval(c,BASE,189,420,207,451);
        // Viền/đường phân tách cơ để tăng độ rõ
        p.setColor(0xFF20242B); p.setStrokeWidth(3); p.setStyle(Paint.Style.STROKE);
        c.drawLine(170,164,170,270,p); c.drawLine(170,278,170,328,p);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFE7EBF0); p.setTextSize(13); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(back?"MẶT SAU":"MẶT TRƯỚC",170,462,p);
        c.restore();
    }
}
