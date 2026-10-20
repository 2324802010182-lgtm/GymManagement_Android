package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.ArrayList;
import java.util.List;

/** Duyệt / từ chối yêu cầu đăng ký (Chủ Gym). */
public class RequestsViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<MembershipRequest>>> list = new MutableLiveData<>();
    private List<MembershipRequest> all = new ArrayList<>();
    private String filter = MembershipRequest.PENDING; // "" = tất cả

    public LiveData<Resource<List<MembershipRequest>>> list() {
        return list;
    }

    public String filter() {
        return filter;
    }

    public void setFilter(String f) {
        filter = f;
        publish();
    }

    public void load() {
        String gid = Session.gymId();
        if (gid == null) {
            list.setValue(Resource.<List<MembershipRequest>>empty());
            return;
        }
        list.setValue(Resource.<List<MembershipRequest>>loading());
        Repos.memberships().listGymRequests(gid, new Callback<List<MembershipRequest>>() {
            @Override
            public void onSuccess(List<MembershipRequest> data) {
                all = data;
                publish();
            }

            @Override
            public void onError(AppError e) {
                list.setValue(Resource.<List<MembershipRequest>>error(e));
            }
        });
    }

    private void publish() {
        List<MembershipRequest> out = new ArrayList<>();
        for (MembershipRequest r : all) if (filter.isEmpty() || filter.equals(r.status)) out.add(r);
        list.setValue(Resource.ofList(out));
    }

    public void approve(MembershipRequest r) {
        runActionWith("Đã duyệt yêu cầu và tạo membership.",
                (Callback<Membership> c) -> Repos.memberships().approve(r, Session.user, c));
    }

    public void reject(MembershipRequest r, String reason) {
        runAction("Đã từ chối yêu cầu.", c -> Repos.memberships().reject(r, Session.user, reason, c));
    }
}
