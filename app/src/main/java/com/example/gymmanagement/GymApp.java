package com.example.gymmanagement;

import android.app.Application;

import com.example.gymmanagement.util.Fb;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

public class GymApp extends Application {
    private static GymApp instance;

    public static GymApp app() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (Fb.isConfigured(this)) {
            try {
                // Bộ nhớ đệm trong RAM (không lưu cục bộ): dữ liệu luôn lấy từ Firestore thật.
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .build();
                FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            } catch (IllegalStateException ignored) {
                // Firestore đã được khởi tạo trước đó
            }
        }
    }
}
