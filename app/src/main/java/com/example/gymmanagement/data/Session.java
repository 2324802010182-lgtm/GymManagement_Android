package com.example.gymmanagement.data;

import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.User;

/** Phiên đăng nhập hiện tại (nạp lại từ Firestore mỗi lần khởi động ứng dụng). */
public final class Session {
    public static User user;
    public static Gym ownerGym;
    public static boolean platformAdmin;

    private Session() {
    }

    public static boolean isReady() {
        return user != null;
    }

    public static String uid() {
        return user == null ? null : user.uid;
    }

    public static String gymId() {
        return ownerGym == null ? null : ownerGym.id;
    }

    public static void clear() {
        user = null;
        ownerGym = null;
        platformAdmin = false;
    }
}
