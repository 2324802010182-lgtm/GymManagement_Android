package com.example.gymmanagement.ui.components;

/** Dữ liệu điểm cho biểu đồ tự vẽ. */
public final class ChartViews {
    private ChartViews() {
    }

    public static class Point {
        public final float x; // thời gian (ms) hoặc chỉ số
        public final float y;
        public final String label;

        public Point(float x, float y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }
    }
}
