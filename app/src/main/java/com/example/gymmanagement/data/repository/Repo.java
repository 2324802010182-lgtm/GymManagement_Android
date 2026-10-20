package com.example.gymmanagement.data.repository;

import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tiện ích dùng chung cho các repository. */
final class Repo {
    private Repo() {
    }

    interface Mapper<T> {
        T map(DocumentSnapshot d);
    }

    /** Trả về false (và gọi onError) nếu chưa cấu hình Firebase hoặc mất mạng. */
    static boolean guard(Callback<?> cb) {
        AppError e = Fb.precheck();
        if (e != null) {
            cb.onError(e);
            return false;
        }
        return true;
    }

    /** Kiểm tra độ dài tối đa (khớp Security Rules); báo lỗi qua callback nếu vượt quá. */
    static boolean limit(Callback<?> cb, String value, int max, String label) {
        if (value != null && value.trim().length() > max) {
            cb.onError(AppError.invalid(label + " tối đa " + max + " ký tự."));
            return false;
        }
        return true;
    }

    static <T> List<T> map(QuerySnapshot qs, Mapper<T> m) {
        List<T> out = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) out.add(m.map(d));
        return out;
    }

    static int cmpDesc(Date a, Date b) {
        long x = a == null ? 0 : a.getTime();
        long y = b == null ? 0 : b.getTime();
        return Long.compare(y, x);
    }

    static int cmpAsc(Date a, Date b) {
        return -cmpDesc(a, b);
    }

    /** Thông báo đúng các khóa mà Security Rules yêu cầu (users/{uid}/notifications). */
    static Map<String, Object> notification(String recipientUid, String gymId, String gymName, String title,
                                            String body, String type, String relatedId, String senderUid) {
        Map<String, Object> m = new HashMap<>();
        m.put("recipientUid", recipientUid);
        m.put("gymId", gymId == null ? "" : gymId);
        m.put("gymName", gymName == null ? "" : gymName);
        m.put("title", clip(title, 120));
        m.put("body", clip(body, 600));
        m.put("type", type);
        m.put("relatedId", relatedId == null ? "" : relatedId);
        m.put("isRead", false);
        m.put("senderUid", senderUid);
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }

    static String clip(String s, int max) {
        if (s == null || s.isEmpty()) return "-";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
