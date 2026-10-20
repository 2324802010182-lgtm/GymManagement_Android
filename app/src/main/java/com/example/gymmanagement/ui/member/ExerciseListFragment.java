package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.FragmentExerciseListBinding;
import com.example.gymmanagement.model.Exercise;
import com.example.gymmanagement.ui.adapters.ExerciseAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.ExerciseViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/** F05 – danh sách bài tập: tìm kiếm, lọc nhóm cơ & độ khó. */
public class ExerciseListFragment extends Fragment {
    private FragmentExerciseListBinding b;
    private ExerciseViewModel vm;
    private ExerciseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentExerciseListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(ExerciseViewModel.class);
        adapter = new ExerciseAdapter(this::open);
        b.exList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.exList.setAdapter(adapter);

        fillChips(b.exCategories, Exercise.CATEGORIES, true);
        fillChips(b.exDifficulties, Exercise.DIFFICULTIES, false);

        b.exSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int bf, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                b.exSearchClear.setVisibility(e.length() > 0 ? View.VISIBLE : View.GONE);
                vm.setQuery(e.toString());
            }
        });
        b.exSearch.setOnEditorActionListener((tv, a, ev) -> {
            if (a == EditorInfo.IME_ACTION_SEARCH) {
                UiUtil.hideKeyboard(tv);
                return true;
            }
            return false;
        });
        b.exSearchClear.setOnClickListener(v -> b.exSearch.setText(""));

        vm.list().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            boolean f = vm.isFiltering();
            b.exState.render(r, getString(f ? R.string.ex_empty_filter_title : R.string.ex_empty_title),
                    getString(f ? R.string.ex_empty_filter_msg : R.string.ex_empty_msg), vm::load);
            adapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        if (vm.list().getValue() == null) vm.load();
    }

    private void fillChips(ChipGroup group, String[] labels, boolean category) {
        LayoutInflater inf = LayoutInflater.from(requireContext());
        Chip all = (Chip) inf.inflate(R.layout.item_chip, group, false);
        all.setText(R.string.filter_all);
        all.setId(View.generateViewId());
        group.addView(all);
        all.setChecked(true);
        for (String l : labels) {
            Chip c = (Chip) inf.inflate(R.layout.item_chip, group, false);
            c.setText(l);
            c.setId(View.generateViewId());
            group.addView(c);
        }
        group.setOnCheckedStateChangeListener((g, ids) -> {
            String value = "";
            if (!ids.isEmpty()) {
                Chip sel = g.findViewById(ids.get(0));
                if (sel != null && g.indexOfChild(sel) > 0) value = String.valueOf(sel.getText());
            }
            if (category) vm.setCategory(value);
            else vm.setDifficulty(value);
        });
    }

    private void open(Exercise e) {
        Bundle a = new Bundle();
        a.putSerializable(ExerciseDetailFragment.ARG_EXERCISE, e);
        HostActivity.open(requireContext(), HostActivity.EXERCISE_DETAIL, a);
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
