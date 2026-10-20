package com.example.gymmanagement.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;

/** Nạp ảnh từ URL HTTPS; không có URL thì dùng ảnh minh họa cục bộ. */
public final class ImageLoader {
    private ImageLoader() {
    }

    public static void load(ImageView view, String url, int placeholderRes) {
        if (url == null || !url.startsWith("https://")) {
            Glide.with(view).clear(view);
            view.setImageResource(placeholderRes);
            return;
        }
        Glide.with(view).load(url).placeholder(placeholderRes).error(placeholderRes).centerCrop().into(view);
    }
}
