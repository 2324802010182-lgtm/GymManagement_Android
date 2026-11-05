package com.example.gymmanagement.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.List;
import com.example.gymmanagement.model.Measurement;

/** Biểu đồ cân nặng theo các lần đo gần nhất. */
public class MeasurementChartView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Measurement> data;
    public MeasurementChartView(Context c, @Nullable AttributeSet a) { super(c,a); }
    public void setData(List<Measurement> values) { data=values; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); float w=getWidth(), h=getHeight();
        p.setColor(0xFF22252B); p.setStyle(Paint.Style.FILL); c.drawRoundRect(0,0,w,h,24,24,p);
        p.setColor(0xFF555B65); p.setStrokeWidth(1.5f); p.setStyle(Paint.Style.STROKE);
        for(int i=1;i<=3;i++) c.drawLine(20,h*i/4,w-20,h*i/4,p);
        if(data==null||data.size()<2){ p.setStyle(Paint.Style.FILL); p.setColor(0xFFADB4BE); p.setTextSize(13); c.drawText("Thêm ít nhất 2 lần đo để xem tiến trình",20,h/2,p); return; }
        int n=Math.min(6,data.size()); double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
        for(int i=0;i<n;i++){min=Math.min(min,data.get(n-1-i).weightKg);max=Math.max(max,data.get(n-1-i).weightKg);}
        if(max-min<0.1){max+=0.5;min-=0.5;} float left=24,right=w-24,top=22,bottom=h-24;
        p.setColor(0xFFE9433D);p.setStrokeWidth(3.5f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<n-1;i++){float x1=left+(right-left)*i/(n-1),x2=left+(right-left)*(i+1)/(n-1);float y1=bottom-(float)((data.get(n-1-i).weightKg-min)/(max-min))*(bottom-top);float y2=bottom-(float)((data.get(n-2-i).weightKg-min)/(max-min))*(bottom-top);c.drawLine(x1,y1,x2,y2,p);}
        p.setStyle(Paint.Style.FILL); for(int i=0;i<n;i++){float x=left+(right-left)*i/(n-1);float y=bottom-(float)((data.get(n-1-i).weightKg-min)/(max-min))*(bottom-top);c.drawCircle(x,y,4.5f,p);}
        // Series tỷ lệ mỡ dùng trục chuẩn hóa riêng để cùng thể hiện xu hướng.
        double fmin=Double.MAX_VALUE,fmax=-Double.MAX_VALUE; int fn=0;
        for(int i=0;i<n;i++){Double f=data.get(n-1-i).bodyFatPercent;if(f!=null){fmin=Math.min(fmin,f);fmax=Math.max(fmax,f);fn++;}}
        if(fn>1){if(fmax-fmin<0.1){fmax+=0.5;fmin-=0.5;}p.setColor(0xFF9AA0A8);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);float lastX=0,lastY=0;boolean has=false;for(int i=0;i<n;i++){Double f=data.get(n-1-i).bodyFatPercent;if(f==null){has=false;continue;}float x=left+(right-left)*i/(n-1);float y=bottom-(float)((f-fmin)/(fmax-fmin))*(bottom-top);if(has)c.drawLine(lastX,lastY,x,y,p);lastX=x;lastY=y;has=true;}p.setStyle(Paint.Style.FILL);p.setColor(0xFF9AA0A8);for(int i=0;i<n;i++){Double f=data.get(n-1-i).bodyFatPercent;if(f!=null){float x=left+(right-left)*i/(n-1);float y=bottom-(float)((f-fmin)/(fmax-fmin))*(bottom-top);c.drawCircle(x,y,4,p);}}}
    }
}
