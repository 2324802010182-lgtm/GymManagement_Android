package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.SheetMeasurementBinding;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.util.Bmi;
import com.example.gymmanagement.util.Fmt;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.viewmodel.MeasurementViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Date;

/** Thêm/sửa chỉ số cơ thể; BMI tính tự động, kiểm tra miền giá trị hợp lệ. */
public class MeasurementSheet extends BottomSheetDialogFragment {
    private static final String ARG_ID = "id", ARG_W = "w", ARG_H = "h", ARG_F = "f", ARG_AT = "at", ARG_N = "n";

    public static MeasurementSheet newInstance(@Nullable Measurement m) {
        MeasurementSheet s = new MeasurementSheet();
        Bundle a = new Bundle();
        if (m != null) {
            a.putString(ARG_ID, m.id);
            a.putDouble(ARG_W, m.weightKg);
            a.putDouble(ARG_H, m.heightCm);
            a.putDouble(ARG_F, m.bodyFatPercent == null ? -1 : m.bodyFatPercent);
            a.putLong(ARG_AT, m.measuredAt == null ? System.currentTimeMillis() : m.measuredAt.getTime());
            a.putString(ARG_N, m.note);
        }
        s.setArguments(a);
        return s;
    }

    private SheetMeasurementBinding b;
    private MeasurementViewModel vm;
    private boolean submitted;
    private long dateMs = System.currentTimeMillis();
    private String id = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = SheetMeasurementBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireParentFragment()).get(MeasurementViewModel.class);
        Bundle a = getArguments() == null ? new Bundle() : getArguments();
        id = a.getString(ARG_ID, "");
        boolean edit = !id.isEmpty();
        b.mfTitle.setText(edit ? R.string.meas_edit : R.string.meas_add);
        if (savedInstanceState != null) dateMs = savedInstanceState.getLong("date", dateMs);
        else if (edit) {
            dateMs = a.getLong(ARG_AT);
            b.mfWeight.setText(Fmt.number(a.getDouble(ARG_W), 1));
            b.mfHeight.setText(Fmt.number(a.getDouble(ARG_H), 0));
            if (a.getDouble(ARG_F) >= 0) b.mfFat.setText(Fmt.number(a.getDouble(ARG_F), 1));
            b.mfNote.setText(a.getString(ARG_N, ""));
        } else {
            // Gợi ý chiều cao từ bản ghi gần nhất (nếu có)
            Resource<java.util.List<Measurement>> cur = vm.list().getValue();
            if (cur != null && cur.status == Resource.Status.SUCCESS && !cur.data.isEmpty())
                b.mfHeight.setText(Fmt.number(cur.data.get(0).heightCm, 0));
        }
        showDate();
        b.mfDate.setOnClickListener(v -> pickDate());
        TextWatcher tw = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            public void onTextChanged(CharSequence s, int st, int bf, int c) { }
            public void afterTextChanged(Editable e) { updateBmi(); }
        };
        b.mfWeight.addTextChangedListener(tw);
        b.mfHeight.addTextChangedListener(tw);
        updateBmi();
        b.mfCancel.setOnClickListener(v -> dismiss());
        b.mfSave.setOnClickListener(v -> save());
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || !submitted) return;
            Resource<String> r = ev.peek();
            boolean loading = r.status == Resource.Status.LOADING;
            b.mfProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.mfSave.setEnabled(!loading);
            if (loading) return;
            if (r.status == Resource.Status.SUCCESS) {
                dismiss();
            } else {
                submitted = false;
                b.mfError.setText(r.error.message);
                b.mfError.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong("date", dateMs);
    }

    private void showDate() {
        b.mfDate.setText(getString(R.string.meas_date_label, Fmt.date(new Date(dateMs))));
    }

    private void pickDate() {
        MaterialDatePicker<Long> p = MaterialDatePicker.Builder.datePicker()
                .setSelection(dateMs)
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now()).build())
                .build();
        p.addOnPositiveButtonClickListener(sel -> {
            // Giữ giờ hiện tại để không bị coi là tương lai khi chọn hôm nay
            java.util.Calendar picked = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(sel);
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(picked.get(java.util.Calendar.YEAR), picked.get(java.util.Calendar.MONTH),
                    picked.get(java.util.Calendar.DAY_OF_MONTH));
            if (c.getTimeInMillis() > System.currentTimeMillis()) c.setTimeInMillis(System.currentTimeMillis());
            dateMs = c.getTimeInMillis();
            showDate();
        });
        p.show(getChildFragmentManager(), "date");
    }

    private Double num(CharSequence s) {
        try {
            String t = String.valueOf(s).trim().replace(',', '.');
            if (t.isEmpty()) return null;
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private void updateBmi() {
        Double w = num(b.mfWeight.getText());
        Double h = num(b.mfHeight.getText());
        if (w == null || h == null || w.isNaN() || h.isNaN() || !Bmi.validWeight(w) || !Bmi.validHeight(h)) {
            b.mfBmi.setText(R.string.meas_bmi_placeholder);
            return;
        }
        double v = Bmi.calc(w, h);
        b.mfBmi.setText(getString(R.string.meas_bmi_value, Fmt.number(v, 1), Bmi.label(v)));
    }

    private void save() {
        b.mfError.setVisibility(View.GONE);
        b.mfWeightLayout.setError(null);
        b.mfHeightLayout.setError(null);
        b.mfFatLayout.setError(null);
        Double w = num(b.mfWeight.getText());
        Double h = num(b.mfHeight.getText());
        Double f = num(b.mfFat.getText());
        boolean ok = true;
        if (w == null || w.isNaN() || !Bmi.validWeight(w)) {
            b.mfWeightLayout.setError(getString(R.string.error_weight_range));
            ok = false;
        }
        if (h == null || h.isNaN() || !Bmi.validHeight(h)) {
            b.mfHeightLayout.setError(getString(R.string.error_height_range));
            ok = false;
        }
        if (f != null && (f.isNaN() || !Bmi.validBodyFat(f))) {
            b.mfFatLayout.setError(getString(R.string.error_fat_range));
            ok = false;
        }
        if (String.valueOf(b.mfNote.getText()).trim().length() > 300) {
            b.mfNoteLayout.setError(getString(R.string.error_too_long));
            ok = false;
        }
        if (!ok) return;
        submitted = true;
        vm.save(id, w, h, f, new Date(Math.min(dateMs, System.currentTimeMillis())), String.valueOf(b.mfNote.getText()));
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
