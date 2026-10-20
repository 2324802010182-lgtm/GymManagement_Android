package com.example.gymmanagement.util;

import java.util.regex.Pattern;

/** Kiểm tra đầu vào phía client (Security Rules vẫn là lớp bảo đảm cuối cùng). */
public final class Validators {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{9,14}$");

    private Validators() {
    }

    public static boolean email(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    public static boolean phone(String s) {
        return s != null && PHONE.matcher(s.trim()).matches();
    }

    public static boolean name(String s) {
        return s != null && s.trim().length() >= 2 && s.trim().length() <= 80;
    }

    public static boolean password(String s) {
        return s != null && s.length() >= 6 && s.length() <= 64;
    }

    /** Tên đã làm sạch: bỏ khoảng trắng thừa, không có ký tự '/'. */
    public static String cleanName(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    public static boolean httpsOrEmpty(String s) {
        return s == null || s.isEmpty() || (s.startsWith("https://") && s.length() <= 500 && s.length() > 10);
    }
}
