package com.example.gymmanagement.util;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** Chuyển exception của Firebase sang AppError tiếng Việt. */
public final class ErrorMapper {
    private ErrorMapper() {
    }

    public static AppError from(Throwable t) {
        if (t == null) return AppError.unknown("Đã xảy ra lỗi không xác định.");
        if (t instanceof FirebaseNetworkException) return AppError.network();
        if (t instanceof FirebaseTooManyRequestsException) {
            return new AppError(AppError.Kind.AUTH, "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau ít phút.");
        }
        if (t instanceof FirebaseAuthWeakPasswordException) {
            return new AppError(AppError.Kind.AUTH, "Mật khẩu quá yếu. Hãy dùng ít nhất 6 ký tự.");
        }
        if (t instanceof FirebaseAuthUserCollisionException) {
            return new AppError(AppError.Kind.CONFLICT, "Email này đã được sử dụng cho một tài khoản khác.");
        }
        if (t instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) t).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code)) {
                return new AppError(AppError.Kind.AUTH, "Tài khoản này đã bị vô hiệu hóa.");
            }
            return new AppError(AppError.Kind.AUTH, "Email hoặc mật khẩu không đúng.");
        }
        if (t instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) t).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return new AppError(AppError.Kind.INVALID, "Địa chỉ email không hợp lệ.");
            }
            return new AppError(AppError.Kind.AUTH, "Email hoặc mật khẩu không đúng.");
        }
        if (t instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) t).getErrorCode();
            if ("ERROR_NETWORK_REQUEST_FAILED".equals(code)) return AppError.network();
            return new AppError(AppError.Kind.AUTH, "Lỗi xác thực (" + code + "). Vui lòng thử lại.");
        }
        if (t instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) t).getCode()) {
                case PERMISSION_DENIED:
                case UNAUTHENTICATED:
                    return AppError.permission();
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return AppError.network();
                case NOT_FOUND:
                    return AppError.notFound("Không tìm thấy dữ liệu yêu cầu.");
                case ALREADY_EXISTS:
                    return new AppError(AppError.Kind.CONFLICT, "Dữ liệu đã tồn tại.");
                case FAILED_PRECONDITION:
                    return new AppError(AppError.Kind.UNKNOWN,
                            "Truy vấn cần chỉ mục Firestore. Hãy triển khai firestore.indexes.json (xem FIREBASE_SETUP_VI.md).");
                default:
                    return AppError.unknown("Lỗi Firestore: " + t.getMessage());
            }
        }
        String msg = t.getMessage();
        if (msg != null && (msg.contains("network") || msg.contains("Unable to resolve host"))) {
            return AppError.network();
        }
        return AppError.unknown("Đã xảy ra lỗi: " + (msg == null ? t.getClass().getSimpleName() : msg));
    }
}
