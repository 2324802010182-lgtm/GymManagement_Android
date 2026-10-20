package com.example.gymmanagement.util;

/** Sự kiện dùng một lần (tránh phát lại thông báo sau khi xoay màn hình). */
public class Event<T> {
    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public T getIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public boolean isHandled() {
        return handled;
    }

    public T peek() {
        return content;
    }
}
