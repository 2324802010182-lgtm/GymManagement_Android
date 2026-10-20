package com.example.gymmanagement.ui.components;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.example.gymmanagement.util.Motion;

import java.util.ArrayList;
import java.util.List;

/** Adapter dùng chung cho RecyclerView + ViewBinding; thẻ xuất hiện nhẹ khi cuộn lần đầu. */
public class SimpleAdapter<T, B extends ViewBinding> extends RecyclerView.Adapter<SimpleAdapter.VH<B>> {

    public interface Inflater<B> {
        B inflate(LayoutInflater inflater, ViewGroup parent, boolean attach);
    }

    public interface Binder<T, B> {
        void bind(B binding, T item, int position);
    }

    public static class VH<B extends ViewBinding> extends RecyclerView.ViewHolder {
        public final B binding;

        VH(B b) {
            super(b.getRoot());
            binding = b;
        }
    }

    private final Inflater<B> inflater;
    private final Binder<T, B> binder;
    private final List<T> items = new ArrayList<>();
    private int lastAnimated = -1;

    public SimpleAdapter(Inflater<B> inflater, Binder<T, B> binder) {
        this.inflater = inflater;
        this.binder = binder;
    }

    public void setItems(List<T> list) {
        items.clear();
        if (list != null) items.addAll(list);
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    public List<T> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH<B> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH<>(inflater.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH<B> holder, int position) {
        binder.bind(holder.binding, items.get(position), position);
        if (position > lastAnimated) {
            lastAnimated = position;
            Motion.appear(holder.itemView, position);
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
