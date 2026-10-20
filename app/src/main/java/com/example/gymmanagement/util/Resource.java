package com.example.gymmanagement.util;

/** Trạng thái dữ liệu của một màn hình: loading / success / empty / error. */
public class Resource<T> {
    public enum Status { LOADING, SUCCESS, EMPTY, ERROR }

    public final Status status;
    public final T data;
    public final AppError error;

    private Resource(Status status, T data, AppError error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> empty() {
        return new Resource<>(Status.EMPTY, null, null);
    }

    public static <T> Resource<T> error(AppError e) {
        return new Resource<>(Status.ERROR, null, e);
    }

    /** Success nếu danh sách có phần tử, ngược lại Empty. */
    public static <E> Resource<java.util.List<E>> ofList(java.util.List<E> list) {
        if (list == null || list.isEmpty()) return empty();
        return success(list);
    }
}
