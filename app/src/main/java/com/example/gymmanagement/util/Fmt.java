package com.example.gymmanagement.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Định dạng tiền VNĐ và ngày giờ tiếng Việt. */
public final class Fmt {
    private static final Locale VI = new Locale("vi", "VN");
    public static final TimeZone VN_TZ = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private Fmt() {
    }

    public static String money(long vnd) {
        return NumberFormat.getInstance(VI).format(vnd) + " đ";
    }

    public static String number(double v, int digits) {
        NumberFormat nf = NumberFormat.getInstance(VI);
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(0);
        return nf.format(v);
    }

    public static String date(Date d) {
        if (d == null) return "—";
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(d);
    }

    public static String time(Date d) {
        if (d == null) return "—";
        return new SimpleDateFormat("HH:mm", VI).format(d);
    }

    public static String dateTime(Date d) {
        if (d == null) return "—";
        return new SimpleDateFormat("HH:mm dd/MM/yyyy", VI).format(d);
    }

    public static String dayLabel(Date d) {
        if (d == null) return "—";
        return new SimpleDateFormat("EEEE, dd/MM", VI).format(d);
    }

    public static String duration(int days) {
        if (days % 365 == 0) return (days / 365) + " năm";
        if (days % 30 == 0) return (days / 30) + " tháng";
        return days + " ngày";
    }

    public static String seconds(long sec) {
        long m = sec / 60;
        long s = sec % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
