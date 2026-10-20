package com.example.gymmanagement.util;

/** Kết quả bất đồng bộ của repository: luôn gọi đúng một trong hai hàm, trên main thread. */
public interface Callback<T> {
    void onSuccess(T data);

    void onError(AppError error);
}
