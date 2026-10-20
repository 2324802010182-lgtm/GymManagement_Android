package com.example.gymmanagement.model;

import com.example.gymmanagement.util.Docs;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.Map;

/** users/{uid} – hồ sơ người dùng. */
public class User {
    public static final String ROLE_MEMBER = "member";
    public static final String ROLE_OWNER = "gym_owner";
    public static final String ROLE_ADMIN = "platform_admin";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_SUSPENDED = "suspended";

    public String uid = "";
    public String fullName = "";
    public String email = "";
    public String phone = "";
    public String role = ROLE_MEMBER;
    public String accountStatus = STATUS_ACTIVE;
    public String applicationNote = "";
    public String proposedGymName = "";
    public Double goalTargetWeightKg;
    public Integer goalWeeklyWorkouts;
    public String goalNote = "";
    public Date createdAt;

    public static User from(DocumentSnapshot s) {
        User u = new User();
        u.uid = s.getId();
        u.fullName = Docs.str(s, "fullName");
        u.email = Docs.str(s, "email");
        u.phone = Docs.str(s, "phone");
        u.role = Docs.str(s, "role");
        u.accountStatus = Docs.str(s, "accountStatus");
        u.applicationNote = Docs.str(s, "applicationNote");
        u.proposedGymName = Docs.str(s, "proposedGymName");
        u.createdAt = Docs.date(s, "createdAt");
        Object g = s.get("goal");
        if (g instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) g;
            Object w = m.get("targetWeightKg");
            Object t = m.get("weeklyWorkoutTarget");
            Object n = m.get("note");
            u.goalTargetWeightKg = w instanceof Number ? ((Number) w).doubleValue() : null;
            u.goalWeeklyWorkouts = t instanceof Number ? ((Number) t).intValue() : null;
            u.goalNote = n instanceof String ? (String) n : "";
        }
        return u;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(accountStatus);
    }

    public boolean isMember() {
        return ROLE_MEMBER.equals(role);
    }

    public boolean isOwner() {
        return ROLE_OWNER.equals(role);
    }

    public String roleLabel() {
        if (ROLE_OWNER.equals(role)) return "Chủ Gym";
        if (ROLE_ADMIN.equals(role)) return "Quản trị nền tảng";
        return "Hội viên";
    }

    public String statusLabel() {
        switch (accountStatus) {
            case STATUS_ACTIVE:
                return "Đang hoạt động";
            case STATUS_PENDING:
                return "Chờ duyệt";
            case STATUS_REJECTED:
                return "Bị từ chối";
            case STATUS_SUSPENDED:
                return "Bị tạm khóa";
            default:
                return accountStatus;
        }
    }
}
