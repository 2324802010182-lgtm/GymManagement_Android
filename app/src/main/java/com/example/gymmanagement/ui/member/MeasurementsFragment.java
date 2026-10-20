package com.example.gymmanagement.ui.member;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.gymmanagement.R;
import com.example.gymmanagement.databinding.FragmentMeasurementsBinding;
import com.example.gymmanagement.model.Measurement;
import com.example.gymmanagement.ui.adapters.MeasurementAdapter;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.MeasurementViewModel;

/** F06 – danh sách chỉ số cơ thể + thêm/sửa/xóa. */
public class MeasurementsFragment extends Fragment {
    private FragmentMeasurementsBinding b;
    private MeasurementViewModel vm;

    public MeasurementViewModel viewModel() {
        return vm;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentMeasurementsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.meas_title);
        vm = new ViewModelProvider(this).get(MeasurementViewModel.class);
        MeasurementAdapter adapter = new MeasurementAdapter(
                m -> MeasurementSheet.newInstance(m).show(getChildFragmentManager(), "meas"),
                m -> Dialogs.confirm(requireContext(), getString(R.string.meas_delete_title),
                        getString(R.string.meas_delete_msg), getString(R.string.action_delete), () -> vm.delete(m)));
        b.measList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.measList.setAdapter(adapter);
        b.measAdd.setOnClickListener(v -> MeasurementSheet.newInstance(null).show(getChildFragmentManager(), "meas"));
        vm.list().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.measState.render(r, getString(R.string.meas_empty_title), getString(R.string.meas_empty_msg), vm::load);
            adapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING) return;
            Resource<String> r = ev.peek();
            boolean sheetOpen = getChildFragmentManager().findFragmentByTag("meas") != null;
            if (ev.getIfNotHandled() == null) return;
            if (r.status == Resource.Status.SUCCESS) {
                UiUtil.snack(b.getRoot(), r.data);
                vm.load();
            } else if (!sheetOpen) {
                UiUtil.snack(b.getRoot(), r.error.message);
            }
        });
        if (vm.list().getValue() == null) vm.load();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
