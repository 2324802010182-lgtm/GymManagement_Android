package com.example.gymmanagement.ui.adapters;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemGalleryBinding;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.ImageLoader;

public class GalleryAdapter extends SimpleAdapter<String, ItemGalleryBinding> {
    public GalleryAdapter() {
        super(ItemGalleryBinding::inflate,
                (b, url, pos) -> ImageLoader.load(b.galleryImage, url, R.drawable.bg_gym_placeholder));
    }
}
