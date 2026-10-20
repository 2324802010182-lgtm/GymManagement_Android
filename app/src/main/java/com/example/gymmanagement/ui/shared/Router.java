package com.example.gymmanagement.ui.shared;

import android.content.Context;
import android.content.Intent;

import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.User;
import com.example.gymmanagement.ui.admin.AdminMainActivity;
import com.example.gymmanagement.ui.auth.PendingApprovalActivity;
import com.example.gymmanagement.ui.member.MemberMainActivity;
import com.example.gymmanagement.ui.owner.OwnerMainActivity;

/** Điều hướng theo vai trò và trạng thái tài khoản thực tế trong Firestore. */
public final class Router {
    private Router() {
    }

    /** Intent trang chính; null nếu tài khoản không được phép vào ứng dụng (bị từ chối / khóa / vai trò lạ). */
    public static Intent destination(Context c, User u) {
        Intent i;
        if (Session.platformAdmin) {
            i = new Intent(c, AdminMainActivity.class);
        } else if (User.STATUS_PENDING.equals(u.accountStatus)) {
            i = new Intent(c, PendingApprovalActivity.class);
        } else if (!u.isActive()) {
            return null;
        } else if (u.isOwner()) {
            i = new Intent(c, OwnerMainActivity.class);
        } else if (u.isMember()) {
            i = new Intent(c, MemberMainActivity.class);
        } else {
            return null;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }

    /** Thông báo cho trường hợp destination() trả null. */
    public static String deniedMessage(User u) {
        if (User.STATUS_REJECTED.equals(u.accountStatus)) {
            return "Tài khoản của bạn đã bị từ chối. Vui lòng liên hệ quản trị viên.";
        }
        if (User.STATUS_SUSPENDED.equals(u.accountStatus)) {
            return "Tài khoản của bạn đang bị tạm khóa.";
        }
        return "Tài khoản không đủ quyền truy cập ứng dụng.";
    }
}
