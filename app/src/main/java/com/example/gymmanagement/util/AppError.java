package com.example.gymmanagement.util;

/** Lỗi nghiệp vụ đã được chuyển sang tiếng Việt, kèm loại lỗi để UI chọn trạng thái hiển thị. */
public class AppError {
    public enum Kind { NETWORK, PERMISSION, NOT_FOUND, INVALID, AUTH, CONFLICT, NOT_CONFIGURED, UNKNOWN }

    public final Kind kind;
    public final String message;

    public AppError(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static AppError network() {
        return new AppError(Kind.NETWORK, "Không có kết nối mạng hoặc máy chủ không phản hồi. Hãy kiểm tra kết nối rồi thử lại.");
    }

    public static AppError permission() {
        return new AppError(Kind.PERMISSION, "Bạn không có quyền thực hiện thao tác này hoặc dữ liệu bị Security Rules từ chối.");
    }

    public static AppError invalid(String msg) {
        return new AppError(Kind.INVALID, msg);
    }

    public static AppError notFound(String msg) {
        return new AppError(Kind.NOT_FOUND, msg);
    }

    public static AppError notConfigured() {
        return new AppError(Kind.NOT_CONFIGURED,
                "Firebase chưa được cấu hình. Hãy đặt file google-services.json thật vào app/ rồi build lại (xem FIREBASE_SETUP_VI.md).");
    }

    public static AppError unknown(String msg) {
        return new AppError(Kind.UNKNOWN, msg);
    }
}
