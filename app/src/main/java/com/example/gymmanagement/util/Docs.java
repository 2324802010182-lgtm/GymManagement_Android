package com.example.gymmanagement.util;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Đọc trường từ DocumentSnapshot an toàn (không ném lỗi khi thiếu/sai kiểu). */
public final class Docs {
    private Docs() {
    }

    public static String str(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof String ? (String) o : "";
    }

    public static String strOrNull(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof String ? (String) o : null;
    }

    public static long lng(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    public static Double dblOrNull(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof Number ? ((Number) o).doubleValue() : null;
    }

    public static double dbl(DocumentSnapshot s, String f) {
        Double d = dblOrNull(s, f);
        return d == null ? 0d : d;
    }

    public static boolean bool(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof Boolean && (Boolean) o;
    }

    public static Date date(DocumentSnapshot s, String f) {
        Object o = s.get(f);
        return o instanceof Timestamp ? ((Timestamp) o).toDate() : null;
    }

    public static List<String> strList(DocumentSnapshot s, String f) {
        List<String> out = new ArrayList<>();
        Object o = s.get(f);
        if (o instanceof List) {
            for (Object e : (List<?>) o) {
                if (e instanceof String) out.add((String) e);
            }
        }
        return out;
    }
}
