package com.example.gymmanagement.util;

/** BMI = cân nặng (kg) / [chiều cao (m)]². Chỉ mang tính tham khảo, không phải chẩn đoán y khoa. */
public final class Bmi {
    private Bmi() {
    }

    public static double calc(double weightKg, double heightCm) {
        double m = heightCm / 100.0;
        return Math.round(weightKg / (m * m) * 100.0) / 100.0;
    }

    public static String label(double bmi) {
        if (bmi < 18.5) return "Thấp hơn mức tham khảo";
        if (bmi < 23) return "Trong mức tham khảo";
        if (bmi < 25) return "Hơi cao so với mức tham khảo";
        return "Cao hơn mức tham khảo";
    }

    public static boolean validWeight(double kg) {
        return kg >= 20 && kg <= 350;
    }

    public static boolean validHeight(double cm) {
        return cm >= 80 && cm <= 250;
    }

    public static boolean validBodyFat(double p) {
        return p >= 2 && p <= 70;
    }
}
