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
import com.example.gymmanagement.databinding.FragmentMyGymsBinding;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.ui.adapters.MemberRequestAdapter;
import com.example.gymmanagement.ui.adapters.MembershipAdapter;
import com.example.gymmanagement.ui.shared.HostActivity;
import com.example.gymmanagement.util.Dialogs;
import com.example.gymmanagement.util.Resource;
import com.example.gymmanagement.util.UiUtil;
import com.example.gymmanagement.viewmodel.MemberRequestsViewModel;

/** "Gym của tôi": membership thực tế và các yêu cầu đăng ký (pending/approved/rejected). */
public class MyGymsFragment extends Fragment {
    private FragmentMyGymsBinding b;
    private MemberRequestsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        b = FragmentMyGymsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setTitle(R.string.mygyms_title);
        vm = new ViewModelProvider(this).get(MemberRequestsViewModel.class);

        MembershipAdapter msAdapter = new MembershipAdapter(m -> {
            Bundle a = new Bundle();
            a.putString(GymDetailFragment.ARG_GYM_ID, m.gymId);
            a.putString(GymDetailFragment.ARG_GYM_NAME, m.gymName);
            HostActivity.open(requireContext(), HostActivity.GYM_DETAIL, a);
        });
        MemberRequestAdapter rqAdapter = new MemberRequestAdapter(r -> Dialogs.confirm(requireContext(),
                getString(R.string.action_cancel_request), getString(R.string.cancel_request_confirm),
                getString(R.string.action_cancel_request), () -> vm.cancel(r)));
        b.mygymsMsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mygymsMsList.setAdapter(msAdapter);
        b.mygymsRqList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.mygymsRqList.setAdapter(rqAdapter);
        b.mygymsRefresh.setColorSchemeResources(R.color.primary);
        b.mygymsRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card);
        b.mygymsRefresh.setOnRefreshListener(vm::load);

        vm.memberships().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.mygymsRefresh.setRefreshing(false);
            b.mygymsMsState.render(r, getString(R.string.mygyms_ms_empty_title), getString(R.string.mygyms_ms_empty_msg), vm::load);
            msAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.requests().observe(getViewLifecycleOwner(), r -> {
            if (r == null) return;
            b.mygymsRqState.render(r, getString(R.string.mygyms_rq_empty_title), getString(R.string.mygyms_rq_empty_msg), vm::load);
            rqAdapter.setItems(r.status == Resource.Status.SUCCESS ? r.data : null);
        });
        vm.action.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null || ev.peek().status == Resource.Status.LOADING || ev.getIfNotHandled() == null) return;
            Resource<String> r = ev.peek();
            UiUtil.snack(b.getRoot(), r.status == Resource.Status.SUCCESS ? r.data : r.error.message);
            if (r.status == Resource.Status.SUCCESS) vm.load();
        });
        if (vm.memberships().getValue() == null) vm.load();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
