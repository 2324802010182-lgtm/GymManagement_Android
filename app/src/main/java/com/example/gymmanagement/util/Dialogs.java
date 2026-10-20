package com.example.gymmanagement.util;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;

import com.example.gymmanagement.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Hộp thoại xác nhận / nhập một dòng. */
public final class Dialogs {
    public interface TextResult {
        void onText(String text);
    }

    private Dialogs() {
    }

    public static void confirm(Context c, String title, String message, String positive, Runnable onYes) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .show();
    }

    public static void input(Context c, String title, String hint, boolean multiline, boolean required,
                             String positive, TextResult result) {
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_input, null);
        TextInputLayout til = v.findViewById(R.id.dialog_input_layout);
        TextInputEditText et = v.findViewById(R.id.dialog_input_text);
        til.setHint(hint);
        if (multiline) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            et.setMinLines(3);
        }
        androidx.appcompat.app.AlertDialog dlg = new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setView(v)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positive, null)
                .create();
        dlg.setOnShowListener(d -> dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String t = et.getText() == null ? "" : et.getText().toString().trim();
            if (required && t.isEmpty()) {
                til.setError(c.getString(R.string.error_required));
                return;
            }
            dlg.dismiss();
            result.onText(t);
        }));
        dlg.show();
    }
}
