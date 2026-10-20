package com.example.gymmanagement.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Mã ngày yyyyMMdd theo giờ Việt Nam – phải khớp hàm vnDateKey() trong firestore.rules. */
public final class DateKeys {
    private DateKeys() {
    }

    public static String keyOf(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        f.setTimeZone(Fmt.VN_TZ);
        return f.format(d);
    }

    public static String today() {
        return keyOf(new Date());
    }

    public static Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }
}
