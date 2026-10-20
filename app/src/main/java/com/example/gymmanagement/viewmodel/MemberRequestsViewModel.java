package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Membership;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Resource;

import java.util.List;

/** "Gym của tôi": membership thật và các yêu cầu đăng ký của hội viên. */
public class MemberRequestsViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<List<Membership>>> memberships = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MembershipRequest>>> requests = new MutableLiveData<>();

    public LiveData<Resource<List<Membership>>> memberships() {
        return memberships;
    }

    public LiveData<Resource<List<MembershipRequest>>> requests() {
        return requests;
    }

    public void load() {
        memberships.setValue(Resource.<List<Membership>>loading());
        requests.setValue(Resource.<List<MembershipRequest>>loading());
        Repos.memberships().listMyMemberships(new Callback<List<Membership>>() {
            @Override
            public void onSuccess(List<Membership> data) {
                memberships.setValue(Resource.ofList(data));
                Repos.notifications().ensureExpiringReminders(Session.user, data);
            }

            @Override
            public void onError(com.example.gymmanagement.util.AppError error) {
                memberships.setValue(Resource.<List<Membership>>error(error));
            }
        });
        Repos.memberships().listMyRequests(intoList(requests));
    }

    public void cancel(MembershipRequest r) {
        runAction("Đã hủy yêu cầu đăng ký.", c -> Repos.memberships().cancelRequest(r, c));
    }
}
