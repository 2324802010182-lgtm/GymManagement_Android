package com.example.gymmanagement.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymmanagement.data.Repos;
import com.example.gymmanagement.data.Session;
import com.example.gymmanagement.model.Gym;
import com.example.gymmanagement.model.GymPackage;
import com.example.gymmanagement.model.MembershipRequest;
import com.example.gymmanagement.util.AppError;
import com.example.gymmanagement.util.Callback;
import com.example.gymmanagement.util.Event;
import com.example.gymmanagement.util.Resource;

import java.util.List;

public class GymDetailViewModel extends BaseViewModel {
    private final MutableLiveData<Resource<Gym>> gym = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<GymPackage>>> packages = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<MembershipRequest>>> myRequests = new MutableLiveData<>();
    private final MutableLiveData<Event<Resource<MembershipRequest>>> submitted = new MutableLiveData<>();
    private String gymId;

    public LiveData<Resource<Gym>> gym() {
        return gym;
    }

    public LiveData<Resource<List<GymPackage>>> packages() {
        return packages;
    }

    public LiveData<Resource<List<MembershipRequest>>> myRequests() {
        return myRequests;
    }

    public LiveData<Event<Resource<MembershipRequest>>> submitted() {
        return submitted;
    }

    public void load(String id) {
        gymId = id;
        gym.setValue(Resource.<Gym>loading());
        packages.setValue(Resource.<List<GymPackage>>loading());
        Repos.gyms().getGym(id, into(gym));
        Repos.packages().listActive(id, intoList(packages));
        loadRequests();
    }

    public void loadRequests() {
        Repos.memberships().listMyRequests(intoList(myRequests));
    }

    /** Yêu cầu đang chờ duyệt của mình tại Gym này (nếu có). */
    public MembershipRequest pendingHere() {
        Resource<List<MembershipRequest>> r = myRequests.getValue();
        if (r == null || r.data == null) return null;
        for (MembershipRequest m : r.data) {
            if (m.gymId.equals(gymId) && m.isPending()) return m;
        }
        return null;
    }

    public void submit(GymPackage pkg, String note) {
        Resource<Gym> g = gym.getValue();
        if (g == null || g.data == null || Session.user == null) return;
        submitted.setValue(new Event<>(Resource.<MembershipRequest>loading()));
        Repos.memberships().submitRequest(g.data, pkg, Session.user, note, new Callback<MembershipRequest>() {
            @Override
            public void onSuccess(MembershipRequest data) {
                submitted.setValue(new Event<>(Resource.success(data)));
                loadRequests();
            }

            @Override
            public void onError(AppError error) {
                submitted.setValue(new Event<>(Resource.<MembershipRequest>error(error)));
            }
        });
    }

    public void cancelPending() {
        MembershipRequest r = pendingHere();
        if (r == null) return;
        runAction("Đã hủy yêu cầu đăng ký.", c -> Repos.memberships().cancelRequest(r, c));
        loadRequestsLater();
    }

    private void loadRequestsLater() {
        action.observeForever(new androidx.lifecycle.Observer<Event<Resource<String>>>() {
            @Override
            public void onChanged(Event<Resource<String>> e) {
                if (e != null && e.peek().status != Resource.Status.LOADING) {
                    action.removeObserver(this);
                    loadRequests();
                }
            }
        });
    }
}
