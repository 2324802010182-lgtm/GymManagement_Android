package com.example.gymmanagement.util;

import android.content.Context;

import com.example.gymmanagement.GymApp;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/** Truy cập Firebase tập trung; kiểm tra Firebase đã được cấu hình bằng google-services.json thật hay chưa. */
public final class Fb {
    private Fb() {
    }

    public static boolean isConfigured(Context ctx) {
        try {
            return !FirebaseApp.getApps(ctx).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static String uid() {
        FirebaseUser u = auth().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    /** Trả về lỗi nếu chưa cấu hình Firebase hoặc mất mạng; null nếu ổn. */
    public static AppError precheck() {
        Context c = GymApp.app();
        if (!isConfigured(c)) return AppError.notConfigured();
        if (!NetworkUtil.isOnline(c)) return AppError.network();
        return null;
    }
}
