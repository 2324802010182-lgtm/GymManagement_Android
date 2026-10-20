package com.example.gymmanagement.ui.adapters;

import android.content.Context;
import android.view.View;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.ItemMeasurementBinding;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.ui.components.SimpleAdapter;
import com.example.gymmanagement.util.Bmi;
import com.example.gymmanagement.util.Fmt;

import java.util.function.Consumer;

public class MeasurementAdapter extends SimpleAdapter<Measurement, ItemMeasurementBinding> {
    public MeasurementAdapter(Consumer<Measurement> onEdit, Consumer<Measurement> onDelete) {
        super(ItemMeasurementBinding::inflate, (b, m, pos) -> {
            Context c = b.getRoot().getContext();
            b.meDate.setText(Fmt.date(m.measuredAt));
            b.meWeight.setText(c.getString(R.string.ms_weight_kg, Fmt.number(m.weightKg, 1)));
            String fat = m.bodyFatPercent == null ? "" : c.getString(R.string.ms_fat_part, Fmt.number(m.bodyFatPercent, 1));
            b.meDetail.setText(c.getString(R.string.ms_detail_line, Fmt.number(m.heightCm, 0),
                    Fmt.number(m.bmi, 1), Bmi.label(m.bmi)) + fat);
            b.meNote.setVisibility(m.note == null || m.note.isEmpty() ? View.GONE : View.VISIBLE);
            b.meNote.setText(m.note);
            b.meEdit.setOnClickListener(v -> onEdit.accept(m));
            b.meDelete.setOnClickListener(v -> onDelete.accept(m));
        });
    }
}
