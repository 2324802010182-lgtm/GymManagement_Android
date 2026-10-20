package com.example.gymmanagement.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.ui.auth.SplashActivity;

/** Activity nền: edge-to-edge và bảo vệ phiên (nếu tiến trình bị hủy thì quay về Splash để nạp lại hồ sơ). */
public abstract class BaseActivity extends AppCompatActivity {

    protected boolean requiresSession() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        if (requiresSession() && !Session.isReady()) {
            startActivity(new Intent(this, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }
}
